package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
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
import com.example.data.local.DoctorFavoriteMedEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalLabEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.MessageSenderRole
import com.example.data.local.NotificationAudience
import com.example.data.local.PatientAccountEntity
import com.example.data.local.SlotStatus
import com.example.data.local.TebDao
import com.example.ai.GeminiMedicalAnalyzer
import com.example.data.medications.EgyptianMedicationService
import com.example.data.medications.MedicationFetchResult
import com.example.ui.AppLanguage
import com.example.util.BilingualMedicalTranslator
import com.example.util.ChronicConditionsCatalog
import com.example.util.ImageStorageHelper
import com.example.util.MedicalSummaryAnalyzer
import com.example.util.TimeUtils
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.Calendar
import java.util.UUID
import org.json.JSONObject

private val Context.tebDataStore by preferencesDataStore(name = "teb_prefs")

data class LabPatientSearchResult(
    val account: PatientAccountEntity,
    val familyMembers: List<FamilyMemberEntity>,
)

data class RemoteVersionCheckResult(
    val hasNewerApk: Boolean,
    val latestVersion: String,
    val latestVersionCode: Int,
    val apkDownloadUrl: String,
    val reachable: Boolean,
)

class TebRepository(
    private val dao: TebDao,
    private val appContext: Context,
) {
    private val seedMutex = Mutex()
    private val secureRandom = SecureRandom()
    private val geminiAnalyzer by lazy { GeminiMedicalAnalyzer() }

    private val activeAccountKey = stringPreferencesKey("active_account_id")
    private val activeMemberKey = stringPreferencesKey("active_member_id")
    private val activeDoctorPortalKey = stringPreferencesKey("active_doctor_portal_id")
    private val loggedInDoctorIdKey = stringPreferencesKey("logged_in_doctor_id")
    private val loggedInLabIdKey = stringPreferencesKey("logged_in_lab_id")
    private val adminUsernameKey = stringPreferencesKey("admin_username")
    private val adminPasswordKey = stringPreferencesKey("admin_password")
    private val adminPinKey = stringPreferencesKey("admin_pin")
    private val adminFailedAttemptsKey = intPreferencesKey("admin_failed_attempts")
    private val adminLockoutUntilKey = longPreferencesKey("admin_lockout_until_millis")
    private val cfgPlatformNameKey = stringPreferencesKey("cfg_platform_name")
    private val cfgVillageNameKey = stringPreferencesKey("cfg_village_name")
    private val cfgHospitalNameKey = stringPreferencesKey("cfg_hospital_name")
    private val cfgSupportPhoneKey = stringPreferencesKey("cfg_support_phone")
    private val cfgAnnouncementKey = stringPreferencesKey("cfg_home_announcement")
    private val cfgApkUrlKey = stringPreferencesKey("cfg_apk_url")
    private val cfgMedsApiKey = stringPreferencesKey("cfg_meds_api_url")
    private val cfgGlobalChatEnabledKey = booleanPreferencesKey("cfg_global_chat_enabled")
    private val cfgFbApiKey = stringPreferencesKey("cfg_fb_api_key")
    private val cfgFbAuthDomainKey = stringPreferencesKey("cfg_fb_auth_domain")
    private val cfgFbProjectIdKey = stringPreferencesKey("cfg_fb_project_id")
    private val cfgFbStorageBucketKey = stringPreferencesKey("cfg_fb_storage_bucket")
    private val cfgFbMessagingSenderIdKey = stringPreferencesKey("cfg_fb_messaging_sender_id")
    private val cfgFbAppIdKey = stringPreferencesKey("cfg_fb_app_id")
    private val cfgFbVapidKey = stringPreferencesKey("cfg_fb_vapid_key")
    private val cfgWorkerUrlKey = stringPreferencesKey("cfg_worker_url")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val seededRealDoctorsKey = booleanPreferencesKey("seeded_real_doctors_v1")

    val appLanguageFlow: Flow<AppLanguage> = appContext.tebDataStore.data
        .map { prefs -> AppLanguage.fromCode(prefs[appLanguageKey]) }

    val activeAccountIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeAccountKey].orEmpty() }

    val activeMemberIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeMemberKey].orEmpty() }

    val activeDoctorPortalIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeDoctorPortalKey].orEmpty() }

    val loggedInDoctorIdFlow: Flow<String?> = appContext.tebDataStore.data
        .map { prefs -> prefs[loggedInDoctorIdKey]?.takeIf { it.isNotBlank() } }

    val loggedInLabIdFlow: Flow<String?> = appContext.tebDataStore.data
        .map { prefs -> prefs[loggedInLabIdKey]?.takeIf { it.isNotBlank() } }

    val adminUsernameFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[adminUsernameKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminUsername }

    val adminPasswordFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[adminPasswordKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminPassword }

    val brandConfigFlow: Flow<PlatformBrandConfig> = appContext.tebDataStore.data
        .map { prefs ->
            AppConfig.defaultBrand.copy(
                platformNameAr = prefs[cfgPlatformNameKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.platformNameAr,
                villageNameAr = prefs[cfgVillageNameKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.villageNameAr,
                hospitalNameAr = prefs[cfgHospitalNameKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.hospitalNameAr,
                supportPhone = prefs[cfgSupportPhoneKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.supportPhone,
                homeAnnouncementAr = prefs[cfgAnnouncementKey] ?: AppConfig.defaultBrand.homeAnnouncementAr,
                apkDownloadUrl = prefs[cfgApkUrlKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.apkDownloadUrl,
                medicationsApiUrl = prefs[cfgMedsApiKey] ?: AppConfig.defaultBrand.medicationsApiUrl,
                adminUsername = prefs[adminUsernameKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminUsername,
                adminPassword = prefs[adminPasswordKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminPassword,
                adminPin = prefs[adminPinKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminPin,
                globalChatEnabled = prefs[cfgGlobalChatEnabledKey] ?: AppConfig.defaultBrand.globalChatEnabled,
                firebaseApiKey = prefs[cfgFbApiKey].orEmpty(),
                firebaseAuthDomain = prefs[cfgFbAuthDomainKey].orEmpty(),
                firebaseProjectId = prefs[cfgFbProjectIdKey].orEmpty(),
                firebaseStorageBucket = prefs[cfgFbStorageBucketKey].orEmpty(),
                firebaseMessagingSenderId = prefs[cfgFbMessagingSenderIdKey].orEmpty(),
                firebaseAppId = prefs[cfgFbAppIdKey].orEmpty(),
                firebaseVapidKey = prefs[cfgFbVapidKey].orEmpty(),
                workerUrl = prefs[cfgWorkerUrlKey].orEmpty(),
            )
        }

    val adminSecurityFlow: Flow<Pair<Int, Long>> = appContext.tebDataStore.data
        .map { prefs ->
            (prefs[adminFailedAttemptsKey] ?: 0) to (prefs[adminLockoutUntilKey] ?: 0L)
        }

    val doctorsFlow: Flow<List<ClinicDoctorEntity>> = dao.observeDoctors()
    val labsFlow: Flow<List<MedicalLabEntity>> = dao.observeLabs()
    val allSlotsFlow: Flow<List<AppointmentSlotEntity>> = dao.observeAllSlots()
    val allAccountsFlow: Flow<List<PatientAccountEntity>> = dao.observeAllAccounts()
    val allFamilyMembersFlow: Flow<List<FamilyMemberEntity>> = dao.observeAllFamilyMembers()
    val allMedicalRecordsFlow: Flow<List<MedicalRecordEntity>> = dao.observeAllMedicalRecords()
    val notificationsFlow: Flow<List<DelayNotificationEntity>> = dao.observeNotifications()
    val directMessagesFlow: Flow<List<DirectMessageEntity>> = dao.observeAllDirectMessages()
    val allDoctorFavoritesFlow: Flow<List<DoctorFavoriteMedEntity>> = dao.observeAllDoctorFavorites()
    val blockedPhonesFlow: Flow<List<BlockedPhoneEntity>> = dao.observeBlockedPhones()
    val activationCodesFlow: Flow<List<ActivationOtpCodeEntity>> = dao.observeAllActivationCodes()

    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>> =
        dao.observeFamilyMembers(accountId)

    fun observeSlotsForDoctor(doctorId: String): Flow<List<AppointmentSlotEntity>> =
        dao.observeSlotsForDoctor(doctorId)

    suspend fun setAppLanguage(language: AppLanguage) {
        appContext.tebDataStore.edit { it[appLanguageKey] = language.code }
    }

    suspend fun setActiveMember(memberId: String) {
        appContext.tebDataStore.edit { it[activeMemberKey] = memberId }
    }

    suspend fun setActiveDoctorPortal(doctorId: String) {
        appContext.tebDataStore.edit { it[activeDoctorPortalKey] = doctorId }
    }

    internal suspend fun clearPrefsForTesting() {
        appContext.tebDataStore.edit { it.clear() }
    }

    suspend fun updateAdminCredentials(username: String, password: String) {
        val cleanUser = username.trim().ifEmpty { AppConfig.defaultBrand.adminUsername }
        val cleanPass = password.trim().ifEmpty { AppConfig.defaultBrand.adminPassword }
        appContext.tebDataStore.edit { prefs ->
            prefs[adminUsernameKey] = cleanUser
            prefs[adminPasswordKey] = cleanPass
        }
    }

    suspend fun verifyAdminPin(enteredPin: String, nowMillis: Long = System.currentTimeMillis()): Result<Unit> {
        val prefs = appContext.tebDataStore.data.first()
        val lockoutUntil = prefs[adminLockoutUntilKey] ?: 0L
        if (lockoutUntil > nowMillis) {
            val remainingMin = ((lockoutUntil - nowMillis) / 60_000L) + 1L
            return Result.failure(
                IllegalStateException("تم قفل بوابة الإدارة مؤقتاً بسبب 3 محاولات خاطئة. حاول بعد $remainingMin دقيقة.")
            )
        }
        val expectedPin = toAsciiDigits(prefs[adminPinKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminPin).trim()
        if (toAsciiDigits(enteredPin).trim() == expectedPin) {
            appContext.tebDataStore.edit { editable ->
                editable[adminFailedAttemptsKey] = 0
                editable[adminLockoutUntilKey] = 0L
            }
            return Result.success(Unit)
        }

        val currentFails = if (lockoutUntil in 1L..nowMillis) 0 else (prefs[adminFailedAttemptsKey] ?: 0)
        val nextFails = currentFails + 1
        return if (nextFails >= MAX_ADMIN_PIN_ATTEMPTS) {
            val lockedUntil = nowMillis + ADMIN_LOCKOUT_MILLIS
            appContext.tebDataStore.edit { editable ->
                editable[adminFailedAttemptsKey] = nextFails
                editable[adminLockoutUntilKey] = lockedUntil
            }
            Result.failure(
                IllegalStateException("تم إدخال رقم سري خاطئ 3 مرات. تم قفل الإدارة لمدة 15 دقيقة.")
            )
        } else {
            appContext.tebDataStore.edit { editable ->
                editable[adminFailedAttemptsKey] = nextFails
                editable[adminLockoutUntilKey] = 0L
            }
            val remainingTries = MAX_ADMIN_PIN_ATTEMPTS - nextFails
            Result.failure(
                IllegalArgumentException("الرقم السري غير صحيح. المحاولات المتبقية قبل القفل: $remainingTries")
            )
        }
    }

    suspend fun updateFullPlatformConfig(
        platformNameAr: String,
        villageNameAr: String,
        hospitalNameAr: String,
        supportPhone: String,
        homeAnnouncementAr: String,
        apkUrl: String,
        medsApiUrl: String,
        adminPin: String,
        firebaseApiKey: String? = null,
        firebaseAuthDomain: String? = null,
        firebaseProjectId: String? = null,
        firebaseStorageBucket: String? = null,
        firebaseMessagingSenderId: String? = null,
        firebaseAppId: String? = null,
        firebaseVapidKey: String? = null,
        workerUrl: String? = null,
    ) {
        appContext.tebDataStore.edit { prefs ->
            prefs[cfgPlatformNameKey] = platformNameAr.trim().ifEmpty { AppConfig.defaultBrand.platformNameAr }
            prefs[cfgVillageNameKey] = villageNameAr.trim().ifEmpty { AppConfig.defaultBrand.villageNameAr }
            prefs[cfgHospitalNameKey] = hospitalNameAr.trim().ifEmpty { AppConfig.defaultBrand.hospitalNameAr }
            prefs[cfgSupportPhoneKey] = supportPhone.trim().ifEmpty { AppConfig.defaultBrand.supportPhone }
            prefs[cfgAnnouncementKey] = homeAnnouncementAr.trim()
            prefs[cfgApkUrlKey] = apkUrl.trim().ifEmpty { AppConfig.defaultBrand.apkDownloadUrl }
            prefs[cfgMedsApiKey] = medsApiUrl.trim()
            val cleanPin = adminPin.trim()
            if (cleanPin.length >= 4) {
                prefs[adminPinKey] = cleanPin
            }
            if (firebaseApiKey != null) prefs[cfgFbApiKey] = firebaseApiKey.trim()
            if (firebaseAuthDomain != null) prefs[cfgFbAuthDomainKey] = firebaseAuthDomain.trim()
            if (firebaseProjectId != null) prefs[cfgFbProjectIdKey] = firebaseProjectId.trim()
            if (firebaseStorageBucket != null) prefs[cfgFbStorageBucketKey] = firebaseStorageBucket.trim()
            if (firebaseMessagingSenderId != null) prefs[cfgFbMessagingSenderIdKey] = firebaseMessagingSenderId.trim()
            if (firebaseAppId != null) prefs[cfgFbAppIdKey] = firebaseAppId.trim()
            if (firebaseVapidKey != null) prefs[cfgFbVapidKey] = firebaseVapidKey.trim()
            if (workerUrl != null) prefs[cfgWorkerUrlKey] = workerUrl.trim()
        }
    }

    suspend fun resetFullPlatformConfig() {
        appContext.tebDataStore.edit { prefs ->
            prefs.remove(cfgPlatformNameKey)
            prefs.remove(cfgVillageNameKey)
            prefs.remove(cfgHospitalNameKey)
            prefs.remove(cfgSupportPhoneKey)
            prefs.remove(cfgAnnouncementKey)
            prefs.remove(cfgApkUrlKey)
            prefs.remove(cfgMedsApiKey)
            prefs.remove(adminPinKey)
            prefs.remove(adminUsernameKey)
            prefs.remove(adminPasswordKey)
            prefs.remove(cfgGlobalChatEnabledKey)
            prefs.remove(cfgFbApiKey)
            prefs.remove(cfgFbAuthDomainKey)
            prefs.remove(cfgFbProjectIdKey)
            prefs.remove(cfgFbStorageBucketKey)
            prefs.remove(cfgFbMessagingSenderIdKey)
            prefs.remove(cfgFbAppIdKey)
            prefs.remove(cfgFbVapidKey)
            prefs.remove(cfgWorkerUrlKey)
        }
    }

    suspend fun blockPhone(phone: String, reason: String = "حظر إداري بسبب حجز وهمي / مخالفة"): Result<Unit> {
        val cleanPhone = phone.trim()
        if (cleanPhone.length < 8) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح للحظر"))
        }
        dao.insertBlockedPhone(BlockedPhoneEntity(phone = cleanPhone, reason = reason.trim().ifEmpty { "حظر إداري" }))
        val activeBooked = dao.observeAllSlots().first()
            .filter { it.status == SlotStatus.BOOKED && it.patientPhone.trim() == cleanPhone }
        activeBooked.forEach { slot ->
            dao.updateSlot(
                slot.copy(
                    status = SlotStatus.AVAILABLE,
                    patientName = "",
                    patientPhone = "",
                    familyMemberId = "",
                    bookingSource = BookingSource.NONE,
                    bookedAtMillis = 0L,
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun unblockPhone(phone: String) {
        dao.unblockPhone(phone.trim())
    }

    suspend fun markAccountPhoneVerified(accountId: String, verifiedBy: String = "كود OTP") {
        dao.markAccountPhoneVerified(accountId, verifiedBy, System.currentTimeMillis())
    }

    private suspend fun generateUniqueUserOtpCode(): String {
        val forbidden = HashSet<String>()
        forbidden.add(MASTER_ADMIN_OTP)
        dao.getAllAccountsOnce().forEach { if (it.otpCode.isNotBlank()) forbidden.add(it.otpCode) }
        dao.getAllActivationCodesOnce().forEach { forbidden.add(it.code) }
        repeat(80) {
            val candidate = (100000 + secureRandom.nextInt(900000)).toString()
            if (!forbidden.contains(candidate)) return candidate
        }
        return (100000 + secureRandom.nextInt(900000)).toString()
    }

    suspend fun regenerateAccountOtp(
        accountId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<PatientAccountEntity> {
        val acc = dao.getAccountById(accountId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        val freshCode = generateUniqueUserOtpCode()
        val expiresAt = nowMillis + OTP_TTL_MILLIS
        dao.updateAccountOtp(
            accountId = accountId,
            otpCode = freshCode,
            createdAtMillis = nowMillis,
            expiresAtMillis = expiresAt,
        )
        val updated = dao.getAccountById(accountId) ?: acc.copy(
            otpCode = freshCode,
            otpCreatedAtMillis = nowMillis,
            otpExpiresAtMillis = expiresAt,
            otpUsed = false,
        )
        return Result.success(updated)
    }

    suspend fun generateActivationCodeBatch(count: Int, generatedBy: String = "ADMIN"): Result<Int> {
        val safeCount = count.coerceIn(1, 100)
        val existingCodes = dao.getAllActivationCodesOnce().map { it.code }.toHashSet()
        existingCodes.add(MASTER_ADMIN_OTP)
        val now = System.currentTimeMillis()
        val generated = ArrayList<ActivationOtpCodeEntity>(safeCount)
        var attempts = 0
        while (generated.size < safeCount && attempts < safeCount * 30) {
            attempts++
            val candidate = (100000 + secureRandom.nextInt(900000)).toString()
            if (existingCodes.add(candidate)) {
                generated += ActivationOtpCodeEntity(
                    code = candidate,
                    isUsed = false,
                    createdAtMillis = now + generated.size,
                    usedByName = "",
                    usedByPhone = "",
                    usedAtMillis = 0L,
                    generatedBy = generatedBy.ifBlank { "ADMIN" },
                )
            }
        }
        if (generated.isEmpty()) {
            return Result.failure(IllegalStateException("تعذر توليد أكواد جديدة حالياً"))
        }
        dao.insertActivationCodes(generated)
        return Result.success(generated.size)
    }

    suspend fun verifyAndBurnActivationCode(
        accountId: String,
        accountName: String,
        accountPhone: String,
        enteredCode: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<Unit> {
        val cleanCode = toAsciiDigits(enteredCode).filter { it in '0'..'9' }
        if (cleanCode.length != 6) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كود التفعيل المكون من 6 أرقام"))
        }
        if (cleanCode == MASTER_ADMIN_OTP) {
            dao.markAccountPhoneVerified(accountId, "كود OTP", nowMillis)
            return Result.success(Unit)
        }

        // 1) التحقق من الكود الفريد المرتبط بحساب المستخدم نفسه (صالح لمدة 24 ساعة فقط)
        val boundAccount = dao.getAccountById(accountId)
        if (boundAccount != null && boundAccount.otpCode.isNotBlank() && boundAccount.otpCode == cleanCode) {
            if (boundAccount.otpUsed) {
                return Result.failure(
                    IllegalStateException("تم استخدام هذا الكود من قبل ومنتهي الصلاحية. اضغط على زر 'توليد كود جديد' إذا لزم الأمر.")
                )
            }
            if (boundAccount.otpExpiresAtMillis <= 0L || nowMillis > boundAccount.otpExpiresAtMillis) {
                return Result.failure(
                    IllegalStateException("انتهت صلاحية كود التفعيل الخاص بحسابك (مضى أكثر من 24 ساعة). اضغط على زر 'توليد كود جديد'.")
                )
            }
            dao.markAccountPhoneVerified(accountId, "كود OTP", nowMillis)
            return Result.success(Unit)
        }

        // 2) التحقق الاحتياطي من سجل أكواد الإدارة المولدة مسبقاً
        val codeEntity = dao.getActivationCode(cleanCode)
            ?: return Result.failure(IllegalArgumentException("كود التفعيل غير صحيح أو غير مطابق لكود حسابك. اطلب الكود عبر واتساب أو من العيادة."))
        if (codeEntity.isUsed) {
            val usedInfo = if (codeEntity.usedByName.isNotBlank()) " (${codeEntity.usedByName})" else ""
            return Result.failure(IllegalStateException("هذا الكود تم استخدامه بالفعل ومنتهي الصلاحية$usedInfo، ولا يمكن استخدامه مجدداً."))
        }
        val redeemed = dao.redeemActivationCodeForAccount(
            code = cleanCode,
            accountId = accountId,
            usedByName = accountName.trim(),
            usedByPhone = accountPhone.trim(),
            usedAtMillis = nowMillis,
        )
        return if (redeemed) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("هذا الكود تم استخدامه بالفعل ومنتهي الصلاحية."))
        }
    }

    suspend fun searchAccountsByPhoneForClinic(rawPhoneQuery: String): List<LabPatientSearchResult> {
        val cleanText = toAsciiDigits(rawPhoneQuery).trim()
        val qDigits = normalizePhoneDigits(cleanText)
        if (qDigits.length < 3 && cleanText.length < 3) return emptyList()
        val accounts = dao.getAllAccountsOnce()
        val matched = accounts.filter { acc ->
            val accDigits = normalizePhoneDigits(acc.phone)
            (qDigits.isNotEmpty() && (accDigits.contains(qDigits) || qDigits.contains(accDigits))) ||
                toAsciiDigits(acc.phone).contains(cleanText, ignoreCase = true) ||
                acc.fullName.contains(rawPhoneQuery.trim(), ignoreCase = true)
        }
        return matched.map { acc ->
            LabPatientSearchResult(account = acc, familyMembers = dao.getFamilyMembersOnce(acc.id))
        }
    }

    suspend fun activatePatientByClinic(accountId: String, doctorName: String): Result<PatientAccountEntity> {
        val acc = dao.getAccountById(accountId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        val cleanDoc = doctorName.trim().ifEmpty { "العيادة" }
        val source = if (cleanDoc.startsWith("عيادة")) cleanDoc else "عيادة $cleanDoc"
        val now = System.currentTimeMillis()
        dao.markAccountPhoneVerified(accountId, source, now)
        val updated = dao.getAccountById(accountId) ?: acc.copy(
            isPhoneVerified = true,
            verifiedBy = source,
            verifiedAtMillis = now,
        )
        return Result.success(updated)
    }

    suspend fun setAccountVerificationByAdmin(accountId: String, verified: Boolean) {
        val now = System.currentTimeMillis()
        dao.setAccountVerificationStatus(
            accountId = accountId,
            isVerified = verified,
            verifiedBy = if (verified) "الإدارة" else "",
            verifiedAtMillis = if (verified) now else 0L,
            otpUsed = verified,
        )
        if (!verified) {
            regenerateAccountOtp(accountId, now)
        }
    }

    suspend fun updateAccountByAdmin(
        accountId: String,
        fullName: String,
        phone: String,
        newPassword: String? = null,
    ): Result<PatientAccountEntity> {
        val existing = dao.getAccountById(accountId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        val cleanName = fullName.trim()
        val cleanPhone = toAsciiDigits(phone).trim()
        if (cleanName.isEmpty() || cleanPhone.length < 6) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الاسم الكامل ورقم هاتف صحيح"))
        }
        val conflict = dao.getAllAccountsOnce().firstOrNull {
            it.id != accountId && normalizePhoneDigits(it.phone) == normalizePhoneDigits(cleanPhone)
        }
        if (conflict != null) {
            return Result.failure(IllegalArgumentException("رقم الهاتف مستخدم بالفعل لحساب عميل آخر"))
        }
        val resolvedPass = newPassword?.trim()?.takeIf { it.isNotEmpty() } ?: existing.password
        dao.updateAccountDetails(accountId, cleanName, cleanPhone, resolvedPass)
        dao.updateSelfMemberDetails(accountId, cleanName, cleanPhone)
        val updated = dao.getAccountById(accountId) ?: existing.copy(
            fullName = cleanName,
            phone = cleanPhone,
            password = resolvedPass,
        )
        return Result.success(updated)
    }

    suspend fun deleteAccountByAdmin(accountId: String) {
        dao.deleteAccountCascade(accountId)
        appContext.tebDataStore.edit { prefs ->
            if (prefs[activeAccountKey] == accountId) {
                prefs.remove(activeAccountKey)
                prefs.remove(activeMemberKey)
            }
        }
    }

    suspend fun checkLiveWebHasUpdate(url: String, previousSignature: String?): Pair<String?, Boolean> =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext previousSignature to false
            val cleanBase = url.substringBefore("?").trimEnd('/')
            val verSig = fetchRemoteVersionSignature(cleanBase)
            var conn: HttpURLConnection? = null
            try {
                val targetUrl = "$cleanBase/index.html?_t=${System.currentTimeMillis()}"
                conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 4500
                    readTimeout = 4500
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                    setRequestProperty("Pragma", "no-cache")
                }
                val code = conn.responseCode
                if (code !in 200..399 && verSig.isBlank()) {
                    return@withContext previousSignature to false
                }
                val pageSig = if (code in 200..399) {
                    "${conn.getHeaderField("ETag").orEmpty()}|${conn.getHeaderField("Last-Modified").orEmpty()}|${conn.getHeaderField("Content-Length").orEmpty()}"
                } else {
                    ""
                }
                val combinedSig = "$verSig||$pageSig"
                val hasMeaningfulSig = combinedSig.replace("|", "").isNotBlank()
                val changed = !previousSignature.isNullOrBlank() && hasMeaningfulSig && combinedSig != previousSignature
                combinedSig to changed
            } catch (_: Exception) {
                if (verSig.isNotBlank()) {
                    val changed = !previousSignature.isNullOrBlank() && verSig != previousSignature.substringBefore("||")
                    val combinedSig = "$verSig||"
                    combinedSig to changed
                } else {
                    previousSignature to false
                }
            } finally {
                conn?.disconnect()
            }
        }

    private fun fetchRemoteVersionSignature(cleanBaseUrl: String): String {
        var verConn: HttpURLConnection? = null
        return try {
            val verUrl = "$cleanBaseUrl/version.json?_t=${System.currentTimeMillis()}"
            verConn = (URL(verUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }
            if (verConn.responseCode !in 200..299) return ""
            val body = verConn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val version = json.optString("version", "1.0.0")
            val versionCode = json.optInt("versionCode", 1)
            val updatedAt = json.optString("updatedAt", "")
            "v:$version:$versionCode:$updatedAt"
        } catch (_: Exception) {
            ""
        } finally {
            verConn?.disconnect()
        }
    }

    suspend fun checkAppVersionStatus(
        liveBaseUrl: String,
        fallbackApkUrl: String,
        installedVersion: String = BuildConfig.VERSION_NAME,
        installedVersionCode: Int = BuildConfig.VERSION_CODE,
    ): RemoteVersionCheckResult = withContext(Dispatchers.IO) {
        val cleanBase = liveBaseUrl.substringBefore("?").trimEnd('/')
        if (cleanBase.isBlank()) {
            return@withContext RemoteVersionCheckResult(
                hasNewerApk = false,
                latestVersion = installedVersion,
                latestVersionCode = installedVersionCode,
                apkDownloadUrl = fallbackApkUrl,
                reachable = false,
            )
        }
        var verConn: HttpURLConnection? = null
        try {
            val verUrl = "$cleanBase/version.json?_t=${System.currentTimeMillis()}"
            verConn = (URL(verUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }
            if (verConn.responseCode !in 200..299) {
                return@withContext RemoteVersionCheckResult(
                    hasNewerApk = false,
                    latestVersion = installedVersion,
                    latestVersionCode = installedVersionCode,
                    apkDownloadUrl = fallbackApkUrl,
                    reachable = false,
                )
            }
            val body = verConn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val remoteVersion = json.optString("version", json.optString("versionName", installedVersion))
                .removePrefix("v")
                .removePrefix("V")
                .trim()
                .ifEmpty { installedVersion }
            val remoteCode = json.optInt("versionCode", installedVersionCode)
            val apkUrl = json.optString("apkDownloadUrl", fallbackApkUrl).trim().ifEmpty { fallbackApkUrl }
            val hasNewer = remoteCode > installedVersionCode ||
                compareAppVersions(remoteVersion, installedVersion) > 0
            RemoteVersionCheckResult(
                hasNewerApk = hasNewer,
                latestVersion = remoteVersion,
                latestVersionCode = remoteCode,
                apkDownloadUrl = apkUrl,
                reachable = true,
            )
        } catch (_: Exception) {
            RemoteVersionCheckResult(
                hasNewerApk = false,
                latestVersion = installedVersion,
                latestVersionCode = installedVersionCode,
                apkDownloadUrl = fallbackApkUrl,
                reachable = false,
            )
        } finally {
            verConn?.disconnect()
        }
    }

    internal fun compareAppVersions(remote: String, installed: String): Int {
        val rParts = remote.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
        val iParts = installed.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(rParts.size, iParts.size)
        for (idx in 0 until len) {
            val rv = rParts.getOrElse(idx) { 0 }
            val iv = iParts.getOrElse(idx) { 0 }
            if (rv > iv) return 1
            if (rv < iv) return -1
        }
        return 0
    }

    suspend fun loginDoctor(identifier: String, password: String): Result<ClinicDoctorEntity> {
        val cleanUser = identifier.trim()
        val cleanPass = password.trim()
        if (cleanUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم أو رقم الهاتف"))
        }
        val doc = dao.findDoctorByUsername(cleanUser)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على حساب طبيب بهذا الاسم أو الرقم"))
        if (doc.password != cleanPass) {
            return Result.failure(IllegalArgumentException("كلمة المرور غير صحيحة"))
        }
        appContext.tebDataStore.edit { prefs ->
            prefs[loggedInDoctorIdKey] = doc.id
            prefs[activeDoctorPortalKey] = doc.id
        }
        return Result.success(doc)
    }

    suspend fun logoutDoctor() {
        appContext.tebDataStore.edit { prefs ->
            prefs.remove(loggedInDoctorIdKey)
        }
    }

    suspend fun addLabByAdmin(
        labName: String,
        phone: String,
        locationLabel: String,
        username: String,
        password: String,
    ): Result<MedicalLabEntity> {
        val cleanLab = labName.trim()
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanLab.isEmpty() || cleanUser.isEmpty() || cleanPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المعمل واسم المستخدم وكلمة المرور"))
        }
        if (dao.findLabByUsername(cleanUser) != null) {
            return Result.failure(IllegalArgumentException("اسم المستخدم مسجل بالفعل لمعمل آخر"))
        }
        val lab = MedicalLabEntity(
            id = "lab_${UUID.randomUUID()}",
            labName = cleanLab,
            phone = phone.trim(),
            locationLabel = locationLabel.trim().ifEmpty { "قرية الحجايزة" },
            username = cleanUser,
            password = cleanPass,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertLab(lab)
        return Result.success(lab)
    }

    suspend fun updateLabByAdmin(
        labId: String,
        labName: String,
        phone: String,
        locationLabel: String,
        username: String,
        password: String,
    ): Result<MedicalLabEntity> {
        val existing = dao.getLabById(labId)
            ?: return Result.failure(IllegalStateException("المعمل غير موجود"))
        val cleanLab = labName.trim()
        val cleanUser = username.trim()
        val cleanPass = password.trim().ifEmpty { existing.password }
        if (cleanLab.isEmpty() || cleanUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المعمل واسم المستخدم"))
        }
        val conflict = dao.findLabByUsername(cleanUser)
        if (conflict != null && conflict.id != labId) {
            return Result.failure(IllegalArgumentException("اسم المستخدم مسجل بالفعل لمعمل آخر"))
        }
        val updated = existing.copy(
            labName = cleanLab,
            phone = phone.trim(),
            locationLabel = locationLabel.trim().ifEmpty { existing.locationLabel },
            username = cleanUser,
            password = cleanPass,
        )
        dao.updateLab(updated)
        return Result.success(updated)
    }

    suspend fun deleteLabByAdmin(labId: String) {
        dao.deleteLabById(labId)
        appContext.tebDataStore.edit { prefs ->
            if (prefs[loggedInLabIdKey] == labId) {
                prefs.remove(loggedInLabIdKey)
            }
        }
    }

    suspend fun loginLab(username: String, password: String): Result<MedicalLabEntity> {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور للمعمل"))
        }
        val lab = dao.findLabByUsername(cleanUser)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على معمل تحاليل بهذا الاسم"))
        if (lab.password != cleanPass) {
            return Result.failure(IllegalArgumentException("كلمة مرور المعمل غير صحيحة"))
        }
        appContext.tebDataStore.edit { prefs ->
            prefs[loggedInLabIdKey] = lab.id
        }
        return Result.success(lab)
    }

    suspend fun logoutLab() {
        appContext.tebDataStore.edit { prefs ->
            prefs.remove(loggedInLabIdKey)
        }
    }

    suspend fun searchPatientsByPhoneForLab(rawPhoneQuery: String): List<LabPatientSearchResult> {
        val qDigits = normalizePhoneDigits(rawPhoneQuery)
        val qText = rawPhoneQuery.trim()
        if (qDigits.length < 4 && qText.length < 4) return emptyList()
        val accounts = dao.getAllAccountsOnce()
        val matched = accounts.filter { acc ->
            val accDigits = normalizePhoneDigits(acc.phone)
            (qDigits.isNotEmpty() && (accDigits.contains(qDigits) || qDigits.contains(accDigits))) ||
                acc.phone.contains(qText, ignoreCase = true)
        }
        return matched.map { acc ->
            val members = dao.getFamilyMembersOnce(acc.id)
            LabPatientSearchResult(account = acc, familyMembers = members)
        }
    }

    suspend fun addDoctorByAdmin(
        doctorName: String,
        specialty: String,
        clinicName: String,
        clinicType: String,
        locationLabel: String,
        bio: String,
        mapsUrl: String,
        feeEgp: Int,
        workStartMinutes: Int,
        workEndMinutes: Int,
        slotDurationMinutes: Int,
        username: String,
        password: String,
        additionalSpecialties: String = "",
        doctorNameEn: String = "",
        clinicNameEn: String = "",
        locationLabelEn: String = "",
    ): Result<ClinicDoctorEntity> {
        val cleanUser = username.trim()
        val cleanName = doctorName.trim()
        val cleanSpec = specialty.trim()
        if (cleanName.isEmpty() || cleanSpec.isEmpty() || cleanUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الدكتور والتخصص واسم المستخدم/الهاتف"))
        }
        if (workEndMinutes <= workStartMinutes) {
            return Result.failure(IllegalArgumentException("وقت النهاية يجب أن يكون بعد وقت البداية"))
        }
        val existing = dao.findDoctorByUsername(cleanUser)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("اسم المستخدم أو الهاتف مسجل بالفعل لطبيب آخر"))
        }
        val resolvedClinicName = clinicName.trim().ifEmpty {
            if (clinicType == ClinicType.HOSPITAL) "${AppConfig.defaultBrand.hospitalNameAr} - $cleanSpec"
            else "عيادة $cleanName"
        }
        val doctor = ClinicDoctorEntity(
            id = "doc_${UUID.randomUUID()}",
            doctorName = cleanName,
            specialty = cleanSpec,
            clinicName = resolvedClinicName,
            clinicType = clinicType,
            locationLabel = locationLabel.trim().ifEmpty { "قرية الحجايزة" },
            feeEgp = feeEgp.coerceAtLeast(0),
            workStartMinutes = workStartMinutes,
            workEndMinutes = workEndMinutes,
            slotDurationMinutes = slotDurationMinutes,
            currentDelayMinutes = 0,
            rating = 4.9f,
            experienceYears = 10,
            bio = bio.trim(),
            mapsUrl = mapsUrl.trim(),
            username = cleanUser,
            password = password.trim().ifEmpty { "1234" },
            doctorNameEn = doctorNameEn.trim(),
            clinicNameEn = clinicNameEn.trim(),
            locationLabelEn = locationLabelEn.trim(),
            additionalSpecialties = additionalSpecialties.trim(),
        )
        dao.insertDoctor(doctor)
        val initialSlots = buildSlotsForDoctor(doctor)
        dao.insertSlots(initialSlots)
        return Result.success(doctor)
    }

    suspend fun updateDoctorByAdmin(
        doctorId: String,
        doctorName: String,
        specialty: String,
        clinicName: String,
        clinicType: String,
        locationLabel: String,
        bio: String,
        mapsUrl: String,
        feeEgp: Int,
        workStartMinutes: Int,
        workEndMinutes: Int,
        slotDurationMinutes: Int,
        username: String,
        password: String,
        additionalSpecialties: String = "",
        doctorNameEn: String = "",
        clinicNameEn: String = "",
        locationLabelEn: String = "",
    ): Result<ClinicDoctorEntity> {
        val existingDoc = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("الدكتور غير موجود"))
        val cleanUser = username.trim()
        val cleanName = doctorName.trim()
        val cleanSpec = specialty.trim()
        if (cleanName.isEmpty() || cleanSpec.isEmpty() || cleanUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الدكتور والتخصص واسم المستخدم/الهاتف"))
        }
        if (workEndMinutes <= workStartMinutes) {
            return Result.failure(IllegalArgumentException("وقت النهاية يجب أن يكون بعد وقت البداية"))
        }
        val conflict = dao.findDoctorByUsername(cleanUser)
        if (conflict != null && conflict.id != doctorId) {
            return Result.failure(IllegalArgumentException("اسم المستخدم أو الهاتف مسجل بالفعل لطبيب آخر"))
        }
        val scheduleChanged = existingDoc.workStartMinutes != workStartMinutes ||
            existingDoc.workEndMinutes != workEndMinutes ||
            existingDoc.slotDurationMinutes != slotDurationMinutes

        val updated = existingDoc.copy(
            doctorName = cleanName,
            specialty = cleanSpec,
            clinicName = clinicName.trim().ifEmpty { existingDoc.clinicName },
            clinicType = clinicType,
            locationLabel = locationLabel.trim().ifEmpty { existingDoc.locationLabel },
            bio = bio.trim(),
            mapsUrl = mapsUrl.trim(),
            feeEgp = feeEgp.coerceAtLeast(0),
            workStartMinutes = workStartMinutes,
            workEndMinutes = workEndMinutes,
            slotDurationMinutes = slotDurationMinutes,
            currentDelayMinutes = if (scheduleChanged) 0 else existingDoc.currentDelayMinutes,
            username = cleanUser,
            password = password.trim().ifEmpty { existingDoc.password },
            doctorNameEn = doctorNameEn.trim().ifEmpty { existingDoc.doctorNameEn },
            clinicNameEn = clinicNameEn.trim().ifEmpty { existingDoc.clinicNameEn },
            locationLabelEn = locationLabelEn.trim().ifEmpty { existingDoc.locationLabelEn },
            additionalSpecialties = additionalSpecialties.trim(),
        )
        dao.updateDoctor(updated)
        if (scheduleChanged) {
            val newSlots = buildSlotsForDoctor(updated)
            dao.replaceDoctorSchedule(
                doctorId = doctorId,
                startMin = workStartMinutes,
                endMin = workEndMinutes,
                durationMin = slotDurationMinutes,
                newSlots = newSlots,
            )
        }
        return Result.success(updated)
    }

    suspend fun deleteDoctorByAdmin(doctorId: String) {
        dao.deleteDoctorCascade(doctorId)
        appContext.tebDataStore.edit { prefs ->
            if (prefs[loggedInDoctorIdKey] == doctorId) {
                prefs.remove(loggedInDoctorIdKey)
            }
        }
    }

    suspend fun toggleFavoriteMedication(doctorId: String, medicationId: String) {
        if (dao.isMedicationFavorite(doctorId, medicationId) > 0) {
            dao.deleteFavoriteMed(doctorId, medicationId)
        } else {
            dao.insertFavoriteMed(DoctorFavoriteMedEntity(doctorId, medicationId))
        }
    }

    suspend fun loadEgyptianMedications(apiUrl: String): MedicationFetchResult {
        return EgyptianMedicationService.fetchMedications(apiUrl)
    }

    /**
     * تهيئة نظيفة لبيئة الإنتاج:
     * تبدأ بدون أي مرضى أو سجلات وهمية، وتُدرج لمرة واحدة فقط بيانات الطبيبين الحقيقيين بقرية الحجايزة
     * (د. محمد فكري ود. شادي) مع قابلية التعديل والحذف من لوحة الإدارة دون إعادة فرضهما.
     */
    suspend fun ensureSeeded() {
        seedMutex.withLock {
            val existingAccounts = dao.getAllAccountsOnce()
            val prefs = appContext.tebDataStore.data.first()
            if (prefs[seededRealDoctorsKey] != true) {
                val baselineDoctors = listOf(
                    ClinicDoctorEntity(
                        id = "doc_fekry_hajayzah",
                        doctorName = "د. محمد فكري",
                        doctorNameEn = "Dr. Mohamed Fekry",
                        specialty = "النساء والتوليد وتأخر الإنجاب",
                        clinicName = "عيادة د. محمد فكري للنساء والتوليد",
                        clinicNameEn = "Dr. Mohamed Fekry OB/GYN Clinic",
                        clinicType = ClinicType.PRIVATE,
                        locationLabel = "قرية الحجايزة",
                        locationLabelEn = "Al-Hajayzah Village",
                        feeEgp = 150,
                        workStartMinutes = 17 * 60,
                        workEndMinutes = 21 * 60,
                        slotDurationMinutes = 20,
                        currentDelayMinutes = 0,
                        rating = 4.9f,
                        experienceYears = 12,
                        bio = "أخصائي النساء والتوليد وتأخر الإنجاب ومتابعة الحمل",
                        mapsUrl = "",
                        username = "dr_fekry",
                        password = "1234",
                        additionalSpecialties = "",
                    ),
                    ClinicDoctorEntity(
                        id = "doc_shady_hajayzah",
                        doctorName = "د. شادي",
                        doctorNameEn = "Dr. Shady",
                        specialty = "طب الأطفال وحديثي الولادة",
                        clinicName = "عيادة د. شادي لطب الأطفال",
                        clinicNameEn = "Dr. Shady Pediatrics Clinic",
                        clinicType = ClinicType.PRIVATE,
                        locationLabel = "قرية الحجايزة",
                        locationLabelEn = "Al-Hajayzah Village",
                        feeEgp = 150,
                        workStartMinutes = 17 * 60,
                        workEndMinutes = 21 * 60,
                        slotDurationMinutes = 20,
                        currentDelayMinutes = 0,
                        rating = 4.9f,
                        experienceYears = 10,
                        bio = "أخصائي طب الأطفال وحديثي الولادة ومتابعة النمو",
                        mapsUrl = "",
                        username = "dr_shady",
                        password = "1234",
                        additionalSpecialties = "",
                    ),
                )
                baselineDoctors.forEach { baseDoc ->
                    if (dao.getDoctorById(baseDoc.id) == null && dao.findDoctorByUsername(baseDoc.username) == null) {
                        dao.insertDoctor(baseDoc)
                        dao.insertSlots(buildSlotsForDoctor(baseDoc))
                    }
                }
                appContext.tebDataStore.edit { editPrefs ->
                    editPrefs[seededRealDoctorsKey] = true
                }
            }

            val savedAccountId = prefs[activeAccountKey].orEmpty()
            val savedDoctorId = prefs[loggedInDoctorIdKey].orEmpty()
            val savedLabId = prefs[loggedInLabIdKey].orEmpty()
            val accountMissing = savedAccountId.isNotEmpty() && existingAccounts.none { it.id == savedAccountId }
            val doctorMissing = savedDoctorId.isNotEmpty() && dao.getDoctorById(savedDoctorId) == null
            val labMissing = savedLabId.isNotEmpty() && dao.getLabById(savedLabId) == null
            if (accountMissing || doctorMissing || labMissing) {
                appContext.tebDataStore.edit { editPrefs ->
                    if (accountMissing) {
                        editPrefs.remove(activeAccountKey)
                        editPrefs.remove(activeMemberKey)
                    }
                    if (doctorMissing) editPrefs.remove(loggedInDoctorIdKey)
                    if (labMissing) editPrefs.remove(loggedInLabIdKey)
                }
            }
            existingAccounts.forEach { acc ->
                if (!acc.isPhoneVerified && acc.otpCode.isBlank()) {
                    regenerateAccountOtp(acc.id)
                }
            }
            if (dao.getAllActivationCodesOnce().isEmpty()) {
                generateActivationCodeBatch(5, "SYSTEM")
            }
        }
    }

    private fun buildSlotsForDoctor(doctor: ClinicDoctorEntity): List<AppointmentSlotEntity> {
        val list = mutableListOf<AppointmentSlotEntity>()
        var current = doctor.workStartMinutes
        var index = 0
        while (current < doctor.workEndMinutes) {
            list += AppointmentSlotEntity(
                id = "${doctor.id}_today_$index",
                doctorId = doctor.id,
                dateLabel = "اليوم",
                startMinutes = current,
                delayMinutes = 0,
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
            current += doctor.slotDurationMinutes
            index++
        }
        return list
    }

    fun currentLocalMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    suspend fun bookSlot(
        slotId: String,
        patientName: String,
        patientPhone: String,
        familyMemberId: String,
        source: String,
    ): Result<Unit> {
        val cleanPhone = patientPhone.trim()
        if (cleanPhone.isNotEmpty() && dao.isPhoneBlocked(cleanPhone) > 0) {
            return Result.failure(IllegalStateException("عذراً، هذا الرقم محظور من الحجز عبر المنصة"))
        }
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الموعد غير موجود"))
        if (slot.status != SlotStatus.AVAILABLE) {
            return Result.failure(IllegalStateException("عذراً، تم حجز هذا الموعد للتو"))
        }
        dao.updateSlot(
            slot.copy(
                status = SlotStatus.BOOKED,
                patientName = patientName.trim(),
                patientPhone = cleanPhone,
                familyMemberId = familyMemberId,
                bookingSource = source,
                bookedAtMillis = System.currentTimeMillis(),
            )
        )
        val doc = dao.getDoctorById(slot.doctorId)
        val formattedTime = TimeUtils.formatMinutesAr(slot.effectiveMinutes, false)
        dispatchWorkerPushAsync(
            eventType = "BOOKING_CONFIRMED",
            title = "تأكيد حجز جديد — ${doc?.clinicName.orEmpty()}",
            body = "تم حجز موعد الساعة $formattedTime للمريض ${patientName.trim()}.",
            targetAudience = NotificationAudience.CLINIC,
            targetId = slot.doctorId,
            namespacePath = CloudNamespacePaths.APPOINTMENTS,
        )
        return Result.success(Unit)
    }

    suspend fun cancelSlot(slotId: String) {
        cancelSlotByClinic(slotId)
    }

    suspend fun cancelSlotByPatient(
        slotId: String,
        nowMinutesOfDay: Int = currentLocalMinutesOfDay(),
        enforceTwoHourRule: Boolean = true,
    ): Result<Unit> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الموعد غير موجود"))
        if (slot.status != SlotStatus.BOOKED) {
            return Result.failure(IllegalStateException("هذا الموعد غير محجوز حالياً"))
        }
        val minutesRemaining = slot.effectiveMinutes - nowMinutesOfDay
        if (enforceTwoHourRule && minutesRemaining < PATIENT_MODIFY_CUTOFF_MINUTES) {
            return Result.failure(
                IllegalStateException("عذراً، لا يمكن إلغاء الموعد قبل الكشف بأقل من ساعتين — يرجى التواصل مع سكرتيرة العيادة.")
            )
        }
        val doctor = dao.getDoctorById(slot.doctorId)
        val oldTime = TimeUtils.formatMinutesAr(slot.effectiveMinutes, false)
        val oldPatientName = slot.patientName
        dao.updateSlot(
            slot.copy(
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
        )
        val notifMsg = "إلغاء حجز من المريض: قام $oldPatientName بإلغاء موعد الساعة $oldTime وإتاحته مجدداً."
        dao.insertNotification(
            DelayNotificationEntity(
                doctorId = slot.doctorId,
                doctorName = doctor?.doctorName.orEmpty(),
                clinicName = doctor?.clinicName.orEmpty(),
                delayMinutes = 0,
                message = notifMsg,
                targetAudience = NotificationAudience.CLINIC,
                targetId = slot.doctorId,
                senderLabel = oldPatientName.ifBlank { "مريض" },
            )
        )
        dispatchWorkerPushAsync(
            eventType = "PATIENT_CANCELLED",
            title = "إلغاء موعد — ${doctor?.clinicName.orEmpty()}",
            body = notifMsg,
            targetAudience = NotificationAudience.CLINIC,
            targetId = slot.doctorId,
            namespacePath = CloudNamespacePaths.NOTIFICATIONS,
        )
        return Result.success(Unit)
    }

    suspend fun rescheduleSlotByPatient(
        fromSlotId: String,
        toSlotId: String,
        nowMinutesOfDay: Int = currentLocalMinutesOfDay(),
        enforceTwoHourRule: Boolean = true,
    ): Result<Unit> {
        if (fromSlotId == toSlotId) {
            return Result.failure(IllegalArgumentException("يرجى اختيار موعد جديد مختلف"))
        }
        val fromSlot = dao.getSlotById(fromSlotId)
            ?: return Result.failure(IllegalStateException("الموعد الحالي غير موجود"))
        val toSlot = dao.getSlotById(toSlotId)
            ?: return Result.failure(IllegalStateException("الموعد الجديد غير موجود"))
        if (fromSlot.status != SlotStatus.BOOKED) {
            return Result.failure(IllegalStateException("الموعد الحالي غير محجوز"))
        }
        if (toSlot.doctorId != fromSlot.doctorId) {
            return Result.failure(IllegalArgumentException("يجب اختيار موعد بديل في نفس العيادة"))
        }
        if (toSlot.status != SlotStatus.AVAILABLE) {
            return Result.failure(IllegalStateException("عذراً، الموعد الجديد تم حجزه للتو"))
        }
        val minutesRemaining = fromSlot.effectiveMinutes - nowMinutesOfDay
        if (enforceTwoHourRule && minutesRemaining < PATIENT_MODIFY_CUTOFF_MINUTES) {
            return Result.failure(
                IllegalStateException("عذراً، لا يمكن تعديل الموعد قبل الكشف بأقل من ساعتين — يرجى التواصل مع سكرتيرة العيادة.")
            )
        }
        val doctor = dao.getDoctorById(fromSlot.doctorId)
        val oldTime = TimeUtils.formatMinutesAr(fromSlot.effectiveMinutes, false)
        val newTime = TimeUtils.formatMinutesAr(toSlot.effectiveMinutes, false)
        val patientName = fromSlot.patientName

        dao.updateSlot(
            toSlot.copy(
                status = SlotStatus.BOOKED,
                patientName = fromSlot.patientName,
                patientPhone = fromSlot.patientPhone,
                familyMemberId = fromSlot.familyMemberId,
                bookingSource = fromSlot.bookingSource,
                bookedAtMillis = System.currentTimeMillis(),
            )
        )
        dao.updateSlot(
            fromSlot.copy(
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
        )
        val notifMsg = "تعديل موعد من المريض: نقل $patientName حجزه من الساعة $oldTime إلى الساعة $newTime."
        dao.insertNotification(
            DelayNotificationEntity(
                doctorId = fromSlot.doctorId,
                doctorName = doctor?.doctorName.orEmpty(),
                clinicName = doctor?.clinicName.orEmpty(),
                delayMinutes = 0,
                message = notifMsg,
                targetAudience = NotificationAudience.CLINIC,
                targetId = fromSlot.doctorId,
                senderLabel = patientName.ifBlank { "مريض" },
            )
        )
        dispatchWorkerPushAsync(
            eventType = "PATIENT_RESCHEDULED",
            title = "تعديل موعد — ${doctor?.clinicName.orEmpty()}",
            body = notifMsg,
            targetAudience = NotificationAudience.CLINIC,
            targetId = fromSlot.doctorId,
            namespacePath = CloudNamespacePaths.NOTIFICATIONS,
        )
        return Result.success(Unit)
    }

    suspend fun cancelSlotByClinic(slotId: String): Result<Unit> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الموعد غير موجود"))
        if (slot.status != SlotStatus.BOOKED) {
            return Result.success(Unit)
        }
        val doctor = dao.getDoctorById(slot.doctorId)
        val oldTime = TimeUtils.formatMinutesAr(slot.effectiveMinutes, false)
        val targetPatientId = slot.familyMemberId.ifBlank { slot.patientPhone }
        val oldPatientName = slot.patientName
        dao.updateSlot(
            slot.copy(
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
        )
        if (targetPatientId.isNotBlank()) {
            val notifMsg = "إشعار من ${doctor?.clinicName ?: "العيادة"}: تم إلغاء حجز موعدك الساعة $oldTime ($oldPatientName). يمكنك الحجز في موعد آخر متاح."
            dao.insertNotification(
                DelayNotificationEntity(
                    doctorId = slot.doctorId,
                    doctorName = doctor?.doctorName.orEmpty(),
                    clinicName = doctor?.clinicName.orEmpty(),
                    delayMinutes = 0,
                    message = notifMsg,
                    targetAudience = NotificationAudience.PATIENT,
                    targetId = targetPatientId,
                    senderLabel = doctor?.clinicName.orEmpty(),
                )
            )
            dispatchWorkerPushAsync(
                eventType = "CLINIC_CANCELLED",
                title = "تحديث الحجز — ${doctor?.clinicName.orEmpty()}",
                body = notifMsg,
                targetAudience = NotificationAudience.PATIENT,
                targetId = targetPatientId,
                namespacePath = CloudNamespacePaths.NOTIFICATIONS,
            )
        }
        return Result.success(Unit)
    }

    suspend fun rescheduleSlotByClinic(
        fromSlotId: String,
        toSlotId: String,
    ): Result<Unit> {
        if (fromSlotId == toSlotId) {
            return Result.failure(IllegalArgumentException("يرجى اختيار موعد جديد مختلف"))
        }
        val fromSlot = dao.getSlotById(fromSlotId)
            ?: return Result.failure(IllegalStateException("الموعد الحالي غير موجود"))
        val toSlot = dao.getSlotById(toSlotId)
            ?: return Result.failure(IllegalStateException("الموعد الجديد غير موجود"))
        if (fromSlot.status != SlotStatus.BOOKED) {
            return Result.failure(IllegalStateException("الموعد الحالي غير محجوز"))
        }
        if (toSlot.doctorId != fromSlot.doctorId) {
            return Result.failure(IllegalArgumentException("يجب اختيار موعد بديل في نفس العيادة"))
        }
        if (toSlot.status != SlotStatus.AVAILABLE) {
            return Result.failure(IllegalStateException("عذراً، الموعد الجديد غير متاح"))
        }
        val doctor = dao.getDoctorById(fromSlot.doctorId)
        val oldTime = TimeUtils.formatMinutesAr(fromSlot.effectiveMinutes, false)
        val newTime = TimeUtils.formatMinutesAr(toSlot.effectiveMinutes, false)
        val targetPatientId = fromSlot.familyMemberId.ifBlank { fromSlot.patientPhone }

        dao.updateSlot(
            toSlot.copy(
                status = SlotStatus.BOOKED,
                patientName = fromSlot.patientName,
                patientPhone = fromSlot.patientPhone,
                familyMemberId = fromSlot.familyMemberId,
                bookingSource = fromSlot.bookingSource,
                bookedAtMillis = System.currentTimeMillis(),
            )
        )
        dao.updateSlot(
            fromSlot.copy(
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
        )
        if (targetPatientId.isNotBlank()) {
            val notifMsg = "تحديث من ${doctor?.clinicName ?: "العيادة"}: تم تعديل موعد ${fromSlot.patientName} من الساعة $oldTime إلى الساعة $newTime."
            dao.insertNotification(
                DelayNotificationEntity(
                    doctorId = fromSlot.doctorId,
                    doctorName = doctor?.doctorName.orEmpty(),
                    clinicName = doctor?.clinicName.orEmpty(),
                    delayMinutes = 0,
                    message = notifMsg,
                    targetAudience = NotificationAudience.PATIENT,
                    targetId = targetPatientId,
                    senderLabel = doctor?.clinicName.orEmpty(),
                )
            )
            dispatchWorkerPushAsync(
                eventType = "CLINIC_RESCHEDULED",
                title = "تعديل موعدك — ${doctor?.clinicName.orEmpty()}",
                body = notifMsg,
                targetAudience = NotificationAudience.PATIENT,
                targetId = targetPatientId,
                namespacePath = CloudNamespacePaths.NOTIFICATIONS,
            )
        }
        return Result.success(Unit)
    }

    private suspend fun dispatchWorkerPushAsync(
        eventType: String,
        title: String,
        body: String,
        targetAudience: String,
        targetId: String,
        namespacePath: String,
    ) {
        val brand = runCatching { brandConfigFlow.first() }.getOrNull() ?: return
        val urlStr = brand.workerUrl.trim()
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) return
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4000
                    readTimeout = 4000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val payload = JSONObject().apply {
                    put("namespace", CloudNamespacePaths.ROOT)
                    put("collectionPath", namespacePath)
                    put("eventType", eventType)
                    put("title", title)
                    put("body", body)
                    put("targetAudience", targetAudience)
                    put("targetId", targetId)
                    put("projectId", brand.firebaseProjectId)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            }
        }
    }

    suspend fun generateDoctorSchedule(
        doctorId: String,
        startMinutes: Int,
        endMinutes: Int,
        durationMinutes: Int,
    ): Result<Int> {
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        if (endMinutes <= startMinutes) {
            return Result.failure(IllegalArgumentException("وقت النهاية يجب أن يكون بعد وقت البداية"))
        }
        val updatedDoctor = doctor.copy(
            workStartMinutes = startMinutes,
            workEndMinutes = endMinutes,
            slotDurationMinutes = durationMinutes,
            currentDelayMinutes = 0,
        )
        val newSlots = buildSlotsForDoctor(updatedDoctor)
        dao.replaceDoctorSchedule(
            doctorId = doctorId,
            startMin = startMinutes,
            endMin = endMinutes,
            durationMin = durationMinutes,
            newSlots = newSlots,
        )
        return Result.success(newSlots.size)
    }

    suspend fun delayDoctorAppointments(doctorId: String, additionalMinutes: Int): Result<Unit> {
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        dao.addDoctorDelay(doctorId, additionalMinutes)
        dao.shiftUpcomingSlotsDelay(doctorId, additionalMinutes)
        val newTotalDelay = doctor.currentDelayMinutes + additionalMinutes
        dao.insertNotification(
            DelayNotificationEntity(
                doctorId = doctorId,
                doctorName = doctor.doctorName,
                clinicName = doctor.clinicName,
                delayMinutes = additionalMinutes,
                message = "تنبيه عاجل: تم تأخير مواعيد ${doctor.doctorName} (${doctor.clinicName}) لمدة $additionalMinutes دقيقة (إجمالي التأخير: $newTotalDelay دقيقة). تم تحديث موعدك تلقائياً.",
                targetAudience = NotificationAudience.ALL,
                targetId = "",
                senderLabel = doctor.clinicName,
            )
        )
        return Result.success(Unit)
    }

    suspend fun completeVisitAndSavePrescription(
        slotId: String,
        diagnosis: String,
        medications: String,
        notes: String,
        attachmentPath: String,
        diagnosisEn: String = "",
        medicationsEn: String = "",
        notesEn: String = "",
        aiReportAr: String = "",
        aiReportEn: String = "",
    ): Result<MedicalRecordEntity> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الحجز غير موجود"))
        val doctor = dao.getDoctorById(slot.doctorId)
            ?: return Result.failure(IllegalStateException("الطبيب غير موجود"))

        val bundle = BilingualMedicalTranslator.buildBilingualBundle(
            patientName = slot.patientName,
            doctorName = doctor.doctorName,
            specialty = doctor.specialty,
            diagnosisInput = diagnosis,
            medicationsInput = medications,
            notesInput = notes,
            existingDiagnosisEn = diagnosisEn,
            existingMedicationsEn = medicationsEn,
            existingNotesEn = notesEn,
        )

        dao.updateSlot(slot.copy(status = SlotStatus.COMPLETED))
        val record = MedicalRecordEntity(
            id = "rec_${UUID.randomUUID()}",
            familyMemberId = slot.familyMemberId,
            patientName = slot.patientName,
            patientPhone = slot.patientPhone,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            specialty = doctor.specialty,
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = bundle.diagnosisAr,
            medications = bundle.medicationsAr.ifBlank { medications.trim() },
            notes = bundle.notesAr.ifBlank { notes.trim() },
            attachmentPath = attachmentPath,
            attachmentMimeType = if (attachmentPath.isNotBlank()) "image/jpeg" else "",
            attachmentName = if (attachmentPath.isNotBlank()) "روشتة_ورقية.jpg" else "",
            recordType = MedicalRecordType.PRESCRIPTION,
            createdAtMillis = System.currentTimeMillis(),
            diagnosisEn = diagnosisEn.trim().ifBlank { bundle.diagnosisEn },
            medicationsEn = medicationsEn.trim().ifBlank { bundle.medicationsEn },
            notesEn = notesEn.trim().ifBlank { bundle.notesEn },
            aiReportAr = aiReportAr.trim().ifBlank { bundle.aiReportAr },
            aiReportEn = aiReportEn.trim().ifBlank { bundle.aiReportEn },
        )
        dao.insertMedicalRecord(record)
        return Result.success(record)
    }

    suspend fun addPatientUploadedAttachment(
        familyMemberId: String,
        patientName: String,
        patientPhone: String,
        title: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String = "image/jpeg",
        attachmentName: String = "",
    ): MedicalRecordEntity {
        val record = MedicalRecordEntity(
            id = "upload_${UUID.randomUUID()}",
            familyMemberId = familyMemberId,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = "",
            doctorName = "رفع شخصي من المريض",
            specialty = "تحاليل وأشعة",
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = title.trim().ifEmpty { "صورة / ملف تحليل أو أشعة" },
            medications = "",
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            attachmentMimeType = attachmentMimeType,
            attachmentName = attachmentName,
            recordType = MedicalRecordType.PATIENT_UPLOAD,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
        return record
    }

    suspend fun updateMemberBioAndChronic(
        memberId: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        bloodType: String,
        chronicConditions: List<String>,
        drugAllergies: String,
        otherChronicNotes: String,
    ): Result<FamilyMemberEntity> {
        val existing = dao.getFamilyMemberById(memberId)
            ?: return Result.failure(IllegalStateException("الملف الشخصي غير موجود"))
        val updated = existing.copy(
            age = age.coerceIn(0, 120),
            weightKg = weightKg.coerceIn(0.0, 400.0),
            heightCm = heightCm.coerceIn(0.0, 260.0),
            bloodType = bloodType.trim(),
            chronicConditions = ChronicConditionsCatalog.encodeKeys(chronicConditions),
            drugAllergies = drugAllergies.trim(),
            otherChronicNotes = otherChronicNotes.trim(),
        )
        dao.updateFamilyMember(updated)
        return Result.success(updated)
    }

    suspend fun refreshMemberAiSummaryWithGemini(
        memberId: String,
        focusRecordId: String? = null,
    ): Result<FamilyMemberEntity> {
        val member = dao.getFamilyMemberById(memberId)
            ?: return Result.failure(IllegalStateException("الملف الشخصي غير موجود"))
        val records = dao.getMedicalRecordsForMemberOnce(member.id, member.fullName)
        val hasBioOrChronic = member.chronicConditions.isNotBlank() ||
            member.drugAllergies.isNotBlank() ||
            member.otherChronicNotes.isNotBlank() ||
            member.weightKg > 0.0 ||
            member.heightCm > 0.0 ||
            member.bloodType.isNotBlank()
        if (records.isEmpty() && !hasBioOrChronic) {
            val cleared = member.copy(
                aiSummaryAr = "",
                aiSummaryEn = "",
                aiSummaryUpdatedAtMillis = 0L,
                aiSummaryError = "",
            )
            dao.updateFamilyMember(cleared)
            return Result.success(cleared)
        }

        val firebaseInitialized = runCatching { FirebaseApp.getApps(appContext).isNotEmpty() }.getOrDefault(false)
        val aiResult = if (firebaseInitialized) {
            geminiAnalyzer.analyzeMemberHistoryAndFiles(
                member = member,
                records = records,
                focusRecordId = focusRecordId,
            )
        } else {
            Result.failure(IllegalStateException("FirebaseApp not initialized in current runtime"))
        }

        val now = System.currentTimeMillis()
        val updated = aiResult.fold(
            onSuccess = { parsed ->
                member.copy(
                    aiSummaryAr = parsed.summaryAr,
                    aiSummaryEn = parsed.summaryEn,
                    aiSummaryUpdatedAtMillis = now,
                    aiSummaryError = "",
                )
            },
            onFailure = { err ->
                Log.w("TebRepository", "Gemini unavailable or quota exceeded; applying Smart Offline Fallback", err)
                val offlineBundle = MedicalSummaryAnalyzer.buildOfflineFallbackBundle(
                    member = member,
                    patientName = member.fullName,
                    records = records,
                )
                member.copy(
                    aiSummaryAr = offlineBundle.summaryAr,
                    aiSummaryEn = offlineBundle.summaryEn,
                    aiSummaryUpdatedAtMillis = now,
                    aiSummaryError = "",
                )
            },
        )
        dao.updateFamilyMember(updated)
        return Result.success(updated)
    }

    suspend fun deleteMedicalRecordOrAttachment(recordId: String): Result<String?> {
        val record = dao.getMedicalRecordById(recordId)
            ?: return Result.failure(IllegalStateException("السجل أو الملف غير موجود"))
        val oldPath = record.attachmentPath
        val affectedMemberId = record.familyMemberId.takeIf { it.isNotBlank() }
        if (record.recordType == MedicalRecordType.PATIENT_UPLOAD ||
            record.recordType == MedicalRecordType.LAB_RESULT
        ) {
            dao.deleteMedicalRecordById(recordId)
        } else {
            dao.clearMedicalRecordAttachment(recordId)
        }
        if (oldPath.isNotBlank() && dao.countRecordsUsingAttachmentPath(oldPath) == 0) {
            ImageStorageHelper.deleteInternalFile(appContext, oldPath)
        }
        return Result.success(affectedMemberId)
    }

    suspend fun addExternalDoctorVisitRecord(
        familyMemberId: String,
        patientName: String,
        patientPhone: String,
        doctorName: String,
        specialty: String,
        visitDateLabel: String,
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String = "",
        attachmentName: String = "",
    ): Result<MedicalRecordEntity> {
        val cleanDoc = doctorName.trim()
        val cleanDiag = diagnosis.trim()
        if (cleanDoc.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة اسم الطبيب الخارجي"))
        }
        if (cleanDiag.isEmpty() && medicationsText.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة التشخيص أو إضافة دواء واحد على الأقل"))
        }
        val record = MedicalRecordEntity(
            id = "ext_${UUID.randomUUID()}",
            familyMemberId = familyMemberId,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = "external",
            doctorName = if (cleanDoc.startsWith("د.") || cleanDoc.startsWith("د ")) cleanDoc else "د. $cleanDoc",
            specialty = "${specialty.trim().ifEmpty { "تخصصات أخرى" }} (زيارة خارجية)",
            visitDateLabel = visitDateLabel.trim().ifEmpty { TimeUtils.formatTodayLabelAr() },
            diagnosis = cleanDiag.ifEmpty { "متابعة لدى طبيب خارجي" },
            medications = medicationsText.trim(),
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            attachmentMimeType = attachmentMimeType,
            attachmentName = attachmentName,
            recordType = MedicalRecordType.EXTERNAL_VISIT,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
        return Result.success(record)
    }

    suspend fun addLabResultRecord(
        lab: MedicalLabEntity,
        member: FamilyMemberEntity,
        accountPhone: String,
        testTitle: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String,
        attachmentName: String,
    ): Result<MedicalRecordEntity> {
        val cleanTitle = testTitle.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة اسم التحليل أو الفحص"))
        }
        if (attachmentPath.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إرفاق ملف نتيجة التحليل (PDF أو صورة)"))
        }
        val record = MedicalRecordEntity(
            id = "labrec_${UUID.randomUUID()}",
            familyMemberId = member.id,
            patientName = member.fullName,
            patientPhone = accountPhone.ifBlank { member.phone },
            doctorId = lab.id,
            doctorName = lab.labName,
            specialty = "نتيجة معمل تحاليل معتمد",
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = cleanTitle,
            medications = "",
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            attachmentMimeType = attachmentMimeType,
            attachmentName = attachmentName,
            recordType = MedicalRecordType.LAB_RESULT,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
        return Result.success(record)
    }

    suspend fun addFamilyMember(
        accountId: String,
        fullName: String,
        relation: String,
        age: Int,
        phone: String,
    ): FamilyMemberEntity {
        val newMember = FamilyMemberEntity(
            id = "mem_${UUID.randomUUID()}",
            accountId = accountId,
            fullName = fullName.trim(),
            relation = relation.trim(),
            age = age,
            phone = phone.trim(),
        )
        dao.insertFamilyMember(newMember)
        setActiveMember(newMember.id)
        return newMember
    }

    suspend fun loginPatient(identifier: String, password: String): Result<PatientAccountEntity> {
        val cleanId = toAsciiDigits(identifier).trim()
        val cleanPass = password.trim()
        if (dao.isPhoneBlocked(cleanId) > 0) {
            return Result.failure(IllegalStateException("عذراً، هذا الرقم محظور من استخدام المنصة"))
        }
        val byPhone = dao.findAccountByPhone(cleanId)
        val byNationalId = if (byPhone == null) dao.findAccountByNationalId(cleanId) else null
        val account = byPhone ?: byNationalId
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على حساب بهذا الرقم"))
        if (dao.isPhoneBlocked(account.phone) > 0) {
            return Result.failure(IllegalStateException("عذراً، هذا الرقم محظور من استخدام المنصة"))
        }
        if (account.password != cleanPass && cleanPass.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("كلمة المرور غير صحيحة"))
        }
        val members = dao.observeFamilyMembers(account.id).first()
        appContext.tebDataStore.edit { prefs ->
            prefs[activeAccountKey] = account.id
            prefs[activeMemberKey] = members.firstOrNull()?.id ?: account.id
        }
        return Result.success(account)
    }

    suspend fun logoutPatient() {
        appContext.tebDataStore.edit { prefs ->
            prefs.remove(activeAccountKey)
            prefs.remove(activeMemberKey)
        }
    }

    suspend fun registerPatientAccount(
        fullName: String,
        phone: String,
        nationalId: String,
        password: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<PatientAccountEntity> {
        val cleanPhone = toAsciiDigits(phone).trim()
        if (dao.isPhoneBlocked(cleanPhone) > 0) {
            return Result.failure(IllegalStateException("عذراً، هذا الرقم محظور من التسجيل في المنصة"))
        }
        val existing = dao.findAccountByPhone(cleanPhone)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("رقم الهاتف مسجل بالفعل"))
        }
        val accountId = "acc_${UUID.randomUUID()}"
        val generatedOtp = generateUniqueUserOtpCode()
        val account = PatientAccountEntity(
            id = accountId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            nationalId = toAsciiDigits(nationalId).trim(),
            password = password.trim().ifEmpty { "1234" },
            isPhoneVerified = false,
            verifiedBy = "",
            verifiedAtMillis = 0L,
            otpCode = generatedOtp,
            otpCreatedAtMillis = nowMillis,
            otpExpiresAtMillis = nowMillis + OTP_TTL_MILLIS,
            otpUsed = false,
        )
        dao.insertAccount(account)
        val selfMemberId = "mem_${UUID.randomUUID()}"
        dao.insertFamilyMember(
            FamilyMemberEntity(
                id = selfMemberId,
                accountId = accountId,
                fullName = account.fullName,
                relation = "نفسي (صاحب الحساب)",
                age = 30,
                phone = account.phone,
            )
        )
        appContext.tebDataStore.edit { prefs ->
            prefs[activeAccountKey] = accountId
            prefs[activeMemberKey] = selfMemberId
        }
        return Result.success(account)
    }

    suspend fun markNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    suspend fun setGlobalChatEnabled(enabled: Boolean) {
        appContext.tebDataStore.edit { prefs ->
            prefs[cfgGlobalChatEnabledKey] = enabled
        }
    }

    suspend fun setClinicChatEnabled(doctorId: String, enabled: Boolean) {
        dao.updateDoctorChatEnabled(doctorId, enabled)
    }

    suspend fun sendPatientToClinicMessage(
        account: PatientAccountEntity,
        doctorId: String,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة نص الرسالة"))
        }
        val brand = brandConfigFlow.first()
        if (!brand.globalChatEnabled) {
            return Result.failure(IllegalStateException("نظام المحادثة مع العيادات معطل حالياً من الإدارة"))
        }
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        if (!doctor.chatEnabled) {
            return Result.failure(IllegalStateException("استقبال رسائل المرضى معطل لهذه العيادة حالياً"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.PATIENT_CLINIC,
            patientAccountId = account.id,
            patientName = account.fullName,
            patientPhone = account.phone,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            senderRole = MessageSenderRole.PATIENT,
            senderName = account.fullName,
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = true,
            isReadByClinic = false,
            isReadByAdmin = true,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendPatientToAdminMessage(
        account: PatientAccountEntity,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة نص الرسالة"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.PATIENT_ADMIN,
            patientAccountId = account.id,
            patientName = account.fullName,
            patientPhone = account.phone,
            doctorId = "",
            doctorName = "",
            clinicName = "",
            senderRole = MessageSenderRole.PATIENT,
            senderName = account.fullName,
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = true,
            isReadByClinic = true,
            isReadByAdmin = false,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendClinicReplyToPatient(
        doctor: ClinicDoctorEntity,
        patientAccountId: String,
        patientName: String,
        patientPhone: String,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الرد أولاً"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.PATIENT_CLINIC,
            patientAccountId = patientAccountId,
            patientName = patientName.ifBlank { "مريض" },
            patientPhone = patientPhone,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            senderRole = MessageSenderRole.CLINIC,
            senderName = "${doctor.doctorName} — ${doctor.clinicName}",
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = false,
            isReadByClinic = true,
            isReadByAdmin = true,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendClinicToPatientAlert(
        doctor: ClinicDoctorEntity,
        patientAccountId: String,
        patientName: String,
        message: String,
    ): Result<DelayNotificationEntity> {
        val cleanMsg = message.trim()
        if (cleanMsg.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة نص التنبيه للمريض"))
        }
        val entity = DelayNotificationEntity(
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            delayMinutes = 0,
            message = "تنبيه من ${doctor.clinicName}: $cleanMsg",
            createdAtMillis = System.currentTimeMillis(),
            isRead = false,
            targetAudience = NotificationAudience.PATIENT,
            targetId = patientAccountId,
            senderLabel = doctor.clinicName,
        )
        val newId = dao.insertNotification(entity)
        return Result.success(entity.copy(id = newId))
    }

    suspend fun sendClinicToAdminMessage(
        doctor: ClinicDoctorEntity,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الرسالة للإدارة"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.CLINIC_ADMIN,
            patientAccountId = "",
            patientName = "",
            patientPhone = doctor.username,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            senderRole = MessageSenderRole.CLINIC,
            senderName = "${doctor.doctorName} (${doctor.clinicName})",
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = true,
            isReadByClinic = true,
            isReadByAdmin = false,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendAdminReplyToPatient(
        patientAccountId: String,
        patientName: String,
        patientPhone: String,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الرد أولاً"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.PATIENT_ADMIN,
            patientAccountId = patientAccountId,
            patientName = patientName.ifBlank { "مريض" },
            patientPhone = patientPhone,
            doctorId = "",
            doctorName = "",
            clinicName = "",
            senderRole = MessageSenderRole.ADMIN,
            senderName = "إدارة منصة طب الحجايزة",
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = false,
            isReadByClinic = true,
            isReadByAdmin = true,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendAdminReplyToClinic(
        doctor: ClinicDoctorEntity,
        body: String,
    ): Result<DirectMessageEntity> {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة الرد للعيادة"))
        }
        val msg = DirectMessageEntity(
            id = "msg_${UUID.randomUUID()}",
            threadType = ChatThreadType.CLINIC_ADMIN,
            patientAccountId = "",
            patientName = "",
            patientPhone = doctor.username,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            senderRole = MessageSenderRole.ADMIN,
            senderName = "إدارة منصة طب الحجايزة",
            body = cleanBody,
            createdAtMillis = System.currentTimeMillis(),
            isReadByPatient = true,
            isReadByClinic = false,
            isReadByAdmin = true,
        )
        dao.insertDirectMessage(msg)
        return Result.success(msg)
    }

    suspend fun sendAdminClinicAlert(
        doctor: ClinicDoctorEntity?,
        message: String,
    ): Result<DelayNotificationEntity> {
        val cleanMsg = message.trim()
        if (cleanMsg.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة نص التنبيه للعيادات"))
        }
        val entity = DelayNotificationEntity(
            doctorId = doctor?.id.orEmpty(),
            doctorName = doctor?.doctorName ?: "جميع العيادات",
            clinicName = doctor?.clinicName ?: "توجيه إداري للعيادات",
            delayMinutes = 0,
            message = if (doctor != null) {
                "إشعار إداري إلى ${doctor.clinicName}: $cleanMsg"
            } else {
                "تعميم إداري لجميع العيادات: $cleanMsg"
            },
            createdAtMillis = System.currentTimeMillis(),
            isRead = false,
            targetAudience = NotificationAudience.CLINIC,
            targetId = doctor?.id.orEmpty(),
            senderLabel = "إدارة المنصة",
        )
        val newId = dao.insertNotification(entity)
        return Result.success(entity.copy(id = newId))
    }

    suspend fun sendAdminGlobalBroadcast(message: String): Result<DelayNotificationEntity> {
        val cleanMsg = message.trim()
        if (cleanMsg.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى كتابة نص الإشعار العام"))
        }
        val entity = DelayNotificationEntity(
            doctorId = "",
            doctorName = "إدارة منصة طب الحجايزة",
            clinicName = "إشعار عام",
            delayMinutes = 0,
            message = "📢 إشعار عام من إدارة المنصة: $cleanMsg",
            createdAtMillis = System.currentTimeMillis(),
            isRead = false,
            targetAudience = NotificationAudience.ALL,
            targetId = "",
            senderLabel = "إدارة المنصة",
        )
        val newId = dao.insertNotification(entity)
        return Result.success(entity.copy(id = newId))
    }

    suspend fun markDirectThreadRead(
        threadType: String,
        viewerRole: String,
        patientAccountId: String = "",
        doctorId: String = "",
    ) {
        when (viewerRole) {
            MessageSenderRole.PATIENT -> {
                if (patientAccountId.isNotBlank()) {
                    dao.markThreadReadByPatient(threadType, patientAccountId, doctorId)
                }
            }
            MessageSenderRole.CLINIC -> {
                if (doctorId.isNotBlank()) {
                    dao.markThreadReadByClinic(threadType, doctorId, patientAccountId)
                }
            }
            MessageSenderRole.ADMIN -> {
                dao.markThreadReadByAdmin(threadType, patientAccountId, doctorId)
            }
        }
    }

    suspend fun markRoleNotificationsRead(
        roleName: String,
        activeAccountId: String,
        activeAccountPhone: String,
        familyMemberIds: Set<String>,
        loggedInDoctorId: String?,
    ) {
        val allNotifs = dao.observeNotifications().first()
        allNotifs.forEach { alert ->
            if (!alert.isRead && isNotificationVisibleForRole(
                    alert = alert,
                    roleName = roleName,
                    activeAccountId = activeAccountId,
                    activeAccountPhone = activeAccountPhone,
                    familyMemberIds = familyMemberIds,
                    loggedInDoctorId = loggedInDoctorId,
                )
            ) {
                dao.markNotificationReadById(alert.id)
            }
        }
        when (roleName) {
            "PATIENT" -> if (activeAccountId.isNotBlank()) dao.markAllPatientMessagesRead(activeAccountId)
            "DOCTOR_ADMIN" -> if (!loggedInDoctorId.isNullOrBlank()) dao.markAllClinicMessagesRead(loggedInDoctorId)
            "SUPER_ADMIN" -> dao.markAllAdminMessagesRead()
        }
    }

    fun isNotificationVisibleForRole(
        alert: DelayNotificationEntity,
        roleName: String,
        activeAccountId: String,
        activeAccountPhone: String,
        familyMemberIds: Set<String>,
        loggedInDoctorId: String?,
    ): Boolean {
        return when (roleName) {
            "DOCTOR_ADMIN" -> {
                alert.targetAudience == NotificationAudience.ALL ||
                    (alert.targetAudience == NotificationAudience.CLINIC &&
                        (alert.targetId.isBlank() || alert.targetId == loggedInDoctorId))
            }
            "SUPER_ADMIN" -> {
                alert.targetAudience == NotificationAudience.ADMIN ||
                    alert.targetAudience == NotificationAudience.ALL
            }
            else -> {
                alert.targetAudience == NotificationAudience.ALL ||
                    (alert.targetAudience == NotificationAudience.PATIENT && (
                        alert.targetId.isBlank() ||
                            (activeAccountId.isNotBlank() && alert.targetId == activeAccountId) ||
                            (activeAccountPhone.isNotBlank() && alert.targetId == activeAccountPhone) ||
                            alert.targetId in familyMemberIds
                        ))
            }
        }
    }

    fun toAsciiDigits(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - 0x0660)).toChar())
                in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - 0x06F0)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun normalizePhoneDigits(phone: String): String {
        val digits = toAsciiDigits(phone).filter { it in '0'..'9' }
        return when {
            digits.startsWith("0020") -> "0${digits.substring(4)}"
            digits.startsWith("20") && digits.length >= 12 -> "0${digits.substring(2)}"
            else -> digits
        }
    }

    companion object {
        const val MAX_ADMIN_PIN_ATTEMPTS = 3
        const val ADMIN_LOCKOUT_MILLIS = 15 * 60_000L
        const val MASTER_ADMIN_OTP = "999000"
        const val OTP_TTL_MILLIS = 24L * 60L * 60L * 1000L
        const val PATIENT_MODIFY_CUTOFF_MINUTES = 120
    }
}
