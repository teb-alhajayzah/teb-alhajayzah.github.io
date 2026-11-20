package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PersistedAttachment(
    val path: String,
    val mimeType: String,
    val displayName: String,
)

object ImageStorageHelper {

    private const val MAX_IMAGE_DIMENSION = 1200
    private const val JPEG_QUALITY = 72
    private const val TARGET_MAX_IMAGE_BYTES = 260 * 1024L

    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val tempFile = File(cacheDir, "cam_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile,
        )
        return uri to tempFile
    }

    fun persistUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return persistDocumentOrImageUri(context, sourceUri)?.path
    }

    fun persistDocumentOrImageUri(context: Context, sourceUri: Uri): PersistedAttachment? {
        return try {
            val resolver = context.contentResolver
            val detectedMime = resolver.getType(sourceUri)?.lowercase().orEmpty()
            val displayName = queryDisplayName(context, sourceUri)
            val isPdf = detectedMime.contains("pdf") || displayName.lowercase().endsWith(".pdf")

            val destDir = File(context.filesDir, "medical_images").apply { mkdirs() }
            if (isPdf) {
                val destFile = File(destDir, "doc_${UUID.randomUUID()}.pdf")
                resolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
                if (!destFile.exists() || destFile.length() == 0L) return null
                PersistedAttachment(
                    path = destFile.absolutePath,
                    mimeType = "application/pdf",
                    displayName = displayName.ifBlank { "تحليل_طبي.pdf" },
                )
            } else {
                val destFile = File(destDir, "med_${UUID.randomUUID()}.jpg")
                val compressed = tryCompressUriToJpeg(context, sourceUri, destFile)
                if (!compressed) {
                    resolver.openInputStream(sourceUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return null
                }
                if (!destFile.exists() || destFile.length() == 0L) return null
                PersistedAttachment(
                    path = destFile.absolutePath,
                    mimeType = "image/jpeg",
                    displayName = displayName.ifBlank { "صورة_طبية.jpg" },
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun persistFileToInternalStorage(context: Context, sourceFile: File): String? {
        return try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) return null
            val destDir = File(context.filesDir, "medical_images").apply { mkdirs() }
            val destFile = File(destDir, "med_${UUID.randomUUID()}.jpg")
            val compressed = tryCompressFileToJpeg(sourceFile, destFile)
            if (!compressed) {
                sourceFile.copyTo(destFile, overwrite = true)
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun isPdfAttachment(path: String, mimeType: String = ""): Boolean {
        return mimeType.equals("application/pdf", ignoreCase = true) ||
            path.lowercase().endsWith(".pdf")
    }

    suspend fun renderPdfPagesToBitmaps(
        filePath: String,
        maxWidthPx: Int = 2200,
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile || file.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("ملف PDF غير موجود أو تالف"))
            }
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pages = pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val pageCount = renderer.pageCount
                    if (pageCount <= 0) {
                        return@withContext Result.failure(IllegalStateException("ملف PDF لا يحتوي على صفحات صالحة"))
                    }
                    val rendered = ArrayList<Bitmap>(pageCount)
                    for (i in 0 until pageCount) {
                        renderer.openPage(i).use { page ->
                            val targetWidth = maxOf(maxWidthPx, page.width * 3).coerceIn(1000, 2600)
                            val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1).toFloat()
                            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            val destRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
                            page.render(bmp, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            rendered += bmp
                        }
                    }
                    rendered
                }
            }
            Result.success(pages)
        } catch (ex: Exception) {
            Result.failure(
                IllegalStateException(
                    ex.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "تعذر عرض صفحات ملف PDF داخل التطبيق",
                ),
            )
        }
    }

    suspend fun saveAttachmentToDownloads(
        context: Context,
        filePath: String,
        mimeType: String,
        displayName: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(IllegalStateException("الملف المرفق غير موجود على الجهاز"))
            }
            val resolvedMime = when {
                mimeType.isNotBlank() -> mimeType
                filePath.lowercase().endsWith(".pdf") -> "application/pdf"
                else -> "image/jpeg"
            }
            val cleanName = sanitizeFileName(
                raw = displayName.ifBlank { file.name },
                fallbackExt = if (resolvedMime == "application/pdf") "pdf" else "jpg",
            )
            file.inputStream().use { input ->
                writeStreamToDownloads(context, input, resolvedMime, cleanName)
            }
            Result.success(cleanName)
        } catch (ex: Exception) {
            Result.failure(
                IllegalStateException(
                    ex.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "تعذر حفظ الملف في مجلد التنزيلات",
                ),
            )
        }
    }

    suspend fun saveBase64ToDownloads(
        context: Context,
        rawDataUrlOrBase64: String,
        mimeType: String,
        displayName: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val commaIdx = rawDataUrlOrBase64.indexOf(',')
            val pureBase64 = if (commaIdx >= 0) rawDataUrlOrBase64.substring(commaIdx + 1) else rawDataUrlOrBase64
            val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("الملف فارغ"))
            }
            val resolvedMime = mimeType.ifBlank {
                if (rawDataUrlOrBase64.startsWith("data:application/pdf", ignoreCase = true)) {
                    "application/pdf"
                } else {
                    "application/octet-stream"
                }
            }
            val cleanName = sanitizeFileName(
                raw = displayName,
                fallbackExt = if (resolvedMime == "application/pdf") "pdf" else "bin",
            )
            bytes.inputStream().use { input ->
                writeStreamToDownloads(context, input, resolvedMime, cleanName)
            }
            Result.success(cleanName)
        } catch (ex: Exception) {
            Result.failure(
                IllegalStateException(ex.localizedMessage ?: "تعذر تنزيل الملف"),
            )
        }
    }

    private fun writeStreamToDownloads(
        context: Context,
        input: java.io.InputStream,
        mimeType: String,
        fileName: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TebAlHajayzah")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف في التنزيلات")
            resolver.openOutputStream(itemUri)?.use { out ->
                input.copyTo(out)
            } ?: throw IllegalStateException("تعذر الكتابة في مجلد التنزيلات")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TebAlHajayzah",
            ).apply { mkdirs() }
            val dest = File(downloadsDir, fileName)
            dest.outputStream().use { out -> input.copyTo(out) }
        }
    }

    private fun sanitizeFileName(raw: String, fallbackExt: String): String {
        val cleaned = raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val base = cleaned.ifBlank { "teb_report_${System.currentTimeMillis()}.$fallbackExt" }
        return if (base.contains('.')) base else "$base.$fallbackExt"
    }

    fun deleteInternalFile(context: Context, filePath: String) {
        if (filePath.isBlank()) return
        try {
            val file = File(filePath)
            val internalDir = File(context.filesDir, "medical_images").canonicalPath
            if (file.exists() && file.canonicalPath.startsWith(internalDir)) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    fun openAttachmentExternally(
        context: Context,
        filePath: String,
        mimeType: String,
        onError: (String) -> Unit,
    ) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                onError("الملف المرفق غير موجود على الجهاز")
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val resolvedMime = when {
                mimeType.isNotBlank() -> mimeType
                filePath.lowercase().endsWith(".pdf") -> "application/pdf"
                else -> "image/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "فتح المرفق الطبي"))
        } catch (_: Exception) {
            onError("لا يوجد تطبيق مثبت على الجهاز لفتح ملفات PDF / الصور")
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx).orEmpty() else ""
                    } else ""
                }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun tryCompressUriToJpeg(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return false
            val sampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return false
            writeScaledJpeg(bitmap, destFile)
        } catch (_: Exception) {
            false
        }
    }

    private fun tryCompressFileToJpeg(sourceFile: File, destFile: File): Boolean {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return false
            val sampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts) ?: return false
            writeScaledJpeg(bitmap, destFile)
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        val maxSide = maxOf(width, height)
        while ((maxSide / inSampleSize) > MAX_IMAGE_DIMENSION * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun writeScaledJpeg(bitmap: Bitmap, destFile: File): Boolean {
        return try {
            val maxSide = maxOf(bitmap.width, bitmap.height)
            val finalBitmap = if (maxSide > MAX_IMAGE_DIMENSION) {
                val scale = MAX_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }
            var quality = JPEG_QUALITY
            do {
                destFile.outputStream().use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                quality -= 8
            } while (destFile.length() > TARGET_MAX_IMAGE_BYTES && quality >= 52)
            if (finalBitmap !== bitmap) finalBitmap.recycle()
            bitmap.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getMedicalStorageUsageMb(context: Context): Double {
        return try {
            val destDir = File(context.filesDir, "medical_images")
            if (!destDir.exists()) return 0.0
            val totalBytes = destDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            totalBytes.toDouble() / (1024.0 * 1024.0)
        } catch (_: Exception) {
            0.0
        }
    }

    fun cleanupOldestAttachments(context: Context, keepLatestCount: Int = 15): Int {
        return try {
            val destDir = File(context.filesDir, "medical_images")
            if (!destDir.exists()) return 0
            val files = destDir.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
            if (files.size <= keepLatestCount) return 0
            var removed = 0
            files.drop(keepLatestCount).forEach { f ->
                if (f.delete()) removed++
            }
            removed
        } catch (_: Exception) {
            0
        }
    }
}
