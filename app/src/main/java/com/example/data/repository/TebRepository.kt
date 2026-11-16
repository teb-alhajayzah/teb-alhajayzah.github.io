package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.config.AppConfig
import com.example.config.PlatformBrandConfig
import com.example.data.local.ActivationOtpCodeEntity
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BlockedPhoneEntity
import com.example.data.local.BookingSource
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.DoctorFavoriteMedEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.PatientAccountEntity
import com.example.data.local.SlotStatus
import com.example.data.local.TebDao
import com.example.data.medications.EgyptianMedicationService
import com.example.data.medications.MedicationFetchResult
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.UUID

private val Context.tebDataStore by preferencesDataStore(name = "teb_prefs")

class TebRepository(
    private val dao: TebDao,
    private val appContext: Context,
) {
    private val seedMutex = Mutex()
    private val secureRandom = SecureRandom()

    private val activeAccountKey = stringPreferencesKey("active_account_id")
    private val activeMemberKey = stringPreferencesKey("active_member_id")
    private val activeDoctorPortalKey = stringPreferencesKey("active_doctor_portal_id")
    private val loggedInDoctorIdKey = stringPreferencesKey("logged_in_doctor_id")
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
    private val cfgSmsGateServerUrlKey = stringPreferencesKey("cfg_smsgate_server_url")
    private val cfgSmsGateUsernameKey = stringPreferencesKey("cfg_smsgate_username")
    private val cfgSmsGatePasswordKey = stringPreferencesKey("cfg_smsgate_password")

    val activeAccountIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeAccountKey] ?: DEFAULT_ACCOUNT_ID }

    val activeMemberIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeMemberKey] ?: DEFAULT_MEMBER_SELF_ID }

    val activeDoctorPortalIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeDoctorPortalKey] ?: "doc_batna_1" }

    val loggedInDoctorIdFlow: Flow<String?> = appContext.tebDataStore.data
        .map { prefs -> prefs[loggedInDoctorIdKey]?.takeIf { it.isNotBlank() } }

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
                smsGateServerUrl = prefs[cfgSmsGateServerUrlKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.smsGateServerUrl,
                smsGateUsername = prefs[cfgSmsGateUsernameKey] ?: AppConfig.defaultBrand.smsGateUsername,
                smsGatePassword = prefs[cfgSmsGatePasswordKey] ?: AppConfig.defaultBrand.smsGatePassword,
            )
        }

    val adminSecurityFlow: Flow<Pair<Int, Long>> = appContext.tebDataStore.data
        .map { prefs ->
            (prefs[adminFailedAttemptsKey] ?: 0) to (prefs[adminLockoutUntilKey] ?: 0L)
        }

    val doctorsFlow: Flow<List<ClinicDoctorEntity>> = dao.observeDoctors()
    val allSlotsFlow: Flow<List<AppointmentSlotEntity>> = dao.observeAllSlots()
    val allAccountsFlow: Flow<List<PatientAccountEntity>> = dao.observeAllAccounts()
    val allMedicalRecordsFlow: Flow<List<MedicalRecordEntity>> = dao.observeAllMedicalRecords()
    val notificationsFlow: Flow<List<DelayNotificationEntity>> = dao.observeNotifications()
    val allDoctorFavoritesFlow: Flow<List<DoctorFavoriteMedEntity>> = dao.observeAllDoctorFavorites()
    val blockedPhonesFlow: Flow<List<BlockedPhoneEntity>> = dao.observeBlockedPhones()
    val activationCodesFlow: Flow<List<ActivationOtpCodeEntity>> = dao.observeAllActivationCodes()

    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>> =
        dao.observeFamilyMembers(accountId)

    fun observeSlotsForDoctor(doctorId: String): Flow<List<AppointmentSlotEntity>> =
        dao.observeSlotsForDoctor(doctorId)

    suspend fun setActiveMember(memberId: String) {
        appContext.tebDataStore.edit { it[activeMemberKey] = memberId }
    }

    suspend fun setActiveDoctorPortal(doctorId: String) {
        appContext.tebDataStore.edit { it[activeDoctorPortalKey] = doctorId }
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
        val expectedPin = prefs[adminPinKey]?.takeIf { it.isNotBlank() } ?: AppConfig.defaultBrand.adminPin
        if (enteredPin.trim() == expectedPin) {
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
        }
    }

    suspend fun updateSmsGateSettings(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        val cleanUrl = serverUrl.trim().ifEmpty { AppConfig.defaultBrand.smsGateServerUrl }
        appContext.tebDataStore.edit { prefs ->
            prefs[cfgSmsGateServerUrlKey] = cleanUrl
            prefs[cfgSmsGateUsernameKey] = username.trim()
            prefs[cfgSmsGatePasswordKey] = password.trim()
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
            prefs.remove(cfgSmsGateServerUrlKey)
            prefs.remove(cfgSmsGateUsernameKey)
            prefs.remove(cfgSmsGatePasswordKey)
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

    suspend fun markAccountPhoneVerified(accountId: String) {
        dao.markAccountPhoneVerified(accountId)
    }

    suspend fun generateActivationCodeBatch(count: Int, generatedBy: String = "ADMIN"): Result<Int> {
        val safeCount = count.coerceIn(1, 100)
        val existingCodes = dao.getAllActivationCodesOnce().map { it.code }.toHashSet()
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
    ): Result<Unit> {
        val cleanCode = enteredCode.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كود التفعيل المكون من 6 أرقام"))
        }
        val codeEntity = dao.getActivationCode(cleanCode)
            ?: return Result.failure(IllegalArgumentException("كود التفعيل غير صحيح أو غير مسجل. اطلب الكود من السكرتيرة أو عبر واتساب."))
        if (codeEntity.isUsed) {
            val usedInfo = if (codeEntity.usedByName.isNotBlank()) " (${codeEntity.usedByName})" else ""
            return Result.failure(IllegalStateException("هذا الكود تم استخدامه بالفعل ومنتهي الصلاحية$usedInfo، ولا يمكن استخدامه مجدداً."))
        }
        val redeemed = dao.redeemActivationCodeForAccount(
            code = cleanCode,
            accountId = accountId,
            usedByName = accountName.trim(),
            usedByPhone = accountPhone.trim(),
            usedAtMillis = System.currentTimeMillis(),
        )
        return if (redeemed) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("هذا الكود تم استخدامه بالفعل ومنتهي الصلاحية."))
        }
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
            locationLabel = locationLabel.trim().ifEmpty { "الحجايزة - الشارع الرئيسي" },
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

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (dao.getAllActivationCodesOnce().isEmpty()) {
                generateActivationCodeBatch(5, "SYSTEM")
            }
            if (dao.countDoctors() > 0) return

            val defaultAccount = PatientAccountEntity(
                id = DEFAULT_ACCOUNT_ID,
                fullName = "أحمد محمد السيد",
                phone = "01012345678",
                nationalId = "29005141200451",
                password = "1234",
            )
            dao.insertAccount(defaultAccount)

            val familyMembers = listOf(
                FamilyMemberEntity(
                    id = DEFAULT_MEMBER_SELF_ID,
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "أحمد محمد السيد",
                    relation = "نفسي (صاحب الحساب)",
                    age = 36,
                    phone = "01012345678",
                ),
                FamilyMemberEntity(
                    id = "mem_mother",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "سعاد حسن عبد الله",
                    relation = "الأم",
                    age = 62,
                    phone = "01012345678",
                ),
                FamilyMemberEntity(
                    id = "mem_wife",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "فاطمة علي محمود",
                    relation = "الزوجة",
                    age = 31,
                    phone = "01098765432",
                ),
                FamilyMemberEntity(
                    id = "mem_son",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "كريم أحمد محمد",
                    relation = "الابن",
                    age = 9,
                    phone = "01012345678",
                ),
            )
            dao.insertFamilyMembers(familyMembers)

            val hospitalName = AppConfig.defaultBrand.hospitalNameAr
            val defaultMapsUrl = "https://maps.google.com/?q=الحجايزة+السنبلاوين+الدقهلية"
            val doctors = listOf(
                ClinicDoctorEntity(
                    id = "doc_batna_1",
                    doctorName = "د. محمد عبد الرحمن",
                    specialty = "باطنة وجهاز هضمي",
                    clinicName = "عيادة الشفاء التخصصية",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "شارع المستشفى، الحجايزة",
                    feeEgp = 150,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 21 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 15,
                    bio = "أخصائي أمراض الباطنة والجهاز الهضمي والكبد — خبرة 15 عاماً.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001001",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_kids_1",
                    doctorName = "د. سارة محمود النجار",
                    specialty = "أطفال وحديثي الولادة",
                    clinicName = "عيادة براعم الحجايزة",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "بجوار المسجد الكبير، الحجايزة",
                    feeEgp = 130,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 11,
                    bio = "أخصائي طب الأطفال وحديثي الولادة ومتابعة النمو والتغذية.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001002",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_ortho_1",
                    doctorName = "د. طارق حسن الشافعي",
                    specialty = "عظام ومفاصل",
                    clinicName = "$hospitalName - قسم العظام",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأول",
                    feeEgp = 100,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 21 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 14,
                    bio = "استشاري جراحة العظام والكسور والمفاصل وإصابات الملاعب.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001003",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_obgyn_1",
                    doctorName = "د. نادية إبراهيم زكي",
                    specialty = "نساء وتوليد",
                    clinicName = "$hospitalName - قسم النساء",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الثاني",
                    feeEgp = 120,
                    workStartMinutes = 18 * 60,
                    workEndMinutes = 21 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 16,
                    bio = "أخصائي أمراض النساء والتوليد ومتابعة الحمل بالسونار.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001004",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_dental_1",
                    doctorName = "د. كريم طارق المنصوري",
                    specialty = "أسنان",
                    clinicName = "عيادة الابتسامة للأسنان",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "أمام الوحدة المحلية، الحجايزة",
                    feeEgp = 150,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 10,
                    bio = "طب وجراحة الفم والأسنان وعلاج الجذور والتركيبات.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001005",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_cardio_1",
                    doctorName = "د. خالد مصطفى العطار",
                    specialty = "قلب وأوعية دموية",
                    clinicName = "$hospitalName - وحدة القلب",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأرضي",
                    feeEgp = 160,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 18,
                    bio = "استشاري أمراض القلب والأوعية الدموية ورسم القلب والإيكو.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001006",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_eye_1",
                    doctorName = "د. هالة فؤاد سلامة",
                    specialty = "رمد وجراحة عيون",
                    clinicName = "مركز النور للعيون",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "ميدان الحجايزة الرئيسي",
                    feeEgp = 140,
                    workStartMinutes = 17 * 60 + 20,
                    workEndMinutes = 20 * 60 + 40,
                    slotDurationMinutes = 20,
                    rating = 4.7f,
                    experienceYears = 10,
                    bio = "أخصائي طب وجراحة العيون وفحص قاع العين والنظارات.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001007",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_ent_1",
                    doctorName = "د. عمرو يحيى البنا",
                    specialty = "أنف وأذن وحنجرة",
                    clinicName = "$hospitalName - عيادة 4",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأول",
                    feeEgp = 100,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 19 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.7f,
                    experienceYears = 9,
                    bio = "أخصائي جراحة الأنف والأذن والحنجرة وعلاج الجيوب الأنفية.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001008",
                    password = "1234",
                ),
                ClinicDoctorEntity(
                    id = "doc_derm_1",
                    doctorName = "د. ريم عادل الدسوقي",
                    specialty = "جلدية وتجميل",
                    clinicName = "عيادة الندى للجلدية",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "طريق المركز، الحجايزة",
                    feeEgp = 120,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 20 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 8,
                    bio = "أخصائي الأمراض الجلدية والتناسلية والعناية بالبشرة والشعر.",
                    mapsUrl = defaultMapsUrl,
                    username = "01001001009",
                    password = "1234",
                ),
            )
            dao.insertDoctors(doctors)

            // Seed default doctor favorites
            listOf(
                DoctorFavoriteMedEntity("doc_batna_1", "med_controloc"),
                DoctorFavoriteMedEntity("doc_batna_1", "med_gaviscon"),
                DoctorFavoriteMedEntity("doc_batna_1", "med_motilium"),
                DoctorFavoriteMedEntity("doc_kids_1", "med_augmentin_ped"),
                DoctorFavoriteMedEntity("doc_kids_1", "med_brufen_ped"),
                DoctorFavoriteMedEntity("doc_dental_1", "med_augmentin_1g"),
                DoctorFavoriteMedEntity("doc_dental_1", "med_catafast"),
            ).forEach { dao.insertFavoriteMed(it) }

            val allSlots = mutableListOf<AppointmentSlotEntity>()
            doctors.forEach { doc ->
                allSlots += buildSlotsForDoctor(
                    doctor = doc,
                    preBookedIndices = when (doc.id) {
                        "doc_batna_1" -> mapOf(
                            0 to Triple("محمود إبراهيم علي", "01022334455", SlotStatus.COMPLETED),
                            1 to Triple("أحمد محمد السيد", "01012345678", SlotStatus.BOOKED),
                            4 to Triple("سيد عبد الفتاح", "01144556677", SlotStatus.BOOKED),
                        )
                        "doc_kids_1" -> mapOf(
                            2 to Triple("كريم أحمد محمد", "01012345678", SlotStatus.BOOKED),
                        )
                        "doc_ortho_1" -> mapOf(
                            1 to Triple("سعاد حسن عبد الله", "01012345678", SlotStatus.BOOKED),
                            3 to Triple("حسن عبد القادر", "01288990011", SlotStatus.BOOKED),
                        )
                        else -> mapOf(
                            1 to Triple("علاء فتحي سالم", "01077889900", SlotStatus.BOOKED),
                        )
                    },
                )
            }
            dao.insertSlots(allSlots)

            val now = System.currentTimeMillis()
            val sampleRecords = listOf(
                MedicalRecordEntity(
                    id = "rec_1",
                    familyMemberId = DEFAULT_MEMBER_SELF_ID,
                    patientName = "أحمد محمد السيد",
                    patientPhone = "01012345678",
                    doctorId = "doc_batna_1",
                    doctorName = "د. محمد عبد الرحمن",
                    specialty = "باطنة وجهاز هضمي",
                    visitDateLabel = "10 نوفمبر 2026",
                    diagnosis = "التهاب بسيط بالمعدة وارتجاع مريئي",
                    medications = "• كونترولوك (Controloc) 40 مجم — قرص قبل الفطار بنصف ساعة\n• جافيسكون (Gaviscon) — ملعقة كبيرة بعد الأكل وعند اللزوم\n• موتيليوم (Motilium) 10 مجم — قرص قبل الغداء والعشاء",
                    notes = "الابتعاد عن الأطعمة الدسمة والمشروبات الغازية لمدة أسبوعين.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 5 * 86_400_000L,
                ),
                MedicalRecordEntity(
                    id = "rec_2",
                    familyMemberId = "mem_mother",
                    patientName = "سعاد حسن عبد الله",
                    patientPhone = "01012345678",
                    doctorId = "doc_cardio_1",
                    doctorName = "د. خالد مصطفى العطار",
                    specialty = "قلب وأوعية دموية",
                    visitDateLabel = "3 نوفمبر 2026",
                    diagnosis = "متابعة ضغط الدم المرتفع (مستقر)",
                    medications = "• كونكور (Concor) 5 مجم — قرص صباحاً يومياً\n• أسبوسيد (Aspocid) 75 مجم — قرص بعد الغداء",
                    notes = "قياس الضغط مرتين أسبوعياً وتقليل الملح في الطعام.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 12 * 86_400_000L,
                ),
                MedicalRecordEntity(
                    id = "rec_3",
                    familyMemberId = "mem_son",
                    patientName = "كريم أحمد محمد",
                    patientPhone = "01012345678",
                    doctorId = "doc_kids_1",
                    doctorName = "د. سارة محمود النجار",
                    specialty = "أطفال وحديثي الولادة",
                    visitDateLabel = "28 أكتوبر 2026",
                    diagnosis = "نزلة شعبية حادة مع ارتفاع طفيف في الحرارة",
                    medications = "• أوجمنتين شراب (Augmentin) 457 مجم/5 مل — 5 سم كل 12 ساعة لمدة 7 أيام\n• بروفين شراب (Brufen) 100 مجم/5 مل — عند اللزوم لخفض الحرارة\n• فاركولين محلول (Farcolin) — جلسة بخار صباحاً ومساءً",
                    notes = "شرب سوائل دافئة والراحة التامة.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 18 * 86_400_000L,
                ),
            )
            dao.insertMedicalRecords(sampleRecords)
        }
    }

    private fun buildSlotsForDoctor(
        doctor: ClinicDoctorEntity,
        preBookedIndices: Map<Int, Triple<String, String, String>> = emptyMap(),
    ): List<AppointmentSlotEntity> {
        val list = mutableListOf<AppointmentSlotEntity>()
        var current = doctor.workStartMinutes
        var index = 0
        while (current < doctor.workEndMinutes) {
            val pre = preBookedIndices[index]
            val familyMemberId = when (pre?.first) {
                "أحمد محمد السيد" -> DEFAULT_MEMBER_SELF_ID
                "سعاد حسن عبد الله" -> "mem_mother"
                "فاطمة علي محمود" -> "mem_wife"
                "كريم أحمد محمد" -> "mem_son"
                else -> ""
            }
            list += AppointmentSlotEntity(
                id = "${doctor.id}_today_$index",
                doctorId = doctor.id,
                dateLabel = "اليوم",
                startMinutes = current,
                delayMinutes = 0,
                status = pre?.third ?: SlotStatus.AVAILABLE,
                patientName = pre?.first ?: "",
                patientPhone = pre?.second ?: "",
                familyMemberId = familyMemberId,
                bookingSource = if (pre != null) BookingSource.APP else BookingSource.NONE,
                bookedAtMillis = if (pre != null) System.currentTimeMillis() else 0L,
            )
            current += doctor.slotDurationMinutes
            index++
        }
        return list
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
        return Result.success(Unit)
    }

    suspend fun cancelSlot(slotId: String) {
        val slot = dao.getSlotById(slotId) ?: return
        if (slot.status == SlotStatus.BOOKED) {
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
    ): Result<MedicalRecordEntity> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الحجز غير موجود"))
        val doctor = dao.getDoctorById(slot.doctorId)
            ?: return Result.failure(IllegalStateException("الطبيب غير موجود"))

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
            diagnosis = diagnosis.trim(),
            medications = medications.trim(),
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            recordType = MedicalRecordType.PRESCRIPTION,
            createdAtMillis = System.currentTimeMillis(),
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
    ) {
        val record = MedicalRecordEntity(
            id = "upload_${UUID.randomUUID()}",
            familyMemberId = familyMemberId,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = "",
            doctorName = "رفع شخصي من المريض",
            specialty = "تحاليل وأشعة",
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = title.trim().ifEmpty { "صورة تحليل / أشعة" },
            medications = "",
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            recordType = MedicalRecordType.PATIENT_UPLOAD,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
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
        val cleanId = identifier.trim()
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

    suspend fun registerPatientAccount(
        fullName: String,
        phone: String,
        nationalId: String,
        password: String,
    ): Result<PatientAccountEntity> {
        val cleanPhone = phone.trim()
        if (dao.isPhoneBlocked(cleanPhone) > 0) {
            return Result.failure(IllegalStateException("عذراً، هذا الرقم محظور من التسجيل في المنصة"))
        }
        val existing = dao.findAccountByPhone(cleanPhone)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("رقم الهاتف مسجل بالفعل"))
        }
        val accountId = "acc_${UUID.randomUUID()}"
        val account = PatientAccountEntity(
            id = accountId,
            fullName = fullName.trim(),
            phone = cleanPhone,
            nationalId = nationalId.trim(),
            password = password.trim().ifEmpty { "1234" },
            isPhoneVerified = false,
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

    companion object {
        const val DEFAULT_ACCOUNT_ID = "acc_ahmed"
        const val DEFAULT_MEMBER_SELF_ID = "mem_self_ahmed"
        const val MAX_ADMIN_PIN_ATTEMPTS = 3
        const val ADMIN_LOCKOUT_MILLIS = 15 * 60_000L
    }
}
