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
import com.example.data.local.MedicalLabEntity
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

data class LabPatientSearchResult(
    val account: PatientAccountEntity,
    val familyMembers: List<FamilyMemberEntity>,
)

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

    /**
     * تهيئة نظيفة لبيئة الإنتاج:
     * تبدأ بدون أي بيانات وهمية أو حسابات مسبقة، وتجهز فقط دفعة أكواد تفعيل نظامية عند الحاجة.
     */
    suspend fun ensureSeeded() {
        seedMutex.withLock {
            val existingAccounts = dao.getAllAccountsOnce()
            val prefs = appContext.tebDataStore.data.first()
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
            attachmentMimeType = if (attachmentPath.isNotBlank()) "image/jpeg" else "",
            attachmentName = if (attachmentPath.isNotBlank()) "روشتة_ورقية.jpg" else "",
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
        attachmentMimeType: String = "image/jpeg",
        attachmentName: String = "",
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

    private fun normalizePhoneDigits(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("0020") -> "0${digits.substring(4)}"
            digits.startsWith("20") && digits.length >= 12 -> "0${digits.substring(2)}"
            else -> digits
        }
    }

    companion object {
        const val MAX_ADMIN_PIN_ATTEMPTS = 3
        const val ADMIN_LOCKOUT_MILLIS = 15 * 60_000L
    }
}
