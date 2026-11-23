package com.example.data.remote

import android.util.Base64
import com.example.config.CloudNamespacePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * مساعد رفع الملفات الثقيلة (PDF / صور الأشعة والتحاليل) إلى Google Apps Script Web App
 * الخاص بكل عيادة أو إلى الرابط الاحتياطي العام للمنصة على Google Drive.
 */
object GoogleDriveStorageHelper {

    private const val CONNECT_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 20000
    private const val MAX_DIRECT_UPLOAD_BYTES = 12 * 1024 * 1024

    fun resolveWebAppUrl(
        clinicDriveUrl: String,
        platformFallbackDriveUrl: String,
    ): String {
        val clinic = clinicDriveUrl.trim()
        if (clinic.startsWith("https://") || clinic.startsWith("http://")) {
            return clinic
        }
        val fallback = platformFallbackDriveUrl.trim()
        if (fallback.startsWith("https://") || fallback.startsWith("http://")) {
            return fallback
        }
        return ""
    }

    suspend fun uploadAttachmentToDrive(
        webAppUrl: String,
        localFilePath: String,
        fileName: String,
        mimeType: String,
        clinicId: String,
        clinicName: String,
        patientName: String,
        familyMemberId: String,
        recordId: String,
        recordType: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!FirestoreCloudSyncService.isCloudNetworkAllowed()) {
            return@withContext Result.failure(IllegalStateException("Network skipped in test environment"))
        }
        val cleanUrl = webAppUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("No Drive Web App URL configured"))
        }
        val file = File(localFilePath)
        if (!file.exists() || !file.isFile) {
            return@withContext Result.failure(IllegalStateException("Attachment file missing"))
        }
        if (file.length() > MAX_DIRECT_UPLOAD_BYTES) {
            return@withContext Result.failure(IllegalStateException("File exceeds 12MB direct upload limit"))
        }

        runCatching {
            val bytes = file.readBytes()
            val payload = JSONObject().apply {
                put("action", "UPLOAD_MEDICAL_FILE")
                put("namespace", CloudNamespacePaths.ROOT)
                put("clinicId", clinicId)
                put("clinicName", clinicName)
                put("patientName", patientName)
                put("familyMemberId", familyMemberId)
                put("recordId", recordId)
                put("recordType", recordType)
                put("fileName", fileName.ifBlank { file.name })
                put("mimeType", mimeType.ifBlank { "application/octet-stream" })
                put("base64Data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                put("uploadedAtMillis", System.currentTimeMillis())
            }
            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

            var currentUrl = cleanUrl
            var method = "POST"
            var redirects = 0
            var responseCode = 0
            var responseBody = ""

            while (redirects < 4) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    useCaches = false
                    if (method == "POST") {
                        doOutput = true
                        setRequestProperty("Content-Type", "text/plain;charset=utf-8")
                    }
                }
                if (method == "POST") {
                    conn.outputStream.use { it.write(payloadBytes) }
                }
                responseCode = conn.responseCode
                if (responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location").orEmpty()
                    conn.disconnect()
                    if (location.isBlank()) break
                    currentUrl = location
                    // Google Apps Script often redirects POST -> 302 script.googleusercontent.com GET for JSON result
                    if (responseCode in listOf(301, 302, 303)) {
                        method = "GET"
                    }
                    redirects++
                    continue
                }
                responseBody = runCatching {
                    val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                conn.disconnect()
                break
            }

            if (responseCode !in 200..299) {
                error("Drive WebApp HTTP $responseCode")
            }

            val parsedUrl = runCatching {
                val json = JSONObject(responseBody.trim())
                listOf("fileUrl", "url", "driveUrl", "webViewLink")
                    .firstNotNullOfOrNull { key -> json.optString(key, "").trim().takeIf { it.isNotBlank() } }
            }.getOrNull()

            parsedUrl ?: cleanUrl
        }
    }
}
