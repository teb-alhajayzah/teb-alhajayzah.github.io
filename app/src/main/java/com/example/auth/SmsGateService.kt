package com.example.auth

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class SmsSendResult(
    val isSuccess: Boolean,
    val timedOut: Boolean = false,
    val formattedPhone: String = "",
    val endpoint: String = "",
    val errorMessage: String = "",
)

object SmsGateService {
    const val DEFAULT_SERVER_URL = "https://sms-gate.app"
    const val DEFAULT_CLOUD_MESSAGES_ENDPOINT = "https://api.sms-gate.app/3rdparty/v1/messages"
    const val DEFAULT_TIMEOUT_MILLIS = 25_000L

    fun resolveMessagesEndpoint(serverUrl: String): String {
        var clean = serverUrl.trim().trimEnd('/')
        if (clean.isEmpty()) clean = DEFAULT_SERVER_URL
        if (!clean.startsWith("http://", ignoreCase = true) &&
            !clean.startsWith("https://", ignoreCase = true)
        ) {
            clean = "https://$clean"
        }
        val lower = clean.lowercase()
        if (lower.endsWith("/3rdparty/v1/message")) return "${clean}s"
        if (lower.endsWith("/messages") || lower.endsWith("/message")) return clean
        if (lower == "https://sms-gate.app" ||
            lower == "http://sms-gate.app" ||
            lower == "https://www.sms-gate.app" ||
            lower == "http://www.sms-gate.app" ||
            lower == "https://api.sms-gate.app" ||
            lower == "http://api.sms-gate.app"
        ) {
            return DEFAULT_CLOUD_MESSAGES_ENDPOINT
        }
        if (lower.endsWith("/3rdparty/v1")) {
            return "$clean/messages"
        }
        return "$clean/3rdparty/v1/messages"
    }

    fun formatEgyptianE164(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("0020") && digits.length >= 14 -> "+${digits.substring(2)}"
            digits.startsWith("20") && digits.length >= 12 -> "+$digits"
            digits.startsWith("01") && digits.length == 11 -> "+2$digits"
            digits.startsWith("1") && digits.length == 10 -> "+20$digits"
            digits.isNotEmpty() -> "+2$digits"
            else -> ""
        }
    }

    suspend fun sendSms(
        serverUrl: String,
        username: String,
        password: String,
        rawPhone: String,
        messageText: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): SmsSendResult {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            return SmsSendResult(
                isSuccess = false,
                errorMessage = "بيانات بوابة الرسائل (SMSGate) غير مكتملة في لوحة الإدارة",
            )
        }

        val e164Phone = formatEgyptianE164(rawPhone)
        val phoneDigitsCount = e164Phone.count { it.isDigit() }
        if (e164Phone.isEmpty() || phoneDigitsCount < 11) {
            return SmsSendResult(
                isSuccess = false,
                errorMessage = "رقم الهاتف المستلم غير صحيح",
            )
        }

        val endpoint = resolveMessagesEndpoint(serverUrl)
        val safeTimeout = timeoutMillis.coerceAtLeast(1_000L)

        val result = withTimeoutOrNull(safeTimeout) {
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL(endpoint)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = safeTimeout.toInt().coerceAtMost(15_000)
                        readTimeout = safeTimeout.toInt()
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        val basicToken = Base64.encodeToString(
                            "$cleanUser:$cleanPass".toByteArray(Charsets.UTF_8),
                            Base64.NO_WRAP,
                        )
                        setRequestProperty("Authorization", "Basic $basicToken")
                    }

                    val payload = JSONObject().apply {
                        put("message", messageText)
                        put("phoneNumbers", JSONArray().apply { put(e164Phone) })
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }

                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val responseText = if (stream != null) {
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                    } else {
                        ""
                    }

                    if (code in 200..299) {
                        SmsSendResult(
                            isSuccess = true,
                            formattedPhone = e164Phone,
                            endpoint = endpoint,
                        )
                    } else {
                        val shortErr = responseText.take(120).trim()
                        SmsSendResult(
                            isSuccess = false,
                            formattedPhone = e164Phone,
                            endpoint = endpoint,
                            errorMessage = if (shortErr.isNotEmpty()) {
                                "فشل الإرسال من خادم SMSGate (رمز $code): $shortErr"
                            } else {
                                "فشل الإرسال من خادم SMSGate (رمز $code)"
                            },
                        )
                    }
                } catch (_: SocketTimeoutException) {
                    SmsSendResult(
                        isSuccess = false,
                        timedOut = true,
                        formattedPhone = e164Phone,
                        endpoint = endpoint,
                        errorMessage = "تعذر إرسال الرسالة خلال 25 ثانية",
                    )
                } catch (e: Exception) {
                    SmsSendResult(
                        isSuccess = false,
                        formattedPhone = e164Phone,
                        endpoint = endpoint,
                        errorMessage = "تعذر الاتصال بخادم SMSGate: ${e.message ?: "خطأ شبكة"}",
                    )
                } finally {
                    conn?.disconnect()
                }
            }
        }

        return result ?: SmsSendResult(
            isSuccess = false,
            timedOut = true,
            formattedPhone = e164Phone,
            endpoint = endpoint,
            errorMessage = "تعذر إرسال الرسالة خلال 25 ثانية",
        )
    }
}
