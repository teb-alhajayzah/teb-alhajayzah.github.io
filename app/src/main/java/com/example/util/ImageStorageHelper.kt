package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ImageStorageHelper {

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
        return try {
            val destDir = File(context.filesDir, "medical_images").apply { mkdirs() }
            val destFile = File(destDir, "med_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun persistFileToInternalStorage(context: Context, sourceFile: File): String? {
        return try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) return null
            val destDir = File(context.filesDir, "medical_images").apply { mkdirs() }
            val destFile = File(destDir, "med_${UUID.randomUUID()}.jpg")
            sourceFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
