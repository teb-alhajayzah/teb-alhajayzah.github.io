package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class PersistedAttachment(
    val path: String,
    val mimeType: String,
    val displayName: String,
)

object ImageStorageHelper {

    private const val MAX_IMAGE_DIMENSION = 1600
    private const val JPEG_QUALITY = 82

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
            destFile.outputStream().use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (finalBitmap !== bitmap) finalBitmap.recycle()
            bitmap.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }
}
