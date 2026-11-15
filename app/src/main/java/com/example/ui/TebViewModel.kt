package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.config.PlatformBrandConfig
import com.example.data.local.AppointmentSlotEntity
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
    val activeAdminDoctorId: String = "doc_batna_1",
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

    val unreadAlertsCount: Int
        get() = notifications.count { !it.isRead }

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
) : ViewModel() {

    private val _roleMode = MutableStateFlow(AppRoleMode.PATIENT)
    private val _patientTab = MutableStateFlow(PatientTab.HOME)
    private val _selectedBookingDoctorId = MutableStateFlow<String?>(null)
    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _brandConfig = MutableStateFlow(AppConfig.defaultBrand)
    private val _isSeeded = MutableStateFlow(false)
    private val _medications = MutableStateFlow(MedicationCatalog.builtInMedications)
    private val _isLiveMedicationsApi = MutableStateFlow(false)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isSeeded.value = true
        }
        refreshEgyptianMedications(_brandConfig.value.medicationsApiUrl)
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
    )

    private data class AccountDataBundle(
        val activeAccountId: String,
        val activeMemberId: String,
        val accounts: List<PatientAccountEntity>,
        val familyMembers: List<FamilyMemberEntity>,
        val activeAdminDoctorId: String,
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
        _brandConfig,
    ) { values ->
        CoreUiSelection(
            isSeeded = values[0] as Boolean,
            roleMode = values[1] as AppRoleMode,
            patientTab = values[2] as PatientTab,
            bookingDoctorId = values[3] as String?,
            specialty = values[4] as String?,
            searchQuery = values[5] as String,
            brandConfig = values[6] as PlatformBrandConfig,
        )
    }

    private val accountBundleFlow = combine(
        repository.activeAccountIdFlow,
        repository.activeMemberIdFlow,
        repository.allAccountsFlow,
        activeFamilyFlow,
        repository.activeDoctorPortalIdFlow,
    ) { activeAccountId, activeMemberId, accounts, familyMembers, adminDoctorId ->
        AccountDataBundle(
            activeAccountId = activeAccountId,
            activeMemberId = activeMemberId,
            accounts = accounts,
            familyMembers = familyMembers,
            activeAdminDoctorId = adminDoctorId,
        )
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
        repository.doctorsFlow,
        repository.allSlotsFlow,
        repository.allMedicalRecordsFlow,
        repository.notificationsFlow,
        medicationBundleFlow,
    ) { values ->
        val sel = values[0] as CoreUiSelection
        val acc = values[1] as AccountDataBundle
        @Suppress("UNCHECKED_CAST")
        val doctors = values[2] as List<ClinicDoctorEntity>
        @Suppress("UNCHECKED_CAST")
        val slots = values[3] as List<AppointmentSlotEntity>
        @Suppress("UNCHECKED_CAST")
        val records = values[4] as List<MedicalRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val notifications = values[5] as List<DelayNotificationEntity>
        val medBundle = values[6] as MedicationBundle

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
            doctors = doctors,
            allSlots = slots,
            medicalRecords = records,
            notifications = notifications,
            activeAdminDoctorId = acc.activeAdminDoctorId,
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

    fun selectAdminDoctorPortal(doctorId: String) {
        viewModelScope.launch {
            repository.setActiveDoctorPortal(doctorId)
        }
    }

    fun updateBrandConfig(
        platformNameAr: String,
        villageNameAr: String,
        hospitalNameAr: String,
        apkUrl: String,
        medsApiUrl: String,
    ) {
        _brandConfig.value = _brandConfig.value.copy(
            platformNameAr = platformNameAr.trim().ifEmpty { AppConfig.defaultBrand.platformNameAr },
            villageNameAr = villageNameAr.trim().ifEmpty { AppConfig.defaultBrand.villageNameAr },
            hospitalNameAr = hospitalNameAr.trim().ifEmpty { AppConfig.defaultBrand.hospitalNameAr },
            apkDownloadUrl = apkUrl.trim().ifEmpty { AppConfig.defaultBrand.apkDownloadUrl },
            medicationsApiUrl = medsApiUrl.trim(),
        )
        refreshEgyptianMedications(medsApiUrl)
        _snackbarMessages.tryEmit("تم تحديث إعدادات المنصة والروابط بنجاح")
    }

    fun resetBrandConfig() {
        _brandConfig.value = AppConfig.defaultBrand
        refreshEgyptianMedications(AppConfig.defaultBrand.medicationsApiUrl)
        _snackbarMessages.tryEmit("تم استعادة الإعدادات الافتراضية للمنصة")
    }

    fun bookAppointmentAsPatient(slotId: String, member: FamilyMemberEntity) {
        viewModelScope.launch {
            val result = repository.bookSlot(
                slotId = slotId,
                patientName = member.fullName,
                patientPhone = member.phone,
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
                _snackbarMessages.tryEmit("تم إنشاء الحساب وتسجيل الدخول باسم ${acc.fullName}")
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
