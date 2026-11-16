package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.AppRoleMode
import com.example.ui.PatientTab
import com.example.ui.TebViewModel
import com.example.ui.admin.SuperAdminDashboardScreen
import com.example.ui.components.AccountAndFamilyDialog
import com.example.ui.components.AdminPinLockDialog
import com.example.ui.components.FullImagePreviewDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.TebTopHeader
import com.example.ui.doctor.DoctorDashboardScreen
import com.example.ui.doctor.DoctorLoginGateScreen
import com.example.ui.patient.ClinicsDirectoryScreen
import com.example.ui.patient.DoctorBookingScreen
import com.example.ui.patient.MedicalFileScreen
import com.example.ui.patient.PatientHomeScreen
import com.example.ui.theme.TebAlHajayzahTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TebViewModel by viewModels {
        val db = TebDatabase.getInstance(applicationContext)
        val repo = TebRepository(db.tebDao(), applicationContext)
        TebViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TebAlHajayzahTheme {
                    TebAlHajayzahAppRoot(viewModel = viewModel)
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
fun TebAlHajayzahAppRoot(viewModel: TebViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<Pair<String, String>?>(null) }

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

    val destinations = remember {
        listOf(
            NavDestination(PatientTab.HOME, "الرئيسية", Icons.Filled.Home, "nav_home"),
            NavDestination(PatientTab.PRIVATE_CLINICS, "العيادات", Icons.Filled.MedicalServices, "nav_private_clinics"),
            NavDestination(PatientTab.HOSPITAL, "المستشفى", Icons.Filled.LocalHospital, "nav_hospital"),
            NavDestination(PatientTab.SPECIALTIES, "التخصصات", Icons.Filled.MedicalInformation, "nav_specialties"),
            NavDestination(PatientTab.MEDICAL_FILE, "ملفي الطبي", Icons.Filled.FolderShared, "nav_medical_file"),
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
                    roleMode = state.roleMode,
                    unreadAlertsCount = state.unreadAlertsCount,
                    activeAccount = state.activeAccount,
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
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
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
                            text = "جاري تجهيز منصة ${state.brandConfig.platformNameAr}...",
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
                                        onSelectFamilyMember = viewModel::selectActiveFamilyMember,
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
                                                title = "العيادات الخاصة بالحجايزة",
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
                                                title = state.brandConfig.hospitalNameAr,
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
                                                title = "التخصصات الطبية الشاملة",
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
                                                onUploadAttachment = viewModel::uploadPatientMedicalAttachment,
                                                onPreviewImage = { path, title -> previewImage = path to title },
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
                                if (state.loggedInDoctor == null) {
                                    DoctorLoginGateScreen(
                                        adminOwnerName = state.brandConfig.adminOwnerName,
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
                                        onCompleteVisit = viewModel::completeVisitWithPrescription,
                                        onToggleFavoriteMed = viewModel::toggleFavoriteMedication,
                                        onGenerateOtpBatch = viewModel::generateActivationCodes,
                                        onPreviewImage = { path, title -> previewImage = path to title },
                                        onShowMessage = showMessage,
                                    )
                                }
                            }

                            AppRoleMode.SUPER_ADMIN -> {
                                BackHandler {
                                    viewModel.logoutAdmin()
                                }
                                if (state.isAdminLoggedIn) {
                                    SuperAdminDashboardScreen(
                                        state = state,
                                        onAddDoctor = viewModel::addDoctorByAdmin,
                                        onUpdateDoctor = viewModel::updateDoctorByAdmin,
                                        onDeleteDoctor = viewModel::deleteDoctorByAdmin,
                                        onCancelBooking = viewModel::cancelAppointment,
                                        onBlockPhone = viewModel::blockPhoneByAdmin,
                                        onUnblockPhone = viewModel::unblockPhoneByAdmin,
                                        onSavePlatformSettings = viewModel::updateBrandConfig,
                                        onResetPlatformSettings = viewModel::resetBrandConfig,
                                        onGenerateOtpBatch = viewModel::generateActivationCodes,
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
            activeOtpChallenge = state.activeOtpChallenge,
            supportPhone = state.brandConfig.supportPhone,
            appVersionLabel = state.brandConfig.appVersionLabel,
            onSelectMember = viewModel::selectActiveFamilyMember,
            onAddFamilyMember = { name, relation, age, phone ->
                if (activeAccount != null) {
                    viewModel.addFamilyMember(activeAccount.id, name, relation, age, phone)
                }
            },
            onLogin = viewModel::loginPatient,
            onRegister = viewModel::registerPatient,
            onSendOrResendOtp = viewModel::sendOrResendOtpForActiveAccount,
            onVerifyOtp = viewModel::verifyActiveAccountOtp,
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
            ownerName = state.brandConfig.adminOwnerName,
            failedAttempts = state.adminFailedAttempts,
            lockoutUntilMillis = state.adminLockoutUntilMillis,
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
            onDismiss = { showNotificationsDialog = false },
        )
    }

    val preview = previewImage
    if (preview != null) {
        FullImagePreviewDialog(
            imagePath = preview.first,
            title = preview.second,
            onDismiss = { previewImage = null },
        )
    }
}
