package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
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
import com.example.data.local.MessageSenderRole
import com.example.data.local.NotificationAudience
import com.example.data.local.PatientAccountEntity
import com.example.data.local.PermanentMedicalLogEntity
import com.example.data.medications.EgyptianMedication
import com.example.data.medications.MedicationCatalog
import com.example.data.repository.LabPatientSearchResult
import com.example.data.repository.TebRepository
import com.example.util.SpecialtyCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppRoleMode(val labelAr: String, val labelEn: String) {
    PATIENT("واجهة المريض", "Patient Portal"),
    DOCTOR_ADMIN("العيادات والمعامل", "Clinics & Labs"),
    SUPER_ADMIN("إدارة المنصة", "Super Admin");

    fun label(isEn: Boolean): String = if (isEn) labelEn else labelAr
}

enum class StaffPortalTab(val labelAr: String, val labelEn: String) {
    DOCTOR("بوابة الأطباء والعيادات", "Doctors & Clinics Portal"),
    LAB("بوابة معامل التحاليل", "Medical Labs Portal");

    fun label(isEn: Boolean): String = if (isEn) labelEn else labelAr
}

enum class PatientTab(val labelAr: String, val labelEn: String) {
    HOME("الرئيسية", "Home"),
    PRIVATE_CLINICS("العيادات الخاصة", "Private Clinics"),
    HOSPITAL("المستشفى التخصصي", "Specialized Hospital"),
    SPECIALTIES("التخصصات", "Specialties"),
    MEDICAL_FILE("ملفي الطبي", "Medical File");

    fun label(isEn: Boolean): String = if (isEn) labelEn else labelAr
}

data class TebUiState(
    val isReady: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.AR,
    val roleMode: AppRoleMode = AppRoleMode.PATIENT,
    val staffPortalTab: StaffPortalTab = StaffPortalTab.DOCTOR,
    val patientTab: PatientTab = PatientTab.HOME,
    val selectedBookingDoctorId: String? = null,
    val selectedSpecialty: String? = null,
    val searchQuery: String = "",
    val brandConfig: PlatformBrandConfig = AppConfig.defaultBrand,
    val activeAccount: PatientAccountEntity? = null,
    val allAccounts: List<PatientAccountEntity> = emptyList(),
    val allFamilyMembers: List<FamilyMemberEntity> = emptyList(),
    val familyMembers: List<FamilyMemberEntity> = emptyList(),
    val activeMemberId: String = "",
    val doctors: List<ClinicDoctorEntity> = emptyList(),
    val labs: List<MedicalLabEntity> = emptyList(),
    val allSlots: List<AppointmentSlotEntity> = emptyList(),
    val medicalRecords: List<MedicalRecordEntity> = emptyList(),
    val permanentLogs: List<PermanentMedicalLogEntity> = emptyList(),
    val notifications: List<DelayNotificationEntity> = emptyList(),
    val directMessages: List<DirectMessageEntity> = emptyList(),
    val blockedPhones: List<BlockedPhoneEntity> = emptyList(),
    val loggedInDoctorId: String? = null,
    val loggedInLabId: String? = null,
    val labSearchResults: List<LabPatientSearchResult> = emptyList(),
    val clinicPatientSearchResults: List<LabPatientSearchResult> = emptyList(),
    val isAdminLoggedIn: Boolean = false,
    val adminFailedAttempts: Int = 0,
    val adminLockoutUntilMillis: Long = 0L,
    val adminUsername: String = AppConfig.defaultBrand.adminUsername,
    val adminPassword: String = AppConfig.defaultBrand.adminPassword,
    val medications: List<EgyptianMedication> = MedicationCatalog.builtInMedications,
    val isLiveMedicationsApi: Boolean = false,
    val favoriteMedsByDoctor: Map<String, Set<String>> = emptyMap(),
    val activationCodes: List<ActivationOtpCodeEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val isBookingSubmitting: Boolean = false,
    val hasNewDataBanner: Boolean = false,
    val analyzingAiMemberIds: Set<String> = emptySet(),
) {
    val isEn: Boolean
        get() = appLanguage == AppLanguage.EN

    val activeMember: FamilyMemberEntity?
        get() = familyMembers.firstOrNull { it.id == activeMemberId } ?: familyMembers.firstOrNull()

    val privateClinics: List<ClinicDoctorEntity>
        get() = doctors.filter { it.clinicType == ClinicType.PRIVATE }

    val hospitalClinics: List<ClinicDoctorEntity>
        get() = doctors.filter { it.clinicType == ClinicType.HOSPITAL }

    val specialties: List<String>
        get() = SpecialtyCatalog.all.map { it.nameAr }

    val activeDoctorSpecialties: List<String>
        get() = doctors
            .flatMap { SpecialtyCatalog.allDoctorSpecialties(it.specialty, it.additionalSpecialties) }
            .distinct()

    val selectedBookingDoctor: ClinicDoctorEntity?
        get() = doctors.firstOrNull { it.id == selectedBookingDoctorId }

    val loggedInDoctor: ClinicDoctorEntity?
        get() = doctors.firstOrNull { it.id == loggedInDoctorId }

    val loggedInLab: MedicalLabEntity?
        get() = labs.firstOrNull { it.id == loggedInLabId }

    fun canPatientChatWith(doctor: ClinicDoctorEntity): Boolean =
        brandConfig.globalChatEnabled && doctor.chatEnabled

    fun visibleNotificationsForCurrentRole(): List<DelayNotificationEntity> {
        val accId = activeAccount?.id.orEmpty()
        val accPhone = activeAccount?.phone.orEmpty()
        val famIds = familyMembers.map { it.id }.toSet()
        return notifications.filter { alert ->
            when (roleMode) {
                AppRoleMode.DOCTOR_ADMIN -> {
                    alert.targetAudience == NotificationAudience.ALL ||
                        (alert.targetAudience == NotificationAudience.CLINIC &&
                            (alert.targetId.isBlank() || alert.targetId == loggedInDoctorId))
                }
                AppRoleMode.SUPER_ADMIN -> {
                    alert.targetAudience == NotificationAudience.ADMIN ||
                        alert.targetAudience == NotificationAudience.ALL
                }
                AppRoleMode.PATIENT -> {
                    alert.targetAudience == NotificationAudience.ALL ||
                        (alert.targetAudience == NotificationAudience.PATIENT && (
                            alert.targetId.isBlank() ||
                                (accId.isNotBlank() && alert.targetId == accId) ||
                                (accPhone.isNotBlank() && alert.targetId == accPhone) ||
                                alert.targetId in famIds
                            ))
                }
            }
        }
    }

    fun unreadIncomingMessagesCountForCurrentRole(): Int {
        return when (roleMode) {
            AppRoleMode.PATIENT -> {
                val accId = activeAccount?.id.orEmpty()
                if (accId.isBlank()) 0 else directMessages.count {
                    it.patientAccountId == accId &&
                        (it.threadType == ChatThreadType.PATIENT_CLINIC || it.threadType == ChatThreadType.PATIENT_ADMIN) &&
                        !it.isReadByPatient
                }
            }
            AppRoleMode.DOCTOR_ADMIN -> {
                val docId = loggedInDoctorId.orEmpty()
                if (docId.isBlank()) 0 else directMessages.count {
                    it.doctorId == docId &&
                        (it.threadType == ChatThreadType.PATIENT_CLINIC || it.threadType == ChatThreadType.CLINIC_ADMIN) &&
                        !it.isReadByClinic
                }
            }
            AppRoleMode.SUPER_ADMIN -> {
                directMessages.count {
                    (it.threadType == ChatThreadType.PATIENT_ADMIN || it.threadType == ChatThreadType.CLINIC_ADMIN) &&
                        !it.isReadByAdmin
                }
            }
        }
    }

    fun unreadPatientSupportMessagesFromAdmin(): Int {
        val accId = activeAccount?.id.orEmpty()
        if (accId.isBlank()) return 0
        return directMessages.count {
            it.threadType == ChatThreadType.PATIENT_ADMIN &&
                it.patientAccountId == accId &&
                !it.isReadByPatient
        }
    }

    fun unreadMessagesCountForDoctor(doctorId: String): Int =
        directMessages.count {
            it.doctorId == doctorId &&
                (it.threadType == ChatThreadType.PATIENT_CLINIC || it.threadType == ChatThreadType.CLINIC_ADMIN) &&
                !it.isReadByClinic
        }

    fun unreadSupportInboxCountForAdmin(): Int =
        directMessages.count {
            (it.threadType == ChatThreadType.PATIENT_ADMIN || it.threadType == ChatThreadType.CLINIC_ADMIN) &&
                !it.isReadByAdmin
        }

    val unreadAlertsCount: Int
        get() = visibleNotificationsForCurrentRole().count { !it.isRead } +
            unreadIncomingMessagesCountForCurrentRole()

    val isAccountVerified: Boolean
        get() = activeAccount?.isPhoneVerified == true

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

    fun permanentLogsForMember(member: FamilyMemberEntity?): List<PermanentMedicalLogEntity> {
        if (member == null) return emptyList()
        return permanentLogs.filter { log ->
            log.familyMemberId == member.id ||
                (log.patientName.isNotBlank() && log.patientName == member.fullName)
        }.sortedByDescending { it.eventTimestampMillis }
    }

    fun permanentLogsForPatient(
        familyMemberId: String,
        patientName: String,
        patientPhone: String,
    ): List<PermanentMedicalLogEntity> {
        return permanentLogs.filter { log ->
            (familyMemberId.isNotBlank() && log.familyMemberId == familyMemberId) ||
                (patientName.isNotBlank() && log.patientName == patientName) ||
                (patientPhone.isNotBlank() && log.patientPhone == patientPhone && log.patientName == patientName)
        }.sortedByDescending { it.eventTimestampMillis }
    }
}

class TebViewModel(
    private val repository: TebRepository,
) : ViewModel() {

    private val _roleMode = MutableStateFlow(AppRoleMode.PATIENT)
    private val _staffPortalTab = MutableStateFlow(StaffPortalTab.DOCTOR)
    private val _patientTab = MutableStateFlow(PatientTab.HOME)
    private val _selectedBookingDoctorId = MutableStateFlow<String?>(null)
    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSeeded = MutableStateFlow(false)
    private val _isAdminLoggedIn = MutableStateFlow(false)
    private val _medications = MutableStateFlow(MedicationCatalog.builtInMedications)
    private val _isLiveMedicationsApi = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isBookingSubmitting = MutableStateFlow(false)
    private val _labSearchResults = MutableStateFlow<List<LabPatientSearchResult>>(emptyList())
    private val _clinicPatientSearchResults = MutableStateFlow<List<LabPatientSearchResult>>(emptyList())
    private val _clinicLastSearchQuery = MutableStateFlow("")
    private val _hasNewDataBanner = MutableStateFlow(false)
    private val _analyzingAiMemberIds = MutableStateFlow<Set<String>>(emptySet())
    private var _lastLiveWebSignature: String? = null

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _openAccountForOtpEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val openAccountForOtpEvents = _openAccountForOtpEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isSeeded.value = true
            repository.syncWithCloudFirestoreOnce()
        }
        refreshEgyptianMedications(AppConfig.defaultBrand.medicationsApiUrl)
        viewModelScope.launch {
            val (sig, _) = repository.checkLiveWebHasUpdate(AppConfig.defaultBrand.liveWebUrl, null)
            _lastLiveWebSignature = sig
            while (isActive) {
                delay(20_000L)
                val cfg = uiState.value.brandConfig
                repository.syncWithCloudFirestoreOnce()
                if (cfg.medicationsApiUrl.isNotBlank()) {
                    val result = repository.loadEgyptianMedications(cfg.medicationsApiUrl)
                    _medications.value = result.medications
                    _isLiveMedicationsApi.value = result.isLiveRemote
                }
                val (newSig, changed) = repository.checkLiveWebHasUpdate(cfg.liveWebUrl, _lastLiveWebSignature)
                _lastLiveWebSignature = newSig
                if (changed) {
                    _hasNewDataBanner.value = true
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeFamilyFlow = repository.activeAccountIdFlow.flatMapLatest { accountId ->
        repository.observeFamilyMembers(accountId)
    }

    private data class CoreUiSelection(
        val isSeeded: Boolean,
        val roleMode: AppRoleMode,
        val staffPortalTab: StaffPortalTab,
        val patientTab: PatientTab,
        val bookingDoctorId: String?,
        val specialty: String?,
        val searchQuery: String,
        val brandConfig: PlatformBrandConfig,
        val isAdminLoggedIn: Boolean,
    )

    private data class AccountDataBundle(
        val activeAccountId: String,
        val activeMemberId: String,
        val accounts: List<PatientAccountEntity>,
        val familyMembers: List<FamilyMemberEntity>,
        val allFamilyMembers: List<FamilyMemberEntity>,
        val loggedInDoctorId: String?,
        val loggedInLabId: String?,
        val adminFailedAttempts: Int,
        val adminLockoutUntilMillis: Long,
    )

    private data class ClinicDataBundle(
        val doctors: List<ClinicDoctorEntity>,
        val labs: List<MedicalLabEntity>,
        val slots: List<AppointmentSlotEntity>,
        val records: List<MedicalRecordEntity>,
        val notifications: List<DelayNotificationEntity>,
        val blockedPhones: List<BlockedPhoneEntity>,
        val directMessages: List<DirectMessageEntity>,
        val permanentLogs: List<PermanentMedicalLogEntity>,
    )

    private data class MedicationBundle(
        val medications: List<EgyptianMedication>,
        val isLiveRemote: Boolean,
        val favorites: List<DoctorFavoriteMedEntity>,
        val isRefreshing: Boolean,
        val isBookingSubmitting: Boolean,
    )

    private val selectionFlow = combine(
        _isSeeded,
        _roleMode,
        _staffPortalTab,
        _patientTab,
        _selectedBookingDoctorId,
        _selectedSpecialty,
        _searchQuery,
        repository.brandConfigFlow,
        _isAdminLoggedIn,
    ) { values ->
        CoreUiSelection(
            isSeeded = values[0] as Boolean,
            roleMode = values[1] as AppRoleMode,
            staffPortalTab = values[2] as StaffPortalTab,
            patientTab = values[3] as PatientTab,
            bookingDoctorId = values[4] as String?,
            specialty = values[5] as String?,
            searchQuery = values[6] as String,
            brandConfig = values[7] as PlatformBrandConfig,
            isAdminLoggedIn = values[8] as Boolean,
        )
    }

    private val accountIdsFlow = combine(
        repository.activeAccountIdFlow,
        repository.activeMemberIdFlow,
        repository.allAccountsFlow,
        activeFamilyFlow,
        repository.allFamilyMembersFlow,
    ) { activeAccountId, activeMemberId, accounts, familyMembers, allMembers ->
        listOf(activeAccountId, activeMemberId, accounts, familyMembers, allMembers)
    }

    private val authCredsFlow = combine(
        repository.loggedInDoctorIdFlow,
        repository.loggedInLabIdFlow,
        repository.adminSecurityFlow,
    ) { loggedDocId, loggedLabId, sec ->
        listOf(loggedDocId, loggedLabId, sec.first, sec.second)
    }

    private val accountBundleFlow = combine(accountIdsFlow, authCredsFlow) { ids, creds ->
        @Suppress("UNCHECKED_CAST")
        AccountDataBundle(
            activeAccountId = ids[0] as String,
            activeMemberId = ids[1] as String,
            accounts = ids[2] as List<PatientAccountEntity>,
            familyMembers = ids[3] as List<FamilyMemberEntity>,
            allFamilyMembers = ids[4] as List<FamilyMemberEntity>,
            loggedInDoctorId = creds[0] as String?,
            loggedInLabId = creds[1] as String?,
            adminFailedAttempts = creds[2] as Int,
            adminLockoutUntilMillis = creds[3] as Long,
        )
    }

    private val clinicBundleFlow = combine(
        repository.doctorsFlow,
        repository.labsFlow,
        repository.allSlotsFlow,
        repository.allMedicalRecordsFlow,
        repository.notificationsFlow,
        repository.blockedPhonesFlow,
        repository.directMessagesFlow,
        repository.allPermanentLogsFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ClinicDataBundle(
            doctors = values[0] as List<ClinicDoctorEntity>,
            labs = values[1] as List<MedicalLabEntity>,
            slots = values[2] as List<AppointmentSlotEntity>,
            records = values[3] as List<MedicalRecordEntity>,
            notifications = values[4] as List<DelayNotificationEntity>,
            blockedPhones = values[5] as List<BlockedPhoneEntity>,
            directMessages = values[6] as List<DirectMessageEntity>,
            permanentLogs = values[7] as List<PermanentMedicalLogEntity>,
        )
    }

    private val medicationBundleFlow = combine(
        _medications,
        _isLiveMedicationsApi,
        repository.allDoctorFavoritesFlow,
        _isRefreshing,
        _isBookingSubmitting,
    ) { meds, isLive, favs, isRefreshing, isBookingSubmitting ->
        MedicationBundle(meds, isLive, favs, isRefreshing, isBookingSubmitting)
    }

    private val adminOpsFlow = combine(
        repository.activationCodesFlow,
        _labSearchResults,
        repository.appLanguageFlow,
        _clinicPatientSearchResults,
        _hasNewDataBanner,
        _analyzingAiMemberIds,
    ) { values ->
        values.toList()
    }

    val uiState: StateFlow<TebUiState> = combine(
        selectionFlow,
        accountBundleFlow,
        clinicBundleFlow,
        medicationBundleFlow,
        adminOpsFlow,
    ) { sel, acc, clinic, medBundle, adminOps ->
        val favMap = medBundle.favorites
            .groupBy { it.doctorId }
            .mapValues { (_, list) -> list.map { it.medicationId }.toSet() }

        val activeAccount = if (acc.activeAccountId.isNotBlank()) {
            acc.accounts.firstOrNull { it.id == acc.activeAccountId }
        } else {
            null
        }

        @Suppress("UNCHECKED_CAST")
        val activationCodes = adminOps[0] as List<ActivationOtpCodeEntity>
        @Suppress("UNCHECKED_CAST")
        val labResults = adminOps[1] as List<LabPatientSearchResult>
        val appLang = adminOps[2] as AppLanguage
        @Suppress("UNCHECKED_CAST")
        val clinicResults = adminOps[3] as List<LabPatientSearchResult>
        val hasBanner = adminOps[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val analyzingIds = adminOps[5] as Set<String>

        TebUiState(
            isReady = sel.isSeeded,
            appLanguage = appLang,
            roleMode = sel.roleMode,
            staffPortalTab = sel.staffPortalTab,
            patientTab = sel.patientTab,
            selectedBookingDoctorId = sel.bookingDoctorId,
            selectedSpecialty = sel.specialty,
            searchQuery = sel.searchQuery,
            brandConfig = sel.brandConfig,
            activeAccount = activeAccount,
            allAccounts = acc.accounts,
            allFamilyMembers = acc.allFamilyMembers,
            familyMembers = if (activeAccount != null) acc.familyMembers else emptyList(),
            activeMemberId = if (activeAccount != null) acc.activeMemberId else "",
            doctors = clinic.doctors,
            labs = clinic.labs,
            allSlots = clinic.slots,
            medicalRecords = clinic.records,
            permanentLogs = clinic.permanentLogs,
            notifications = clinic.notifications,
            directMessages = clinic.directMessages,
            blockedPhones = clinic.blockedPhones,
            loggedInDoctorId = acc.loggedInDoctorId,
            loggedInLabId = acc.loggedInLabId,
            labSearchResults = labResults,
            clinicPatientSearchResults = clinicResults,
            isAdminLoggedIn = sel.isAdminLoggedIn,
            adminFailedAttempts = acc.adminFailedAttempts,
            adminLockoutUntilMillis = acc.adminLockoutUntilMillis,
            adminUsername = sel.brandConfig.adminUsername,
            adminPassword = sel.brandConfig.adminPassword,
            medications = medBundle.medications,
            isLiveMedicationsApi = medBundle.isLiveRemote,
            favoriteMedsByDoctor = favMap,
            activationCodes = activationCodes,
            isRefreshing = medBundle.isRefreshing,
            isBookingSubmitting = medBundle.isBookingSubmitting,
            hasNewDataBanner = hasBanner,
            analyzingAiMemberIds = analyzingIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TebUiState(),
    )

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
        }
    }

    fun dismissNewDataBanner() {
        _hasNewDataBanner.value = false
    }

    fun checkLiveWebNow() {
        viewModelScope.launch {
            val cfg = uiState.value.brandConfig
            repository.syncWithCloudFirestoreOnce()
            val (newSig, changed) = repository.checkLiveWebHasUpdate(cfg.liveWebUrl, _lastLiveWebSignature)
            _lastLiveWebSignature = newSig
            if (changed) {
                _hasNewDataBanner.value = true
            }
        }
    }

    fun smartRefreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _hasNewDataBanner.value = false
            try {
                repository.syncWithCloudFirestoreOnce()
                val cfg = uiState.value.brandConfig
                val result = repository.loadEgyptianMedications(cfg.medicationsApiUrl)
                _medications.value = result.medications
                _isLiveMedicationsApi.value = result.isLiveRemote
                val (newSig, _) = repository.checkLiveWebHasUpdate(cfg.liveWebUrl, _lastLiveWebSignature)
                _lastLiveWebSignature = newSig
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "تم تحديث المواعيد والبيانات بنجاح دون تسجيل الخروج",
                        "Appointments and data refreshed without logging out",
                    )
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun checkAppUpdateAndSync(onOpenApkDownload: (String) -> Unit) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncWithCloudFirestoreOnce()
                val cfg = uiState.value.brandConfig
                val isEn = uiState.value.isEn
                val verResult = repository.checkAppVersionStatus(
                    liveBaseUrl = cfg.liveWebUrl,
                    fallbackApkUrl = cfg.apkDownloadUrl,
                )
                val medRes = repository.loadEgyptianMedications(cfg.medicationsApiUrl)
                _medications.value = medRes.medications
                _isLiveMedicationsApi.value = medRes.isLiveRemote
                val (newSig, _) = repository.checkLiveWebHasUpdate(cfg.liveWebUrl, _lastLiveWebSignature)
                _lastLiveWebSignature = newSig

                if (verResult.hasNewerApk) {
                    _snackbarMessages.tryEmit(
                        bi(
                            isEn,
                            "يتوفر إصدار جديد (v${verResult.latestVersion}) — جاري التحميل والتثبيت فوراً...",
                            "New version available (v${verResult.latestVersion}) — Downloading & installing...",
                        )
                    )
                    onOpenApkDownload(verResult.apkDownloadUrl)
                } else {
                    _snackbarMessages.tryEmit(
                        bi(
                            isEn,
                            "أنت تستخدم أحدث إصدار بالفعل (${cfg.appVersionLabel}) ✓",
                            "You are already using the latest version (${cfg.appVersionLabel}) ✓",
                        )
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

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

    fun switchStaffPortalTab(tab: StaffPortalTab) {
        _staffPortalTab.value = tab
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

    fun openAccountDialogForVerification() {
        _openAccountForOtpEvents.tryEmit(Unit)
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
        firebaseApiKey: String = "",
        firebaseAuthDomain: String = "",
        firebaseProjectId: String = "",
        firebaseStorageBucket: String = "",
        firebaseMessagingSenderId: String = "",
        firebaseAppId: String = "",
        firebaseVapidKey: String = "",
        workerUrl: String = "",
        defaultDriveWebAppUrl: String = "",
        fileRetentionDays: Int = 360,
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
                firebaseApiKey = firebaseApiKey,
                firebaseAuthDomain = firebaseAuthDomain,
                firebaseProjectId = firebaseProjectId,
                firebaseStorageBucket = firebaseStorageBucket,
                firebaseMessagingSenderId = firebaseMessagingSenderId,
                firebaseAppId = firebaseAppId,
                firebaseVapidKey = firebaseVapidKey,
                workerUrl = workerUrl,
                defaultDriveWebAppUrl = defaultDriveWebAppUrl,
                fileRetentionDays = fileRetentionDays,
            )
            refreshEgyptianMedications(medsApiUrl)
            _snackbarMessages.tryEmit("تم تحديث إعدادات المنصة والربط السحابي بنجاح")
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

    fun addLabByAdmin(
        labName: String,
        phone: String,
        locationLabel: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            val res = repository.addLabByAdmin(labName, phone, locationLabel, username, password)
            res.onSuccess { lab ->
                _snackbarMessages.tryEmit("تمت إضافة ${lab.labName} لبوابة معامل التحاليل بنجاح")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إضافة المعمل")
            }
        }
    }

    fun updateLabByAdmin(
        labId: String,
        labName: String,
        phone: String,
        locationLabel: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            val res = repository.updateLabByAdmin(labId, labName, phone, locationLabel, username, password)
            res.onSuccess { lab ->
                _snackbarMessages.tryEmit("تم تحديث بيانات ${lab.labName} بنجاح")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تحديث بيانات المعمل")
            }
        }
    }

    fun deleteLabByAdmin(labId: String) {
        viewModelScope.launch {
            repository.deleteLabByAdmin(labId)
            _snackbarMessages.tryEmit("تم حذف المعمل من المنصة")
        }
    }

    fun loginLab(username: String, password: String) {
        viewModelScope.launch {
            val res = repository.loginLab(username, password)
            res.onSuccess { lab ->
                _labSearchResults.value = emptyList()
                _snackbarMessages.tryEmit("أهلاً بك في بوابة ${lab.labName}")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "بيانات الدخول للمعمل غير صحيحة")
            }
        }
    }

    fun logoutLab() {
        viewModelScope.launch {
            repository.logoutLab()
            _labSearchResults.value = emptyList()
            _snackbarMessages.tryEmit("تم تسجيل الخروج من بوابة المعمل")
        }
    }

    fun searchPatientsForLab(phoneQuery: String) {
        viewModelScope.launch {
            val results = repository.searchPatientsByPhoneForLab(phoneQuery)
            _labSearchResults.value = results
            if (results.isEmpty() && phoneQuery.trim().length >= 4) {
                _snackbarMessages.tryEmit("لم يتم العثور على حساب مريض مسجل بهذا الرقم")
            }
        }
    }

    fun uploadLabResultForMember(
        member: FamilyMemberEntity,
        accountPhone: String,
        testTitle: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String,
        attachmentName: String,
        onUploaded: () -> Unit = {},
    ) {
        val activeLab = uiState.value.loggedInLab
        if (activeLab == null) {
            _snackbarMessages.tryEmit("يرجى تسجيل الدخول بحساب المعمل أولاً")
            return
        }
        viewModelScope.launch {
            val res = repository.addLabResultRecord(
                lab = activeLab,
                member = member,
                accountPhone = accountPhone,
                testTitle = testTitle,
                notes = notes,
                attachmentPath = attachmentPath,
                attachmentMimeType = attachmentMimeType,
                attachmentName = attachmentName,
            )
            res.onSuccess { rec ->
                launchAiSummaryRefresh(member.id, rec.id)
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم رفع نتيجة التحليل إلى ملف ${member.fullName} وجاري تحديث الملخص الطبي تلقائياً",
                        "✓ Lab result uploaded to ${member.fullName} and AI medical summary updated",
                    )
                )
                onUploaded()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر حفظ نتيجة التحليل")
            }
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
        additionalSpecialties: String = "",
        doctorNameEn: String = "",
        clinicNameEn: String = "",
        locationLabelEn: String = "",
        driveWebAppUrl: String = "",
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
                additionalSpecialties = additionalSpecialties,
                doctorNameEn = doctorNameEn,
                clinicNameEn = clinicNameEn,
                locationLabelEn = locationLabelEn,
                driveWebAppUrl = driveWebAppUrl,
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
        additionalSpecialties: String = "",
        doctorNameEn: String = "",
        clinicNameEn: String = "",
        locationLabelEn: String = "",
        driveWebAppUrl: String = "",
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
                additionalSpecialties = additionalSpecialties,
                doctorNameEn = doctorNameEn,
                clinicNameEn = clinicNameEn,
                locationLabelEn = locationLabelEn,
                driveWebAppUrl = driveWebAppUrl,
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

    fun generateActivationCodes(count: Int, generatedBy: String) {
        viewModelScope.launch {
            val res = repository.generateActivationCodeBatch(count, generatedBy)
            res.onSuccess { createdCount ->
                _snackbarMessages.tryEmit("تم توليد $createdCount كود تفعيل جديد ذي استخدام واحد بنجاح")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر توليد أكواد التفعيل")
            }
        }
    }

    fun verifyActiveAccountOtp(enteredCode: String) {
        val acc = uiState.value.activeAccount ?: return
        viewModelScope.launch {
            val result = repository.verifyAndBurnActivationCode(
                accountId = acc.id,
                accountName = acc.fullName,
                accountPhone = acc.phone,
                enteredCode = enteredCode,
            )
            result.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم تأكيد وتفعيل الحساب بنجاح! (كود OTP) يمكنك الحجز الآن.",
                        "✓ Account verified & activated! You can book now.",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "كود التفعيل غير صحيح")
            }
        }
    }

    fun regenerateActiveAccountOtp() {
        val acc = uiState.value.activeAccount ?: return
        viewModelScope.launch {
            val res = repository.regenerateAccountOtp(acc.id)
            res.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم توليد كود تفعيل جديد صالح لمدة 24 ساعة. اطلبه الآن عبر واتساب.",
                        "✓ Generated a new 24h activation code. Request it via WhatsApp.",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر توليد كود جديد")
            }
        }
    }

    fun regenerateAccountOtpForAdmin(accountId: String) {
        viewModelScope.launch {
            val res = repository.regenerateAccountOtp(accountId)
            res.onSuccess { updated ->
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم تجديد كود ${updated.fullName} (${updated.otpCode}) لمدة 24 ساعة",
                        "✓ Renewed 24h OTP (${updated.otpCode}) for ${updated.fullName}",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تجديد الكود")
            }
        }
    }

    fun searchPatientsForClinicActivation(phoneQuery: String) {
        _clinicLastSearchQuery.value = phoneQuery
        viewModelScope.launch {
            val results = repository.searchAccountsByPhoneForClinic(phoneQuery)
            _clinicPatientSearchResults.value = results
            if (results.isEmpty() && phoneQuery.trim().length >= 3) {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "لم يتم العثور على مريض مسجل بهذا الرقم",
                        "No registered patient found matching this phone",
                    )
                )
            }
        }
    }

    fun activatePatientByClinic(accountId: String) {
        val doc = uiState.value.loggedInDoctor ?: return
        viewModelScope.launch {
            val res = repository.activatePatientByClinic(accountId, doc.doctorName)
            res.onSuccess { updatedAcc ->
                if (_clinicLastSearchQuery.value.isNotBlank()) {
                    _clinicPatientSearchResults.value = repository.searchAccountsByPhoneForClinic(_clinicLastSearchQuery.value)
                }
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم تفعيل حساب ${updatedAcc.fullName} بنجاح بواسطة ${updatedAcc.verifiedBy}",
                        "✓ Activated ${updatedAcc.fullName} via ${updatedAcc.verifiedBy}",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تفعيل الحساب")
            }
        }
    }

    fun setAccountVerifiedByAdmin(accountId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.setAccountVerificationByAdmin(accountId, verified)
            _snackbarMessages.tryEmit(
                if (verified) {
                    bi(uiState.value.isEn, "✓ تم تفعيل حساب العميل يدوياً بواسطة الإدارة", "✓ Account manually verified by Admin")
                } else {
                    bi(uiState.value.isEn, "تم إلغاء توثيق حساب العميل", "Account verification revoked")
                }
            )
        }
    }

    fun updatePatientAccountByAdmin(
        accountId: String,
        fullName: String,
        phone: String,
        newPassword: String?,
    ) {
        viewModelScope.launch {
            val res = repository.updateAccountByAdmin(accountId, fullName, phone, newPassword)
            res.onSuccess { updated ->
                _snackbarMessages.tryEmit(
                    bi(uiState.value.isEn, "✓ تم تحديث بيانات العميل ${updated.fullName}", "✓ Updated customer ${updated.fullName}")
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تعديل بيانات العميل")
            }
        }
    }

    fun deletePatientAccountByAdmin(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccountByAdmin(accountId)
            _snackbarMessages.tryEmit(
                bi(uiState.value.isEn, "تم حذف حساب العميل من المنصة", "Customer account deleted")
            )
        }
    }

    fun bookAppointmentAsPatient(slotId: String, member: FamilyMemberEntity?) {
        if (_isBookingSubmitting.value) return
        val account = uiState.value.activeAccount
        if (account == null || member == null) {
            _openAccountForOtpEvents.tryEmit(Unit)
            _snackbarMessages.tryEmit("لإتمام الحجز، يرجى تسجيل الدخول أو إنشاء حساب وتفعيله بكود التحقق (OTP).")
            return
        }
        if (!account.isPhoneVerified) {
            _openAccountForOtpEvents.tryEmit(Unit)
            _snackbarMessages.tryEmit("الحساب غير مفعّل: فعّل حسابك مرة واحدة عبر كود واتساب أو من العيادة أو الإدارة.")
            return
        }
        viewModelScope.launch {
            _isBookingSubmitting.value = true
            try {
                val result = repository.bookSlot(
                    slotId = slotId,
                    patientName = member.fullName,
                    patientPhone = account.phone.ifBlank { member.phone },
                    familyMemberId = member.id,
                    source = BookingSource.APP,
                )
                result.onSuccess {
                    _snackbarMessages.tryEmit("تم تأكيد الحجز باسم ${member.fullName} بنجاح")
                }.onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إتمام الحجز")
                }
            } finally {
                _isBookingSubmitting.value = false
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
        cancelBookingByClinic(slotId)
    }

    fun cancelBookingByPatient(slotId: String) {
        viewModelScope.launch {
            val res = repository.cancelSlotByPatient(slotId)
            res.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم إلغاء الموعد وإخطار العيادة وإتاحته للمرضى مجدداً",
                        "✓ Appointment cancelled and clinic notified",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إلغاء الموعد")
            }
        }
    }

    fun rescheduleBookingByPatient(fromSlotId: String, toSlotId: String) {
        viewModelScope.launch {
            val res = repository.rescheduleSlotByPatient(fromSlotId, toSlotId)
            res.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم تعديل موعدك وإخطار العيادة بالتحديث تلقائياً",
                        "✓ Appointment rescheduled and clinic notified",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تعديل الموعد")
            }
        }
    }

    fun cancelBookingByClinic(slotId: String) {
        viewModelScope.launch {
            val res = repository.cancelSlotByClinic(slotId)
            res.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم إلغاء الحجز وإرسال إشعار للمريض وإتاحة الموعد باللون الأخضر",
                        "✓ Booking cancelled, patient notified, slot open",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إلغاء الحجز")
            }
        }
    }

    fun rescheduleBookingByClinic(fromSlotId: String, toSlotId: String) {
        viewModelScope.launch {
            val res = repository.rescheduleSlotByClinic(fromSlotId, toSlotId)
            res.onSuccess {
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم تعديل موعد الحجز وإشعار المريض بالموعد الجديد",
                        "✓ Booking rescheduled and patient notified",
                    )
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تعديل الموعد")
            }
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
        diagnosisEn: String = "",
        medicationsEn: String = "",
        notesEn: String = "",
        aiReportAr: String = "",
        aiReportEn: String = "",
    ) {
        viewModelScope.launch {
            if (diagnosis.isBlank() && diagnosisEn.isBlank()) {
                _snackbarMessages.tryEmit("يرجى كتابة التشخيص الطبي")
                return@launch
            }
            val result = repository.completeVisitAndSavePrescription(
                slotId = slotId,
                diagnosis = diagnosis.ifBlank { diagnosisEn },
                medications = medications,
                notes = notes,
                attachmentPath = attachmentPath,
                diagnosisEn = diagnosisEn,
                medicationsEn = medicationsEn,
                notesEn = notesEn,
                aiReportAr = aiReportAr,
                aiReportEn = aiReportEn,
            )
            result.onSuccess { rec ->
                if (rec.familyMemberId.isNotBlank()) {
                    launchAiSummaryRefresh(rec.familyMemberId, rec.id)
                }
                _snackbarMessages.tryEmit("تم إنهاء الكشف وإرسال الروشتة والتقرير لملف المريض")
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
        attachmentMimeType: String = "image/jpeg",
        attachmentName: String = "",
    ) {
        viewModelScope.launch {
            val savedRec = repository.addPatientUploadedAttachment(
                familyMemberId = member.id,
                patientName = member.fullName,
                patientPhone = member.phone,
                title = title,
                notes = notes,
                attachmentPath = attachmentPath,
                attachmentMimeType = attachmentMimeType,
                attachmentName = attachmentName,
            )
            launchAiSummaryRefresh(member.id, savedRec.id)
            _snackbarMessages.tryEmit(
                bi(
                    uiState.value.isEn,
                    "✓ تم حفظ المرفق في ملف ${member.fullName} وجاري تحليل المحتوى وتحديث الملخص الطبي",
                    "✓ Saved to ${member.fullName}'s file. Analyzing attachment to update AI summary",
                )
            )
        }
    }

    fun addExternalDoctorVisit(
        member: FamilyMemberEntity,
        doctorName: String,
        specialty: String,
        visitDateLabel: String,
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String = "",
        attachmentName: String = "",
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val res = repository.addExternalDoctorVisitRecord(
                familyMemberId = member.id,
                patientName = member.fullName,
                patientPhone = member.phone,
                doctorName = doctorName,
                specialty = specialty,
                visitDateLabel = visitDateLabel,
                diagnosis = diagnosis,
                medicationsText = medicationsText,
                notes = notes,
                attachmentPath = attachmentPath,
                attachmentMimeType = attachmentMimeType,
                attachmentName = attachmentName,
            )
            res.onSuccess { rec ->
                launchAiSummaryRefresh(member.id, rec.id)
                _snackbarMessages.tryEmit("✓ تم تسجيل زيارة الطبيب الخارجي في ملف ${member.fullName} وتحديث الملخص الطبي")
                onSaved()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر تسجيل الزيارة الخارجية")
            }
        }
    }

    fun updateMemberBioAndChronic(
        memberId: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        bloodType: String,
        chronicConditions: List<String>,
        drugAllergies: String,
        otherChronicNotes: String,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val res = repository.updateMemberBioAndChronic(
                memberId = memberId,
                age = age,
                weightKg = weightKg,
                heightCm = heightCm,
                bloodType = bloodType,
                chronicConditions = chronicConditions,
                drugAllergies = drugAllergies,
                otherChronicNotes = otherChronicNotes,
            )
            res.onSuccess {
                launchAiSummaryRefresh(memberId, null)
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم حفظ البيانات الحيوية والأمراض المزمنة وتحديث الملخص الطبي",
                        "✓ Patient vitals & chronic conditions saved; updating medical summary",
                    )
                )
                onSaved()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر حفظ البيانات الحيوية")
            }
        }
    }

    fun refreshAiSummaryForMember(memberId: String, focusRecordId: String? = null) {
        launchAiSummaryRefresh(memberId, focusRecordId)
    }

    private fun launchAiSummaryRefresh(memberId: String, focusRecordId: String? = null) {
        if (memberId.isBlank()) return
        viewModelScope.launch {
            _analyzingAiMemberIds.value = _analyzingAiMemberIds.value + memberId
            try {
                repository.refreshMemberAiSummaryWithGemini(memberId, focusRecordId)
            } finally {
                _analyzingAiMemberIds.value = _analyzingAiMemberIds.value - memberId
            }
        }
    }

    fun deleteMedicalRecordFile(recordId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteMedicalRecordOrAttachment(recordId)
            res.onSuccess { affectedMemberId ->
                if (!affectedMemberId.isNullOrBlank()) {
                    launchAiSummaryRefresh(affectedMemberId, null)
                }
                _snackbarMessages.tryEmit(
                    bi(
                        uiState.value.isEn,
                        "✓ تم حذف الملف بنجاح وتحديث الملخص الطبي",
                        "✓ File deleted and medical summary updated",
                    )
                )
                onDeleted()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر حذف الملف")
            }
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

    fun logoutPatient() {
        viewModelScope.launch {
            repository.logoutPatient()
            _snackbarMessages.tryEmit("تم تسجيل الخروج بنجاح")
        }
    }

    fun registerPatient(fullName: String, phone: String, nationalId: String, password: String) {
        viewModelScope.launch {
            if (fullName.isBlank() || phone.isBlank()) {
                _snackbarMessages.tryEmit("يرجى إدخال الاسم ورقم الهاتف")
                return@launch
            }
            val result = repository.registerPatientAccount(fullName, phone, nationalId, password)
            result.onSuccess {
                _openAccountForOtpEvents.tryEmit(Unit)
                _snackbarMessages.tryEmit("تم إنشاء الحساب وتوليد كود تفعيل صالح لمدة 24 ساعة! اطلبه عبر واتساب أو من العيادة.")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "تعذر إنشاء الحساب")
            }
        }
    }

    fun markNotificationsRead() {
        markCurrentRoleNotificationsRead()
    }

    fun markCurrentRoleNotificationsRead() {
        viewModelScope.launch {
            val s = uiState.value
            repository.markRoleNotificationsRead(
                roleName = s.roleMode.name,
                activeAccountId = s.activeAccount?.id.orEmpty(),
                activeAccountPhone = s.activeAccount?.phone.orEmpty(),
                familyMemberIds = s.familyMembers.map { it.id }.toSet(),
                loggedInDoctorId = s.loggedInDoctorId,
            )
        }
    }

    fun setGlobalChatEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGlobalChatEnabled(enabled)
            _snackbarMessages.tryEmit(
                if (enabled) {
                    bi(uiState.value.isEn, "✓ تم تفعيل المحادثات مع جميع العيادات", "✓ Global clinic messaging enabled")
                } else {
                    bi(uiState.value.isEn, "✓ تم إيقاف المحادثات مع جميع العيادات على مستوى المنصة", "✓ Global clinic messaging disabled")
                }
            )
        }
    }

    fun setClinicChatEnabled(doctorId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setClinicChatEnabled(doctorId, enabled)
            _snackbarMessages.tryEmit(
                if (enabled) {
                    bi(uiState.value.isEn, "✓ تم تفعيل استقبال رسائل المرضى لهذه العيادة", "✓ Clinic chat enabled")
                } else {
                    bi(uiState.value.isEn, "✓ تم إيقاف استقبال رسائل المرضى لهذه العيادة", "✓ Clinic chat disabled")
                }
            )
        }
    }

    fun sendPatientToClinicMessage(doctorId: String, body: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val acc = uiState.value.activeAccount
            if (acc == null) {
                _openAccountForOtpEvents.tryEmit(Unit)
                _snackbarMessages.tryEmit(bi(uiState.value.isEn, "يرجى تسجيل الدخول أولاً لمراسلة العيادة", "Please sign in first to message the clinic"))
                return@launch
            }
            repository.sendPatientToClinicMessage(acc, doctorId, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال رسالتك إلى العيادة", "✓ Message sent to clinic"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرسالة")
                }
        }
    }

    fun sendPatientToAdminMessage(body: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val acc = uiState.value.activeAccount
            if (acc == null) {
                _openAccountForOtpEvents.tryEmit(Unit)
                _snackbarMessages.tryEmit(bi(uiState.value.isEn, "يرجى تسجيل الدخول أولاً لمراسلة الإدارة", "Please sign in first to message support"))
                return@launch
            }
            repository.sendPatientToAdminMessage(acc, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال رسالتك إلى الإدارة والدعم الفني", "✓ Message sent to Admin support"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرسالة")
                }
        }
    }

    fun sendClinicReplyToPatient(
        patientAccountId: String,
        patientName: String,
        patientPhone: String,
        body: String,
        onSent: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val doc = uiState.value.loggedInDoctor ?: return@launch
            repository.sendClinicReplyToPatient(doc, patientAccountId, patientName, patientPhone, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال الرد للمريض", "✓ Reply sent to patient"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرد")
                }
        }
    }

    fun sendClinicToPatientAlert(
        patientAccountId: String,
        patientName: String,
        message: String,
        onSent: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val doc = uiState.value.loggedInDoctor ?: return@launch
            repository.sendClinicToPatientAlert(doc, patientAccountId, patientName, message)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال تنبيه مباشر للمريض $patientName", "✓ Direct alert sent to $patientName"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال التنبيه")
                }
        }
    }

    fun sendClinicToAdminMessage(body: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val doc = uiState.value.loggedInDoctor ?: return@launch
            repository.sendClinicToAdminMessage(doc, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال رسالتك إلى الإدارة العليا", "✓ Message sent to Super Admin"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرسالة")
                }
        }
    }

    fun sendAdminReplyToPatient(
        patientAccountId: String,
        patientName: String,
        patientPhone: String,
        body: String,
        onSent: () -> Unit = {},
    ) {
        viewModelScope.launch {
            repository.sendAdminReplyToPatient(patientAccountId, patientName, patientPhone, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال رد الإدارة للمريض", "✓ Admin reply sent to patient"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرد")
                }
        }
    }

    fun sendAdminReplyToClinic(doctorId: String, body: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val doc = uiState.value.doctors.firstOrNull { it.id == doctorId } ?: return@launch
            repository.sendAdminReplyToClinic(doc, body)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال رد الإدارة للعيادة", "✓ Admin reply sent to clinic"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الرد")
                }
        }
    }

    fun sendAdminClinicAlert(doctorId: String?, message: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val doc = if (doctorId.isNullOrBlank()) null else uiState.value.doctors.firstOrNull { it.id == doctorId }
            repository.sendAdminClinicAlert(doc, message)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "✓ تم إرسال التنبيه الإداري للعيادات", "✓ Clinic alert sent"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال التنبيه")
                }
        }
    }

    fun sendAdminGlobalBroadcast(message: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sendAdminGlobalBroadcast(message)
                .onSuccess {
                    onSent()
                    _snackbarMessages.tryEmit(bi(uiState.value.isEn, "📢 تم إرسال الإشعار العام لجميع مستخدمي المنصة", "📢 Platform broadcast sent"))
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر إرسال الإشعار العام")
                }
        }
    }

    fun markDirectThreadRead(
        threadType: String,
        viewerRole: String,
        patientAccountId: String = "",
        doctorId: String = "",
    ) {
        viewModelScope.launch {
            repository.markDirectThreadRead(threadType, viewerRole, patientAccountId, doctorId)
        }
    }

    fun deleteDirectMessageByAdmin(messageId: String) {
        viewModelScope.launch {
            repository.deleteDirectMessageByAdmin(messageId)
                .onSuccess {
                    _snackbarMessages.tryEmit(
                        bi(uiState.value.isEn, "🗑️ تم حذف الرسالة نهائياً", "🗑️ Message deleted permanently")
                    )
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر حذف الرسالة")
                }
        }
    }

    fun deleteChatThreadByAdmin(
        threadType: String,
        patientAccountId: String = "",
        doctorId: String = "",
    ) {
        viewModelScope.launch {
            repository.deleteChatThreadByAdmin(threadType, patientAccountId, doctorId)
                .onSuccess { count ->
                    _snackbarMessages.tryEmit(
                        bi(
                            uiState.value.isEn,
                            "🗑️ تم مسح المحادثة بالكامل ($count رسالة)",
                            "🗑️ Conversation cleared ($count messages)",
                        )
                    )
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.message ?: "تعذر مسح المحادثة")
                }
        }
    }

    class Factory(private val repository: TebRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TebViewModel(repository) as T
        }
    }
}
