package com.example.data.remote

import android.os.Build
import com.example.config.AppConfig
import com.example.config.CloudNamespacePaths
import com.example.config.PlatformBrandConfig
import com.example.data.local.ActivationOtpCodeEntity
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BlockedPhoneEntity
import com.example.data.local.BookingSource
import com.example.data.local.ChatThreadType
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.DirectMessageEntity
import com.example.data.local.FamilyLinkRequestEntity
import com.example.data.local.FamilyLinkStatus
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalLabEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.MessageSenderRole
import com.example.data.local.NotificationAudience
import com.example.data.local.PatientAccountEntity
import com.example.data.local.PermanentLogEventType
import com.example.data.local.PermanentMedicalLogEntity
import com.example.data.local.PlatformVisitorHitEntity
import com.example.data.local.SlotStatus
import com.example.util.ChronicConditionsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CloudConnectionStatus(
    val isConnected: Boolean = false,
    val hasAttempted: Boolean = false,
    val errorMessage: String = "",
    val lastSuccessMillis: Long = 0L,
)

/**
 * خدمة المزامنة السحابية عبر Firestore REST API تحت مسار العزل الموحد (teb_alhajayzah/)
 * تضمن تطابق البيانات والإعدادات لحظياً بين نسخة الويب (Chrome/WebView) وتطبيق الأندرويد.
 */
object FirestoreCloudSyncService {

    private const val CONNECT_TIMEOUT_MS = 3000
    private const val READ_TIMEOUT_MS = 3000

    private val _cloudStatusFlow = MutableStateFlow(CloudConnectionStatus())
    val cloudStatusFlow: StateFlow<CloudConnectionStatus> = _cloudStatusFlow.asStateFlow()

    private fun markConnected() {
        _cloudStatusFlow.value = CloudConnectionStatus(
            isConnected = true,
            hasAttempted = true,
            errorMessage = "",
            lastSuccessMillis = System.currentTimeMillis(),
        )
    }

    private fun markError(reason: String) {
        _cloudStatusFlow.value = _cloudStatusFlow.value.copy(
            isConnected = false,
            hasAttempted = true,
            errorMessage = reason.ifBlank { "Network Error" }.take(80),
        )
    }

    private fun readHttpErrorDetail(conn: HttpURLConnection, code: Int): String {
        val rawErr = runCatching {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }.getOrDefault("")
        val parsedMsg = runCatching {
            if (rawErr.isNotBlank()) JSONObject(rawErr).optJSONObject("error")?.optString("message", "").orEmpty() else ""
        }.getOrDefault("")
        return if (parsedMsg.isNotBlank()) "HTTP $code ($parsedMsg)" else "HTTP $code"
    }

    fun isCloudNetworkAllowed(): Boolean =
        !"robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    fun safeCloudDocId(rawId: String): String =
        rawId.replace(Regex("[^\\w\\-]"), "_")
            .take(180)
            .ifBlank { "doc_${System.currentTimeMillis()}" }

    fun normalizePhoneDigits(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
            }
        }
        val digits = sb.toString()
        return when {
            digits.startsWith("0020") -> "0${digits.substring(4)}"
            digits.startsWith("20") && digits.length >= 12 -> "0${digits.substring(2)}"
            else -> digits
        }
    }

    fun accountCloudDocId(phone: String, fallbackId: String = ""): String =
        normalizePhoneDigits(phone).ifBlank { fallbackId.trim() }

    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun baseDocumentsUrl(projectId: String): String =
        "https://firestore.googleapis.com/v1/projects/${enc(projectId.trim())}/databases/${enc(CloudNamespacePaths.FIRESTORE_DATABASE_ID)}/documents"

    fun resolveCollectionName(subCollection: String): String = when (subCollection.trim().lowercase()) {
        "accounts", "users", CloudNamespacePaths.USERS -> CloudNamespacePaths.USERS
        "doctors", "clinics", CloudNamespacePaths.CLINICS -> CloudNamespacePaths.CLINICS
        "slots", "appointments", CloudNamespacePaths.APPOINTMENTS -> CloudNamespacePaths.APPOINTMENTS
        "messages", CloudNamespacePaths.MESSAGES -> CloudNamespacePaths.MESSAGES
        "records", CloudNamespacePaths.RECORDS -> CloudNamespacePaths.RECORDS
        "fcm_tokens", CloudNamespacePaths.FCM_TOKENS -> CloudNamespacePaths.FCM_TOKENS
        "settings", "config", CloudNamespacePaths.SETTINGS -> CloudNamespacePaths.SETTINGS
        "family_members", CloudNamespacePaths.FAMILY_MEMBERS -> CloudNamespacePaths.FAMILY_MEMBERS
        "labs", CloudNamespacePaths.LABS -> CloudNamespacePaths.LABS
        "permanent_logs", CloudNamespacePaths.PERMANENT_LOGS -> CloudNamespacePaths.PERMANENT_LOGS
        "notifications", CloudNamespacePaths.NOTIFICATIONS -> CloudNamespacePaths.NOTIFICATIONS
        "blocked_phones", CloudNamespacePaths.BLOCKED_PHONES -> CloudNamespacePaths.BLOCKED_PHONES
        "activation_codes", CloudNamespacePaths.ACTIVATION_CODES -> CloudNamespacePaths.ACTIVATION_CODES
        "family_link_requests", CloudNamespacePaths.FAMILY_LINK_REQUESTS -> CloudNamespacePaths.FAMILY_LINK_REQUESTS
        "visitor_hits", CloudNamespacePaths.VISITOR_HITS -> CloudNamespacePaths.VISITOR_HITS
        else -> {
            val trimmed = subCollection.trim()
            if (trimmed.startsWith("${CloudNamespacePaths.ROOT}_")) trimmed else "${CloudNamespacePaths.ROOT}_$trimmed"
        }
    }

    fun settingsUrl(projectId: String, apiKey: String): String =
        "${baseDocumentsUrl(projectId)}/${CloudNamespacePaths.SETTINGS}/${CloudNamespacePaths.SETTINGS_DOC}?key=${enc(apiKey.trim())}"

    fun collectionUrl(projectId: String, apiKey: String, subCollection: String, pageToken: String? = null): String {
        val resolvedCol = resolveCollectionName(subCollection)
        val base = "${baseDocumentsUrl(projectId)}/${enc(resolvedCol)}?pageSize=500&key=${enc(apiKey.trim())}"
        return if (!pageToken.isNullOrBlank()) "$base&pageToken=${enc(pageToken)}" else base
    }

    fun documentUrl(projectId: String, apiKey: String, subCollection: String, docId: String): String {
        val resolvedCol = resolveCollectionName(subCollection)
        return "${baseDocumentsUrl(projectId)}/${enc(resolvedCol)}/${enc(safeCloudDocId(docId))}?key=${enc(apiKey.trim())}"
    }

    fun toFirestoreFields(obj: JSONObject): JSONObject {
        val fields = JSONObject()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.opt(k)
            val encoded = encodeFirestoreValue(v)
            if (encoded != null) {
                fields.put(k, encoded)
            }
        }
        fields.put("_cloudNamespace", JSONObject().put("stringValue", CloudNamespacePaths.ROOT))
        fields.put("cloudNamespace", JSONObject().put("stringValue", CloudNamespacePaths.ROOT))
        fields.put("updatedAtMillis", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
        return fields
    }

    private fun encodeFirestoreValue(value: Any?): JSONObject? {
        return when (value) {
            null, JSONObject.NULL -> null
            is Boolean -> JSONObject().put("booleanValue", value)
            is Int -> JSONObject().put("integerValue", value.toString())
            is Long -> JSONObject().put("integerValue", value.toString())
            is Float -> JSONObject().put("doubleValue", value.toDouble())
            is Double -> JSONObject().put("doubleValue", value)
            is String -> JSONObject().put("stringValue", value)
            is JSONArray -> {
                val arr = JSONArray()
                for (i in 0 until value.length()) {
                    encodeFirestoreValue(value.opt(i))?.let { arr.put(it) }
                }
                JSONObject().put("arrayValue", JSONObject().put("values", arr))
            }
            is JSONObject -> {
                val sub = JSONObject()
                val ks = value.keys()
                while (ks.hasNext()) {
                    val k = ks.next()
                    encodeFirestoreValue(value.opt(k))?.let { sub.put(k, it) }
                }
                JSONObject().put("mapValue", JSONObject().put("fields", sub))
            }
            else -> JSONObject().put("stringValue", value.toString())
        }
    }

    fun fromFirestoreDocument(docJson: JSONObject): JSONObject {
        val fields = docJson.optJSONObject("fields") ?: return JSONObject()
        val out = fromFirestoreFields(fields)
        val docName = docJson.optString("name", "")
        if (docName.isNotBlank()) {
            val shortId = docName.substringAfterLast('/')
            if (shortId.isNotBlank() && !out.has("id")) {
                out.put("_docId", shortId)
            }
        }
        return out
    }

    private fun fromFirestoreFields(fields: JSONObject): JSONObject {
        val out = JSONObject()
        val keys = fields.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val wrapper = fields.optJSONObject(k) ?: continue
            val decoded = decodeFirestoreValue(wrapper)
            if (decoded != null) {
                out.put(k, decoded)
            }
        }
        return out
    }

    private fun decodeFirestoreValue(wrapper: JSONObject): Any? {
        return when {
            wrapper.has("stringValue") -> wrapper.optString("stringValue", "")
            wrapper.has("booleanValue") -> wrapper.optBoolean("booleanValue", false)
            wrapper.has("integerValue") -> {
                val raw = wrapper.opt("integerValue")
                when (raw) {
                    is Number -> raw.toLong()
                    is String -> raw.toLongOrNull() ?: 0L
                    else -> 0L
                }
            }
            wrapper.has("doubleValue") -> wrapper.optDouble("doubleValue", 0.0)
            wrapper.has("arrayValue") -> {
                val values = wrapper.optJSONObject("arrayValue")?.optJSONArray("values")
                val arr = JSONArray()
                if (values != null) {
                    for (i in 0 until values.length()) {
                        val itemObj = values.optJSONObject(i) ?: continue
                        decodeFirestoreValue(itemObj)?.let { arr.put(it) }
                    }
                }
                arr
            }
            wrapper.has("mapValue") -> {
                val sub = wrapper.optJSONObject("mapValue")?.optJSONObject("fields")
                if (sub != null) fromFirestoreFields(sub) else JSONObject()
            }
            else -> null
        }
    }

    suspend fun fetchSettings(brand: PlatformBrandConfig): JSONObject? = withContext(Dispatchers.IO) {
        if (!isCloudNetworkAllowed() || !brand.isCloudConfigured) return@withContext null
        runCatching {
            val conn = (URL(settingsUrl(brand.firebaseProjectId, brand.firebaseApiKey)).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
            }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
            val errDetail = if (code !in 200..299 && code != 404) readHttpErrorDetail(conn, code) else ""
            conn.disconnect()
            if (code == 404) return@runCatching null
            if (code !in 200..299 || body.isBlank()) {
                if (errDetail.isNotBlank()) markError(errDetail)
                return@runCatching null
            }
            markConnected()
            fromFirestoreDocument(JSONObject(body))
        }.onFailure { err ->
            markError(err.localizedMessage ?: err.javaClass.simpleName)
        }.getOrNull()
    }

    suspend fun pushSettings(brand: PlatformBrandConfig): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudNetworkAllowed() || !brand.isCloudConfigured) return@withContext false
        runCatching {
            val conn = (URL(settingsUrl(brand.firebaseProjectId, brand.firebaseApiKey)).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().put("fields", toFirestoreFields(settingsToJson(brand)))
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val errDetail = if (code !in 200..299) readHttpErrorDetail(conn, code) else ""
            conn.disconnect()
            if (code in 200..299) {
                markConnected()
                true
            } else {
                markError(errDetail)
                false
            }
        }.onFailure { err ->
            markError(err.localizedMessage ?: err.javaClass.simpleName)
            android.util.Log.e("TebFirestoreSync", "pushSettings failed", err)
        }.getOrDefault(false)
    }

    suspend fun listCollection(brand: PlatformBrandConfig, subCollection: String): List<JSONObject>? =
        withContext(Dispatchers.IO) {
            if (!isCloudNetworkAllowed() || !brand.isCloudConfigured) return@withContext null
            runCatching {
                val results = mutableListOf<JSONObject>()
                var pageToken: String? = null
                var safetyPages = 0
                do {
                    safetyPages++
                    val url = collectionUrl(brand.firebaseProjectId, brand.firebaseApiKey, subCollection, pageToken)
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        useCaches = false
                    }
                    val code = conn.responseCode
                    val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
                    val errDetail = if (code !in 200..299 && code != 404) readHttpErrorDetail(conn, code) else ""
                    conn.disconnect()
                    if (code == 404) {
                        markConnected()
                        return@runCatching emptyList<JSONObject>()
                    }
                    if (code !in 200..299) {
                        markError(errDetail)
                        return@runCatching null
                    }
                    if (body.isBlank()) break
                    val json = JSONObject(body)
                    val docs = json.optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val d = docs.optJSONObject(i) ?: continue
                            results += fromFirestoreDocument(d)
                        }
                    }
                    pageToken = json.optString("nextPageToken", "").takeIf { it.isNotBlank() }
                } while (pageToken != null && safetyPages < 10)
                markConnected()
                results
            }.onFailure { err ->
                markError(err.localizedMessage ?: err.javaClass.simpleName)
            }.getOrNull()
        }

    suspend fun upsertDocument(
        brand: PlatformBrandConfig,
        subCollection: String,
        docId: String,
        payloadJson: JSONObject,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudNetworkAllowed() || !brand.isCloudConfigured || docId.isBlank()) return@withContext false
        runCatching {
            val url = documentUrl(brand.firebaseProjectId, brand.firebaseApiKey, subCollection, docId)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().put("fields", toFirestoreFields(payloadJson))
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val errDetail = if (code !in 200..299) readHttpErrorDetail(conn, code) else ""
            conn.disconnect()
            if (code in 200..299) {
                markConnected()
                true
            } else {
                markError(errDetail)
                false
            }
        }.onFailure { err ->
            markError(err.localizedMessage ?: err.javaClass.simpleName)
            android.util.Log.e("TebFirestoreSync", "upsertDocument($subCollection/$docId) failed", err)
        }.getOrDefault(false)
    }

    fun fcmTokensDocUrl(projectId: String, apiKey: String, docId: String = "registry"): String =
        "${baseDocumentsUrl(projectId)}/${CloudNamespacePaths.FCM_TOKENS}/${enc(safeCloudDocId(docId))}?key=${enc(apiKey.trim())}"

    suspend fun upsertFcmToken(
        brand: PlatformBrandConfig,
        userId: String,
        tokenPayload: JSONObject,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudNetworkAllowed() || !brand.isCloudConfigured || userId.isBlank()) return@withContext false
        val safeKey = safeCloudDocId(userId)
        upsertDocument(brand, "fcm_tokens", safeKey, tokenPayload)
    }

    suspend fun deleteDocument(
        brand: PlatformBrandConfig,
        subCollection: String,
        docId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudNetworkAllowed() || !brand.isCloudConfigured || docId.isBlank()) return@withContext false
        runCatching {
            val url = documentUrl(brand.firebaseProjectId, brand.firebaseApiKey, subCollection, docId)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            val code = conn.responseCode
            val errDetail = if (code !in 200..299 && code != 404) readHttpErrorDetail(conn, code) else ""
            conn.disconnect()
            if (code in 200..299 || code == 404) {
                markConnected()
                true
            } else {
                markError(errDetail)
                false
            }
        }.onFailure { err ->
            markError(err.localizedMessage ?: err.javaClass.simpleName)
            android.util.Log.e("TebFirestoreSync", "deleteDocument($subCollection/$docId) failed", err)
        }.getOrDefault(false)
    }

    // =========================================================================
    // محولات الكيانات المتوافقة 100% مع نسخة الويب (index.html)
    // =========================================================================

    fun settingsToJson(brand: PlatformBrandConfig): JSONObject = JSONObject().apply {
        put("platformNameAr", brand.platformNameAr)
        put("platformNameEn", brand.platformNameEn)
        put("villageNameAr", brand.villageNameAr)
        put("villageNameEn", brand.villageNameEn)
        put("hospitalNameAr", brand.hospitalNameAr)
        put("hospitalNameEn", brand.hospitalNameEn)
        put("taglineAr", brand.taglineAr)
        put("taglineEn", brand.taglineEn)
        put("homeAnnouncementAr", brand.homeAnnouncementAr)
        put("supportPhone", brand.supportPhone)
        put("otpWhatsappPhone", brand.otpWhatsappPhone)
        put("apkDownloadUrl", brand.apkDownloadUrl)
        put("medicationsApiUrl", brand.medicationsApiUrl)
        put("adminPin", brand.adminPin)
        put("adminUsername", brand.adminUsername)
        put("adminPassword", brand.adminPassword)
        put("globalChatEnabled", brand.globalChatEnabled)
        put("appVersionLabel", "v1.0.0")
        put("firebaseApiKey", brand.firebaseApiKey.ifBlank { AppConfig.defaultBrand.firebaseApiKey })
        put("firebaseAuthDomain", brand.firebaseAuthDomain.ifBlank { AppConfig.defaultBrand.firebaseAuthDomain })
        put("firebaseProjectId", brand.firebaseProjectId.ifBlank { AppConfig.defaultBrand.firebaseProjectId })
        put("firebaseStorageBucket", brand.firebaseStorageBucket.ifBlank { AppConfig.defaultBrand.firebaseStorageBucket })
        put("firebaseMessagingSenderId", brand.firebaseMessagingSenderId.ifBlank { AppConfig.defaultBrand.firebaseMessagingSenderId })
        put("firebaseAppId", brand.firebaseAppId.ifBlank { AppConfig.defaultBrand.firebaseAppId })
        put("firebaseVapidKey", brand.firebaseVapidKey.ifBlank { AppConfig.defaultBrand.firebaseVapidKey })
        put("workerUrl", brand.workerUrl.ifBlank { AppConfig.defaultBrand.workerUrl })
        put("defaultDriveWebAppUrl", brand.defaultDriveWebAppUrl)
        put("fileRetentionDays", normalizeRetentionDays(brand.fileRetentionDays))
    }

    fun normalizeRetentionDays(raw: Int): Int = when (raw) {
        90, 180, 360 -> raw
        else -> 360
    }

    fun settingsFromJson(remote: JSONObject, current: PlatformBrandConfig): PlatformBrandConfig {
        val def = AppConfig.defaultBrand
        fun str(key: String, fallback: String): String =
            remote.optString(key, "").trim().ifEmpty { fallback }
        val remoteRetention = if (remote.has("fileRetentionDays")) {
            normalizeRetentionDays(remote.optInt("fileRetentionDays", current.fileRetentionDays))
        } else {
            normalizeRetentionDays(current.fileRetentionDays)
        }
        val rawRemotePin = remote.optString("adminPin", "").trim()
        val resolvedAdminPin = when {
            rawRemotePin.isEmpty() || rawRemotePin == "202600" -> {
                if (current.adminPin.isBlank() || current.adminPin == "202600") def.adminPin else current.adminPin
            }
            else -> rawRemotePin
        }
        return current.copy(
            platformNameAr = str("platformNameAr", current.platformNameAr),
            villageNameAr = str("villageNameAr", current.villageNameAr),
            hospitalNameAr = str("hospitalNameAr", current.hospitalNameAr),
            supportPhone = str("supportPhone", current.supportPhone),
            homeAnnouncementAr = if (remote.has("homeAnnouncementAr")) remote.optString("homeAnnouncementAr", "") else current.homeAnnouncementAr,
            apkDownloadUrl = str("apkDownloadUrl", current.apkDownloadUrl),
            medicationsApiUrl = if (remote.has("medicationsApiUrl")) remote.optString("medicationsApiUrl", "") else current.medicationsApiUrl,
            adminPin = resolvedAdminPin,
            adminUsername = str("adminUsername", current.adminUsername),
            adminPassword = str("adminPassword", current.adminPassword),
            globalChatEnabled = if (remote.has("globalChatEnabled")) remote.optBoolean("globalChatEnabled", current.globalChatEnabled) else current.globalChatEnabled,
            appVersionLabel = "v1.0.0",
            firebaseApiKey = str("firebaseApiKey", def.firebaseApiKey),
            firebaseAuthDomain = str("firebaseAuthDomain", def.firebaseAuthDomain),
            firebaseProjectId = str("firebaseProjectId", def.firebaseProjectId),
            firebaseStorageBucket = str("firebaseStorageBucket", def.firebaseStorageBucket),
            firebaseMessagingSenderId = str("firebaseMessagingSenderId", def.firebaseMessagingSenderId),
            firebaseAppId = str("firebaseAppId", def.firebaseAppId),
            firebaseVapidKey = str("firebaseVapidKey", def.firebaseVapidKey),
            workerUrl = str("workerUrl", def.workerUrl),
            defaultDriveWebAppUrl = if (remote.has("defaultDriveWebAppUrl")) remote.optString("defaultDriveWebAppUrl", "").trim() else current.defaultDriveWebAppUrl,
            fileRetentionDays = remoteRetention,
        )
    }

    fun doctorToJson(d: ClinicDoctorEntity): JSONObject = JSONObject().apply {
        put("id", d.id)
        put("doctorName", d.doctorName)
        put("doctorNameEn", d.doctorNameEn)
        put("specialty", d.specialty)
        put("additionalSpecialties", JSONArray(d.additionalSpecialties.split("|").map { it.trim() }.filter { it.isNotEmpty() }))
        put("clinicName", d.clinicName)
        put("clinicNameEn", d.clinicNameEn)
        put("clinicType", d.clinicType)
        put("locationLabel", d.locationLabel)
        put("locationLabelEn", d.locationLabelEn)
        put("feeEgp", d.feeEgp)
        put("workStartMinutes", d.workStartMinutes)
        put("workEndMinutes", d.workEndMinutes)
        put("slotDurationMinutes", d.slotDurationMinutes)
        put("currentDelayMinutes", d.currentDelayMinutes)
        put("rating", d.rating.toDouble())
        put("experienceYears", d.experienceYears)
        put("bio", d.bio)
        put("mapsUrl", d.mapsUrl)
        put("username", d.username)
        put("password", d.password)
        put("chatEnabled", d.chatEnabled)
        put("driveWebAppUrl", d.driveWebAppUrl)
        put("secondaryDriveWebAppUrl", d.secondaryDriveWebAppUrl)
    }

    fun doctorFromJson(j: JSONObject): ClinicDoctorEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val name = j.optString("doctorName", "").trim()
        if (id.isEmpty() || name.isEmpty()) return null
        val addSpecRaw = j.opt("additionalSpecialties")
        val addSpec = when (addSpecRaw) {
            is JSONArray -> (0 until addSpecRaw.length()).mapNotNull { addSpecRaw.optString(it).trim().takeIf { s -> s.isNotEmpty() } }.joinToString("|")
            is String -> addSpecRaw.trim()
            else -> ""
        }
        return ClinicDoctorEntity(
            id = id,
            doctorName = name,
            specialty = j.optString("specialty", "طب عام").trim(),
            clinicName = j.optString("clinicName", "عيادة $name").trim(),
            clinicType = j.optString("clinicType", ClinicType.PRIVATE).ifBlank { ClinicType.PRIVATE },
            locationLabel = j.optString("locationLabel", "قرية الحجايزة"),
            feeEgp = j.optInt("feeEgp", 150),
            workStartMinutes = j.optInt("workStartMinutes", 17 * 60),
            workEndMinutes = j.optInt("workEndMinutes", 21 * 60),
            slotDurationMinutes = j.optInt("slotDurationMinutes", 20).coerceIn(5, 120),
            currentDelayMinutes = j.optInt("currentDelayMinutes", 0),
            rating = j.optDouble("rating", 4.9).toFloat(),
            experienceYears = j.optInt("experienceYears", 10),
            bio = j.optString("bio", ""),
            mapsUrl = j.optString("mapsUrl", ""),
            username = j.optString("username", id),
            password = j.optString("password", "1234"),
            doctorNameEn = j.optString("doctorNameEn", ""),
            clinicNameEn = j.optString("clinicNameEn", ""),
            locationLabelEn = j.optString("locationLabelEn", ""),
            additionalSpecialties = addSpec,
            chatEnabled = if (j.has("chatEnabled")) j.optBoolean("chatEnabled", true) else true,
            driveWebAppUrl = j.optString("driveWebAppUrl", "").trim(),
            secondaryDriveWebAppUrl = j.optString("secondaryDriveWebAppUrl", "").trim(),
        )
    }

    fun labToJson(l: MedicalLabEntity): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("labName", l.labName)
        put("phone", l.phone)
        put("locationLabel", l.locationLabel)
        put("username", l.username)
        put("password", l.password)
        put("createdAtMillis", l.createdAtMillis)
    }

    fun labFromJson(j: JSONObject): MedicalLabEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val name = j.optString("labName", "").trim()
        if (id.isEmpty() || name.isEmpty()) return null
        return MedicalLabEntity(
            id = id,
            labName = name,
            phone = j.optString("phone", ""),
            locationLabel = j.optString("locationLabel", "قرية الحجايزة"),
            username = j.optString("username", id),
            password = j.optString("password", "1234"),
            createdAtMillis = j.optLong("createdAtMillis", System.currentTimeMillis()),
        )
    }

    fun accountToJson(a: PatientAccountEntity): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("fullName", a.fullName)
        put("phone", a.phone)
        put("nationalId", a.nationalId)
        put("password", a.password)
        put("isPhoneVerified", a.isPhoneVerified)
        put("isVerified", a.isPhoneVerified)
        put("verifiedBy", a.verifiedBy)
        put("verifiedAtMillis", a.verifiedAtMillis)
        put("otpCode", a.otpCode)
        put("otpCreatedAtMillis", a.otpCreatedAtMillis)
        put("otpExpiresAtMillis", a.otpExpiresAtMillis)
        put("otpUsed", a.otpUsed)
        put("linkedSpouseAccountId", a.linkedSpouseAccountId)
        put("linkedSpousePhone", a.linkedSpousePhone)
        put("linkedSpouseName", a.linkedSpouseName)
        put("allowSpouseViewPersonalRecords", a.allowSpouseViewPersonalRecords)
    }

    fun accountFromJson(j: JSONObject): PatientAccountEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val phone = j.optString("phone", "").trim()
        if (id.isEmpty() || phone.isEmpty()) return null
        val resolvedVerified = when {
            j.has("isPhoneVerified") -> j.optBoolean("isPhoneVerified", false)
            j.has("isVerified") -> j.optBoolean("isVerified", false)
            else -> false
        }
        return PatientAccountEntity(
            id = id,
            fullName = j.optString("fullName", "مريض"),
            phone = phone,
            nationalId = j.optString("nationalId", ""),
            password = j.optString("password", "1234"),
            isPhoneVerified = resolvedVerified,
            verifiedBy = j.optString("verifiedBy", ""),
            verifiedAtMillis = j.optLong("verifiedAtMillis", 0L),
            otpCode = j.optString("otpCode", ""),
            otpCreatedAtMillis = j.optLong("otpCreatedAtMillis", 0L),
            otpExpiresAtMillis = j.optLong("otpExpiresAtMillis", 0L),
            otpUsed = if (j.has("otpUsed")) j.optBoolean("otpUsed", false) else false,
            linkedSpouseAccountId = j.optString("linkedSpouseAccountId", j.optString("spouseAccountId", "")),
            linkedSpousePhone = j.optString("linkedSpousePhone", j.optString("spousePhone", "")),
            linkedSpouseName = j.optString("linkedSpouseName", j.optString("spouseName", "")),
            allowSpouseViewPersonalRecords = j.optBoolean("allowSpouseViewPersonalRecords", false),
        )
    }

    fun familyMemberToJson(m: FamilyMemberEntity): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("accountId", m.accountId)
        put("fullName", m.fullName)
        put("relation", m.relation)
        put("age", m.age)
        put("phone", m.phone)
        put("weightKg", m.weightKg)
        put("heightCm", m.heightCm)
        put("bloodType", m.bloodType)
        put("chronicConditions", JSONArray(ChronicConditionsCatalog.parseKeys(m.chronicConditions)))
        put("drugAllergies", m.drugAllergies)
        put("otherChronicNotes", m.otherChronicNotes)
        put("aiSummaryAr", m.aiSummaryAr)
        put("aiSummaryEn", m.aiSummaryEn)
        put("aiSummaryUpdatedAtMillis", m.aiSummaryUpdatedAtMillis)
        put("nationalId", m.nationalId)
    }

    fun familyMemberFromJson(j: JSONObject): FamilyMemberEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val accId = j.optString("accountId", "").trim()
        val name = j.optString("fullName", "").trim()
        if (id.isEmpty() || accId.isEmpty() || name.isEmpty()) return null
        val chronicsRaw = j.opt("chronicConditions")
        val chronicStr = when (chronicsRaw) {
            is JSONArray -> ChronicConditionsCatalog.encodeKeys(
                (0 until chronicsRaw.length()).mapNotNull { chronicsRaw.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
            )
            is String -> chronicsRaw.trim()
            else -> ""
        }
        return FamilyMemberEntity(
            id = id,
            accountId = accId,
            fullName = name,
            relation = j.optString("relation", "فرد أسرة"),
            age = j.optInt("age", 30),
            phone = j.optString("phone", ""),
            weightKg = j.optDouble("weightKg", 0.0),
            heightCm = j.optDouble("heightCm", 0.0),
            bloodType = j.optString("bloodType", ""),
            chronicConditions = chronicStr,
            drugAllergies = j.optString("drugAllergies", ""),
            otherChronicNotes = j.optString("otherChronicNotes", ""),
            aiSummaryAr = j.optString("aiSummaryAr", ""),
            aiSummaryEn = j.optString("aiSummaryEn", ""),
            aiSummaryUpdatedAtMillis = j.optLong("aiSummaryUpdatedAtMillis", 0L),
            aiSummaryError = "",
            nationalId = j.optString("nationalId", ""),
        )
    }

    fun slotToJson(s: AppointmentSlotEntity): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("doctorId", s.doctorId)
        put("dateLabel", s.dateLabel)
        put("startMinutes", s.startMinutes)
        put("delayMinutes", s.delayMinutes)
        put("status", s.status)
        put("patientName", s.patientName)
        put("patientPhone", s.patientPhone)
        put("familyMemberId", s.familyMemberId)
        put("bookingSource", s.bookingSource)
        put("bookedAtMillis", s.bookedAtMillis)
    }

    fun slotFromJson(j: JSONObject): AppointmentSlotEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val docId = j.optString("doctorId", "").trim()
        if (id.isEmpty() || docId.isEmpty()) return null
        return AppointmentSlotEntity(
            id = id,
            doctorId = docId,
            dateLabel = j.optString("dateLabel", "اليوم"),
            startMinutes = j.optInt("startMinutes", 17 * 60),
            delayMinutes = j.optInt("delayMinutes", 0),
            status = j.optString("status", SlotStatus.AVAILABLE).ifBlank { SlotStatus.AVAILABLE },
            patientName = j.optString("patientName", ""),
            patientPhone = j.optString("patientPhone", ""),
            familyMemberId = j.optString("familyMemberId", ""),
            bookingSource = j.optString("bookingSource", BookingSource.NONE),
            bookedAtMillis = j.optLong("bookedAtMillis", 0L),
        )
    }

    fun recordToJson(r: MedicalRecordEntity): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("familyMemberId", r.familyMemberId)
        put("patientName", r.patientName)
        put("patientPhone", r.patientPhone)
        put("doctorId", r.doctorId)
        put("doctorName", r.doctorName)
        put("specialty", r.specialty)
        put("visitDateLabel", r.visitDateLabel)
        put("diagnosis", r.diagnosis)
        put("medications", r.medications)
        put("notes", r.notes)
        put("attachmentMimeType", r.attachmentMimeType)
        put("attachmentName", r.attachmentName)
        put("recordType", r.recordType)
        put("createdAtMillis", r.createdAtMillis)
        put("diagnosisEn", r.diagnosisEn)
        put("medicationsEn", r.medicationsEn)
        put("notesEn", r.notesEn)
        put("aiReportAr", r.aiReportAr)
        put("aiReportEn", r.aiReportEn)
        put("driveFileUrl", r.driveFileUrl)
    }

    fun recordFromJson(j: JSONObject, existingLocalAttachmentPath: String = ""): MedicalRecordEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val diag = j.optString("diagnosis", "").trim()
        if (id.isEmpty()) return null
        return MedicalRecordEntity(
            id = id,
            familyMemberId = j.optString("familyMemberId", ""),
            patientName = j.optString("patientName", ""),
            patientPhone = j.optString("patientPhone", ""),
            doctorId = j.optString("doctorId", ""),
            doctorName = j.optString("doctorName", ""),
            specialty = j.optString("specialty", ""),
            visitDateLabel = j.optString("visitDateLabel", ""),
            diagnosis = diag.ifBlank { j.optString("attachmentName", "سجل طبي") },
            medications = j.optString("medications", ""),
            notes = j.optString("notes", ""),
            attachmentPath = existingLocalAttachmentPath,
            attachmentMimeType = j.optString("attachmentMimeType", ""),
            attachmentName = j.optString("attachmentName", ""),
            recordType = j.optString("recordType", MedicalRecordType.PRESCRIPTION).ifBlank { MedicalRecordType.PRESCRIPTION },
            createdAtMillis = j.optLong("createdAtMillis", System.currentTimeMillis()),
            diagnosisEn = j.optString("diagnosisEn", ""),
            medicationsEn = j.optString("medicationsEn", ""),
            notesEn = j.optString("notesEn", ""),
            aiReportAr = j.optString("aiReportAr", ""),
            aiReportEn = j.optString("aiReportEn", ""),
            driveFileUrl = j.optString("driveFileUrl", "").trim(),
        )
    }

    fun permanentLogToJson(l: PermanentMedicalLogEntity): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("familyMemberId", l.familyMemberId)
        put("patientName", l.patientName)
        put("patientPhone", l.patientPhone)
        put("sourceRecordId", l.sourceRecordId)
        put("eventType", l.eventType)
        put("eventTimestampMillis", l.eventTimestampMillis)
        put("eventDateTimeLabel", l.eventDateTimeLabel)
        put("providerName", l.providerName)
        put("specialtyOrCategory", l.specialtyOrCategory)
        put("title", l.title)
        put("narrativeAr", l.narrativeAr)
        put("narrativeEn", l.narrativeEn)
        put("isAiGenerated", l.isAiGenerated)
    }

    fun permanentLogFromJson(j: JSONObject): PermanentMedicalLogEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val narrativeAr = j.optString("narrativeAr", "").trim()
        val narrativeEn = j.optString("narrativeEn", "").trim()
        if (id.isEmpty() || (narrativeAr.isEmpty() && narrativeEn.isEmpty())) return null
        return PermanentMedicalLogEntity(
            id = id,
            familyMemberId = j.optString("familyMemberId", ""),
            patientName = j.optString("patientName", ""),
            patientPhone = j.optString("patientPhone", ""),
            sourceRecordId = j.optString("sourceRecordId", ""),
            eventType = j.optString("eventType", PermanentLogEventType.CLINIC_VISIT).ifBlank { PermanentLogEventType.CLINIC_VISIT },
            eventTimestampMillis = j.optLong("eventTimestampMillis", System.currentTimeMillis()),
            eventDateTimeLabel = j.optString("eventDateTimeLabel", ""),
            providerName = j.optString("providerName", ""),
            specialtyOrCategory = j.optString("specialtyOrCategory", ""),
            title = j.optString("title", ""),
            narrativeAr = narrativeAr,
            narrativeEn = narrativeEn,
            isAiGenerated = j.optBoolean("isAiGenerated", false),
        )
    }

    fun notificationToJson(n: DelayNotificationEntity): JSONObject = JSONObject().apply {
        put("id", "notif_${n.id}_${n.createdAtMillis}")
        put("doctorId", n.doctorId)
        put("doctorName", n.doctorName)
        put("clinicName", n.clinicName)
        put("delayMinutes", n.delayMinutes)
        put("message", n.message)
        put("createdAtMillis", n.createdAtMillis)
        put("isRead", n.isRead)
        put("targetAudience", n.targetAudience)
        put("targetId", n.targetId)
        put("senderLabel", n.senderLabel)
    }

    fun notificationFromJson(j: JSONObject): DelayNotificationEntity? {
        val message = j.optString("message", "").trim()
        if (message.isEmpty()) return null
        val created = j.optLong("createdAtMillis", System.currentTimeMillis())
        return DelayNotificationEntity(
            id = 0L,
            doctorId = j.optString("doctorId", ""),
            doctorName = j.optString("doctorName", ""),
            clinicName = j.optString("clinicName", ""),
            delayMinutes = j.optInt("delayMinutes", 0),
            message = message,
            createdAtMillis = created,
            isRead = j.optBoolean("isRead", false),
            targetAudience = j.optString("targetAudience", NotificationAudience.ALL).ifBlank { NotificationAudience.ALL },
            targetId = j.optString("targetId", ""),
            senderLabel = j.optString("senderLabel", ""),
        )
    }

    fun messageToJson(m: DirectMessageEntity): JSONObject = JSONObject().apply {
        val generallyRead = when {
            m.threadType == ChatThreadType.PATIENT_ADMIN && m.senderRole == MessageSenderRole.PATIENT -> m.isReadByAdmin
            m.threadType == ChatThreadType.CLINIC_ADMIN && m.senderRole == MessageSenderRole.CLINIC -> m.isReadByAdmin
            m.threadType == ChatThreadType.PATIENT_CLINIC && m.senderRole == MessageSenderRole.PATIENT -> m.isReadByClinic
            else -> m.isReadByPatient
        }
        put("id", m.id)
        put("threadType", m.threadType)
        put("patientAccountId", m.patientAccountId)
        put("patientName", m.patientName)
        put("patientPhone", m.patientPhone)
        put("doctorId", m.doctorId)
        put("doctorName", m.doctorName)
        put("clinicName", m.clinicName)
        put("senderRole", m.senderRole)
        put("senderName", m.senderName)
        put("body", m.body)
        put("createdAtMillis", m.createdAtMillis)
        put("isRead", generallyRead)
        put("isReadByPatient", m.isReadByPatient)
        put("isReadByClinic", m.isReadByClinic)
        put("isReadByAdmin", m.isReadByAdmin)
    }

    fun messageFromJson(j: JSONObject): DirectMessageEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val body = j.optString("body", "").trim()
        if (id.isEmpty() || body.isEmpty()) return null
        val threadType = j.optString("threadType", ChatThreadType.PATIENT_CLINIC)
        val senderRole = j.optString("senderRole", MessageSenderRole.PATIENT)
        val legacyRead = j.optBoolean("isRead", false)
        val defaultReadByPatient = if (senderRole == MessageSenderRole.PATIENT || threadType == ChatThreadType.CLINIC_ADMIN) true else legacyRead
        val defaultReadByClinic = if (senderRole == MessageSenderRole.CLINIC || threadType == ChatThreadType.PATIENT_ADMIN) true else legacyRead
        val defaultReadByAdmin = if (senderRole == MessageSenderRole.ADMIN || threadType == ChatThreadType.PATIENT_CLINIC) true else legacyRead
        return DirectMessageEntity(
            id = id,
            threadType = threadType,
            patientAccountId = j.optString("patientAccountId", ""),
            patientName = j.optString("patientName", ""),
            patientPhone = j.optString("patientPhone", ""),
            doctorId = j.optString("doctorId", ""),
            doctorName = j.optString("doctorName", ""),
            clinicName = j.optString("clinicName", ""),
            senderRole = senderRole,
            senderName = j.optString("senderName", ""),
            body = body,
            createdAtMillis = j.optLong("createdAtMillis", System.currentTimeMillis()),
            isReadByPatient = if (j.has("isReadByPatient")) j.optBoolean("isReadByPatient", defaultReadByPatient) else defaultReadByPatient,
            isReadByClinic = if (j.has("isReadByClinic")) j.optBoolean("isReadByClinic", defaultReadByClinic) else defaultReadByClinic,
            isReadByAdmin = if (j.has("isReadByAdmin")) j.optBoolean("isReadByAdmin", defaultReadByAdmin) else defaultReadByAdmin,
        )
    }

    fun blockedPhoneToJson(b: BlockedPhoneEntity): JSONObject = JSONObject().apply {
        put("phone", b.phone)
        put("reason", b.reason)
        put("blockedAtMillis", b.blockedAtMillis)
    }

    fun blockedPhoneFromJson(j: JSONObject): BlockedPhoneEntity? {
        val phone = j.optString("phone", j.optString("_docId", "")).trim()
        if (phone.isEmpty()) return null
        return BlockedPhoneEntity(
            phone = phone,
            reason = j.optString("reason", "حظر إداري بسبب حجز وهمي / مخالفة"),
            blockedAtMillis = j.optLong("blockedAtMillis", System.currentTimeMillis()),
        )
    }

    fun activationCodeToJson(c: ActivationOtpCodeEntity): JSONObject = JSONObject().apply {
        put("code", c.code)
        put("isUsed", c.isUsed)
        put("createdAtMillis", c.createdAtMillis)
        put("usedByName", c.usedByName)
        put("usedByPhone", c.usedByPhone)
        put("usedAtMillis", c.usedAtMillis)
        put("generatedBy", c.generatedBy)
    }

    fun activationCodeFromJson(j: JSONObject): ActivationOtpCodeEntity? {
        val code = j.optString("code", j.optString("_docId", "")).trim()
        if (code.isEmpty()) return null
        return ActivationOtpCodeEntity(
            code = code,
            isUsed = j.optBoolean("isUsed", false),
            createdAtMillis = j.optLong("createdAtMillis", System.currentTimeMillis()),
            usedByName = j.optString("usedByName", ""),
            usedByPhone = j.optString("usedByPhone", ""),
            usedAtMillis = j.optLong("usedAtMillis", 0L),
            generatedBy = j.optString("generatedBy", "ADMIN"),
        )
    }

    fun familyLinkRequestToJson(r: FamilyLinkRequestEntity): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("requesterAccountId", r.requesterAccountId)
        put("requesterName", r.requesterName)
        put("requesterPhone", r.requesterPhone)
        put("targetPhone", r.targetPhone)
        put("relation", r.relation)
        put("proofMarriageContractUrl", r.proofMarriageContractUrl)
        put("proofNationalIdFrontUrl", r.proofNationalIdFrontUrl)
        put("proofNationalIdBackUrl", r.proofNationalIdBackUrl)
        put("status", r.status)
        put("reviewedBy", r.reviewedBy)
        put("reviewedAtMillis", r.reviewedAtMillis)
        put("requestedAtMillis", r.requestedAtMillis)
        put("notes", r.notes)
    }

    fun familyLinkRequestFromJson(j: JSONObject): FamilyLinkRequestEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        val reqId = j.optString("requesterAccountId", "").trim()
        if (id.isEmpty() || reqId.isEmpty()) return null
        return FamilyLinkRequestEntity(
            id = id,
            requesterAccountId = reqId,
            requesterName = j.optString("requesterName", ""),
            requesterPhone = j.optString("requesterPhone", ""),
            targetPhone = j.optString("targetPhone", ""),
            relation = j.optString("relation", "SPOUSE"),
            proofMarriageContractUrl = j.optString("proofMarriageContractUrl", ""),
            proofNationalIdFrontUrl = j.optString("proofNationalIdFrontUrl", ""),
            proofNationalIdBackUrl = j.optString("proofNationalIdBackUrl", ""),
            status = j.optString("status", FamilyLinkStatus.PENDING),
            reviewedBy = j.optString("reviewedBy", ""),
            reviewedAtMillis = j.optLong("reviewedAtMillis", 0L),
            requestedAtMillis = j.optLong("requestedAtMillis", System.currentTimeMillis()),
            notes = j.optString("notes", ""),
        )
    }

    fun visitorHitToJson(h: PlatformVisitorHitEntity): JSONObject = JSONObject().apply {
        put("id", h.id)
        put("platform", h.platform)
        put("timestampMillis", h.timestampMillis)
    }

    fun visitorHitFromJson(j: JSONObject): PlatformVisitorHitEntity? {
        val id = j.optString("id", j.optString("_docId", "")).trim()
        if (id.isEmpty()) return null
        return PlatformVisitorHitEntity(
            id = id,
            platform = j.optString("platform", "APK"),
            timestampMillis = j.optLong("timestampMillis", System.currentTimeMillis()),
        )
    }
}
