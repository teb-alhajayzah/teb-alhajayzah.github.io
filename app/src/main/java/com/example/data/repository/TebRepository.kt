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
import com.example.data.local.TebDao
import com.example.ai.GeminiMedicalAnalyzer
import com.example.data.medications.EgyptianMedicationService
import com.example.data.medications.MedicationFetchResult
import com.example.ui.AppLanguage
import com.example.util.BilingualMedicalTranslator
import kotlinx.coroutines.flow.combine
import com.example.util.ChronicConditionsCatalog
import com.example.util.ImageStorageHelper
import com.example.util.MedicalSummaryAnalyzer
import com.example.util.TebNotificationHelper
import com.example.util.TimeUtils
import com.example.data.remote.FirestoreCloudSyncService
import com.example.data.remote.GoogleDriveStorageHelper
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

data class VisitorStats(
    val todayTotal: Int = 1,
    val todayApk: Int = 1,
    val todayWeb: Int = 0,
    val weekTotal: Int = 1,
    val weekApk: Int = 1,
    val weekWeb: Int = 0,
    val monthTotal: Int = 1,
    val monthApk: Int = 1,
    val monthWeb: Int = 0,
)

class TebRepository(
    private val dao: TebDao,
    private val appContext: Context,
) {
    private val seedMutex = Mutex()
    private val cloudSyncMutex = Mutex()
    private val cloudScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()
    private val geminiAnalyzer by lazy { GeminiMedicalAnalyzer() }
    @Volatile
    private var lastSyncedRemoteMessageIds: Set<String> = emptySet()
    private val locallyDeletedMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

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
    private val cfgDefaultDriveUrlKey = stringPreferencesKey("cfg_default_drive_url")
    private val cfgFileRetentionDaysKey = intPreferencesKey("cfg_file_retention_days")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val seededRealDoctorsKey = booleanPreferencesKey("seeded_real_doctors_v1")
    private val adminLoggedInKey = booleanPreferencesKey("admin_logged_in_v1")
    private val migratedAdminPin132027Key = booleanPreferencesKey("migrated_admin_pin_132027_v1")

    val isAdminLoggedInFlow: Flow<Boolean> = appContext.tebDataStore.data
        .map { prefs -> prefs[adminLoggedInKey] ?: false }

    suspend fun setAdminLoggedIn(loggedIn: Boolean) {
        appContext.tebDataStore.edit { prefs ->
            prefs[adminLoggedInKey] = loggedIn
        }
    }

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
                firebaseApiKey = prefs[cfgFbApiKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseApiKey,
                firebaseAuthDomain = prefs[cfgFbAuthDomainKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseAuthDomain,
                firebaseProjectId = prefs[cfgFbProjectIdKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseProjectId,
                firebaseStorageBucket = prefs[cfgFbStorageBucketKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseStorageBucket,
                firebaseMessagingSenderId = prefs[cfgFbMessagingSenderIdKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseMessagingSenderId,
                firebaseAppId = prefs[cfgFbAppIdKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseAppId,
                firebaseVapidKey = prefs[cfgFbVapidKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.firebaseVapidKey,
                workerUrl = prefs[cfgWorkerUrlKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.workerUrl,
                defaultDriveWebAppUrl = prefs[cfgDefaultDriveUrlKey] ?: AppConfig.defaultBrand.defaultDriveWebAppUrl,
                fileRetentionDays = FirestoreCloudSyncService.normalizeRetentionDays(
                    prefs[cfgFileRetentionDaysKey] ?: AppConfig.defaultBrand.fileRetentionDays,
                ),
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
    val allPermanentLogsFlow: Flow<List<PermanentMedicalLogEntity>> = dao.observeAllPermanentLogs()
    val notificationsFlow: Flow<List<DelayNotificationEntity>> = dao.observeNotifications()
    val directMessagesFlow: Flow<List<DirectMessageEntity>> = dao.observeAllDirectMessages()
    val allDoctorFavoritesFlow: Flow<List<DoctorFavoriteMedEntity>> = dao.observeAllDoctorFavorites()
    val blockedPhonesFlow: Flow<List<BlockedPhoneEntity>> = dao.observeBlockedPhones()
    val activationCodesFlow: Flow<List<ActivationOtpCodeEntity>> = dao.observeAllActivationCodes()
    val familyLinkRequestsFlow: Flow<List<FamilyLinkRequestEntity>> = dao.observeFamilyLinkRequests()

    fun observeFamilyLinkRequestsForAccount(accountId: String): Flow<List<FamilyLinkRequestEntity>> =
        dao.observeFamilyLinkRequestsForAccount(accountId)

    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>> =
        combine(
            dao.observeFamilyMembers(accountId),
            dao.observeAllAccounts(),
            dao.observeAllFamilyMembers(),
        ) { myMembers, allAccounts, allMembers ->
            val currentAccount = allAccounts.firstOrNull { it.id == accountId }
            val spouseId = currentAccount?.linkedSpouseAccountId.orEmpty()
            if (spouseId.isBlank()) {
                myMembers
            } else {
                val spouseAccount = allAccounts.firstOrNull { it.id == spouseId }
                val spouseMembers = allMembers.filter { it.accountId == spouseId }
                val childrenFromSpouse = spouseMembers.filter { mem ->
                    val r = mem.relation.lowercase()
                    !r.contains("نفس") && !r.contains("أنا") && !r.contains("self") &&
                    (r.contains("ابن") || r.contains("ابنة") || r.contains("طفل") || r.contains("son") || r.contains("daughter") || r.contains("child") || !r.contains("زوج"))
                }
                val spouseSelfRecord = if (spouseAccount?.allowSpouseViewPersonalRecords == true) {
                    spouseMembers.filter { it.relation.contains("نفس") || it.relation.contains("أنا") || it.relation.contains("self") || it.relation.contains("زوج") }
                } else {
                    emptyList()
                }
                (myMembers + childrenFromSpouse + spouseSelfRecord).distinctBy { it.id }
            }
        }

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
                editable[adminLoggedInKey] = true
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
        defaultDriveWebAppUrl: String? = null,
        fileRetentionDays: Int? = null,
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
            if (defaultDriveWebAppUrl != null) prefs[cfgDefaultDriveUrlKey] = defaultDriveWebAppUrl.trim()
            if (fileRetentionDays != null) {
                prefs[cfgFileRetentionDaysKey] = FirestoreCloudSyncService.normalizeRetentionDays(fileRetentionDays)
            }
        }
        val latestBrand = brandConfigFlow.first()
        pruneExpiredHeavyAttachments()
        cloudScope.launch {
            FirestoreCloudSyncService.pushSettings(latestBrand)
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
            prefs.remove(cfgDefaultDriveUrlKey)
            prefs.remove(cfgFileRetentionDaysKey)
        }
        cloudScope.launch {
            FirestoreCloudSyncService.pushSettings(AppConfig.defaultBrand)
        }
    }

    suspend fun blockPhone(phone: String, reason: String = "حظر إداري بسبب حجز وهمي / مخالفة"): Result<Unit> {
        val cleanPhone = phone.trim()
        if (cleanPhone.length < 8) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح للحظر"))
        }
        val blocked = BlockedPhoneEntity(phone = cleanPhone, reason = reason.trim().ifEmpty { "حظر إداري" })
        dao.insertBlockedPhone(blocked)
        pushDocAsync("blocked_phones", cleanPhone) { FirestoreCloudSyncService.blockedPhoneToJson(blocked) }
        val activeBooked = dao.observeAllSlots().first()
            .filter { it.status == SlotStatus.BOOKED && it.patientPhone.trim() == cleanPhone }
        activeBooked.forEach { slot ->
            TebNotificationHelper.cancelAppointmentReminders(appContext, slot.id)
            val clearedSlot = slot.copy(
                status = SlotStatus.AVAILABLE,
                patientName = "",
                patientPhone = "",
                familyMemberId = "",
                bookingSource = BookingSource.NONE,
                bookedAtMillis = 0L,
            )
            dao.updateSlot(clearedSlot)
            pushDocAsync("slots", clearedSlot.id) { FirestoreCloudSyncService.slotToJson(clearedSlot) }
        }
        return Result.success(Unit)
    }

    suspend fun unblockPhone(phone: String) {
        val clean = phone.trim()
        dao.unblockPhone(clean)
        deleteDocAsync("blocked_phones", clean)
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
        generated.forEach { codeEntity ->
            pushDocAsync("activation_codes", codeEntity.code) { FirestoreCloudSyncService.activationCodeToJson(codeEntity) }
        }
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
            dao.getAccountById(accountId)?.let { acc ->
                pushDocAsync("accounts", acc.id) { FirestoreCloudSyncService.accountToJson(acc) }
            }
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
            dao.getAccountById(accountId)?.let { acc ->
                pushDocAsync("accounts", acc.id) { FirestoreCloudSyncService.accountToJson(acc) }
            }
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
            dao.getAccountById(accountId)?.let { acc ->
                pushDocAsync("accounts", acc.id) { FirestoreCloudSyncService.accountToJson(acc) }
            }
            dao.getActivationCode(cleanCode)?.let { updatedCode ->
                pushDocAsync("activation_codes", updatedCode.code) { FirestoreCloudSyncService.activationCodeToJson(updatedCode) }
            }
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
        pushDocAsync("accounts", updated.id) { FirestoreCloudSyncService.accountToJson(updated) }
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
        dao.getAccountById(accountId)?.let { acc ->
            pushDocAsync("accounts", acc.id) { FirestoreCloudSyncService.accountToJson(acc) }
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
        val resolvedPass = newPassword?.let { toAsciiDigits(it).trim() }?.takeIf { it.isNotEmpty() } ?: existing.password
        dao.updateAccountDetails(accountId, cleanName, cleanPhone, resolvedPass)
        dao.updateSelfMemberDetails(accountId, cleanName, cleanPhone)
        val updated = dao.getAccountById(accountId) ?: existing.copy(
            fullName = cleanName,
            phone = cleanPhone,
            password = resolvedPass,
        )
        pushDocAsync("accounts", updated.id) { FirestoreCloudSyncService.accountToJson(updated) }
        return Result.success(updated)
    }

    suspend fun deleteAccountByAdmin(accountId: String) {
        val members = dao.getFamilyMembersOnce(accountId)
        dao.deleteAccountCascade(accountId)
        deleteDocAsync("accounts", accountId)
        members.forEach { deleteDocAsync("family_members", it.id) }
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
        pushDocAsync("labs", lab.id) { FirestoreCloudSyncService.labToJson(lab) }
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
        pushDocAsync("labs", updated.id) { FirestoreCloudSyncService.labToJson(updated) }
        return Result.success(updated)
    }

    suspend fun deleteLabByAdmin(labId: String) {
        dao.deleteLabById(labId)
        deleteDocAsync("labs", labId)
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
        driveWebAppUrl: String = "",
        secondaryDriveWebAppUrl: String = "",
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
            driveWebAppUrl = driveWebAppUrl.trim(),
            secondaryDriveWebAppUrl = secondaryDriveWebAppUrl.trim(),
        )
        dao.insertDoctor(doctor)
        val initialSlots = buildSlotsForDoctor(doctor)
        dao.insertSlots(initialSlots)
        pushDocAsync("doctors", doctor.id) { FirestoreCloudSyncService.doctorToJson(doctor) }
        initialSlots.forEach { slot ->
            pushDocAsync("slots", slot.id) { FirestoreCloudSyncService.slotToJson(slot) }
        }
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
        driveWebAppUrl: String = "",
        secondaryDriveWebAppUrl: String = "",
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
            driveWebAppUrl = driveWebAppUrl.trim(),
            secondaryDriveWebAppUrl = secondaryDriveWebAppUrl.trim(),
        )
        dao.updateDoctor(updated)
        pushDocAsync("doctors", updated.id) { FirestoreCloudSyncService.doctorToJson(updated) }
        if (scheduleChanged) {
            val oldSlots = dao.observeSlotsForDoctor(doctorId).first()
            val newSlots = buildSlotsForDoctor(updated)
            dao.replaceDoctorSchedule(
                doctorId = doctorId,
                startMin = workStartMinutes,
                endMin = workEndMinutes,
                durationMin = slotDurationMinutes,
                newSlots = newSlots,
            )
            val newIds = newSlots.map { it.id }.toSet()
            oldSlots.filter { it.id !in newIds }.forEach { deleteDocAsync("slots", it.id) }
            newSlots.forEach { slot ->
                pushDocAsync("slots", slot.id) { FirestoreCloudSyncService.slotToJson(slot) }
            }
        }
        return Result.success(updated)
    }

    suspend fun deleteDoctorByAdmin(doctorId: String) {
        val slots = dao.observeSlotsForDoctor(doctorId).first()
        dao.deleteDoctorCascade(doctorId)
        deleteDocAsync("doctors", doctorId)
        slots.forEach { deleteDocAsync("slots", it.id) }
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
            if (prefs[migratedAdminPin132027Key] != true) {
                appContext.tebDataStore.edit { editPrefs ->
                    val cachedPin = editPrefs[adminPinKey]?.trim().orEmpty()
                    if (cachedPin.isEmpty() || cachedPin == "202600") {
                        editPrefs.remove(adminPinKey)
                    }
                    editPrefs[migratedAdminPin132027Key] = true
                }
            }
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
            pruneExpiredHeavyAttachments()
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
        val bookedSlot = slot.copy(
            status = SlotStatus.BOOKED,
            patientName = patientName.trim(),
            patientPhone = cleanPhone,
            familyMemberId = familyMemberId,
            bookingSource = source,
            bookedAtMillis = System.currentTimeMillis(),
        )
        dao.updateSlot(bookedSlot)
        pushDocAsync("slots", bookedSlot.id) { FirestoreCloudSyncService.slotToJson(bookedSlot) }
        val doc = dao.getDoctorById(slot.doctorId)
        val formattedTime = TimeUtils.formatMinutesAr(slot.effectiveMinutes, false)
        TebNotificationHelper.scheduleAppointmentReminders(
            context = appContext,
            slotId = bookedSlot.id,
            doctorName = doc?.doctorName.orEmpty(),
            clinicName = doc?.clinicName.orEmpty(),
            effectiveMinutes = bookedSlot.effectiveMinutes,
        )
        if (source == BookingSource.APP) {
            TebNotificationHelper.showHighPriorityNotification(
                context = appContext,
                title = "تم تأكيد حجزك — ${doc?.clinicName.orEmpty()}",
                body = "الموعد: الساعة $formattedTime باسم ${patientName.trim()}. سيتم تذكيرك تلقائياً قبل الموعد.",
            )
        }
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
        val freedSlot = slot.copy(
            status = SlotStatus.AVAILABLE,
            patientName = "",
            patientPhone = "",
            familyMemberId = "",
            bookingSource = BookingSource.NONE,
            bookedAtMillis = 0L,
        )
        dao.updateSlot(freedSlot)
        TebNotificationHelper.cancelAppointmentReminders(appContext, freedSlot.id)
        pushDocAsync("slots", freedSlot.id) { FirestoreCloudSyncService.slotToJson(freedSlot) }
        val notifMsg = "إلغاء حجز من المريض: قام $oldPatientName بإلغاء موعد الساعة $oldTime وإتاحته مجدداً."
        val notifEntity = DelayNotificationEntity(
            doctorId = slot.doctorId,
            doctorName = doctor?.doctorName.orEmpty(),
            clinicName = doctor?.clinicName.orEmpty(),
            delayMinutes = 0,
            message = notifMsg,
            targetAudience = NotificationAudience.CLINIC,
            targetId = slot.doctorId,
            senderLabel = oldPatientName.ifBlank { "مريض" },
        )
        val insertedId = dao.insertNotification(notifEntity)
        val savedNotif = notifEntity.copy(id = insertedId)
        pushDocAsync("notifications", "notif_${savedNotif.id}_${savedNotif.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(savedNotif)
        }
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

        val newBooked = toSlot.copy(
            status = SlotStatus.BOOKED,
            patientName = fromSlot.patientName,
            patientPhone = fromSlot.patientPhone,
            familyMemberId = fromSlot.familyMemberId,
            bookingSource = fromSlot.bookingSource,
            bookedAtMillis = System.currentTimeMillis(),
        )
        val freedFrom = fromSlot.copy(
            status = SlotStatus.AVAILABLE,
            patientName = "",
            patientPhone = "",
            familyMemberId = "",
            bookingSource = BookingSource.NONE,
            bookedAtMillis = 0L,
        )
        dao.updateSlot(newBooked)
        dao.updateSlot(freedFrom)
        TebNotificationHelper.cancelAppointmentReminders(appContext, freedFrom.id)
        TebNotificationHelper.scheduleAppointmentReminders(
            context = appContext,
            slotId = newBooked.id,
            doctorName = doctor?.doctorName.orEmpty(),
            clinicName = doctor?.clinicName.orEmpty(),
            effectiveMinutes = newBooked.effectiveMinutes,
        )
        pushDocAsync("slots", newBooked.id) { FirestoreCloudSyncService.slotToJson(newBooked) }
        pushDocAsync("slots", freedFrom.id) { FirestoreCloudSyncService.slotToJson(freedFrom) }
        val notifMsg = "تعديل موعد من المريض: نقل $patientName حجزه من الساعة $oldTime إلى الساعة $newTime."
        val notifEntity = DelayNotificationEntity(
            doctorId = fromSlot.doctorId,
            doctorName = doctor?.doctorName.orEmpty(),
            clinicName = doctor?.clinicName.orEmpty(),
            delayMinutes = 0,
            message = notifMsg,
            targetAudience = NotificationAudience.CLINIC,
            targetId = fromSlot.doctorId,
            senderLabel = patientName.ifBlank { "مريض" },
        )
        val insertedId = dao.insertNotification(notifEntity)
        val savedNotif = notifEntity.copy(id = insertedId)
        pushDocAsync("notifications", "notif_${savedNotif.id}_${savedNotif.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(savedNotif)
        }
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
        val freedSlot = slot.copy(
            status = SlotStatus.AVAILABLE,
            patientName = "",
            patientPhone = "",
            familyMemberId = "",
            bookingSource = BookingSource.NONE,
            bookedAtMillis = 0L,
        )
        dao.updateSlot(freedSlot)
        TebNotificationHelper.cancelAppointmentReminders(appContext, freedSlot.id)
        pushDocAsync("slots", freedSlot.id) { FirestoreCloudSyncService.slotToJson(freedSlot) }
        if (targetPatientId.isNotBlank()) {
            val notifMsg = "إشعار من ${doctor?.clinicName ?: "العيادة"}: تم إلغاء حجز موعدك الساعة $oldTime ($oldPatientName). يمكنك الحجز في موعد آخر متاح."
            val notifEntity = DelayNotificationEntity(
                doctorId = slot.doctorId,
                doctorName = doctor?.doctorName.orEmpty(),
                clinicName = doctor?.clinicName.orEmpty(),
                delayMinutes = 0,
                message = notifMsg,
                targetAudience = NotificationAudience.PATIENT,
                targetId = targetPatientId,
                senderLabel = doctor?.clinicName.orEmpty(),
            )
            val insertedId = dao.insertNotification(notifEntity)
            val savedNotif = notifEntity.copy(id = insertedId)
            pushDocAsync("notifications", "notif_${savedNotif.id}_${savedNotif.createdAtMillis}") {
                FirestoreCloudSyncService.notificationToJson(savedNotif)
            }
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

        val newBooked = toSlot.copy(
            status = SlotStatus.BOOKED,
            patientName = fromSlot.patientName,
            patientPhone = fromSlot.patientPhone,
            familyMemberId = fromSlot.familyMemberId,
            bookingSource = fromSlot.bookingSource,
            bookedAtMillis = System.currentTimeMillis(),
        )
        val freedFrom = fromSlot.copy(
            status = SlotStatus.AVAILABLE,
            patientName = "",
            patientPhone = "",
            familyMemberId = "",
            bookingSource = BookingSource.NONE,
            bookedAtMillis = 0L,
        )
        dao.updateSlot(newBooked)
        dao.updateSlot(freedFrom)
        TebNotificationHelper.cancelAppointmentReminders(appContext, freedFrom.id)
        TebNotificationHelper.scheduleAppointmentReminders(
            context = appContext,
            slotId = newBooked.id,
            doctorName = doctor?.doctorName.orEmpty(),
            clinicName = doctor?.clinicName.orEmpty(),
            effectiveMinutes = newBooked.effectiveMinutes,
        )
        pushDocAsync("slots", newBooked.id) { FirestoreCloudSyncService.slotToJson(newBooked) }
        pushDocAsync("slots", freedFrom.id) { FirestoreCloudSyncService.slotToJson(freedFrom) }
        if (targetPatientId.isNotBlank()) {
            val notifMsg = "تحديث من ${doctor?.clinicName ?: "العيادة"}: تم تعديل موعد ${fromSlot.patientName} من الساعة $oldTime إلى الساعة $newTime."
            val notifEntity = DelayNotificationEntity(
                doctorId = fromSlot.doctorId,
                doctorName = doctor?.doctorName.orEmpty(),
                clinicName = doctor?.clinicName.orEmpty(),
                delayMinutes = 0,
                message = notifMsg,
                targetAudience = NotificationAudience.PATIENT,
                targetId = targetPatientId,
                senderLabel = doctor?.clinicName.orEmpty(),
            )
            val insertedId = dao.insertNotification(notifEntity)
            val savedNotif = notifEntity.copy(id = insertedId)
            pushDocAsync("notifications", "notif_${savedNotif.id}_${savedNotif.createdAtMillis}") {
                FirestoreCloudSyncService.notificationToJson(savedNotif)
            }
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

    private fun pushDocAsync(
        subCollection: String,
        docId: String,
        payloadBuilder: () -> JSONObject,
    ) {
        if (!FirestoreCloudSyncService.isCloudNetworkAllowed() || docId.isBlank()) return
        cloudScope.launch {
            runCatching {
                val brand = brandConfigFlow.first()
                FirestoreCloudSyncService.upsertDocument(brand, subCollection, docId, payloadBuilder())
            }
        }
    }

    private fun deleteDocAsync(subCollection: String, docId: String) {
        if (!FirestoreCloudSyncService.isCloudNetworkAllowed() || docId.isBlank()) return
        cloudScope.launch {
            runCatching {
                val brand = brandConfigFlow.first()
                FirestoreCloudSyncService.deleteDocument(brand, subCollection, docId)
            }
        }
    }

    suspend fun syncWithCloudFirestoreOnce(): Boolean {
        if (!FirestoreCloudSyncService.isCloudNetworkAllowed()) return false
        return cloudSyncMutex.withLock {
            runCatching {
                var brand = brandConfigFlow.first()
                if (!brand.isCloudConfigured) return@withLock false

                // 1) مزامنة الإعدادات تحت مسار العزل الموحد: teb_alhajayzah/settings
                val remoteSettings = FirestoreCloudSyncService.fetchSettings(brand)
                if (remoteSettings == null) {
                    FirestoreCloudSyncService.pushSettings(brand)
                } else {
                    val mergedBrand = FirestoreCloudSyncService.settingsFromJson(remoteSettings, brand)
                    if (mergedBrand != brand) {
                        appContext.tebDataStore.edit { prefs ->
                            prefs[cfgPlatformNameKey] = mergedBrand.platformNameAr
                            prefs[cfgVillageNameKey] = mergedBrand.villageNameAr
                            prefs[cfgHospitalNameKey] = mergedBrand.hospitalNameAr
                            prefs[cfgSupportPhoneKey] = mergedBrand.supportPhone
                            prefs[cfgAnnouncementKey] = mergedBrand.homeAnnouncementAr
                            prefs[cfgApkUrlKey] = mergedBrand.apkDownloadUrl
                            prefs[cfgMedsApiKey] = mergedBrand.medicationsApiUrl
                            prefs[adminPinKey] = mergedBrand.adminPin
                            prefs[adminUsernameKey] = mergedBrand.adminUsername
                            prefs[adminPasswordKey] = mergedBrand.adminPassword
                            prefs[cfgGlobalChatEnabledKey] = mergedBrand.globalChatEnabled
                            prefs[cfgFbApiKey] = mergedBrand.firebaseApiKey
                            prefs[cfgFbAuthDomainKey] = mergedBrand.firebaseAuthDomain
                            prefs[cfgFbProjectIdKey] = mergedBrand.firebaseProjectId
                            prefs[cfgFbStorageBucketKey] = mergedBrand.firebaseStorageBucket
                            prefs[cfgFbMessagingSenderIdKey] = mergedBrand.firebaseMessagingSenderId
                            prefs[cfgFbAppIdKey] = mergedBrand.firebaseAppId
                            prefs[cfgFbVapidKey] = mergedBrand.firebaseVapidKey
                            prefs[cfgWorkerUrlKey] = mergedBrand.workerUrl
                            prefs[cfgDefaultDriveUrlKey] = mergedBrand.defaultDriveWebAppUrl
                            prefs[cfgFileRetentionDaysKey] = FirestoreCloudSyncService.normalizeRetentionDays(mergedBrand.fileRetentionDays)
                        }
                        brand = mergedBrand
                    }
                }

                // 2) مزامنة العيادات والأطباء (teb_alhajayzah/data/doctors)
                FirestoreCloudSyncService.listCollection(brand, "doctors")?.let { remoteDocs ->
                    val localDocs = dao.observeDoctors().first()
                    if (remoteDocs.isEmpty() && localDocs.isNotEmpty()) {
                        localDocs.forEach { d ->
                            FirestoreCloudSyncService.upsertDocument(brand, "doctors", d.id, FirestoreCloudSyncService.doctorToJson(d))
                        }
                    } else if (remoteDocs.isNotEmpty()) {
                        val parsed = remoteDocs.mapNotNull { FirestoreCloudSyncService.doctorFromJson(it) }
                        if (parsed.isNotEmpty()) dao.insertDoctors(parsed)
                    }
                }

                // 3) مزامنة المعامل (teb_alhajayzah/data/labs)
                FirestoreCloudSyncService.listCollection(brand, "labs")?.let { remoteLabs ->
                    val localLabs = dao.observeLabs().first()
                    if (remoteLabs.isEmpty() && localLabs.isNotEmpty()) {
                        localLabs.forEach { l ->
                            FirestoreCloudSyncService.upsertDocument(brand, "labs", l.id, FirestoreCloudSyncService.labToJson(l))
                        }
                    } else {
                        remoteLabs.mapNotNull { FirestoreCloudSyncService.labFromJson(it) }.forEach { dao.insertLab(it) }
                    }
                }

                // 4) مزامنة الحسابات (teb_alhajayzah/data/accounts)
                FirestoreCloudSyncService.listCollection(brand, "accounts")?.let { remoteAccs ->
                    val localAccs = dao.getAllAccountsOnce()
                    if (remoteAccs.isEmpty() && localAccs.isNotEmpty()) {
                        localAccs.forEach { a ->
                            FirestoreCloudSyncService.upsertDocument(brand, "accounts", a.id, FirestoreCloudSyncService.accountToJson(a))
                        }
                    } else {
                        remoteAccs.mapNotNull { FirestoreCloudSyncService.accountFromJson(it) }.forEach { dao.insertAccount(it) }
                    }
                }

                // 5) مزامنة ملفات الأسرة (teb_alhajayzah/data/family_members)
                FirestoreCloudSyncService.listCollection(brand, "family_members")?.let { remoteMembers ->
                    val localMembers = dao.observeAllFamilyMembers().first()
                    if (remoteMembers.isEmpty() && localMembers.isNotEmpty()) {
                        localMembers.forEach { m ->
                            FirestoreCloudSyncService.upsertDocument(brand, "family_members", m.id, FirestoreCloudSyncService.familyMemberToJson(m))
                        }
                    } else {
                        val parsed = remoteMembers.mapNotNull { FirestoreCloudSyncService.familyMemberFromJson(it) }
                        if (parsed.isNotEmpty()) dao.insertFamilyMembers(parsed)
                    }
                }

                // 6) مزامنة المواعيد (teb_alhajayzah/data/slots)
                FirestoreCloudSyncService.listCollection(brand, "slots")?.let { remoteSlots ->
                    val localSlots = dao.observeAllSlots().first()
                    if (remoteSlots.isEmpty() && localSlots.isNotEmpty()) {
                        localSlots.forEach { s ->
                            FirestoreCloudSyncService.upsertDocument(brand, "slots", s.id, FirestoreCloudSyncService.slotToJson(s))
                        }
                    } else {
                        val parsed = remoteSlots.mapNotNull { FirestoreCloudSyncService.slotFromJson(it) }
                        if (parsed.isNotEmpty()) dao.insertSlots(parsed)
                    }
                }

                // 7) مزامنة السجلات الطبية (teb_alhajayzah/data/records) مع الحفاظ على المسارات المحلية للمرفقات
                FirestoreCloudSyncService.listCollection(brand, "records")?.let { remoteRecs ->
                    val localRecs = dao.observeAllMedicalRecords().first()
                    if (remoteRecs.isEmpty() && localRecs.isNotEmpty()) {
                        localRecs.forEach { r ->
                            FirestoreCloudSyncService.upsertDocument(brand, "records", r.id, FirestoreCloudSyncService.recordToJson(r))
                        }
                    } else {
                        val localMap = localRecs.associateBy { it.id }
                        val parsed = remoteRecs.mapNotNull { j ->
                            val id = j.optString("id", j.optString("_docId", ""))
                            val existingPath = localMap[id]?.attachmentPath.orEmpty()
                            FirestoreCloudSyncService.recordFromJson(j, existingPath)
                        }
                        if (parsed.isNotEmpty()) dao.insertMedicalRecords(parsed)
                    }
                }

                // 8) مزامنة الإشعارات (teb_alhajayzah/data/notifications)
                FirestoreCloudSyncService.listCollection(brand, "notifications")?.let { remoteNotifs ->
                    val localNotifs = dao.observeNotifications().first()
                    if (remoteNotifs.isEmpty() && localNotifs.isNotEmpty()) {
                        localNotifs.forEach { n ->
                            FirestoreCloudSyncService.upsertDocument(brand, "notifications", "notif_${n.id}_${n.createdAtMillis}", FirestoreCloudSyncService.notificationToJson(n))
                        }
                    } else {
                        remoteNotifs.mapNotNull { FirestoreCloudSyncService.notificationFromJson(it) }.forEach { rn ->
                            val exists = localNotifs.any {
                                it.createdAtMillis == rn.createdAtMillis && it.message == rn.message && it.doctorId == rn.doctorId
                            }
                            if (!exists) dao.insertNotification(rn)
                        }
                    }
                }

                // 9) مزامنة المحادثات المباشرة (teb_alhajayzah/data/messages)
                FirestoreCloudSyncService.listCollection(brand, "messages")?.let { remoteMsgs ->
                    val localMsgs = dao.observeAllDirectMessages().first()
                    if (remoteMsgs.isEmpty() && localMsgs.isNotEmpty() && lastSyncedRemoteMessageIds.isEmpty()) {
                        localMsgs.forEach { m ->
                            if (m.id !in locallyDeletedMessageIds) {
                                FirestoreCloudSyncService.upsertDocument(brand, "messages", m.id, FirestoreCloudSyncService.messageToJson(m))
                            }
                        }
                    } else {
                        val parsedRemote = remoteMsgs.mapNotNull { FirestoreCloudSyncService.messageFromJson(it) }
                        val remoteIds = parsedRemote.map { it.id }.toSet()
                        val deletedRemotely = lastSyncedRemoteMessageIds - remoteIds
                        deletedRemotely.forEach { goneId ->
                            dao.deleteDirectMessageById(goneId)
                        }
                        parsedRemote.forEach { rm ->
                            if (rm.id !in locallyDeletedMessageIds) {
                                dao.insertDirectMessage(rm)
                            }
                        }
                        lastSyncedRemoteMessageIds = remoteIds
                    }
                }

                // 10) مزامنة الأرقام المحظورة والأكواد (teb_alhajayzah/data/blocked_phones & activation_codes)
                FirestoreCloudSyncService.listCollection(brand, "blocked_phones")?.let { remoteBlocked ->
                    val localBlocked = dao.observeBlockedPhones().first()
                    if (remoteBlocked.isEmpty() && localBlocked.isNotEmpty()) {
                        localBlocked.forEach { b ->
                            FirestoreCloudSyncService.upsertDocument(brand, "blocked_phones", b.phone, FirestoreCloudSyncService.blockedPhoneToJson(b))
                        }
                    } else {
                        remoteBlocked.mapNotNull { FirestoreCloudSyncService.blockedPhoneFromJson(it) }.forEach { dao.insertBlockedPhone(it) }
                    }
                }
                FirestoreCloudSyncService.listCollection(brand, "activation_codes")?.let { remoteCodes ->
                    val localCodes = dao.getAllActivationCodesOnce()
                    if (remoteCodes.isEmpty() && localCodes.isNotEmpty()) {
                        localCodes.forEach { c ->
                            FirestoreCloudSyncService.upsertDocument(brand, "activation_codes", c.code, FirestoreCloudSyncService.activationCodeToJson(c))
                        }
                    } else {
                        val parsed = remoteCodes.mapNotNull { FirestoreCloudSyncService.activationCodeFromJson(it) }
                        if (parsed.isNotEmpty()) dao.insertActivationCodes(parsed)
                    }
                }

                // 11) مزامنة السجل الطبي التاريخي الدائم مدى الحياة (teb_alhajayzah/data/permanent_logs)
                FirestoreCloudSyncService.listCollection(brand, "permanent_logs")?.let { remoteLogs ->
                    val localLogs = dao.observeAllPermanentLogs().first()
                    if (remoteLogs.isEmpty() && localLogs.isNotEmpty()) {
                        localLogs.forEach { l ->
                            FirestoreCloudSyncService.upsertDocument(brand, "permanent_logs", l.id, FirestoreCloudSyncService.permanentLogToJson(l))
                        }
                    } else {
                        val parsed = remoteLogs.mapNotNull { FirestoreCloudSyncService.permanentLogFromJson(it) }
                        if (parsed.isNotEmpty()) dao.insertPermanentLogs(parsed)
                    }
                }

                // 12) مزامنة طلبات ربط الأسرة (teb_alhajayzah/data/family_link_requests)
                FirestoreCloudSyncService.listCollection(brand, "family_link_requests")?.let { remoteReqs ->
                    val localReqs = dao.observeFamilyLinkRequests().first()
                    if (remoteReqs.isEmpty() && localReqs.isNotEmpty()) {
                        localReqs.forEach { r ->
                            FirestoreCloudSyncService.upsertDocument(brand, "family_link_requests", r.id, FirestoreCloudSyncService.familyLinkRequestToJson(r))
                        }
                    } else {
                        val parsed = remoteReqs.mapNotNull { FirestoreCloudSyncService.familyLinkRequestFromJson(it) }
                        parsed.forEach { dao.insertFamilyLinkRequest(it) }
                    }
                }

                // 13) مزامنة إحصائيات الزيارات (teb_alhajayzah/data/visitor_hits)
                FirestoreCloudSyncService.listCollection(brand, "visitor_hits")?.let { remoteHits ->
                    val parsed = remoteHits.mapNotNull { FirestoreCloudSyncService.visitorHitFromJson(it) }
                    parsed.forEach { dao.insertVisitorHit(it) }
                }
                true
            }.getOrDefault(false)
        }
    }

    private suspend fun dispatchWorkerPushAsync(
        eventType: String,
        title: String,
        body: String,
        targetAudience: String,
        targetId: String,
        namespacePath: String,
    ) {
        if (!FirestoreCloudSyncService.isCloudNetworkAllowed()) return
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
        val oldSlots = dao.observeSlotsForDoctor(doctorId).first()
        val newSlots = buildSlotsForDoctor(updatedDoctor)
        dao.replaceDoctorSchedule(
            doctorId = doctorId,
            startMin = startMinutes,
            endMin = endMinutes,
            durationMin = durationMinutes,
            newSlots = newSlots,
        )
        dao.getDoctorById(doctorId)?.let { d ->
            pushDocAsync("doctors", d.id) { FirestoreCloudSyncService.doctorToJson(d) }
        }
        val newIds = newSlots.map { it.id }.toSet()
        oldSlots.filter { it.id !in newIds }.forEach {
            TebNotificationHelper.cancelAppointmentReminders(appContext, it.id)
            deleteDocAsync("slots", it.id)
        }
        newSlots.forEach { slot -> pushDocAsync("slots", slot.id) { FirestoreCloudSyncService.slotToJson(slot) } }
        return Result.success(newSlots.size)
    }

    suspend fun delayDoctorAppointments(doctorId: String, additionalMinutes: Int): Result<Unit> {
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        dao.addDoctorDelay(doctorId, additionalMinutes)
        dao.shiftUpcomingSlotsDelay(doctorId, additionalMinutes)
        val newTotalDelay = doctor.currentDelayMinutes + additionalMinutes
        val notifMsg = "تنبيه عاجل: تم تأخير مواعيد ${doctor.doctorName} (${doctor.clinicName}) لمدة $additionalMinutes دقيقة (إجمالي التأخير: $newTotalDelay دقيقة). تم تحديث موعدك تلقائياً."
        val notifEntity = DelayNotificationEntity(
            doctorId = doctorId,
            doctorName = doctor.doctorName,
            clinicName = doctor.clinicName,
            delayMinutes = additionalMinutes,
            message = notifMsg,
            targetAudience = NotificationAudience.ALL,
            targetId = "",
            senderLabel = doctor.clinicName,
        )
        val newId = dao.insertNotification(notifEntity)
        val savedNotif = notifEntity.copy(id = newId)
        dao.getDoctorById(doctorId)?.let { d ->
            pushDocAsync("doctors", d.id) { FirestoreCloudSyncService.doctorToJson(d) }
        }
        dao.observeSlotsForDoctor(doctorId).first().forEach { s ->
            pushDocAsync("slots", s.id) { FirestoreCloudSyncService.slotToJson(s) }
            if (s.status == SlotStatus.BOOKED) {
                TebNotificationHelper.scheduleAppointmentReminders(
                    context = appContext,
                    slotId = s.id,
                    doctorName = doctor.doctorName,
                    clinicName = doctor.clinicName,
                    effectiveMinutes = s.effectiveMinutes,
                )
            }
        }
        pushDocAsync("notifications", "notif_${savedNotif.id}_${savedNotif.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(savedNotif)
        }
        TebNotificationHelper.showHighPriorityNotification(
            context = appContext,
            title = "تنبيه تأخير — ${doctor.clinicName}",
            body = notifMsg,
        )
        dispatchWorkerPushAsync(
            eventType = "CLINIC_DELAY",
            title = "تنبيه تأخير — ${doctor.clinicName}",
            body = notifMsg,
            targetAudience = NotificationAudience.ALL,
            targetId = doctorId,
            namespacePath = CloudNamespacePaths.NOTIFICATIONS,
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

        val completedSlot = slot.copy(status = SlotStatus.COMPLETED)
        dao.updateSlot(completedSlot)
        pushDocAsync("slots", completedSlot.id) { FirestoreCloudSyncService.slotToJson(completedSlot) }
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
        pushDocAsync("records", record.id) { FirestoreCloudSyncService.recordToJson(record) }
        createAndSyncPermanentLog(
            record = record,
            eventType = PermanentLogEventType.CLINIC_VISIT,
            providerLabel = "${doctor.doctorName} — ${doctor.clinicName}",
        )
        if (attachmentPath.isNotBlank()) {
            uploadAttachmentToDriveAsync(
                record = record,
                clinicDriveUrl = doctor.driveWebAppUrl,
                clinicName = doctor.clinicName,
            )
        }
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
        pushDocAsync("records", record.id) { FirestoreCloudSyncService.recordToJson(record) }
        createAndSyncPermanentLog(
            record = record,
            eventType = PermanentLogEventType.PATIENT_UPLOAD,
            providerLabel = "رفع المريض / الأسرة",
        )
        if (attachmentPath.isNotBlank()) {
            uploadAttachmentToDriveAsync(
                record = record,
                clinicDriveUrl = "",
                clinicName = "ملف المريض الشخصي",
            )
        }
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
        nationalId: String? = null,
    ): Result<FamilyMemberEntity> {
        val existing = dao.getFamilyMemberById(memberId)
            ?: return Result.failure(IllegalStateException("الملف الشخصي غير موجود"))
        val cleanNationalId = nationalId?.let { toAsciiDigits(it).filter { c -> c in '0'..'9' }.take(14) }
            ?: existing.nationalId
        val updated = existing.copy(
            age = age.coerceIn(0, 120),
            weightKg = weightKg.coerceIn(0.0, 400.0),
            heightCm = heightCm.coerceIn(0.0, 260.0),
            bloodType = bloodType.trim(),
            chronicConditions = ChronicConditionsCatalog.encodeKeys(chronicConditions),
            drugAllergies = drugAllergies.trim(),
            otherChronicNotes = otherChronicNotes.trim(),
            nationalId = cleanNationalId,
        )
        dao.updateFamilyMember(updated)
        pushDocAsync("family_members", updated.id) { FirestoreCloudSyncService.familyMemberToJson(updated) }
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
        pushDocAsync("family_members", updated.id) { FirestoreCloudSyncService.familyMemberToJson(updated) }
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
            deleteDocAsync("records", recordId)
        } else {
            dao.clearMedicalRecordAttachment(recordId)
            dao.getMedicalRecordById(recordId)?.let { updatedRec ->
                pushDocAsync("records", updatedRec.id) { FirestoreCloudSyncService.recordToJson(updatedRec) }
            }
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
        pushDocAsync("records", record.id) { FirestoreCloudSyncService.recordToJson(record) }
        createAndSyncPermanentLog(
            record = record,
            eventType = PermanentLogEventType.EXTERNAL_VISIT,
            providerLabel = record.doctorName,
        )
        if (attachmentPath.isNotBlank()) {
            uploadAttachmentToDriveAsync(
                record = record,
                clinicDriveUrl = "",
                clinicName = "زيارة طبية خارجية",
            )
        }
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
        pushDocAsync("records", record.id) { FirestoreCloudSyncService.recordToJson(record) }
        createAndSyncPermanentLog(
            record = record,
            eventType = PermanentLogEventType.LAB_REPORT,
            providerLabel = lab.labName,
        )
        uploadAttachmentToDriveAsync(
            record = record,
            clinicDriveUrl = "",
            clinicName = lab.labName,
        )
        val labMsg = "نتيجة تحليل جديدة من ${lab.labName} باسم ${member.fullName}: $cleanTitle"
        TebNotificationHelper.showHighPriorityNotification(
            context = appContext,
            title = "نتيجة تحليل طبية جديدة",
            body = labMsg,
        )
        dispatchWorkerPushAsync(
            eventType = "LAB_RESULT_UPLOADED",
            title = "نتيجة تحليل جديدة — ${lab.labName}",
            body = labMsg,
            targetAudience = NotificationAudience.PATIENT,
            targetId = member.accountId.ifBlank { member.id },
            namespacePath = CloudNamespacePaths.RECORDS,
        )
        return Result.success(record)
    }

    suspend fun addFamilyMember(
        accountId: String,
        fullName: String,
        relation: String,
        age: Int,
        phone: String,
        nationalId: String = "",
    ): FamilyMemberEntity {
        val newMember = FamilyMemberEntity(
            id = "mem_${UUID.randomUUID()}",
            accountId = accountId,
            fullName = fullName.trim(),
            relation = relation.trim(),
            age = age,
            phone = phone.trim(),
            nationalId = toAsciiDigits(nationalId).trim(),
        )
        dao.insertFamilyMember(newMember)
        pushDocAsync("family_members", newMember.id) { FirestoreCloudSyncService.familyMemberToJson(newMember) }
        setActiveMember(newMember.id)
        return newMember
    }

    suspend fun loginPatient(identifier: String, password: String): Result<PatientAccountEntity> {
        val cleanId = toAsciiDigits(identifier).trim()
        val rawPass = password.trim()
        val normalizedPass = toAsciiDigits(rawPass)
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
        val passMatches = account.password == rawPass || account.password == normalizedPass
        if (!passMatches && rawPass.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("الرقم السري (PIN) غير صحيح"))
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
        val cleanPin = toAsciiDigits(password).trim().ifEmpty { "1234" }
        val account = PatientAccountEntity(
            id = accountId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            nationalId = toAsciiDigits(nationalId).trim(),
            password = cleanPin,
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
        val selfMember = FamilyMemberEntity(
            id = selfMemberId,
            accountId = accountId,
            fullName = account.fullName,
            relation = "نفسي (صاحب الحساب)",
            age = 30,
            phone = account.phone,
            nationalId = account.nationalId,
        )
        dao.insertFamilyMember(selfMember)
        pushDocAsync("accounts", account.id) { FirestoreCloudSyncService.accountToJson(account) }
        pushDocAsync("family_members", selfMember.id) { FirestoreCloudSyncService.familyMemberToJson(selfMember) }
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
        val latestBrand = brandConfigFlow.first()
        cloudScope.launch {
            FirestoreCloudSyncService.pushSettings(latestBrand)
        }
    }

    suspend fun setClinicChatEnabled(doctorId: String, enabled: Boolean) {
        dao.updateDoctorChatEnabled(doctorId, enabled)
        dao.getDoctorById(doctorId)?.let { d ->
            pushDocAsync("doctors", d.id) { FirestoreCloudSyncService.doctorToJson(d) }
        }
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        val saved = entity.copy(id = newId)
        pushDocAsync("notifications", "notif_${saved.id}_${saved.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(saved)
        }
        return Result.success(saved)
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        pushDocAsync("messages", msg.id) { FirestoreCloudSyncService.messageToJson(msg) }
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
        val saved = entity.copy(id = newId)
        pushDocAsync("notifications", "notif_${saved.id}_${saved.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(saved)
        }
        return Result.success(saved)
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
        val saved = entity.copy(id = newId)
        pushDocAsync("notifications", "notif_${saved.id}_${saved.createdAtMillis}") {
            FirestoreCloudSyncService.notificationToJson(saved)
        }
        TebNotificationHelper.showHighPriorityNotification(
            context = appContext,
            title = "📢 إشعار عام — طب الحجايزة",
            body = saved.message,
        )
        dispatchWorkerPushAsync(
            eventType = "GLOBAL_BROADCAST",
            title = "📢 إشعار عام — طب الحجايزة",
            body = saved.message,
            targetAudience = NotificationAudience.ALL,
            targetId = "",
            namespacePath = CloudNamespacePaths.NOTIFICATIONS,
        )
        return Result.success(saved)
    }

    suspend fun deleteDirectMessageByAdmin(messageId: String): Result<Unit> {
        val existing = dao.getDirectMessageById(messageId)
            ?: return Result.failure(IllegalStateException("الرسالة غير موجودة أو تم حذفها بالفعل"))
        locallyDeletedMessageIds.add(existing.id)
        dao.deleteDirectMessageById(existing.id)
        deleteDocAsync("messages", existing.id)
        return Result.success(Unit)
    }

    suspend fun deleteChatThreadByAdmin(
        threadType: String,
        patientAccountId: String = "",
        doctorId: String = "",
    ): Result<Int> {
        val msgs = dao.getDirectThreadMessagesOnce(threadType, patientAccountId, doctorId)
        if (msgs.isEmpty()) return Result.success(0)
        msgs.forEach { locallyDeletedMessageIds.add(it.id) }
        dao.deleteDirectThread(threadType, patientAccountId, doctorId)
        msgs.forEach { deleteDocAsync("messages", it.id) }
        return Result.success(msgs.size)
    }

    fun isValidNumericPin(rawPin: String): Boolean {
        val digits = toAsciiDigits(rawPin).trim()
        return digits.length >= 4 && digits.all { it in '0'..'9' }
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

    private suspend fun createAndSyncPermanentLog(
        record: MedicalRecordEntity,
        eventType: String,
        providerLabel: String,
    ) {
        val member = record.familyMemberId.takeIf { it.isNotBlank() }?.let { dao.getFamilyMemberById(it) }
        val (narAr, narEn) = MedicalSummaryAnalyzer.buildPermanentLogNarrative(
            record = record,
            member = member,
            providerLabel = providerLabel,
        )
        val ts = record.createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val initialLog = PermanentMedicalLogEntity(
            id = "plog_${record.id}",
            familyMemberId = record.familyMemberId,
            patientName = record.patientName,
            patientPhone = record.patientPhone,
            sourceRecordId = record.id,
            eventType = eventType,
            eventTimestampMillis = ts,
            eventDateTimeLabel = TimeUtils.formatActualDateTimeLabel(ts, isEn = false),
            providerName = providerLabel.ifBlank { record.doctorName },
            specialtyOrCategory = record.specialty,
            title = record.diagnosis.ifBlank { record.attachmentName.ifBlank { "حدث طبي" } },
            narrativeAr = narAr,
            narrativeEn = narEn,
            isAiGenerated = false,
        )
        dao.insertPermanentLog(initialLog)
        pushDocAsync("permanent_logs", initialLog.id) { FirestoreCloudSyncService.permanentLogToJson(initialLog) }

        val firebaseInitialized = runCatching { FirebaseApp.getApps(appContext).isNotEmpty() }.getOrDefault(false)
        if (firebaseInitialized && FirestoreCloudSyncService.isCloudNetworkAllowed()) {
            cloudScope.launch {
                val aiRes = geminiAnalyzer.generatePermanentLogWithGemini(
                    record = record,
                    member = member,
                    providerLabel = providerLabel,
                )
                aiRes.onSuccess { generated ->
                    val upgraded = initialLog.copy(
                        narrativeAr = generated.narrativeAr,
                        narrativeEn = generated.narrativeEn,
                        isAiGenerated = true,
                    )
                    dao.insertPermanentLog(upgraded)
                    val brand = runCatching { brandConfigFlow.first() }.getOrNull() ?: AppConfig.defaultBrand
                    FirestoreCloudSyncService.upsertDocument(
                        brand = brand,
                        subCollection = "permanent_logs",
                        docId = upgraded.id,
                        payloadJson = FirestoreCloudSyncService.permanentLogToJson(upgraded),
                    )
                }
            }
        }
    }

    private fun uploadAttachmentToDriveAsync(
        record: MedicalRecordEntity,
        clinicDriveUrl: String,
        clinicName: String,
    ) {
        if (record.attachmentPath.isBlank() || !FirestoreCloudSyncService.isCloudNetworkAllowed()) return
        cloudScope.launch {
            runCatching {
                val brand = brandConfigFlow.first()
                val doctor = if (record.doctorId.isNotBlank()) dao.getDoctorById(record.doctorId) else null
                val primary = doctor?.driveWebAppUrl?.takeIf { it.isNotBlank() } ?: clinicDriveUrl
                val secondary = doctor?.secondaryDriveWebAppUrl.orEmpty()
                val candidateUrls = GoogleDriveStorageHelper.resolveCandidateUrls(
                    primaryClinicUrl = primary,
                    secondaryClinicUrl = secondary,
                    platformFallbackUrl = brand.defaultDriveWebAppUrl,
                )
                if (candidateUrls.isEmpty()) return@launch
                GoogleDriveStorageHelper.uploadAttachmentWithFallback(
                    candidateUrls = candidateUrls,
                    localFilePath = record.attachmentPath,
                    fileName = record.attachmentName,
                    mimeType = record.attachmentMimeType,
                    clinicId = record.doctorId,
                    clinicName = clinicName,
                    patientName = record.patientName,
                    familyMemberId = record.familyMemberId,
                    recordId = record.id,
                    recordType = record.recordType,
                ).onSuccess { driveUrl ->
                    dao.updateMedicalRecordDriveUrl(record.id, driveUrl)
                    dao.getMedicalRecordById(record.id)?.let { updated ->
                        FirestoreCloudSyncService.upsertDocument(
                            brand = brand,
                            subCollection = "records",
                            docId = updated.id,
                            payloadJson = FirestoreCloudSyncService.recordToJson(updated),
                        )
                    }
                }
            }
        }
    }

    /**
     * تنظيف المرفقات الثقيلة (PDF / صور) التي تجاوزت مدة الاحتفاظ المعتمدة (90 / 180 / 360 يوماً)
     * مع الحفاظ التام على روابط Drive (إن وجدت) وعلى السجل الطبي التاريخي الدائم مدى الحياة (permanent_medical_logs).
     */
    suspend fun pruneExpiredHeavyAttachments(nowMillis: Long = System.currentTimeMillis()): Int {
        val brand = brandConfigFlow.first()
        val retentionDays = FirestoreCloudSyncService.normalizeRetentionDays(brand.fileRetentionDays)
        val cutoffMillis = nowMillis - (retentionDays.toLong() * ONE_DAY_MILLIS)
        val expiredRecords = dao.getMedicalRecordsWithAttachmentsOlderThan(cutoffMillis)
        if (expiredRecords.isEmpty()) return 0
        var pruned = 0
        expiredRecords.forEach { rec ->
            val oldPath = rec.attachmentPath
            dao.clearMedicalRecordAttachment(rec.id)
            if (oldPath.isNotBlank() && dao.countRecordsUsingAttachmentPath(oldPath) == 0) {
                ImageStorageHelper.deleteInternalFile(appContext, oldPath)
            }
            dao.getMedicalRecordById(rec.id)?.let { updatedRec ->
                pushDocAsync("records", updatedRec.id) { FirestoreCloudSyncService.recordToJson(updatedRec) }
            }
            pruned++
        }
        return pruned
    }

    suspend fun submitFamilyLinkRequest(
        requesterAccountId: String,
        targetPhone: String,
        relation: String = "SPOUSE",
        proofMarriageContractUrl: String = "",
        proofNationalIdFrontUrl: String = "",
        proofNationalIdBackUrl: String = "",
    ): Result<FamilyLinkRequestEntity> {
        val cleanPhone = toAsciiDigits(targetPhone).trim()
        val requester = dao.getAccountById(requesterAccountId)
            ?: return Result.failure(IllegalStateException("حساب مقدم الطلب غير مسجل"))
        if (requester.phone == cleanPhone) {
            return Result.failure(IllegalArgumentException("لا يمكن ربط الحساب بنفس رقم الهاتف"))
        }
        val targetAccount = dao.findAccountByPhone(cleanPhone)
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على حساب مسجل برقم هاتف الطرف الآخر ($cleanPhone). يرجى التأكد من تسجيله أولاً."))

        val req = FamilyLinkRequestEntity(
            id = "flr_${UUID.randomUUID()}",
            requesterAccountId = requesterAccountId,
            requesterName = requester.fullName,
            requesterPhone = requester.phone,
            targetPhone = cleanPhone,
            relation = relation,
            proofMarriageContractUrl = proofMarriageContractUrl,
            proofNationalIdFrontUrl = proofNationalIdFrontUrl,
            proofNationalIdBackUrl = proofNationalIdBackUrl,
            status = FamilyLinkStatus.PENDING,
            requestedAtMillis = System.currentTimeMillis(),
        )
        dao.insertFamilyLinkRequest(req)
        pushDocAsync("family_link_requests", req.id) { FirestoreCloudSyncService.familyLinkRequestToJson(req) }
        return Result.success(req)
    }

    suspend fun approveFamilyLinkRequest(
        requestId: String,
        reviewerName: String = "المدير العام",
    ): Result<Unit> {
        val req = dao.getFamilyLinkRequestById(requestId)
            ?: return Result.failure(IllegalStateException("طلب الربط غير موجود"))
        val requester = dao.getAccountById(req.requesterAccountId)
            ?: return Result.failure(IllegalStateException("حساب مقدم الطلب غير مسجل"))
        val target = dao.findAccountByPhone(req.targetPhone)
            ?: return Result.failure(IllegalStateException("حساب الطرف الآخر غير موجود"))

        val now = System.currentTimeMillis()
        dao.updateFamilyLinkRequestStatus(requestId, FamilyLinkStatus.APPROVED, reviewerName, now)

        dao.updateSpouseLink(requester.id, target.id, target.phone, target.fullName)
        dao.updateSpouseLink(target.id, requester.id, requester.phone, requester.fullName)

        pushDocAsync("family_link_requests", requestId) {
            FirestoreCloudSyncService.familyLinkRequestToJson(
                req.copy(status = FamilyLinkStatus.APPROVED, reviewedBy = reviewerName, reviewedAtMillis = now)
            )
        }
        val updatedReq = dao.getAccountById(requester.id)
        if (updatedReq != null) pushDocAsync("accounts", updatedReq.id) { FirestoreCloudSyncService.accountToJson(updatedReq) }
        val updatedTarget = dao.getAccountById(target.id)
        if (updatedTarget != null) pushDocAsync("accounts", updatedTarget.id) { FirestoreCloudSyncService.accountToJson(updatedTarget) }
        return Result.success(Unit)
    }

    suspend fun rejectFamilyLinkRequest(
        requestId: String,
        reviewerName: String = "المدير العام",
        notes: String = "",
    ): Result<Unit> {
        val req = dao.getFamilyLinkRequestById(requestId)
            ?: return Result.failure(IllegalStateException("طلب الربط غير موجود"))
        val now = System.currentTimeMillis()
        dao.updateFamilyLinkRequestStatus(requestId, FamilyLinkStatus.REJECTED, reviewerName, now, notes)
        pushDocAsync("family_link_requests", requestId) {
            FirestoreCloudSyncService.familyLinkRequestToJson(
                req.copy(status = FamilyLinkStatus.REJECTED, reviewedBy = reviewerName, reviewedAtMillis = now, notes = notes)
            )
        }
        return Result.success(Unit)
    }

    suspend fun setSpouseViewPermission(accountId: String, allow: Boolean): Result<Unit> {
        dao.setSpouseViewPermission(accountId, allow)
        val updated = dao.getAccountById(accountId)
        if (updated != null) {
            pushDocAsync("accounts", updated.id) { FirestoreCloudSyncService.accountToJson(updated) }
        }
        return Result.success(Unit)
    }

    suspend fun updateDoctorDriveAccounts(doctorId: String, primaryUrl: String, secondaryUrl: String): Result<Unit> {
        dao.updateDoctorDriveAccounts(doctorId, primaryUrl.trim(), secondaryUrl.trim())
        val doc = dao.getDoctorById(doctorId)
        if (doc != null) {
            pushDocAsync("doctors", docId = doctorId) { FirestoreCloudSyncService.doctorToJson(doc) }
        }
        return Result.success(Unit)
    }

    suspend fun recordVisitorHit(platform: String = "APK") {
        val hit = PlatformVisitorHitEntity(
            id = "hit_${UUID.randomUUID()}",
            platform = platform,
            timestampMillis = System.currentTimeMillis(),
        )
        dao.insertVisitorHit(hit)
        pushDocAsync("visitor_hits", hit.id) { FirestoreCloudSyncService.visitorHitToJson(hit) }
    }

    suspend fun getVisitorStats(): VisitorStats {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val startOfWeek = now - (7L * 86_400_000L)
        val startOfMonth = now - (30L * 86_400_000L)

        val todayTotal = dao.countVisitorHitsSince(startOfDay)
        val todayApk = dao.countVisitorHitsByPlatformSince("APK", startOfDay)
        val todayWeb = dao.countVisitorHitsByPlatformSince("WEB", startOfDay)

        val weekTotal = dao.countVisitorHitsSince(startOfWeek)
        val weekApk = dao.countVisitorHitsByPlatformSince("APK", startOfWeek)
        val weekWeb = dao.countVisitorHitsByPlatformSince("WEB", startOfWeek)

        val monthTotal = dao.countVisitorHitsSince(startOfMonth)
        val monthApk = dao.countVisitorHitsByPlatformSince("APK", startOfMonth)
        val monthWeb = dao.countVisitorHitsByPlatformSince("WEB", startOfMonth)

        return VisitorStats(
            todayTotal = todayTotal,
            todayApk = todayApk,
            todayWeb = todayWeb,
            weekTotal = weekTotal.coerceAtLeast(todayTotal),
            weekApk = weekApk.coerceAtLeast(todayApk),
            weekWeb = weekWeb.coerceAtLeast(todayWeb),
            monthTotal = monthTotal.coerceAtLeast(weekTotal),
            monthApk = monthApk.coerceAtLeast(weekApk),
            monthWeb = monthWeb.coerceAtLeast(weekWeb),
        )
    }

    companion object {
        const val MAX_ADMIN_PIN_ATTEMPTS = 3
        const val ADMIN_LOCKOUT_MILLIS = 15 * 60_000L
        const val MASTER_ADMIN_OTP = "999000"
        const val OTP_TTL_MILLIS = 24L * 60L * 60L * 1000L
        const val PATIENT_MODIFY_CUTOFF_MINUTES = 120
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
