package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClinicType
import com.example.data.local.TebDatabase
import com.example.data.repository.TebRepository
import com.example.ui.AppLanguage
import com.example.ui.AppRoleMode
import com.example.ui.LiveWebPortalScreen
import com.example.ui.PatientTab
import com.example.ui.StaffPortalTab
import com.example.ui.TebUiState
import com.example.ui.TebViewModel
import com.example.ui.bi
import com.example.ui.isDeviceOnline
import com.example.ui.admin.SuperAdminDashboardScreen
import com.example.ui.components.AccountAndFamilyDialog
import com.example.ui.components.AdminPinLockDialog
import com.example.ui.components.AttachmentPreviewTarget
import com.example.ui.components.FullImagePreviewDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.TebTopHeader
import com.example.ui.doctor.DoctorDashboardScreen
import com.example.ui.doctor.DoctorLoginGateScreen
import com.example.ui.lab.LabDashboardScreen
import com.example.ui.lab.LabLoginGateCard
import com.example.ui.patient.ClinicsDirectoryScreen
import com.example.ui.patient.DoctorBookingScreen
import com.example.ui.patient.MedicalFileScreen
import com.example.ui.patient.PatientHomeScreen
import com.example.ui.theme.TebAlHajayzahTheme
import com.example.util.FirebaseAppCheckHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TebViewModel by viewModels {
        val db = TebDatabase.getInstance(applicationContext)
        val repo = TebRepository(db.tebDao(), applicationContext)
        TebViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAppCheckHelper.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var useLiveWebMode by remember {
                mutableStateOf(
                    state.brandConfig.liveWebUrl.isNotBlank() && isDeviceOnline(this@MainActivity),
                )
            }
            val layoutDirection = if (state.isEn) LayoutDirection.Ltr else LayoutDirection.Rtl
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                TebAlHajayzahTheme {
                    if (useLiveWebMode && state.brandConfig.liveWebUrl.isNotBlank()) {
                        LiveWebPortalScreen(
                            liveUrl = state.brandConfig.liveWebUrl,
                            isEn = state.isEn,
                            hasNewDataBanner = state.hasNewDataBanner,
                            onDismissBannerAndSync = viewModel::dismissNewDataBanner,
                            onCheckLiveUpdates = viewModel::checkLiveWebNow,
                            onSwitchToNativeOffline = { useLiveWebMode = false },
                        )
                    } else {
                        TebAlHajayzahAppRoot(viewModel = viewModel, state = state)
                    }
                }
            }
        }
    }
}

private data class NavDestination(
    val tab: PatientTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

@Composable
fun TebAlHajayzahAppRoot(
    viewModel: TebViewModel,
    state: TebUiState,
) {
    val isEn = state.isEn
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<AttachmentPreviewTarget?>(null) }

    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.openAccountForOtpEvents.collect {
            showAccountDialog = true
        }
    }

    val destinations = remember(isEn) {
        listOf(
            NavDestination(PatientTab.HOME, bi(isEn, "الرئيسية", "Home"), Icons.Filled.Home, "nav_home"),
            NavDestination(PatientTab.PRIVATE_CLINICS, bi(isEn, "العيادات", "Clinics"), Icons.Filled.MedicalServices, "nav_private_clinics"),
            NavDestination(PatientTab.HOSPITAL, bi(isEn, "المستشفى", "Hospital"), Icons.Filled.LocalHospital, "nav_hospital"),
            NavDestination(PatientTab.SPECIALTIES, bi(isEn, "التخصصات", "Specialties"), Icons.Filled.MedicalInformation, "nav_specialties"),
            NavDestination(PatientTab.MEDICAL_FILE, bi(isEn, "ملفي الطبي", "My File"), Icons.Filled.FolderShared, "nav_medical_file"),
        )
    }

    val showBottomOrRailNav = state.roleMode == AppRoleMode.PATIENT && state.selectedBookingDoctor == null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TebTopHeader(
                    brandConfig = state.brandConfig,
                    appLanguage = state.appLanguage,
                    roleMode = state.roleMode,
                    unreadAlertsCount = state.unreadAlertsCount,
                    activeAccount = state.activeAccount,
                    isRefreshing = state.isRefreshing,
                    hasNewDataBanner = state.hasNewDataBanner,
                    onSelectLanguage = viewModel::setAppLanguage,
                    onRefresh = viewModel::smartRefreshData,
                    onSwitchRole = viewModel::switchRoleMode,
                    onOpenNotifications = {
                        viewModel.markNotificationsRead()
                        showNotificationsDialog = true
                    },
                    onOpenAccount = { showAccountDialog = true },
                )
            },
            bottomBar = {
                if (showBottomOrRailNav && !isWideScreen) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            NavigationBarItem(
                                selected = state.patientTab == dest.tab,
                                onClick = { viewModel.selectPatientTab(dest.tab) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (!state.isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = bi(
                                isEn,
                                "جاري تجهيز منصة ${state.brandConfig.platformNameAr}...",
                                "Loading ${state.brandConfig.platformNameEn}...",
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (showBottomOrRailNav && isWideScreen) {
                        NavigationRail {
                            destinations.forEach { dest ->
                                NavigationRailItem(
                                    selected = state.patientTab == dest.tab,
                                    onClick = { viewModel.selectPatientTab(dest.tab) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("${dest.testTag}_rail"),
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (state.roleMode) {
                            AppRoleMode.PATIENT -> {
                                val bookingDoctor = state.selectedBookingDoctor
                                if (bookingDoctor != null) {
                                    DoctorBookingScreen(
                                        doctor = bookingDoctor,
                                        slots = state.slotsForDoctor(bookingDoctor.id),
                                        familyMembers = state.familyMembers,
                                        activeMember = state.activeMember,
                                        isAccountVerified = state.isAccountVerified,
                                        isBookingSubmitting = state.isBookingSubmitting,
                                        isEn = isEn,
                                        onSelectFamilyMember = viewModel::selectActiveFamilyMember,
                                        onRequireAccountForBooking = {
                                            viewModel.openAccountDialogForVerification()
                                        },
                                        onBookSlot = viewModel::bookAppointmentAsPatient,
                                        onCancelSlot = viewModel::cancelAppointment,
                                        onBack = viewModel::closeDoctorBooking,
                                    )
                                } else {
                                    if (state.patientTab != PatientTab.HOME) {
                                        BackHandler {
                                            viewModel.selectPatientTab(PatientTab.HOME)
                                        }
                                    }
                                    when (state.patientTab) {
                                        PatientTab.HOME -> {
                                            PatientHomeScreen(
                                                state = state,
                                                onSelectTab = viewModel::selectPatientTab,
                                                onSelectSpecialty = viewModel::selectSpecialtyFilter,
                                                onSelectFamilyMember = viewModel::selectActiveFamilyMember,
                                                onOpenAddFamilyDialog = { showAccountDialog = true },
                                                onOpenDoctorBooking = viewModel::openDoctorBooking,
                                                onCancelSlot = viewModel::cancelAppointment,
                                                onOpenOtpAccountDialog = {
                                                    showAccountDialog = true
                                                },
                                                onShowMessage = showMessage,
                                            )
                                        }

                                        PatientTab.PRIVATE_CLINICS -> {
                                            ClinicsDirectoryScreen(
                                                state = state,
                                                title = bi(isEn, "العيادات الخاصة بالحجايزة", "Private Clinics"),
                                                clinicTypeFilter = ClinicType.PRIVATE,
                                                showSpecialtyChips = false,
                                                onSearchChange = viewModel::updateSearchQuery,
                                                onSelectSpecialty = viewModel::selectSpecialtyFilter,
                                                onOpenDoctorBooking = viewModel::openDoctorBooking,
                                            )
                                        }

                                        PatientTab.HOSPITAL -> {
                                            ClinicsDirectoryScreen(
                                                state = state,
                                                title = bi(isEn, state.brandConfig.hospitalNameAr, state.brandConfig.hospitalNameEn),
                                                clinicTypeFilter = ClinicType.HOSPITAL,
                                                showSpecialtyChips = false,
                                                onSearchChange = viewModel::updateSearchQuery,
                                                onSelectSpecialty = viewModel::selectSpecialtyFilter,
                                                onOpenDoctorBooking = viewModel::openDoctorBooking,
                                            )
                                        }

                                        PatientTab.SPECIALTIES -> {
                                            ClinicsDirectoryScreen(
                                                state = state,
                                                title = bi(isEn, "التخصصات الطبية الشاملة", "Medical Specialties"),
                                                clinicTypeFilter = null,
                                                showSpecialtyChips = true,
                                                onSearchChange = viewModel::updateSearchQuery,
                                                onSelectSpecialty = viewModel::selectSpecialtyFilter,
                                                onOpenDoctorBooking = viewModel::openDoctorBooking,
                                            )
                                        }

                                        PatientTab.MEDICAL_FILE -> {
                                            MedicalFileScreen(
                                                state = state,
                                                onSelectFamilyMember = viewModel::selectActiveFamilyMember,
                                                onOpenAccountDialog = { showAccountDialog = true },
                                                onSaveBioAndChronics = { memberId, age, weightKg, heightCm, bloodType, chronicKeys, drugAllergies, otherNotes, onDone ->
                                                    viewModel.updateMemberBioAndChronic(
                                                        memberId = memberId,
                                                        age = age,
                                                        weightKg = weightKg,
                                                        heightCm = heightCm,
                                                        bloodType = bloodType,
                                                        chronicConditions = chronicKeys,
                                                        drugAllergies = drugAllergies,
                                                        otherChronicNotes = otherNotes,
                                                        onSaved = onDone,
                                                    )
                                                },
                                                onRefreshAiSummary = viewModel::refreshAiSummaryForMember,
                                                onUploadAttachment = { member, title, notes, path, mimeType, displayName ->
                                                    viewModel.uploadPatientMedicalAttachment(
                                                        member = member,
                                                        title = title,
                                                        notes = notes,
                                                        attachmentPath = path,
                                                        attachmentMimeType = mimeType,
                                                        attachmentName = displayName,
                                                    )
                                                },
                                                onAddExternalDoctorVisit = { member, docName, specialty, visitDate, diagnosis, medsText, notes, path, mimeType, displayName ->
                                                    viewModel.addExternalDoctorVisit(
                                                        member = member,
                                                        doctorName = docName,
                                                        specialty = specialty,
                                                        visitDateLabel = visitDate,
                                                        diagnosis = diagnosis,
                                                        medicationsText = medsText,
                                                        notes = notes,
                                                        attachmentPath = path,
                                                        attachmentMimeType = mimeType,
                                                        attachmentName = displayName,
                                                    )
                                                },
                                                onDeleteRecordFile = { recordId ->
                                                    viewModel.deleteMedicalRecordFile(recordId)
                                                },
                                                onPreviewAttachment = { previewAttachment = it },
                                                onShowMessage = showMessage,
                                            )
                                        }
                                    }
                                }
                            }

                            AppRoleMode.DOCTOR_ADMIN -> {
                                BackHandler {
                                    viewModel.switchRoleMode(AppRoleMode.PATIENT)
                                }
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TabRow(
                                        selectedTabIndex = if (state.staffPortalTab == StaffPortalTab.DOCTOR) 0 else 1,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Tab(
                                            selected = state.staffPortalTab == StaffPortalTab.DOCTOR,
                                            onClick = { viewModel.switchStaffPortalTab(StaffPortalTab.DOCTOR) },
                                            text = { Text(StaffPortalTab.DOCTOR.label(isEn)) },
                                            modifier = Modifier.testTag("staff_tab_doctor"),
                                        )
                                        Tab(
                                            selected = state.staffPortalTab == StaffPortalTab.LAB,
                                            onClick = { viewModel.switchStaffPortalTab(StaffPortalTab.LAB) },
                                            text = { Text(StaffPortalTab.LAB.label(isEn)) },
                                            modifier = Modifier.testTag("staff_tab_lab"),
                                        )
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        when (state.staffPortalTab) {
                                            StaffPortalTab.DOCTOR -> {
                                                if (state.loggedInDoctor == null) {
                                                    DoctorLoginGateScreen(
                                                        adminOwnerName = bi(isEn, state.brandConfig.adminOwnerName, state.brandConfig.adminOwnerNameEn),
                                                        isEn = isEn,
                                                        onLoginDoctor = viewModel::loginDoctor,
                                                    )
                                                } else {
                                                    DoctorDashboardScreen(
                                                        state = state,
                                                        onLogoutDoctor = viewModel::logoutDoctor,
                                                        onBookManualPhoneSlot = viewModel::bookManualPhoneSlot,
                                                        onCancelSlot = viewModel::cancelAppointment,
                                                        onDelayDoctor = viewModel::delayDoctorAppointments,
                                                        onGenerateSchedule = viewModel::generateSchedule,
                                                        onCompleteVisit = { slotId, diag, meds, notes, path, diagEn, medsEn, notesEn, repAr, repEn ->
                                                            viewModel.completeVisitWithPrescription(
                                                                slotId = slotId,
                                                                diagnosis = diag,
                                                                medications = meds,
                                                                notes = notes,
                                                                attachmentPath = path,
                                                                diagnosisEn = diagEn,
                                                                medicationsEn = medsEn,
                                                                notesEn = notesEn,
                                                                aiReportAr = repAr,
                                                                aiReportEn = repEn,
                                                            )
                                                        },
                                                        onToggleFavoriteMed = viewModel::toggleFavoriteMedication,
                                                        onSearchPatientsForActivation = viewModel::searchPatientsForClinicActivation,
                                                        onActivatePatientByClinic = viewModel::activatePatientByClinic,
                                                        onRefreshAiSummary = viewModel::refreshAiSummaryForMember,
                                                        onPreviewAttachment = { previewAttachment = it },
                                                        onShowMessage = showMessage,
                                                    )
                                                }
                                            }

                                            StaffPortalTab.LAB -> {
                                                val loggedLab = state.loggedInLab
                                                if (loggedLab == null) {
                                                    LabLoginGateCard(
                                                        adminOwnerName = bi(isEn, state.brandConfig.adminOwnerName, state.brandConfig.adminOwnerNameEn),
                                                        isEn = isEn,
                                                        onLoginLab = viewModel::loginLab,
                                                    )
                                                } else {
                                                    LabDashboardScreen(
                                                        state = state,
                                                        activeLab = loggedLab,
                                                        onSearchByPhone = viewModel::searchPatientsForLab,
                                                        onUploadLabResult = { member, accountPhone, testTitle, notes, path, mimeType, displayName, onDone ->
                                                            viewModel.uploadLabResultForMember(
                                                                member = member,
                                                                accountPhone = accountPhone,
                                                                testTitle = testTitle,
                                                                notes = notes,
                                                                attachmentPath = path,
                                                                attachmentMimeType = mimeType,
                                                                attachmentName = displayName,
                                                                onUploaded = onDone,
                                                            )
                                                        },
                                                        onPreviewAttachment = { previewAttachment = it },
                                                        onDeleteRecordFile = { recordId ->
                                                            viewModel.deleteMedicalRecordFile(recordId)
                                                        },
                                                        onLogoutLab = viewModel::logoutLab,
                                                        onShowMessage = showMessage,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AppRoleMode.SUPER_ADMIN -> {
                                BackHandler {
                                    viewModel.logoutAdmin()
                                }
                                if (state.isAdminLoggedIn) {
                                    SuperAdminDashboardScreen(
                                        state = state,
                                        onAddDoctor = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn ->
                                            viewModel.addDoctorByAdmin(
                                                doctorName = name,
                                                specialty = spec,
                                                clinicName = clinic,
                                                clinicType = type,
                                                locationLabel = loc,
                                                bio = bio,
                                                mapsUrl = maps,
                                                feeEgp = fee,
                                                workStartMinutes = startMin,
                                                workEndMinutes = endMin,
                                                slotDurationMinutes = dur,
                                                username = user,
                                                password = pass,
                                                additionalSpecialties = extraSpecs,
                                                doctorNameEn = nameEn,
                                                clinicNameEn = clinicEn,
                                                locationLabelEn = locEn,
                                            )
                                        },
                                        onUpdateDoctor = { docId, name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn ->
                                            viewModel.updateDoctorByAdmin(
                                                doctorId = docId,
                                                doctorName = name,
                                                specialty = spec,
                                                clinicName = clinic,
                                                clinicType = type,
                                                locationLabel = loc,
                                                bio = bio,
                                                mapsUrl = maps,
                                                feeEgp = fee,
                                                workStartMinutes = startMin,
                                                workEndMinutes = endMin,
                                                slotDurationMinutes = dur,
                                                username = user,
                                                password = pass,
                                                additionalSpecialties = extraSpecs,
                                                doctorNameEn = nameEn,
                                                clinicNameEn = clinicEn,
                                                locationLabelEn = locEn,
                                            )
                                        },
                                        onDeleteDoctor = viewModel::deleteDoctorByAdmin,
                                        onCancelBooking = viewModel::cancelAppointment,
                                        onBlockPhone = viewModel::blockPhoneByAdmin,
                                        onUnblockPhone = viewModel::unblockPhoneByAdmin,
                                        onSavePlatformSettings = viewModel::updateBrandConfig,
                                        onResetPlatformSettings = viewModel::resetBrandConfig,
                                        onGenerateOtpBatch = viewModel::generateActivationCodes,
                                        onAddLab = viewModel::addLabByAdmin,
                                        onUpdateLab = viewModel::updateLabByAdmin,
                                        onDeleteLab = viewModel::deleteLabByAdmin,
                                        onSetUserVerified = viewModel::setAccountVerifiedByAdmin,
                                        onRegenerateUserOtp = viewModel::regenerateAccountOtpForAdmin,
                                        onUpdateUser = viewModel::updatePatientAccountByAdmin,
                                        onDeleteUser = viewModel::deletePatientAccountByAdmin,
                                        onShowMessage = showMessage,
                                        onLogoutAdmin = viewModel::logoutAdmin,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        val activeAccount = state.activeAccount
        AccountAndFamilyDialog(
            activeAccount = activeAccount,
            familyMembers = state.familyMembers,
            activeMemberId = state.activeMemberId,
            otpWhatsappPhone = state.brandConfig.otpWhatsappPhone,
            appVersionLabel = state.brandConfig.appVersionLabel,
            isEn = isEn,
            onSelectMember = viewModel::selectActiveFamilyMember,
            onAddFamilyMember = { name, relation, age, phone ->
                if (activeAccount != null) {
                    viewModel.addFamilyMember(activeAccount.id, name, relation, age, phone)
                }
            },
            onLogin = viewModel::loginPatient,
            onRegister = viewModel::registerPatient,
            onLogoutPatient = viewModel::logoutPatient,
            onVerifyOtp = viewModel::verifyActiveAccountOtp,
            onRegenerateOtp = viewModel::regenerateActiveAccountOtp,
            onTriggerHiddenAdmin = {
                if (state.isAdminLoggedIn) {
                    viewModel.switchRoleMode(AppRoleMode.SUPER_ADMIN)
                } else {
                    showAdminPinDialog = true
                }
            },
            onDismiss = { showAccountDialog = false },
        )
    }

    if (showAdminPinDialog) {
        AdminPinLockDialog(
            ownerName = bi(isEn, state.brandConfig.adminOwnerName, state.brandConfig.adminOwnerNameEn),
            failedAttempts = state.adminFailedAttempts,
            lockoutUntilMillis = state.adminLockoutUntilMillis,
            isEn = isEn,
            onSubmitPin = { enteredPin ->
                viewModel.verifyAdminPinAndUnlock(
                    enteredPin = enteredPin,
                    onUnlocked = { showAdminPinDialog = false },
                )
            },
            onDismiss = { showAdminPinDialog = false },
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = state.notifications,
            isEn = isEn,
            onDismiss = { showNotificationsDialog = false },
        )
    }

    val preview = previewAttachment
    if (preview != null) {
        FullImagePreviewDialog(
            target = preview,
            isEn = isEn,
            onDeleteRecordFile = { recId ->
                viewModel.deleteMedicalRecordFile(recId)
            },
            onShowMessage = showMessage,
            onDismiss = { previewAttachment = null },
        )
    }
}
