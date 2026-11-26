package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatThreadType
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.MessageSenderRole
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
import com.example.ui.components.DirectChatDialog
import com.example.ui.components.DirectChatTarget
import com.example.ui.components.FullImagePreviewDialog
import com.example.ui.components.LegalModalType
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.PrivacyAndTermsDialog
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
import com.example.util.TebFcmService
import com.example.util.TebNotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
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
        TebNotificationHelper.ensureNotificationChannel(this)
        TebNotificationHelper.scheduleBackgroundSyncAlarm(this)
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    TebFcmService.updateCachedToken(this@MainActivity, token)
                    viewModel.syncDeviceFcmToken(token)
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> viewModel.setAppForeground(true)
                        Lifecycle.Event.ON_PAUSE -> viewModel.setAppForeground(false)
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    TebNotificationHelper.isRuntimeNotificationPermissionRequired(this@MainActivity)
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<AttachmentPreviewTarget?>(null) }
    var activeChatTarget by remember { mutableStateOf<DirectChatTarget?>(null) }

    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val openPatientClinicChat: (String) -> Unit = { doctorId ->
        val doctor = state.doctors.firstOrNull { it.id == doctorId }
        val account = state.activeAccount
        if (doctor != null) {
            if (!state.canPatientChatWith(doctor)) {
                showMessage(bi(isEn, "المحادثة مع هذه العيادة متوقفة حالياً", "Messaging is currently disabled for this clinic"))
            } else if (account == null) {
                showAccountDialog = true
                showMessage(bi(isEn, "يرجى تسجيل الدخول أو إنشاء حساب لمراسلة العيادة", "Please sign in first to message the clinic"))
            } else {
                activeChatTarget = DirectChatTarget(
                    threadType = ChatThreadType.PATIENT_CLINIC,
                    viewerRole = MessageSenderRole.PATIENT,
                    title = if (isEn && doctor.clinicNameEn.isNotBlank()) doctor.clinicNameEn else doctor.clinicName,
                    subtitle = if (isEn && doctor.doctorNameEn.isNotBlank()) doctor.doctorNameEn else doctor.doctorName,
                    patientAccountId = account.id,
                    patientName = account.fullName,
                    patientPhone = account.phone,
                    doctorId = doctor.id,
                )
            }
        }
    }

    val openPatientSupportChat: () -> Unit = {
        val account = state.activeAccount
        if (account == null) {
            showAccountDialog = true
            showMessage(bi(isEn, "يرجى تسجيل الدخول أولاً لمراسلة الإدارة", "Please sign in first to contact Admin"))
        } else {
            activeChatTarget = DirectChatTarget(
                threadType = ChatThreadType.PATIENT_ADMIN,
                viewerRole = MessageSenderRole.PATIENT,
                title = bi(isEn, "الإدارة والدعم الفني", "Platform Admin & Support"),
                subtitle = bi(isEn, "تواصل مباشر مع إدارة منصة طب الحجايزة", "Direct support with Teb Al-Hajayzah Admin"),
                patientAccountId = account.id,
                patientName = account.fullName,
                patientPhone = account.phone,
            )
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
                    onSwitchRole = { mode ->
                        if (mode == AppRoleMode.SUPER_ADMIN) {
                            if (state.isAdminLoggedIn) {
                                viewModel.switchRoleMode(AppRoleMode.SUPER_ADMIN)
                            } else {
                                showAdminPinDialog = true
                            }
                        } else {
                            viewModel.switchRoleMode(mode)
                        }
                    },
                    onOpenNotifications = {
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
                                        canChatWithClinic = state.canPatientChatWith(bookingDoctor),
                                        isEn = isEn,
                                        onSelectFamilyMember = viewModel::selectActiveFamilyMember,
                                        onRequireAccountForBooking = {
                                            viewModel.openAccountDialogForVerification()
                                        },
                                        onChatWithClinic = { openPatientClinicChat(bookingDoctor.id) },
                                        onBookSlot = viewModel::bookAppointmentAsPatient,
                                        onCancelSlot = viewModel::cancelBookingByPatient,
                                        onRescheduleSlot = viewModel::rescheduleBookingByPatient,
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
                                                onCancelSlot = viewModel::cancelBookingByPatient,
                                                onRescheduleSlot = viewModel::rescheduleBookingByPatient,
                                                onOpenOtpAccountDialog = {
                                                    showAccountDialog = true
                                                },
                                                onVerifyOtpInline = viewModel::verifyActiveAccountOtp,
                                                onRegenerateOtpInline = viewModel::regenerateActiveAccountOtp,
                                                onChatWithClinic = openPatientClinicChat,
                                                onChatWithAdmin = {
                                                    val acc = state.activeAccount
                                                    if (acc != null) {
                                                        activeChatTarget = DirectChatTarget(
                                                            threadType = ChatThreadType.PATIENT_ADMIN,
                                                            viewerRole = MessageSenderRole.PATIENT,
                                                            title = bi(isEn, "الإدارة والدعم الفني", "Platform Admin & Support"),
                                                            subtitle = bi(isEn, "محادثة مباشرة مع الإدارة", "Direct support thread"),
                                                            patientAccountId = acc.id,
                                                            patientName = acc.fullName,
                                                            patientPhone = acc.phone,
                                                        )
                                                    } else {
                                                        showAccountDialog = true
                                                    }
                                                },
                                                onCheckAppUpdate = {
                                                    viewModel.checkAppUpdateAndSync { apkUrl ->
                                                        runCatching {
                                                            context.startActivity(
                                                                Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                            )
                                                        }.onFailure {
                                                            showMessage(bi(isEn, "تعذر فتح رابط التحديث", "Could not open update link"))
                                                        }
                                                    }
                                                },
                                                onOpenPrivacyPolicy = viewModel::openPrivacyPolicyModal,
                                                onOpenTermsOfUse = viewModel::openTermsOfUseModal,
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
                                                onChatWithClinic = openPatientClinicChat,
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
                                                onChatWithClinic = openPatientClinicChat,
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
                                                onChatWithClinic = openPatientClinicChat,
                                                onOpenDoctorBooking = viewModel::openDoctorBooking,
                                            )
                                        }

                                        PatientTab.MEDICAL_FILE -> {
                                            MedicalFileScreen(
                                                state = state,
                                                onSelectFamilyMember = viewModel::selectActiveFamilyMember,
                                                onAddFamilyMember = { name, relation, age, phone, natId ->
                                                    val activeAcc = state.activeAccount
                                                    if (activeAcc != null) {
                                                        viewModel.addFamilyMember(activeAcc.id, name, relation, age, phone, natId)
                                                    }
                                                },
                                                onOpenAccountDialog = { showAccountDialog = true },
                                                onSaveBioAndChronics = { memberId, age, weightKg, heightCm, bloodType, chronicKeys, drugAllergies, otherNotes, nationalId, onDone ->
                                                    viewModel.updateMemberBioAndChronic(
                                                        memberId = memberId,
                                                        age = age,
                                                        weightKg = weightKg,
                                                        heightCm = heightCm,
                                                        bloodType = bloodType,
                                                        chronicConditions = chronicKeys,
                                                        drugAllergies = drugAllergies,
                                                        otherChronicNotes = otherNotes,
                                                        nationalId = nationalId,
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
                                                        onCancelSlot = viewModel::cancelBookingByClinic,
                                                        onRescheduleSlot = viewModel::rescheduleBookingByClinic,
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
                                                         onActivatePatientByClinicPhone = { phoneQuery ->
                                                             viewModel.activatePatientByClinicPhone(
                                                                 phoneQuery = phoneQuery,
                                                                 onSuccess = { acc ->
                                                                     showMessage(
                                                                         bi(
                                                                             isEn,
                                                                             "تم توثيق وتفعيل حساب ${acc.fullName} بنجاح ✓",
                                                                             "Account ${acc.fullName} activated successfully ✓",
                                                                         )
                                                                     )
                                                                 },
                                                             )
                                                         },
                                                         onSubmitFamilyLinkRequest = { reqAccId, targetPhone, marriageUrl, idFrontUrl, idBackUrl ->
                                                             viewModel.submitFamilyLinkRequest(
                                                                 requesterAccountId = reqAccId,
                                                                 targetPhone = targetPhone,
                                                                 relation = "SPOUSE",
                                                                 proofMarriageContractUrl = marriageUrl,
                                                                 proofNationalIdFrontUrl = idFrontUrl,
                                                                 proofNationalIdBackUrl = idBackUrl,
                                                             )
                                                         },
                                                         onRefreshAiSummary = viewModel::refreshAiSummaryForMember,
                                                         onOpenChatThread = { target -> activeChatTarget = target },
                                                         onSendPatientAlert = { accId, patName, msg, onSent ->
                                                             viewModel.sendClinicToPatientAlert(accId, patName, msg, onSent)
                                                         },
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
                                        onAddDoctor = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl ->
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
                                                driveWebAppUrl = driveUrl,
                                                secondaryDriveWebAppUrl = secondaryDriveUrl,
                                            )
                                        },
                                        onUpdateDoctor = { docId, name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl ->
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
                                                driveWebAppUrl = driveUrl,
                                                secondaryDriveWebAppUrl = secondaryDriveUrl,
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
                                        onToggleGlobalChat = viewModel::setGlobalChatEnabled,
                                        onToggleClinicChat = viewModel::setClinicChatEnabled,
                                        onSendAdminGlobalBroadcast = { msg, onSent ->
                                            viewModel.sendAdminGlobalBroadcast(msg, onSent)
                                        },
                                        onSendAdminClinicAlert = { docId, msg, onSent ->
                                            viewModel.sendAdminClinicAlert(docId, msg, onSent)
                                        },
                                        onOpenChatThread = { target -> activeChatTarget = target },
                                        onDeleteMessage = viewModel::deleteDirectMessageByAdmin,
                                        onDeleteThread = viewModel::deleteChatThreadByAdmin,
                                        onApproveFamilyLinkRequest = viewModel::approveFamilyLinkRequest,
                                        onRejectFamilyLinkRequest = viewModel::rejectFamilyLinkRequest,
                                        onUpdateDoctorDriveAccounts = viewModel::updateDoctorDriveAccounts,
                                        onPreviewAttachment = { previewAttachment = it },
                                        onOpenNotifications = { showNotificationsDialog = true },
                                        onCleanClinicHeavyFiles = viewModel::cleanClinicHeavyFiles,
                                        onReportStorage90Alert = viewModel::reportStorage90Alert,
                                        onRefreshCloudNow = viewModel::refreshCloudNow,
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
            familyLinkRequests = state.familyLinkRequests,
            otpWhatsappPhone = state.brandConfig.otpWhatsappPhone,
            appVersionLabel = state.brandConfig.appVersionLabel,
            unreadSupportCount = state.unreadPatientSupportMessagesFromAdmin(),
            isEn = isEn,
            onSelectMember = viewModel::selectActiveFamilyMember,
            onAddFamilyMember = { name, relation, age, phone, natId ->
                if (activeAccount != null) {
                    viewModel.addFamilyMember(activeAccount.id, name, relation, age, phone, natId)
                }
            },
            onLogin = viewModel::loginPatient,
            onRegister = viewModel::registerPatient,
            onLogoutPatient = viewModel::logoutPatient,
            onVerifyOtp = viewModel::verifyActiveAccountOtp,
            onRegenerateOtp = viewModel::regenerateActiveAccountOtp,
            onOpenSupportChat = openPatientSupportChat,
            onSubmitFamilyLinkRequest = { targetPhone, marriageUrl, idFrontUrl, idBackUrl ->
                if (activeAccount != null) {
                    viewModel.submitFamilyLinkRequest(
                        requesterAccountId = activeAccount.id,
                        targetPhone = targetPhone,
                        relation = "SPOUSE",
                        proofMarriageContractUrl = marriageUrl,
                        proofNationalIdFrontUrl = idFrontUrl,
                        proofNationalIdBackUrl = idBackUrl,
                    )
                }
            },
            onSetSpouseViewPermission = { allow ->
                if (activeAccount != null) {
                    viewModel.setSpouseViewPermission(activeAccount.id, allow)
                }
            },
            onOpenPrivacyPolicy = viewModel::openPrivacyPolicyModal,
            onOpenTermsOfUse = viewModel::openTermsOfUseModal,
            onShowMessage = showMessage,
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
            notifications = state.visibleNotificationsForCurrentRole(),
            directMessages = state.directMessages,
            allAccounts = state.allAccounts,
            roleMode = state.roleMode,
            activeAccount = state.activeAccount,
            loggedInDoctor = state.loggedInDoctor,
            isEn = isEn,
            onMarkAllRead = viewModel::markCurrentRoleNotificationsRead,
            onOpenChatThread = { target -> activeChatTarget = target },
            onActivateAccount = { accId -> viewModel.setAccountVerifiedByAdmin(accId, true) },
            onRegenerateOtp = { accId -> viewModel.regenerateAccountOtpForAdmin(accId) },
            onDismiss = { showNotificationsDialog = false },
        )
    }

    val chatTarget = activeChatTarget
    if (chatTarget != null) {
        DirectChatDialog(
            target = chatTarget,
            messages = state.directMessages,
            isEn = isEn,
            canDeleteMessages = (chatTarget.viewerRole == MessageSenderRole.ADMIN),
            onDeleteMessage = viewModel::deleteDirectMessageByAdmin,
            onClearThread = {
                viewModel.deleteChatThreadByAdmin(
                    threadType = chatTarget.threadType,
                    patientAccountId = chatTarget.patientAccountId,
                    doctorId = chatTarget.doctorId,
                )
            },
            onMarkThreadRead = {
                viewModel.markDirectThreadRead(
                    threadType = chatTarget.threadType,
                    viewerRole = chatTarget.viewerRole,
                    patientAccountId = chatTarget.patientAccountId,
                    doctorId = chatTarget.doctorId,
                )
            },
            onSendMessage = { body, isWarning, onDone ->
                when (chatTarget.threadType) {
                    ChatThreadType.PATIENT_CLINIC -> {
                        if (chatTarget.viewerRole == MessageSenderRole.PATIENT) {
                            viewModel.sendPatientToClinicMessage(chatTarget.doctorId, body, onDone)
                        } else {
                            viewModel.sendClinicReplyToPatient(
                                patientAccountId = chatTarget.patientAccountId,
                                patientName = chatTarget.patientName,
                                patientPhone = chatTarget.patientPhone,
                                body = body,
                                onSent = onDone,
                            )
                        }
                    }
                    ChatThreadType.PATIENT_ADMIN -> {
                        if (chatTarget.viewerRole == MessageSenderRole.PATIENT) {
                            viewModel.sendPatientToAdminMessage(body, onDone)
                        } else {
                            viewModel.sendAdminReplyToPatient(
                                patientAccountId = chatTarget.patientAccountId,
                                patientName = chatTarget.patientName,
                                patientPhone = chatTarget.patientPhone,
                                body = body,
                                isWarning = isWarning,
                                onSent = onDone,
                            )
                        }
                    }
                    ChatThreadType.CLINIC_ADMIN -> {
                        if (chatTarget.viewerRole == MessageSenderRole.CLINIC) {
                            viewModel.sendClinicToAdminMessage(body, onDone)
                        } else {
                            viewModel.sendAdminReplyToClinic(chatTarget.doctorId, body, onDone)
                        }
                    }
                }
            },
            onDismiss = { activeChatTarget = null },
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

    if (state.isPrivacyModalOpen) {
        PrivacyAndTermsDialog(
            type = LegalModalType.PRIVACY_POLICY,
            isEn = isEn,
            onDismiss = viewModel::dismissPrivacyPolicyModal,
        )
    }

    if (state.isTermsModalOpen) {
        PrivacyAndTermsDialog(
            type = LegalModalType.TERMS_OF_USE,
            isEn = isEn,
            onDismiss = viewModel::dismissTermsOfUseModal,
        )
    }
}
