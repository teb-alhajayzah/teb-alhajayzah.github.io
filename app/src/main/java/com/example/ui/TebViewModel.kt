package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.FirebaseReadyTestOtpProvider
import com.example.auth.OtpChallenge
import com.example.auth.PhoneOtpAuthProvider
import com.example.config.AppConfig
import com.example.config.PlatformBrandConfig
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BlockedPhoneEntity
import com.example.data.local.BookingSource
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.DoctorFavoriteMedEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.PatientAccountEntity
import com.example.data.medications.EgyptianMedication
import com.example.data.medications.MedicationCatalog
import com.example.data.repository.TebRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRoleMode(val labelAr: String) {
    PATIENT("واجهة المريض"),
    DOCTOR_ADMIN("لوحة الدكتور والسكرتيرة"),
    SUPER_ADMIN("إدارة المنصة"),
}

enum class PatientTab(val labelAr: String) {
    HOME("الرئيسية"),
    PRIVATE_CLINICS("العيادات الخاصة"),
    HOSPITAL("المستشفى التخصصي"),
    SPECIALTIES("التخصصات"),
    MEDICAL_FILE("ملفي الطبي"),
}

data class TebUiState(
    val isReady: Boolean = false,
    val roleMode: AppRoleMode = AppRoleMode.PATIENT,
    val patientTab: PatientTab = PatientTab.HOME,
    val selectedBookingDoctorId: String? = null,
    val selectedSpecialty: String? = null,
    val searchQuery: String = "",
    val brandConfig: PlatformBrandConfig = AppConfig.defaultBrand,
    val activeAccount: PatientAccountEntity? = null,
    val familyMembers: List<FamilyMemberEntity> = emptyList(),
    val activeMemberId: String = "",
    val doctors: List<ClinicDoctorEntity> = emptyList(),
    val allSlots: List<AppointmentSlotEntity> = emptyList(),
    val medicalRecords: List<MedicalRecordEntity> = emptyList(),
    val notifications: List<DelayNotificationEntity> = emptyList(),
    val blockedPhones: List<BlockedPhoneEntity> = emptyList(),
    val activeAdminDoctorId: String = "doc_batna_1",
    val loggedInDoctorId: String? = null,
    val isAdminLoggedIn: Boolean = false,
    val adminFailedAttempts: Int = 0,
    val adminLockoutUntilMillis: Long = 0L,
    val activeOtpChallenge: OtpChallenge? = null,
    val adminUsername: String = AppConfig.defaultBrand.adminUsername,
    val adminPassword: String = AppConfig.defaultBrand.adminPassword,
    val medications: List<EgyptianMedication> = MedicationCatalog.builtInMedications,
    val isLiveMedicationsApi: Boolean = false,
    val favoriteMedsByDoctor: Map<String, Set<String>> = emptyMap(),
) {
    val activeMember: FamilyMemberEntity?
        get() = familyMembers.firstOrNull { it.id == activeMemberId } ?: familyMembers.firstOrNull()

    val privateClinics: List<ClinicDoctorEntity>
        get() = doctors.filter { it.clinicType == ClinicType.PRIVATE }

    val hospitalClinics: List<ClinicDoctorEntity>
        get() = doctors.filter { it.clinicType == ClinicType.HOSPITAL }

    val specialties: List<String>
        get() = doctors.map { it.specialty }.distinct()

    val selectedBookingDoctor: ClinicDoctorEntity?
        get() = doctors.firstOrNull { it.id == selectedBookingDoctorId }

    val activeAdminDoctor: ClinicDoctorEntity?
        get() = doctors.firstOrNull { it.id == activeAdminDoctorId } ?: doctors.firstOrNull()

    val loggedInDoctor: ClinicDoctorEntity?
        get() = doctors.firstOrNull { it.id == loggedInDoctorId }

    val unreadAlertsCount: Int
        get() = notifications.count { !it.isRead }

    val isAccountVerified: Boolean
        get() = activeAccount?.isPhoneVerified ?: true

    fun favoriteMedicationIdsForDoctor(doctorId: String): Set<String> =
        favoriteMedsByDoctor[doctorId] ?: emptySet()

    fun slotsForDoctor(doctorId: String): List<AppointmentSlotEntity> =
        allSlots.filter { it.doctorId == doctorId }.sortedBy { it.startMinutes }

    fun recordsForMember(member: FamilyMemberEntity?): List<MedicalRecordEntity> {
        if (member == null) return emptyList()
        return medicalRecords.filter { rec ->
            rec.familyMemberId == member.id ||
                (rec.patientName.isNotBlank() && rec.patientName == member.fullName)
        }.sortedByDescending { it.createdAtMillis }
    }

    fun recordsForPatient(familyMemberId: String, patientName: String, patientPhone: String): List<MedicalRecordEntity> {
        return medicalRecords.filter { rec ->
            (familyMemberId.isNotBlank() && rec.familyMemberId == familyMemberId) ||
                (patientName.isNotBlank() && rec.patientName == patientName) ||
                (patientPhone.isNotBlank() && rec.patientPhone == patientPhone && rec.patientName == patientName)
        }.sortedByDescending { it.createdAtMillis }
    }
}

class TebViewModel(
    private val repository: TebRepository,
    private val otpProvider: PhoneOtpAuthProvider = FirebaseReadyTestOtpProvider(),
) : ViewModel() {

    private val _roleMode = MutableStateFlow(AppRoleMode.PATIENT)
    private val _patientTab = MutableStateFlow(PatientTab.HOME)
    private val _selectedBookingDoctorId = MutableStateFlow<String?>(null)
    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSeeded = MutableStateFlow(false)
    private val _isAdminLoggedIn = MutableStateFlow(false)
    private val _activeOtpChallenge = MutableStateFlow<OtpChallenge?>(null)
    private val _medications = MutableStateFlow(MedicationCatalog.builtInMedications)
    private val _isLiveMedicationsApi = MutableStateFlow(false)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _openAccountForOtpEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val openAccountForOtpEvents = _openAccountForOtpEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isSeeded.value = true
        }
        refreshEgyptianMedications(AppConfig.defaultBrand.medicationsApiUrl)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeFamilyFlow = repository.activeAccountIdFlow.flatMapLatest { accountId ->
        repository.observeFamilyMembers(accountId)
    }

    private data class CoreUiSelection(
        val isSeeded: Boolean,
        val roleMode: AppRoleMode,
        val patientTab: PatientTab,
        val bookingDoctorId: String?,
        val specialty: String?,
        val searchQuery: String,
        val brandConfig: PlatformBrandConfig,
        val isAdminLoggedIn: Boolean,
        val activeOtpChallenge: OtpChallenge?,
    )

    private data class AccountDataBundle(
        val activeAccountId: String,
        val activeMemberId: String,
        val accounts: List<PatientAccountEntity>,
        val familyMembers: List<FamilyMemberEntity>,
        val activeAdminDoctorId: String,
        val loggedInDoctorId: String?,
        val adminFailedAttempts: Int,
        val adminLockoutUntilMillis: Long,
    )

    private data class ClinicDataBundle(
        val doctors: List<ClinicDoctorEntity>,
        val slots: List<AppointmentSlotEntity>,
        val records: List<MedicalRecordEntity>,
        val notifications: List<DelayNotificationEntity>,
        val blockedPhones: List<BlockedPhoneEntity>,
    )

    private data class MedicationBundle(
        val medications: List<EgyptianMedication>,
        val isLiveRemote: Boolean,
        val favorites: List<DoctorFavoriteMedEntity>,
    )

    private val selectionFlow = combine(
        _isSeeded,
        _roleMode,
        _patientTab,
        _selectedBookingDoctorId,
        _selectedSpecialty,
        _searchQuery,
        repository.brandConfigFlow,
        _isAdminLoggedIn,
        _activeOtpChallenge,
    ) { values ->
        CoreUiSelection(
            isSeeded = values[0] as Boolean,
            roleMode = values[1] as AppRoleMode,
            patientTab = values[2] as PatientTab,
            bookingDoctorId = values[3] as String?,
            specialty = values[4] as String?,
            searchQuery = values[5] as String,
            brandConfig = values[6] as PlatformBrandConfig,
            isAdminLoggedIn = values[7] as Boolean,
            activeOtpChallenge = values[8] as OtpChallenge?,
        )
    }

    private val accountIdsFlow = combine(
        repository.activeAccountIdFlow,
        repository.activeMemberIdFlow,
        repository.allAccountsFlow,
        activeFamilyFlow,
        repository.activeDoctorPortalIdFlow,
    ) { activeAccountId, activeMemberId, accounts, familyMembers, adminDoctorId ->
        listOf(activeAccountId, activeMemberId, accounts, familyMembers, adminDoctorId)
    }

    private val authCredsFlow = combine(
        repository.loggedInDoctorIdFlow,
        repository.adminSecurityFlow,
    ) { loggedDocId, sec ->
        Triple(loggedDocId, sec.first, sec.second)
    }

    private val accountBundleFlow = combine(accountIdsFlow, authCredsFlow) { ids, creds ->
        @Suppress("UNCHECKED_CAST")
        AccountDataBundle(
            activeAccountId = ids[0] as String,
            activeMemberId = ids[1] as String,
            accounts = ids[2] as List<PatientAccountEntity>,
            familyMembers = ids[3] as List<FamilyMemberEntity>,
            activeAdminDoctorId = ids[4] as String,
            loggedInDoctorId = creds.first,
            adminFailedAttempts = creds.second,
            adminLockoutUntilMillis = creds.third,
        )
    }

    private val clinicBundleFlow = combine(
        repository.doctorsFlow,
        repository.allSlotsFlow,
        repository.allMedicalRecordsFlow,
        repository.notificationsFlow,
        repository.blockedPhonesFlow,
    ) { doctors, slots, records, notifications, blocked ->
        ClinicDataBundle(doctors, slots, records, notifications, blocked)
    }

    private val medicationBundleFlow = combine(
        _medications,
        _isLiveMedicationsApi,
        repository.allDoctorFavoritesFlow,
    ) { meds, isLive, favs ->
        MedicationBundle(meds, isLive, favs)
    }

    val uiState: StateFlow<TebUiState> = combine(
        selectionFlow,
        accountBundleFlow,
        clinicBundleFlow,
        medicationBundleFlow,
    ) { sel, acc, clinic, medBundle ->
        val favMap = medBundle.favorites
            .groupBy { it.doctorId }
            .mapValues { (_, list) -> list.map { it.medicationId }.toSet() }

        val activeAccount = acc.accounts.firstOrNull { it.id == acc.activeAccountId }
            ?: acc.accounts.firstOrNull()

        TebUiState(
            isReady = sel.isSeeded,
            roleMode = sel.roleMode,
            patientTab = sel.patientTab,
            selectedBookingDoctorId = sel.bookingDoctorId,
            selectedSpecialty = sel.specialty,
            searchQuery = sel.searchQuery,
            brandConfig = sel.brandConfig,
            activeAccount = activeAccount,
            familyMembers = acc.familyMembers,
            activeMemberId = acc.activeMemberId,
            doctors = clinic.doctors,
            allSlots = clinic.slots,
            medicalRecords = clinic.records,
            notifications = clinic.notifications,
            blockedPhones = clinic.blockedPhones,
            activeAdminDoctorId = acc.activeAdminDoctorId,
            loggedInDoctorId = acc.loggedInDoctorId,
            isAdminLoggedIn = sel.isAdminLoggedIn,
            adminFailedAttempts = acc.adminFailedAttempts,
            adminLockoutUntilMillis = acc.adminLockoutUntilMillis,
            activeOtpChallenge = sel.activeOtpChallenge,
            adminUsername = sel.brandConfig.adminUsername,
            adminPassword = sel.brandConfig.adminPassword,
            medications = medBundle.medications,
            isLiveMedicationsApi = medBundle.isLiveRemote,
            favoriteMedsByDoctor = favMap,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TebUiState(),
    )

    fun refreshEgyptianMedications(apiUrl: String) {
        viewModelScope.launch {
            val result = repository.loadEgyptianMedications(apiUrl)
            _medications.value = result.medications
            _isLiveMedicationsApi.value = result.isLiveRemote
        }
    }

    fun toggleFavoriteMedication(doctorId: String, medicationId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteMedication(doctorId, medicationId)
        }
    }

    fun switchRoleMode(mode: AppRoleMode) {
        if (mode == AppRoleMode.SUPER_ADMIN && !_isAdminLoggedIn.value) {
            return
        }
        _roleMode.value = mode
    }

    fun selectPatientTab(tab: PatientTab) {
        _selectedBookingDoctorId.value = null
        _patientTab.value = tab
    }

    fun openDoctorBooking(doctorId: String) {
        _selectedBookingDoctorId.value = doctorId
    }

    fun closeDoctorBooking() {
        _selectedBookingDoctorId.value = null
    }

    fun selectSpecialtyFilter(specialty: String?) {
        _selectedSpecialty.value = specialty
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectActiveFamilyMember(memberId: String) {
        viewModelScope.launch {
            repository.setActiveMember(memberId)
        }
    }

    fun verifyAdminPinAndUnlock(enteredPin: String, onUnlocked: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.verifyAdminPin(enteredPin)
            res.onSuccess {
                _isAdminLoggedIn.value = true
                _roleMode.value = AppRoleMode.SUPER_ADMIN
                _snackbarMessages.tryEmit("مرحباً بك في الإدارة العليا، ${uiState.value.brandConfig.adminOwnerName}")
                onUnlocked()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "الرقم السري غير صحيح")
            }
        }
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
        _roleMode.value = AppRoleMode.PATIENT
        _snackbarMessages.tryEmit("تم قفل والخروج من لوحة الإدارة")
    }

    fun updateBrandConfig(
        platformNameAr: String,
        villageNameAr: String,
        hospitalNameAr: String,
        supportPhone: String,
        homeAnnouncementAr: String,
        apkUrl: String,
        medsApiUrl: String,
        adminPin: String,
    ) {
        viewModelScope.launch {
            repository.updateFullPlatformConfig(
                platformNameAr = platformNameAr,
                villageNameAr = villageNameAr,
                hospitalNameAr = hospitalNameAr,
                supportPhone = supportPhone,
                homeAnnouncementAr = homeAnnouncementAr,
                apkUrl = apkUrl,
                medsApiUrl = medsApiUrl,
                adminPin = adminPin,
            )
            refreshEgyptianMedications(medsApiUrl)
            _snackbarMessages.tryEmit("تم تحديث إعدادات المنصة والرقم السري بنجاح")
        }
    }

    fun resetBrandConfig() {
        viewModelScope.launch {
            repository.resetFullPlatformConfig()
            refreshEgyptianMedications(AppConfig.defaultBrand.medicationsApiUrl)
            _snackbarMessages.tryEmit("تم استعادة الإعدادات الافتراضية للمنصة")
        }
    }

    fun loginDoctor(identifier: String, password: String) {
        viewModelScope.launch {
            val result = repository.loginDoctor(identifier, password)
            result.onSuccess { doc ->
                _snackbarMessages.tryEmit("أهلاً بك، ${doc.doctorName}")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "بيانات الدخول غير صحيحة")
            }
        }
    }

    fun logoutDoctor() {
        viewModelScope.launch {
            repository.logoutDoctor()
            _snackbarMessages.tryEmit("تم تسجيل خروج الطبيب")
        }
    }

    fun addDoctorByAdmin(
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
    ) {
        viewModelScope.launch {
            val res = repository.addDoctorByAdmin(
                doctorName = doctorName,
                specialty = specialty,
                clinicName = clinicName,
                clinicType = clinicType,
                locationLabel = locationLabel,
                bio = bio,
                mapsUrl = mapsUrl,
                feeEgp = feeEgp,
                workStartMinutes = workStartMinutes,
                workEndMinutes = workEndMinutes,
                slotDurationMinutes = slotDurationMinutes,
                username = username,
                password = password,
            )
            res.onSuccess { doc ->
                _snackbarMessages.tryEmit("تمت إضافة ${doc.doctorName} ونشر عيادته في المنصة فوراً")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إضافة الدكتور")
            }
        }
    }

    fun updateDoctorByAdmin(
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
    ) {
        viewModelScope.launch {
            val res = repository.updateDoctorByAdmin(
                doctorId = doctorId,
                doctorName = doctorName,
                specialty = specialty,
                clinicName = clinicName,
                clinicType = clinicType,
                locationLabel = locationLabel,
                bio = bio,
                mapsUrl = mapsUrl,
                feeEgp = feeEgp,
                workStartMinutes = workStartMinutes,
                workEndMinutes = workEndMinutes,
                slotDurationMinutes = slotDurationMinutes,
                username = username,
                password = password,
            )
            res.onSuccess { doc ->
                _snackbarMessages.tryEmit("تم تحديث بيانات ${doc.doctorName} فوراً")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تعديل البيانات")
            }
        }
    }

    fun deleteDoctorByAdmin(doctorId: String) {
        viewModelScope.launch {
            repository.deleteDoctorByAdmin(doctorId)
            _snackbarMessages.tryEmit("تم حذف الدكتور/القسم نهائياً من المنصة")
        }
    }

    fun blockPhoneByAdmin(phone: String, reason: String) {
        viewModelScope.launch {
            val res = repository.blockPhone(phone, reason)
            res.onSuccess {
                _snackbarMessages.tryEmit("تم حظر الرقم $phone وإلغاء أي حجوزات نشطة له")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر حظر الرقم")
            }
        }
    }

    fun unblockPhoneByAdmin(phone: String) {
        viewModelScope.launch {
            repository.unblockPhone(phone)
            _snackbarMessages.tryEmit("تم رفع الحظر عن الرقم $phone")
        }
    }

    fun sendOrResendOtpForActiveAccount() {
        val acc = uiState.value.activeAccount ?: return
        val res = otpProvider.sendVerificationCode(acc.phone)
        res.onSuccess { challenge ->
            _activeOtpChallenge.value = challenge
            _snackbarMessages.tryEmit("رمز التحقق التجريبي (Test OTP) لـ ${acc.phone}: ${challenge.testOtpCode}")
        }.onFailure { err ->
            _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال رمز التحقق")
        }
    }

    fun verifyActiveAccountOtp(enteredCode: String) {
        val acc = uiState.value.activeAccount ?: return
        val challenge = _activeOtpChallenge.value
        if (challenge == null) {
            _snackbarMessages.tryEmit("اضغط على إرسال رمز التحقق أولاً")
            return
        }
        val check = otpProvider.verifyCode(challenge, enteredCode)
        check.onSuccess {
            viewModelScope.launch {
                repository.markAccountPhoneVerified(acc.id)
                _activeOtpChallenge.value = null
                _snackbarMessages.tryEmit("تم تأكيد رقم الهاتف وتفعيل الحساب بنجاح! يمكنك الحجز الآن.")
            }
        }.onFailure { err ->
            _snackbarMessages.tryEmit(err.message ?: "رمز التحقق غير صحيح")
        }
    }

    fun bookAppointmentAsPatient(slotId: String, member: FamilyMemberEntity) {
        val account = uiState.value.activeAccount
        if (account != null && !account.isPhoneVerified) {
            sendOrResendOtpForActiveAccount()
            _openAccountForOtpEvents.tryEmit(Unit)
            _snackbarMessages.tryEmit("الحساب غير مفعّل: يرجى إدخال رمز التحقق (OTP) أولاً لمنع الحجوزات الوهمية")
            return
        }
        viewModelScope.launch {
            val result = repository.bookSlot(
                slotId = slotId,
                patientName = member.fullName,
                patientPhone = account?.phone?.ifBlank { member.phone } ?: member.phone,
                familyMemberId = member.id,
                source = BookingSource.APP,
            )
            result.onSuccess {
                _snackbarMessages.tryEmit("تم تأكيد الحجز باسم ${member.fullName} بنجاح")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إتمام الحجز")
            }
        }
    }

    fun bookManualPhoneSlot(slotId: String, patientName: String, patientPhone: String) {
        viewModelScope.launch {
            if (patientName.isBlank() || patientPhone.isBlank()) {
                _snackbarMessages.tryEmit("يرجى كتابة اسم المريض ورقم الهاتف")
                return@launch
            }
            val result = repository.bookSlot(
                slotId = slotId,
                patientName = patientName,
                patientPhone = patientPhone,
                familyMemberId = "",
                source = BookingSource.PHONE_WALKIN,
            )
            result.onSuccess {
                _snackbarMessages.tryEmit("تم تسجيل الحجز الهاتفي وإغلاق الموعد فوراً")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تسجيل الحجز")
            }
        }
    }

    fun cancelAppointment(slotId: String) {
        viewModelScope.launch {
            repository.cancelSlot(slotId)
            _snackbarMessages.tryEmit("تم إلغاء الحجز وعاد الموعد متاحاً باللون الأخضر")
        }
    }

    fun generateSchedule(doctorId: String, startMinutes: Int, endMinutes: Int, durationMinutes: Int) {
        viewModelScope.launch {
            val result = repository.generateDoctorSchedule(doctorId, startMinutes, endMinutes, durationMinutes)
            result.onSuccess { count ->
                _snackbarMessages.tryEmit("تم توليد $count موعداً تلقائياً بنجاح")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر توليد الجدول")
            }
        }
    }

    fun delayDoctorAppointments(doctorId: String, delayMinutes: Int) {
        viewModelScope.launch {
            val result = repository.delayDoctorAppointments(doctorId, delayMinutes)
            result.onSuccess {
                _snackbarMessages.tryEmit("تم تأخير المواعيد $delayMinutes دقيقة وإرسال تنبيه فوري للمرضى")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تأخير المواعيد")
            }
        }
    }

    fun completeVisitWithPrescription(
        slotId: String,
        diagnosis: String,
        medications: String,
        notes: String,
        attachmentPath: String,
    ) {
        viewModelScope.launch {
            if (diagnosis.isBlank()) {
                _snackbarMessages.tryEmit("يرجى كتابة التشخيص الطبي")
                return@launch
            }
            val result = repository.completeVisitAndSavePrescription(
                slotId = slotId,
                diagnosis = diagnosis,
                medications = medications,
                notes = notes,
                attachmentPath = attachmentPath,
            )
            result.onSuccess {
                _snackbarMessages.tryEmit("تم إنهاء الكشف وإرسال الروشتة فوراً لملف المريض")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر حفظ الروشتة")
            }
        }
    }

    fun uploadPatientMedicalAttachment(
        member: FamilyMemberEntity,
        title: String,
        notes: String,
        attachmentPath: String,
    ) {
        viewModelScope.launch {
            repository.addPatientUploadedAttachment(
                familyMemberId = member.id,
                patientName = member.fullName,
                patientPhone = member.phone,
                title = title,
                notes = notes,
                attachmentPath = attachmentPath,
            )
            _snackbarMessages.tryEmit("تم حفظ صورة التحليل/الأشعة في ملف ${member.fullName}")
        }
    }

    fun addFamilyMember(
        accountId: String,
        fullName: String,
        relation: String,
        age: Int,
        phone: String,
    ) {
        viewModelScope.launch {
            if (fullName.isBlank()) {
                _snackbarMessages.tryEmit("يرجى كتابة اسم فرد العائلة")
                return@launch
            }
            repository.addFamilyMember(accountId, fullName, relation, age, phone)
            _snackbarMessages.tryEmit("تمت إضافة $fullName إلى قائمة العائلة")
        }
    }

    fun loginPatient(identifier: String, password: String) {
        viewModelScope.launch {
            val result = repository.loginPatient(identifier, password)
            result.onSuccess { acc ->
                _activeOtpChallenge.value = null
                _snackbarMessages.tryEmit("مرحباً بك، ${acc.fullName}")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "بيانات الدخول غير صحيحة")
            }
        }
    }

    fun registerPatient(fullName: String, phone: String, nationalId: String, password: String) {
        viewModelScope.launch {
            if (fullName.isBlank() || phone.isBlank()) {
                _snackbarMessages.tryEmit("يرجى إدخال الاسم ورقم الهاتف")
                return@launch
            }
            val result = repository.registerPatientAccount(fullName, phone, nationalId, password)
            result.onSuccess { acc ->
                val challengeRes = otpProvider.sendVerificationCode(acc.phone)
                challengeRes.onSuccess { challenge ->
                    _activeOtpChallenge.value = challenge
                    _openAccountForOtpEvents.tryEmit(Unit)
                    _snackbarMessages.tryEmit("تم إنشاء الحساب! رمز التحقق التجريبي (Test OTP): ${challenge.testOtpCode}")
                }.onFailure { otpErr ->
                    _snackbarMessages.tryEmit(otpErr.message ?: "تم إنشاء الحساب، يرجى التحقق من الرقم")
                }
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إنشاء الحساب")
            }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead()
        }
    }

    class Factory(private val repository: TebRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TebViewModel(repository) as T
        }
    }
}
