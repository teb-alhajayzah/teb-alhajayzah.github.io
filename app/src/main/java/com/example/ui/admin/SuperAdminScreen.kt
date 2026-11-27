package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatThreadType
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.FamilyLinkRequestEntity
import com.example.data.local.FamilyLinkStatus
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalLabEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MessageSenderRole
import com.example.data.local.PatientAccountEntity
import com.example.data.local.SlotStatus
import com.example.ui.TebUiState
import com.example.ui.bi
import com.example.ui.components.AttachmentPreviewTarget
import com.example.ui.components.DirectChatTarget
import com.example.ui.components.NotificationStatusCard
import com.example.ui.components.PasswordOutlinedTextField
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.util.SpecialtyCatalog
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    state: TebUiState,
    onAddDoctor: (
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
        additionalSpecialties: String,
        doctorNameEn: String,
        clinicNameEn: String,
        locationLabelEn: String,
        driveWebAppUrl: String,
        secondaryDriveWebAppUrl: String,
    ) -> Unit,
    onUpdateDoctor: (
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
        additionalSpecialties: String,
        doctorNameEn: String,
        clinicNameEn: String,
        locationLabelEn: String,
        driveWebAppUrl: String,
        secondaryDriveWebAppUrl: String,
    ) -> Unit,
    onDeleteDoctor: (String) -> Unit,
    onCancelBooking: (slotId: String) -> Unit,
    onBlockPhone: (phone: String, reason: String) -> Unit,
    onUnblockPhone: (phone: String) -> Unit,
    onSavePlatformSettings: (
        platformNameAr: String,
        villageNameAr: String,
        hospitalNameAr: String,
        supportPhone: String,
        homeAnnouncementAr: String,
        apkUrl: String,
        medsApiUrl: String,
        adminPin: String,
        firebaseApiKey: String,
        firebaseAuthDomain: String,
        firebaseProjectId: String,
        firebaseStorageBucket: String,
        firebaseMessagingSenderId: String,
        firebaseAppId: String,
        firebaseVapidKey: String,
        workerUrl: String,
        defaultDriveWebAppUrl: String,
        fileRetentionDays: Int,
    ) -> Unit,
    onResetPlatformSettings: () -> Unit,
    onGenerateOtpBatch: (count: Int, generatedBy: String) -> Unit = { _, _ -> },
    onAddLab: (labName: String, phone: String, locationLabel: String, username: String, password: String) -> Unit,
    onUpdateLab: (labId: String, labName: String, phone: String, locationLabel: String, username: String, password: String) -> Unit,
    onDeleteLab: (labId: String) -> Unit,
    onSetUserVerified: (accountId: String, verified: Boolean) -> Unit,
    onRegenerateUserOtp: (accountId: String) -> Unit = {},
    onUpdateUser: (accountId: String, fullName: String, phone: String, newPassword: String?) -> Unit,
    onDeleteUser: (accountId: String) -> Unit,
    onToggleGlobalChat: (Boolean) -> Unit = {},
    onToggleClinicChat: (doctorId: String, Boolean) -> Unit = { _, _ -> },
    onSendAdminGlobalBroadcast: (message: String, onSent: () -> Unit) -> Unit = { _, _ -> },
    onSendAdminClinicAlert: (doctorId: String?, message: String, onSent: () -> Unit) -> Unit = { _, _, _ -> },
    onOpenChatThread: (DirectChatTarget) -> Unit = {},
    onDeleteMessage: (messageId: String) -> Unit = {},
    onDeleteThread: (threadType: String, patientAccountId: String, doctorId: String) -> Unit = { _, _, _ -> },
    onApproveFamilyLinkRequest: (requestId: String) -> Unit = {},
    onRejectFamilyLinkRequest: (requestId: String, notes: String) -> Unit = { _, _ -> },
    onUpdateDoctorDriveAccounts: (doctorId: String, primaryUrl: String, secondaryUrl: String) -> Unit = { _, _, _ -> },
    onPreviewAttachment: (AttachmentPreviewTarget) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onCleanClinicHeavyFiles: (doctorId: String) -> Unit = {},
    onReportStorage90Alert: (accountLabel: String, usedPercent: Int) -> Unit = { _, _ -> },
    onRefreshCloudNow: () -> Unit = {},
    onSendTestAdminPush: ((Boolean, String) -> Unit) -> Unit = { _ -> },
    onShowMessage: (String) -> Unit,
    onLogoutAdmin: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isEn = state.isEn
    val currencyLabel = bi(isEn, state.brandConfig.currencyLabelAr, state.brandConfig.currencyLabelEn)
    val unreadAdminInbox = state.unreadSupportInboxCountForAdmin()
    val unverifiedUsersCount = state.allAccounts.count { !it.isPhoneVerified }
    val pendingFamilyLinksCount = state.familyLinkRequests.count { it.status == FamilyLinkStatus.PENDING }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var usersSearchQuery by remember { mutableStateOf("") }
    var usersStatusFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedTab) {
        if (selectedTab in 5..8) {
            onRefreshCloudNow()
        }
    }
    var globalBroadcastDraft by remember { mutableStateOf("") }
    var clinicAlertDraft by remember { mutableStateOf("") }
    var selectedAlertDoctorId by remember { mutableStateOf<String?>(null) }

    // All Admin sections start collapsed by default (Accordion UI)
    var cloudSettingsExpanded by remember { mutableStateOf(false) }
    var platformSettingsExpanded by remember { mutableStateOf(false) }
    var driveStorageExpanded by remember { mutableStateOf(false) }
    var driveClinicsListExpanded by remember { mutableStateOf(false) }
    var notifCardExpanded by remember { mutableStateOf(false) }
    var blockPhoneExpanded by remember { mutableStateOf(false) }
    var activeBookingsExpanded by remember { mutableStateOf(false) }
    var chatBroadcastExpanded by remember { mutableStateOf(false) }
    var clinicChatExpanded by remember { mutableStateOf(false) }
    var threadsListExpanded by remember { mutableStateOf(true) }
    var familyLinksAccordionExpanded by remember { mutableStateOf(false) }

    var selectedDriveDocId by remember(state.doctors) { mutableStateOf(state.doctors.firstOrNull()?.id.orEmpty()) }
    var driveDropdownExpanded by remember { mutableStateOf(false) }
    val selectedDriveDoc = state.doctors.firstOrNull { it.id == selectedDriveDocId } ?: state.doctors.firstOrNull()
    var clinicDrivePrimaryInput by remember(selectedDriveDoc?.id, selectedDriveDoc?.driveWebAppUrl) {
        mutableStateOf(selectedDriveDoc?.driveWebAppUrl.orEmpty())
    }
    var clinicDriveSecondaryInput by remember(selectedDriveDoc?.id, selectedDriveDoc?.secondaryDriveWebAppUrl) {
        mutableStateOf(selectedDriveDoc?.secondaryDriveWebAppUrl.orEmpty())
    }

    var expandedThreadKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var defaultFormClinicType by remember { mutableStateOf(ClinicType.PRIVATE) }
    var showAddForm by remember { mutableStateOf(false) }
    var doctorToEdit by remember { mutableStateOf<ClinicDoctorEntity?>(null) }
    var doctorToDelete by remember { mutableStateOf<ClinicDoctorEntity?>(null) }

    var showAddLabForm by remember { mutableStateOf(false) }
    var labToEdit by remember { mutableStateOf<MedicalLabEntity?>(null) }
    var labToDelete by remember { mutableStateOf<MedicalLabEntity?>(null) }

    var userToEdit by remember { mutableStateOf<PatientAccountEntity?>(null) }
    var userToDelete by remember { mutableStateOf<PatientAccountEntity?>(null) }

    var manualBlockPhone by remember { mutableStateOf("") }
    var manualBlockReason by remember { mutableStateOf(bi(isEn, "حجز وهمي / مخالفة", "Fake booking / Violation")) }

    // General platform settings state
    var cfgPlatformName by remember(state.brandConfig.platformNameAr) { mutableStateOf(state.brandConfig.platformNameAr) }
    var cfgVillageName by remember(state.brandConfig.villageNameAr) { mutableStateOf(state.brandConfig.villageNameAr) }
    var cfgHospitalName by remember(state.brandConfig.hospitalNameAr) { mutableStateOf(state.brandConfig.hospitalNameAr) }
    var cfgSupportPhone by remember(state.brandConfig.supportPhone) { mutableStateOf(state.brandConfig.supportPhone) }
    var cfgAnnouncement by remember(state.brandConfig.homeAnnouncementAr) { mutableStateOf(state.brandConfig.homeAnnouncementAr) }
    var cfgApkUrl by remember(state.brandConfig.apkDownloadUrl) { mutableStateOf(state.brandConfig.apkDownloadUrl) }
    var cfgMedsApi by remember(state.brandConfig.medicationsApiUrl) { mutableStateOf(state.brandConfig.medicationsApiUrl) }
    var cfgAdminPin by remember(state.brandConfig.adminPin) { mutableStateOf(state.brandConfig.adminPin) }
    var cfgFbApiKey by remember(state.brandConfig.firebaseApiKey) { mutableStateOf(state.brandConfig.firebaseApiKey) }
    var cfgFbAuthDomain by remember(state.brandConfig.firebaseAuthDomain) { mutableStateOf(state.brandConfig.firebaseAuthDomain) }
    var cfgFbProjectId by remember(state.brandConfig.firebaseProjectId) { mutableStateOf(state.brandConfig.firebaseProjectId) }
    var cfgFbStorageBucket by remember(state.brandConfig.firebaseStorageBucket) { mutableStateOf(state.brandConfig.firebaseStorageBucket) }
    var cfgFbMessagingSenderId by remember(state.brandConfig.firebaseMessagingSenderId) { mutableStateOf(state.brandConfig.firebaseMessagingSenderId) }
    var cfgFbAppId by remember(state.brandConfig.firebaseAppId) { mutableStateOf(state.brandConfig.firebaseAppId) }
    var cfgFbVapidKey by remember(state.brandConfig.firebaseVapidKey) { mutableStateOf(state.brandConfig.firebaseVapidKey) }
    var cfgWorkerUrl by remember(state.brandConfig.workerUrl) { mutableStateOf(state.brandConfig.workerUrl) }
    var cfgDefaultDriveUrl by remember(state.brandConfig.defaultDriveWebAppUrl) { mutableStateOf(state.brandConfig.defaultDriveWebAppUrl) }
    var cfgRetentionDays by remember(state.brandConfig.fileRetentionDays) {
        mutableIntStateOf(
            when (state.brandConfig.fileRetentionDays) {
                90, 180, 360 -> state.brandConfig.fileRetentionDays
                else -> 360
            },
        )
    }

    val q = searchQuery.trim()
    val privateDoctors = remember(state.doctors, q) {
        state.doctors.filter { doc ->
            doc.clinicType == ClinicType.PRIVATE &&
                (q.isEmpty() || doc.doctorName.contains(q, true) || doc.specialty.contains(q, true) || doc.clinicName.contains(q, true))
        }
    }
    val hospitalDoctors = remember(state.doctors, q) {
        state.doctors.filter { doc ->
            doc.clinicType == ClinicType.HOSPITAL &&
                (q.isEmpty() || doc.doctorName.contains(q, true) || doc.specialty.contains(q, true) || doc.clinicName.contains(q, true))
        }
    }
    val bookedSlots = remember(state.allSlots) {
        state.allSlots.filter { it.status == SlotStatus.BOOKED }.sortedBy { it.effectiveMinutes }
    }
    val uq = usersSearchQuery.trim()
    val filteredUsers = remember(state.allAccounts, state.allFamilyMembers, uq, usersStatusFilter) {
        state.allAccounts.filter { acc ->
            val verifiedOk = when (usersStatusFilter) {
                "VERIFIED" -> acc.isPhoneVerified
                "UNVERIFIED" -> !acc.isPhoneVerified
                else -> true
            }
            val members = state.allFamilyMembers.filter { it.accountId == acc.id }
            val queryOk = uq.isEmpty() ||
                acc.fullName.contains(uq, true) ||
                acc.phone.contains(uq, true) ||
                acc.verifiedBy.contains(uq, true) ||
                members.any { it.fullName.contains(uq, true) }
            verifiedOk && queryOk
        }.sortedWith(compareBy<PatientAccountEntity> { it.isPhoneVerified }.thenByDescending { it.otpCreatedAtMillis }.thenBy { it.fullName })
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TealPrimary, TealPrimaryDark)))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bi(isEn, "مرحباً بك، ${state.brandConfig.adminOwnerName}", "Welcome, ${state.brandConfig.adminOwnerNameEn}"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                )
                                Text(
                                    text = bi(
                                        isEn,
                                        "الإدارة العامة لمنصة طب الحجايزة (Super Admin)",
                                        "General Administration — Teb Al-Hajayzah Platform",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = onOpenNotifications,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("admin_bell_button"),
                                ) {
                                    BadgedBox(
                                        badge = {
                                            val count = state.unreadSupportInboxCountForAdmin()
                                            if (count > 0) {
                                                Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                                                    Text(count.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = bi(isEn, "الإشعارات", "Notifications"),
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                Button(
                                    onClick = onLogoutAdmin,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TealPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("admin_logout_button"),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "خروج وتأمين", "Lock & Exit"))
                                }
                            }
                        }

                        // Live Firestore REST Cloud Connection Status Badge + Manual Refresh Now
                        val cs = state.cloudStatus
                        val badgeBg = when {
                            cs.isConnected -> Color(0xFFDCFCE7)
                            cs.hasAttempted -> Color(0xFFFEE2E2)
                            else -> Color(0xFFFEF3C7)
                        }
                        val badgeBorder = when {
                            cs.isConnected -> Color(0xFF16A34A)
                            cs.hasAttempted -> Color(0xFFDC2626)
                            else -> Color(0xFFD97706)
                        }
                        val badgeTextColor = when {
                            cs.isConnected -> Color(0xFF14532D)
                            cs.hasAttempted -> Color(0xFF991B1B)
                            else -> Color(0xFF92400E)
                        }
                        val badgeLabel = when {
                            cs.isConnected -> bi(isEn, "🟢 متصل بالسحابة الحية", "🟢 Connected to Live Cloud")
                            cs.hasAttempted -> bi(isEn, "🔴 خطأ اتصال: ${cs.errorMessage}", "🔴 Connection Error: ${cs.errorMessage}")
                            else -> bi(isEn, "🟡 جاري الاتصال بالسحابة الحية...", "🟡 Connecting to Live Cloud...")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.2.dp, badgeBorder),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("admin_cloud_status_badge"),
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onRefreshCloudNow()
                                    onShowMessage(bi(isEn, "🔄 جاري تحديث البيانات من السحابة الحية...", "🔄 Refreshing from Live Cloud..."))
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("admin_refresh_cloud_now_button"),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bi(isEn, "🔄 تحديث فوري الآن", "🔄 Refresh Now"),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            item {
                val tabs = listOf(
                    Triple(0, bi(isEn, "الأطباء والعيادات", "Doctors & Clinics"), null),
                    Triple(1, bi(isEn, "المستشفى والأقسام", "Hospital Depts"), null),
                    Triple(2, bi(isEn, "الحجوزات والحظر", "Bookings & Bans"), null),
                    Triple(3, bi(isEn, "الإعدادات و Drive", "Settings & Drive"), null),
                    Triple(4, bi(isEn, "إدارة معامل التحاليل", "Medical Labs"), "admin_labs_tab"),
                    Triple(5, bi(isEn, "🔑 التوثيق وأكواد OTP", "🔑 Verification & OTP"), "admin_otp_verify_tab"),
                    Triple(6, bi(isEn, "قائمة العملاء", "Users"), "admin_users_tab"),
                    Triple(7, bi(isEn, "📩 الرسائل والدعم", "Messages"), "admin_messages_control_tab"),
                    Triple(8, bi(isEn, "👨‍👩‍👧‍👦 ربط الأسرة", "Family Links"), "admin_family_links_tab"),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tabs.chunked(3).forEach { rowTabs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowTabs.forEach { (tabIdx, tabTitle, testTagKey) ->
                                val isSelected = selectedTab == tabIdx
                                val badgeCount = when (tabIdx) {
                                    5, 6 -> unverifiedUsersCount
                                    7 -> unreadAdminInbox
                                    8 -> pendingFamilyLinksCount
                                    else -> 0
                                }
                                Surface(
                                    onClick = { selectedTab = tabIdx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .let { if (testTagKey != null) it.testTag(testTagKey) else it },
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = tabTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                            )
                                            if (badgeCount > 0) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Badge(containerColor = Color(0xFFDC2626), contentColor = Color.White) {
                                                    Text(badgeCount.toString())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowTabs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdminMetricBox(
                        label = bi(isEn, "العملاء والمشتركين", "Users"),
                        value = state.allAccounts.size.toString(),
                        modifier = Modifier.weight(1f),
                        testTagName = "admin_metric_users",
                        onClick = { selectedTab = 6 },
                    )
                    AdminMetricBox(
                        label = bi(isEn, "عيادات / مستشفى", "Clinics"),
                        value = state.doctors.size.toString(),
                        modifier = Modifier.weight(1f),
                        testTagName = "admin_metric_clinics",
                        onClick = { selectedTab = 0 },
                    )
                    AdminMetricBox(
                        label = bi(isEn, "حجوزات نشطة", "Bookings"),
                        value = bookedSlots.size.toString(),
                        modifier = Modifier.weight(1f),
                        testTagName = "admin_metric_bookings",
                        onClick = {
                            selectedTab = 2
                            activeBookingsExpanded = true
                        },
                    )
                    AdminMetricBox(
                        label = bi(isEn, "أرقام محظورة", "Blocked"),
                        value = state.blockedPhones.size.toString(),
                        modifier = Modifier.weight(1f),
                        testTagName = "admin_metric_blocked",
                        onClick = {
                            selectedTab = 2
                            blockPhoneExpanded = true
                        },
                    )
                }
            }

            item {
                val vs = state.visitorStats
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_visitor_stats_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = bi(isEn, "📊 إحصائيات الزيارات الحية للمنصة (APK vs Web)", "📊 Live Platform Visitor Stats (APK vs Web)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            VisitorStatTile(
                                title = bi(isEn, "زوار اليوم", "Today"),
                                total = vs.todayTotal,
                                apk = vs.todayApk,
                                web = vs.todayWeb,
                                modifier = Modifier.weight(1f),
                            )
                            VisitorStatTile(
                                title = bi(isEn, "آخر 7 أيام", "Last 7 Days"),
                                total = vs.weekTotal,
                                apk = vs.weekApk,
                                web = vs.weekWeb,
                                modifier = Modifier.weight(1f),
                            )
                            VisitorStatTile(
                                title = bi(isEn, "آخر 30 يوماً", "Last 30 Days"),
                                total = vs.monthTotal,
                                apk = vs.monthApk,
                                web = vs.monthWeb,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Button(
                            onClick = {
                                defaultFormClinicType = ClinicType.PRIVATE
                                showAddForm = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_add_doctor_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(bi(isEn, "إضافة دكتور / عيادة خاصة جديدة", "Add New Doctor / Private Clinic"))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(bi(isEn, "ابحث بالاسم أو التخصص أو العيادة...", "Search name, specialty, or clinic...")) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(privateDoctors, key = { it.id }) { doc ->
                        AdminDoctorManageCard(
                            doctor = doc,
                            isEn = isEn,
                            currencyLabel = currencyLabel,
                            onChatClinic = {
                                onOpenChatThread(
                                    DirectChatTarget(
                                        threadType = ChatThreadType.CLINIC_ADMIN,
                                        viewerRole = MessageSenderRole.ADMIN,
                                        title = doc.clinicName.ifBlank { doc.doctorName },
                                        subtitle = doc.doctorName,
                                        doctorId = doc.id,
                                    )
                                )
                            },
                            onEdit = { doctorToEdit = doc },
                            onDelete = { doctorToDelete = doc },
                        )
                    }
                }

                1 -> {
                    item {
                        Button(
                            onClick = {
                                defaultFormClinicType = ClinicType.HOSPITAL
                                showAddForm = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_add_hospital_dept_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.LocalHospital, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(bi(isEn, "إضافة قسم / طبيب في المستشفى التخصصي", "Add Hospital Department / Doctor"))
                        }
                    }
                    items(hospitalDoctors, key = { it.id }) { doc ->
                        AdminDoctorManageCard(
                            doctor = doc,
                            isEn = isEn,
                            currencyLabel = currencyLabel,
                            onChatClinic = {
                                onOpenChatThread(
                                    DirectChatTarget(
                                        threadType = ChatThreadType.CLINIC_ADMIN,
                                        viewerRole = MessageSenderRole.ADMIN,
                                        title = doc.clinicName.ifBlank { doc.doctorName },
                                        subtitle = doc.doctorName,
                                        doctorId = doc.id,
                                    )
                                )
                            },
                            onEdit = { doctorToEdit = doc },
                            onDelete = { doctorToDelete = doc },
                        )
                    }
                }

                2 -> {
                    item {
                        AdminAccordionCard(
                            title = bi(
                                isEn,
                                "🚫 حظر رقم مخالف وإدارة المحظورين (${state.blockedPhones.size})",
                                "🚫 Block Phone & Blocked List (${state.blockedPhones.size})",
                            ),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء", "Tap to expand/collapse"),
                            expanded = blockPhoneExpanded,
                            onToggle = { blockPhoneExpanded = !blockPhoneExpanded },
                            testTagName = "admin_accordion_block_phone",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = manualBlockPhone,
                                    onValueChange = { manualBlockPhone = it },
                                    label = { Text(bi(isEn, "رقم الهاتف المراد حظره", "Phone to Block")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_block_phone_input"),
                                )
                                OutlinedTextField(
                                    value = manualBlockReason,
                                    onValueChange = { manualBlockReason = it },
                                    label = { Text(bi(isEn, "سبب الحظر", "Reason")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        onBlockPhone(manualBlockPhone, manualBlockReason)
                                        manualBlockPhone = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_block_phone_submit"),
                                ) {
                                    Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "حظر الرقم وإلغاء حجوزاته", "Block & Cancel Bookings"))
                                }

                                if (state.blockedPhones.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        bi(isEn, "الأرقام المحظورة (${state.blockedPhones.size})", "Blocked Numbers (${state.blockedPhones.size})"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    state.blockedPhones.forEach { blocked ->
                                        Surface(
                                            color = SlotBookedBg,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(blocked.phone, style = MaterialTheme.typography.titleSmall, color = SlotBookedText, fontWeight = FontWeight.Bold)
                                                    Text(blocked.reason, style = MaterialTheme.typography.bodySmall, color = SlotBookedText)
                                                }
                                                OutlinedButton(onClick = { onUnblockPhone(blocked.phone) }) {
                                                    Text(bi(isEn, "رفع الحظر", "Unblock"))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AdminAccordionCard(
                            title = bi(isEn, "📅 جميع الحجوزات النشطة في المنصة (${bookedSlots.size})", "📅 All Active Bookings (${bookedSlots.size})"),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء الحجوزات النشطة", "Tap to expand/collapse active bookings"),
                            expanded = activeBookingsExpanded,
                            onToggle = { activeBookingsExpanded = !activeBookingsExpanded },
                            testTagName = "admin_accordion_active_bookings",
                        ) {
                            if (bookedSlots.isEmpty()) {
                                Text(
                                    text = bi(isEn, "لا توجد حجوزات نشطة حالياً.", "No active bookings at the moment."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    bookedSlots.forEach { slot ->
                                        val doctor = state.doctors.firstOrNull { it.id == slot.doctorId }
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(slot.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                        Text(bi(isEn, "هاتف: ${slot.patientPhone}", "Phone: ${slot.patientPhone}"), style = MaterialTheme.typography.bodySmall)
                                                        Text(
                                                            text = "${doctor?.doctorName ?: ""} • ${doctor?.clinicName ?: ""}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TealPrimary,
                                                        )
                                                    }
                                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                                                        Text(
                                                            text = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { onCancelBooking(slot.id) },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(bi(isEn, "إلغاء الحجز", "Cancel Booking"))
                                                    }
                                                    if (slot.patientPhone.isNotBlank()) {
                                                        Button(
                                                            onClick = { onBlockPhone(slot.patientPhone, bi(isEn, "حجز وهمي متكرر", "Repeated fake bookings")) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                                                            modifier = Modifier.weight(1f),
                                                        ) {
                                                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(bi(isEn, "حظر الرقم", "Block Phone"))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        var isTestingPush by remember { mutableStateOf(false) }
                        var testPushStatus by remember { mutableStateOf("") }
                        var testPushOk by remember { mutableStateOf<Boolean?>(null) }
                        AdminAccordionCard(
                            title = bi(isEn, "🔔 حالة إشعارات المدير العام", "🔔 Admin Notification Status"),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء", "Tap to expand/collapse"),
                            expanded = notifCardExpanded,
                            onToggle = { notifCardExpanded = !notifCardExpanded },
                            testTagName = "admin_accordion_notif",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                NotificationStatusCard(
                                    isEn = isEn,
                                    tagPrefix = "admin_notif",
                                    onShowMessage = onShowMessage,
                                )
                                Button(
                                    onClick = {
                                        if (!isTestingPush) {
                                            isTestingPush = true
                                            testPushStatus = bi(isEn, "⏳ جاري إرسال إشعار تجريبي عبر الخادم السحابي...", "⏳ Sending live push via Cloudflare Worker...")
                                            testPushOk = null
                                            onSendTestAdminPush { ok, details ->
                                                isTestingPush = false
                                                testPushOk = ok
                                                testPushStatus = details
                                            }
                                        }
                                    },
                                    enabled = !isTestingPush,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TealPrimary,
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_test_fcm_push_btn"),
                                ) {
                                    Text(
                                        text = if (isTestingPush) {
                                            bi(isEn, "⏳ جاري الاختبار...", "⏳ Testing Push...")
                                        } else {
                                            bi(isEn, "🔔 إرسال إشعار تجريبي لهاتفي الآن", "🔔 Send Live Test Push to My Phone")
                                        },
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (testPushStatus.isNotBlank()) {
                                    Surface(
                                        color = when (testPushOk) {
                                            true -> SlotAvailableBg
                                            false -> SlotBookedBg
                                            null -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            when (testPushOk) {
                                                true -> SlotAvailableBorder
                                                false -> SlotBookedBorder
                                                null -> TealPrimary.copy(alpha = 0.4f)
                                            },
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = testPushStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (testPushOk) {
                                                true -> SlotAvailableText
                                                false -> SlotBookedText
                                                null -> MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.padding(10.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AdminAccordionCard(
                            title = bi(isEn, "☁️ إدارة مساحات Google Drive لكل عيادة (حسابان 30GB لكل عيادة)", "☁️ Per-Clinic Google Drive Accounts (2 per Clinic)"),
                            subtitle = bi(isEn, "اضغط لإعداد روابط Drive للعيادات + المساحة الاحتياطية", "Tap to manage Clinic Drive URLs & Fallback Storage"),
                            expanded = driveStorageExpanded,
                            onToggle = { driveStorageExpanded = !driveStorageExpanded },
                            testTagName = "admin_accordion_drive",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    color = SlotAvailableBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SlotAvailableBorder.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = bi(
                                            isEn,
                                            "كل عيادة تحصل على حساب أساسي (15GB) وحساب ثانوي احتياطي (+15GB) بإجمالي 30GB لحفظ التحاليل والأشعة مع تحويل فوري للسجل الكتابي الدائم.",
                                            "Each clinic gets a Primary Drive (15GB) + Secondary Drive (+15GB) = 30GB with instant Permanent Medical Log extraction.",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlotAvailableText,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                                if (selectedDriveDoc != null) {
                                    ExposedDropdownMenuBox(
                                        expanded = driveDropdownExpanded,
                                        onExpandedChange = { driveDropdownExpanded = !driveDropdownExpanded },
                                    ) {
                                        OutlinedTextField(
                                            value = "${selectedDriveDoc.clinicName} (${selectedDriveDoc.doctorName})",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(bi(isEn, "اختر العيادة لتخصيص روابط Google Drive", "Select Clinic for Drive Storage")) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driveDropdownExpanded) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth()
                                                .testTag("admin_drive_clinic_selector"),
                                        )
                                        ExposedDropdownMenu(
                                            expanded = driveDropdownExpanded,
                                            onDismissRequest = { driveDropdownExpanded = false },
                                        ) {
                                            state.doctors.forEach { d ->
                                                DropdownMenuItem(
                                                    text = { Text("${d.clinicName} — ${d.doctorName}") },
                                                    onClick = {
                                                        selectedDriveDocId = d.id
                                                        driveDropdownExpanded = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = clinicDrivePrimaryInput,
                                        onValueChange = { clinicDrivePrimaryInput = it },
                                        label = { Text(bi(isEn, "حساب Google Drive الأساسي (Apps Script URL 1 - 15GB)", "Primary Clinic Drive URL (Apps Script 1)")) },
                                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("admin_clinic_drive_primary"),
                                    )
                                    OutlinedTextField(
                                        value = clinicDriveSecondaryInput,
                                        onValueChange = { clinicDriveSecondaryInput = it },
                                        label = { Text(bi(isEn, "حساب Google Drive الثانوي الإضافي (Apps Script URL 2 - +15GB)", "Secondary Clinic Drive URL (Apps Script 2)")) },
                                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("admin_clinic_drive_secondary"),
                                    )
                                    Button(
                                        onClick = {
                                            onUpdateDoctorDriveAccounts(
                                                selectedDriveDoc.id,
                                                clinicDrivePrimaryInput,
                                                clinicDriveSecondaryInput,
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("admin_save_clinic_drive_button"),
                                    ) {
                                        Text(bi(isEn, "💾 حفظ حسابي Google Drive للعيادة المختارة", "💾 Save Both Drive Accounts for Clinic"))
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                OutlinedTextField(
                                    value = cfgDefaultDriveUrl,
                                    onValueChange = { cfgDefaultDriveUrl = it },
                                    label = {
                                        Text(
                                            bi(
                                                isEn,
                                                "رابط Google Drive الاحتياطي العام للمنصة (Fallback Apps Script URL)",
                                                "Platform Fallback Google Drive Apps Script URL",
                                            ),
                                        )
                                    },
                                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_default_drive_url_input"),
                                )
                                Text(
                                    text = bi(
                                        isEn,
                                        "مدة الاحتفاظ بالملفات الثقيلة (PDF والأشعة) — مع بقاء السجل الكتابي الدائم مدى الحياة:",
                                        "Heavy attachment retention (PDF/Images) — Permanent medical logs remain forever:",
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        90 to bi(isEn, "90 يوماً", "90 Days"),
                                        180 to bi(isEn, "180 يوماً", "180 Days"),
                                        360 to bi(
                                            isEn,
                                            "360 يوماً — سنة كاملة لمتابعات الحمل والأمراض المزمنة",
                                            "360 Days — Full Year for Pregnancy & Chronic Care",
                                        ),
                                    ).forEach { (daysOpt, labelOpt) ->
                                        FilterChip(
                                            selected = cfgRetentionDays == daysOpt,
                                            onClick = { cfgRetentionDays = daysOpt },
                                            label = { Text(labelOpt) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealPrimary,
                                                selectedLabelColor = Color.White,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_retention_$daysOpt"),
                                        )
                                    }
                                }
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = bi(
                                            isEn,
                                            "🛡️ ضمان حفظ السجل الطبي: تفريغ الملفات الثقيلة (PDF/الصور) يحذف المرفقات فقط لتحرير المساحة، مع بقاء كافة السجلات الطبية الكتابية الدائمة محفوظة 100% مدى الحياة.",
                                            "🛡️ Permanent Log Guarantee: Cleaning heavy PDF/image attachments frees Drive space while keeping 100% of written permanent medical history logs forever.",
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF1E3A8A),
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }

                                // مؤشر استهلاك مساحة التخزين الاحتياطية للمنصة (15 GB)
                                val fallbackRecords = remember(state.medicalRecords, state.doctors) {
                                    val docsById = state.doctors.associateBy { it.id }
                                    state.medicalRecords.filter { rec ->
                                        (rec.attachmentPath.isNotBlank() || rec.driveFileUrl.isNotBlank()) &&
                                            (rec.doctorId.isBlank() || docsById[rec.doctorId]?.driveWebAppUrl.isNullOrBlank())
                                    }
                                }
                                val fallbackUsedGb = remember(fallbackRecords) {
                                    computeDriveUsedGb(fallbackRecords)
                                }
                                val fallbackPercent = computeDrivePercent(fallbackUsedGb, 15.0, fallbackRecords.size)
                                LaunchedEffect(fallbackPercent) {
                                    if (fallbackPercent >= 90) {
                                        onReportStorage90Alert(
                                            bi(isEn, "الاحتياطي العام للمنصة", "Platform Fallback Drive"),
                                            fallbackPercent,
                                        )
                                    }
                                }
                                DriveAccountUsageMeterCard(
                                    accountTitle = bi(isEn, "📦 حساب المنصة الاحتياطي العام (Fallback Drive)", "📦 Platform Fallback Drive Account"),
                                    usedGb = fallbackUsedGb,
                                    quotaGb = 15.0,
                                    usedPercent = fallbackPercent,
                                    heavyFilesCount = fallbackRecords.size,
                                    isLinked = state.brandConfig.defaultDriveWebAppUrl.isNotBlank(),
                                    isEn = isEn,
                                    testTagPrefix = "drive_meter_fallback",
                                    onCleanHeavyFiles = { onCleanClinicHeavyFiles("__FALLBACK__") },
                                )

                                AdminAccordionCard(
                                    title = bi(isEn, "قائمة حسابات Drive المرتبطة بالعيادات (${state.doctors.size})", "Linked Clinic Drive Accounts (${state.doctors.size})"),
                                    subtitle = bi(isEn, "متابعة نسبة الاستهلاك (1% - 100%) والتفريغ اليدوي", "Monitor usage (1%–100%) & manual cleanup"),
                                    expanded = driveClinicsListExpanded,
                                    onToggle = { driveClinicsListExpanded = !driveClinicsListExpanded },
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.doctors.forEach { d ->
                                            val docHeavyRecords = state.medicalRecords.filter { rec ->
                                                rec.doctorId == d.id && (rec.attachmentPath.isNotBlank() || rec.driveFileUrl.isNotBlank())
                                            }
                                            val primaryUsedGb = computeDriveUsedGb(docHeavyRecords)
                                            val primaryPercent = computeDrivePercent(primaryUsedGb, 15.0, docHeavyRecords.size)
                                            val secondaryRecords = if (d.secondaryDriveWebAppUrl.isNotBlank()) {
                                                docHeavyRecords.filter { it.driveFileUrl.contains("secondary", ignoreCase = true) }
                                            } else emptyList()
                                            val secondaryUsedGb = computeDriveUsedGb(secondaryRecords)
                                            val secondaryPercent = computeDrivePercent(secondaryUsedGb, 15.0, secondaryRecords.size)

                                            LaunchedEffect(primaryPercent, d.id) {
                                                if (primaryPercent >= 90) {
                                                    onReportStorage90Alert("${d.clinicName} (الأساسي)", primaryPercent)
                                                }
                                            }
                                            LaunchedEffect(secondaryPercent, d.id) {
                                                if (secondaryPercent >= 90) {
                                                    onReportStorage90Alert("${d.clinicName} (الثانوي)", secondaryPercent)
                                                }
                                            }

                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Text("${d.clinicName} (${d.doctorName})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    DriveAccountUsageMeterCard(
                                                        accountTitle = bi(isEn, "1️⃣ الحساب الأساسي للعيادة (Primary 15 GB)", "1️⃣ Primary Clinic Drive (15 GB)"),
                                                        usedGb = primaryUsedGb,
                                                        quotaGb = 15.0,
                                                        usedPercent = primaryPercent,
                                                        heavyFilesCount = docHeavyRecords.size,
                                                        isLinked = d.driveWebAppUrl.isNotBlank(),
                                                        isEn = isEn,
                                                        testTagPrefix = "drive_meter_primary_${d.id}",
                                                        onCleanHeavyFiles = { onCleanClinicHeavyFiles(d.id) },
                                                    )
                                                    DriveAccountUsageMeterCard(
                                                        accountTitle = bi(isEn, "2️⃣ الحساب الثانوي الاحتياطي (Secondary +15 GB)", "2️⃣ Secondary Backup Drive (+15 GB)"),
                                                        usedGb = secondaryUsedGb,
                                                        quotaGb = 15.0,
                                                        usedPercent = secondaryPercent,
                                                        heavyFilesCount = secondaryRecords.size,
                                                        isLinked = d.secondaryDriveWebAppUrl.isNotBlank(),
                                                        isEn = isEn,
                                                        testTagPrefix = "drive_meter_secondary_${d.id}",
                                                        onCleanHeavyFiles = { onCleanClinicHeavyFiles(d.id) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = {
                                        onSavePlatformSettings(
                                            cfgPlatformName,
                                            cfgVillageName,
                                            cfgHospitalName,
                                            cfgSupportPhone,
                                            cfgAnnouncement,
                                            cfgApkUrl,
                                            cfgMedsApi,
                                            cfgAdminPin,
                                            cfgFbApiKey,
                                            cfgFbAuthDomain,
                                            cfgFbProjectId,
                                            cfgFbStorageBucket,
                                            cfgFbMessagingSenderId,
                                            cfgFbAppId,
                                            cfgFbVapidKey,
                                            cfgWorkerUrl,
                                            cfgDefaultDriveUrl,
                                            cfgRetentionDays,
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_save_drive_settings_btn"),
                                ) {
                                    Text(
                                        text = bi(isEn, "💾 حفظ وتطبيق الإعدادات", "💾 Save & Apply Settings"),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AdminAccordionCard(
                            title = bi(isEn, "⚙️ إعدادات المنصة العامة والرقم السري (PIN: 132027)", "⚙️ General Platform Settings & Admin PIN"),
                            subtitle = bi(isEn, "اضغط للتعديل (اسم المنصة، الإعلان، APK، وتغيير Admin PIN)", "Tap to edit platform details & Admin PIN"),
                            expanded = platformSettingsExpanded,
                            onToggle = { platformSettingsExpanded = !platformSettingsExpanded },
                            testTagName = "admin_accordion_platform",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = cfgPlatformName,
                                    onValueChange = { cfgPlatformName = it },
                                    label = { Text(bi(isEn, "اسم المنصة", "Platform Name")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgVillageName,
                                    onValueChange = { cfgVillageName = it },
                                    label = { Text(bi(isEn, "اسم القرية / المركز", "Village / Center Name")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgHospitalName,
                                    onValueChange = { cfgHospitalName = it },
                                    label = { Text(bi(isEn, "اسم المستشفى الرئيسي", "Main Hospital Name")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgSupportPhone,
                                    onValueChange = { cfgSupportPhone = it },
                                    label = { Text(bi(isEn, "رقم التواصل والدعم الفني", "Support Phone")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgAnnouncement,
                                    onValueChange = { cfgAnnouncement = it },
                                    label = { Text(bi(isEn, "إعلان التنبيه بالصفحة الرئيسية (اختياري)", "Home Announcement Banner (optional)")) },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgApkUrl,
                                    onValueChange = { cfgApkUrl = it },
                                    label = { Text(bi(isEn, "رابط تحميل تطبيق الأندرويد المباشر (APK)", "Direct Android APK Download URL")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgMedsApi,
                                    onValueChange = { cfgMedsApi = it },
                                    label = { Text(bi(isEn, "رابط API الأدوية (اختياري)", "Medications API URL (optional)")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                PasswordOutlinedTextField(
                                    value = cfgAdminPin,
                                    onValueChange = { cfgAdminPin = it.filter { ch -> ch.isDigit() } },
                                    label = bi(isEn, "الرقم السري للوحة التحكم (Admin PIN)", "Super Admin PIN"),
                                    keyboardType = KeyboardType.NumberPassword,
                                    toggleTestTag = "admin_change_pin_toggle",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_change_pin_input"),
                                )
                                Button(
                                    onClick = {
                                        onSavePlatformSettings(
                                            cfgPlatformName,
                                            cfgVillageName,
                                            cfgHospitalName,
                                            cfgSupportPhone,
                                            cfgAnnouncement,
                                            cfgApkUrl,
                                            cfgMedsApi,
                                            cfgAdminPin,
                                            cfgFbApiKey,
                                            cfgFbAuthDomain,
                                            cfgFbProjectId,
                                            cfgFbStorageBucket,
                                            cfgFbMessagingSenderId,
                                            cfgFbAppId,
                                            cfgFbVapidKey,
                                            cfgWorkerUrl,
                                            cfgDefaultDriveUrl,
                                            cfgRetentionDays,
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_save_general_settings_btn"),
                                ) {
                                    Text(
                                        text = bi(isEn, "💾 حفظ وتطبيق الإعدادات", "💾 Save & Apply Settings"),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { cloudSettingsExpanded = !cloudSettingsExpanded }
                                        .padding(vertical = 4.dp)
                                        .testTag("admin_cloud_settings_toggle"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = SlotAvailableBorder)
                                        Column {
                                            Text(
                                                text = bi(
                                                    isEn,
                                                    "🟢 المزامنة السحابية والإشعارات نشطة (اضغط للتعديل)",
                                                    "🟢 Cloud Sync & Push Active (Tap to Edit)",
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SlotAvailableText,
                                            )
                                            Text(
                                                text = "teb_alhajayzah_settings • Worker & Firebase FCM",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (cloudSettingsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = bi(isEn, "إظهار أو إخفاء إعدادات الربط السحابي", "Toggle Cloud Settings"),
                                        tint = TealPrimary,
                                    )
                                }

                                AnimatedVisibility(visible = cloudSettingsExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(
                                            color = SlotAvailableBg.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, SlotAvailableBorder.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    text = bi(
                                                        isEn,
                                                        "🟢 Auto-Synced Cloud Collections (Database: (default) • gen-lang-client-0514993881):",
                                                        "🟢 متصل تلقائياً بقاعدة Firestore المستقلة ((default) • gen-lang-client-0514993881):",
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SlotAvailableText,
                                                )
                                                Text(
                                                    text = "teb_alhajayzah_accounts • teb_alhajayzah_clinics • teb_alhajayzah_appointments • teb_alhajayzah_messages • teb_alhajayzah_settings • teb_alhajayzah_records",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SlotAvailableText,
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = cfgWorkerUrl,
                                            onValueChange = { cfgWorkerUrl = it },
                                            label = { Text(bi(isEn, "رابط الخادم الوسيط (Cloudflare Worker URL)", "Cloudflare Worker URL (Push & WhatsApp)")) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("admin_worker_url_input"),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbApiKey,
                                            onValueChange = { cfgFbApiKey = it },
                                            label = { Text("Firebase apiKey") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("admin_fb_api_key_input"),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbAuthDomain,
                                            onValueChange = { cfgFbAuthDomain = it },
                                            label = { Text("Firebase authDomain") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbProjectId,
                                            onValueChange = { cfgFbProjectId = it },
                                            label = { Text("Firebase projectId") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("admin_fb_project_id_input"),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbStorageBucket,
                                            onValueChange = { cfgFbStorageBucket = it },
                                            label = { Text("Firebase storageBucket") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbMessagingSenderId,
                                            onValueChange = { cfgFbMessagingSenderId = it },
                                            label = { Text("Firebase messagingSenderId") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbAppId,
                                            onValueChange = { cfgFbAppId = it },
                                            label = { Text("Firebase appId") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = cfgFbVapidKey,
                                            onValueChange = { cfgFbVapidKey = it },
                                            label = { Text("Firebase FCM vapidKey") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Button(
                                            onClick = {
                                                onSavePlatformSettings(
                                                    cfgPlatformName,
                                                    cfgVillageName,
                                                    cfgHospitalName,
                                                    cfgSupportPhone,
                                                    cfgAnnouncement,
                                                    cfgApkUrl,
                                                    cfgMedsApi,
                                                    cfgAdminPin,
                                                    cfgFbApiKey,
                                                    cfgFbAuthDomain,
                                                    cfgFbProjectId,
                                                    cfgFbStorageBucket,
                                                    cfgFbMessagingSenderId,
                                                    cfgFbAppId,
                                                    cfgFbVapidKey,
                                                    cfgWorkerUrl,
                                                    cfgDefaultDriveUrl,
                                                    cfgRetentionDays,
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF16A34A),
                                                contentColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("admin_save_cloud_settings_btn"),
                                        ) {
                                            Text(
                                                text = bi(isEn, "💾 حفظ وتطبيق الإعدادات", "💾 Save & Apply Settings"),
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = onResetPlatformSettings,
                                modifier = Modifier.weight(1f).height(50.dp),
                            ) {
                                Text(bi(isEn, "استعادة الافتراضي", "Reset Default"))
                            }
                            Button(
                                onClick = {
                                    onSavePlatformSettings(
                                        cfgPlatformName,
                                        cfgVillageName,
                                        cfgHospitalName,
                                        cfgSupportPhone,
                                        cfgAnnouncement,
                                        cfgApkUrl,
                                        cfgMedsApi,
                                        cfgAdminPin,
                                        cfgFbApiKey,
                                        cfgFbAuthDomain,
                                        cfgFbProjectId,
                                        cfgFbStorageBucket,
                                        cfgFbMessagingSenderId,
                                        cfgFbAppId,
                                        cfgFbVapidKey,
                                        cfgWorkerUrl,
                                        cfgDefaultDriveUrl,
                                        cfgRetentionDays,
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(50.dp)
                                    .testTag("admin_save_platform_settings"),
                            ) {
                                Text(bi(isEn, "💾 حفظ وتطبيق الإعدادات", "💾 Save & Apply Settings"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        Button(
                            onClick = { showAddLabForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_add_lab_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(bi(isEn, "إضافة معمل تحاليل جديد في الحجايزة", "Add New Medical Lab"))
                        }
                    }

                    if (state.labs.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = bi(
                                        isEn,
                                        "لم تتم إضافة معامل تحاليل بعد. أضف معملاً لإنشاء حساب دخول خاص به.",
                                        "No medical labs added yet. Add a lab to generate its login credentials.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(state.labs, key = { it.id }) { lab ->
                            AdminLabManageCard(
                                lab = lab,
                                isEn = isEn,
                                onEdit = { labToEdit = lab },
                                onDelete = { labToDelete = lab },
                            )
                        }
                    }
                }

                5, 6 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (selectedTab == 5) Icons.Filled.VerifiedUser else Icons.Filled.Group,
                                            contentDescription = null,
                                            tint = TealPrimary,
                                        )
                                        Text(
                                            text = if (selectedTab == 5) {
                                                bi(isEn, "🔑 مركز التوثيق وأكواد OTP للعملاء", "🔑 Live OTP & Verification Center")
                                            } else {
                                                bi(isEn, "قائمة العملاء والمشتركين (Users)", "Registered Customers & Users")
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onRefreshCloudNow()
                                            onShowMessage(bi(isEn, "🔄 جاري التحديث الفوري من السحابة...", "🔄 Syncing from Live Cloud..."))
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("admin_users_refresh_now"),
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(bi(isEn, "تحديث فوري الآن", "Refresh Now"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                val verifiedCount = state.allAccounts.count { it.isPhoneVerified }
                                val unverifiedCount = state.allAccounts.size - verifiedCount
                                Text(
                                    text = bi(
                                        isEn,
                                        "إجمالي المشتركين: ${state.allAccounts.size}  •  ✅ موثق: $verifiedCount  •  ⚠️ غير موثق: $unverifiedCount",
                                        "Total Users: ${state.allAccounts.size}  •  ✅ Verified: $verifiedCount  •  ⚠️ Unverified: $unverifiedCount",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                OutlinedTextField(
                                    value = usersSearchQuery,
                                    onValueChange = { usersSearchQuery = it },
                                    placeholder = { Text(bi(isEn, "ابحث بالاسم أو الهاتف أو أفراد الأسرة أو جهة التفعيل...", "Search name, phone, family, or verifier...")) },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_users_search"),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = usersStatusFilter == "ALL",
                                        onClick = { usersStatusFilter = "ALL" },
                                        label = { Text(bi(isEn, "الكل (${state.allAccounts.size})", "All (${state.allAccounts.size})")) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealPrimary,
                                            selectedLabelColor = Color.White,
                                        ),
                                    )
                                    FilterChip(
                                        selected = usersStatusFilter == "VERIFIED",
                                        onClick = { usersStatusFilter = "VERIFIED" },
                                        label = { Text(bi(isEn, "موثق ($verifiedCount)", "Verified ($verifiedCount)")) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SlotAvailableBorder,
                                            selectedLabelColor = Color.White,
                                        ),
                                    )
                                    FilterChip(
                                        selected = usersStatusFilter == "UNVERIFIED",
                                        onClick = { usersStatusFilter = "UNVERIFIED" },
                                        label = { Text(bi(isEn, "غير موثق ($unverifiedCount)", "Unverified ($unverifiedCount)")) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SlotBookedBorder,
                                            selectedLabelColor = Color.White,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = bi(isEn, "لا توجد حسابات عملاء مسجلة تطابق البحث حالياً.", "No registered customer accounts match right now."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.id }) { userAcc ->
                            val members = state.allFamilyMembers.filter { it.accountId == userAcc.id }
                            AdminUserManageCard(
                                account = userAcc,
                                familyMembers = members,
                                isEn = isEn,
                                onToggleVerified = { onSetUserVerified(userAcc.id, !userAcc.isPhoneVerified) },
                                onCopyOtp = {
                                    val readyMsg = "أهلاً بك في منصة طب الحجايزة. كود تفعيل حسابك هو: ${userAcc.otpCode} (صالح لمدة 24 ساعة). يمكنك الدخول وتأكيد حسابك الآن."
                                    clipboardManager.setText(AnnotatedString(readyMsg))
                                    onShowMessage(
                                        bi(
                                            isEn,
                                            "تم نسخ كود ورسالة الواتساب (${userAcc.otpCode}) لـ ${userAcc.fullName}",
                                            "Copied OTP & WhatsApp message (${userAcc.otpCode}) for ${userAcc.fullName}",
                                        )
                                    )
                                },
                                onRegenerateOtp = { onRegenerateUserOtp(userAcc.id) },
                                onOpenChat = {
                                    onOpenChatThread(
                                        DirectChatTarget(
                                            threadType = ChatThreadType.PATIENT_ADMIN,
                                            viewerRole = MessageSenderRole.ADMIN,
                                            title = "${userAcc.fullName} (${bi(isEn, "دعم فني", "Support")})",
                                            subtitle = userAcc.phone,
                                            patientAccountId = userAcc.id,
                                            patientName = userAcc.fullName,
                                            patientPhone = userAcc.phone,
                                        )
                                    )
                                },
                                onEdit = { userToEdit = userAcc },
                                onDelete = { userToDelete = userAcc },
                            )
                        }
                    }
                }

                7 -> {
                    item {
                        AdminAccordionCard(
                            title = bi(isEn, "📢 الدردشة العامة والإشعار الجماعي للمنصة", "📢 Global Chat Switch & Broadcast"),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء", "Tap to expand/collapse"),
                            expanded = chatBroadcastExpanded,
                            onToggle = { chatBroadcastExpanded = !chatBroadcastExpanded },
                            testTagName = "admin_accordion_chat_broadcast",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = bi(isEn, "الزر الرئيسي للمحادثات مع العيادات", "Master Global Chat Switch"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = bi(
                                                isEn,
                                                "تفعيل أو إيقاف زر (💬 مراسلة العيادة) للمرضى على مستوى جميع العيادات فوراً.",
                                                "Immediately enable or disable Patient ↔ Clinic messaging across all clinics.",
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = state.brandConfig.globalChatEnabled,
                                        onCheckedChange = onToggleGlobalChat,
                                        modifier = Modifier.testTag("admin_global_chat_switch"),
                                    )
                                }

                                HorizontalDivider()

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = TealPrimary)
                                    Text(
                                        text = bi(isEn, "إرسال إشعار عام لجميع مستخدمي المنصة", "Broadcast Alert to All Platform Users"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                OutlinedTextField(
                                    value = globalBroadcastDraft,
                                    onValueChange = { globalBroadcastDraft = it },
                                    label = { Text(bi(isEn, "نص الإشعار العام (يظهر لجميع المرضى والعيادات)", "Platform-wide Broadcast Message")) },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_global_broadcast_input"),
                                )
                                Button(
                                    onClick = {
                                        onSendAdminGlobalBroadcast(globalBroadcastDraft) {
                                            globalBroadcastDraft = ""
                                        }
                                    },
                                    enabled = globalBroadcastDraft.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_global_broadcast_send"),
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "📢 نشر الإشعار العام للجميع", "📢 Publish Global Broadcast"))
                                }
                            }
                        }
                    }

                    item {
                        AdminAccordionCard(
                            title = bi(isEn, "🏥 التحكم في محادثات وتنبيهات كل عيادة (${state.doctors.size})", "🏥 Per-Clinic Chat & Alert Controls (${state.doctors.size})"),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء", "Tap to expand/collapse"),
                            expanded = clinicChatExpanded,
                            onToggle = { clinicChatExpanded = !clinicChatExpanded },
                            testTagName = "admin_accordion_clinic_chat",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedAlertDoctorId == null,
                                            onClick = { selectedAlertDoctorId = null },
                                            label = { Text(bi(isEn, "كل العيادات", "All Clinics")) },
                                        )
                                    }
                                    items(state.doctors, key = { "alert_doc_${it.id}" }) { doc ->
                                        FilterChip(
                                            selected = selectedAlertDoctorId == doc.id,
                                            onClick = { selectedAlertDoctorId = doc.id },
                                            label = { Text(doc.clinicName) },
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = clinicAlertDraft,
                                    onValueChange = { clinicAlertDraft = it },
                                    label = { Text(bi(isEn, "تنبيه إداري موجه للعيادات المحددة", "Targeted Clinic Alert Message")) },
                                    maxLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_clinic_alert_input"),
                                )
                                OutlinedButton(
                                    onClick = {
                                        onSendAdminClinicAlert(selectedAlertDoctorId, clinicAlertDraft) {
                                            clinicAlertDraft = ""
                                        }
                                    },
                                    enabled = clinicAlertDraft.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("admin_all_clinics_alert_send"),
                                ) {
                                    Text(bi(isEn, "إرسال التنبيه الإداري للعيادة / العيادات", "Send Alert to Selected Clinic(s)"))
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                state.doctors.forEach { doc ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.clinicName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (doc.chatEnabled) {
                                                    bi(isEn, "${doc.doctorName} • الدردشة مفعلة", "${doc.doctorName} • Chat ON")
                                                } else {
                                                    bi(isEn, "${doc.doctorName} • الدردشة متوقفة", "${doc.doctorName} • Chat OFF")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Switch(
                                            checked = doc.chatEnabled,
                                            onCheckedChange = { enabled -> onToggleClinicChat(doc.id, enabled) },
                                            modifier = Modifier.testTag("admin_clinic_chat_switch_${doc.id}"),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val allThreads = state.directMessages
                        .groupBy { "${it.threadType}:${it.patientAccountId}:${it.doctorId}" }
                        .entries
                        .mapNotNull { (threadKey, list) ->
                            val sorted = list.sortedBy { it.createdAtMillis }
                            val latest = sorted.lastOrNull() ?: return@mapNotNull null
                            val unread = if (latest.threadType == ChatThreadType.PATIENT_CLINIC) 0 else list.count { !it.isReadByAdmin }
                            Triple(threadKey, sorted, Pair(latest, unread))
                        }
                        .sortedByDescending { it.third.first.createdAtMillis }

                    item {
                        AdminAccordionCard(
                            title = bi(
                                isEn,
                                "💬 إدارة ومراقبة كافة المحادثات والرسائل (${allThreads.size})",
                                "💬 Conversations & Moderation (${allThreads.size})",
                            ),
                            subtitle = bi(isEn, "اضغط لعرض / إخفاء المحادثات", "Tap to expand/collapse conversations"),
                            expanded = threadsListExpanded,
                            onToggle = { threadsListExpanded = !threadsListExpanded },
                            testTagName = "admin_accordion_threads",
                        ) {
                            if (allThreads.isEmpty()) {
                                Text(
                                    text = bi(isEn, "لا توجد محادثات أو رسائل على المنصة حتى الآن.", "No conversations or messages on the platform yet."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    allThreads.forEach { (threadKey, threadMessages, latestPair) ->
                                        val latest = latestPair.first
                                        val unread = latestPair.second
                                        val isExpanded = threadKey in expandedThreadKeys
                                        val isClinicAdmin = latest.threadType == ChatThreadType.CLINIC_ADMIN
                                        val isPatientClinic = latest.threadType == ChatThreadType.PATIENT_CLINIC
                                        val badgeText = when (latest.threadType) {
                                            ChatThreadType.PATIENT_CLINIC -> bi(isEn, "مريض ↔ عيادة", "Patient ↔ Clinic")
                                            ChatThreadType.CLINIC_ADMIN -> bi(isEn, "عيادة ↔ إدارة", "Clinic ↔ Admin")
                                            else -> bi(isEn, "مريض ↔ إدارة", "Patient ↔ Admin")
                                        }
                                        val threadTitle = when (latest.threadType) {
                                            ChatThreadType.CLINIC_ADMIN -> latest.clinicName.ifBlank { latest.doctorName }
                                            ChatThreadType.PATIENT_CLINIC -> "${latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") }} ⇄ ${latest.clinicName.ifBlank { latest.doctorName }}"
                                            else -> latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") }
                                        }
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.2.dp, if (unread > 0) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Surface(
                                                                color = TealPrimary.copy(alpha = 0.12f),
                                                                shape = RoundedCornerShape(8.dp),
                                                            ) {
                                                                Text(
                                                                    text = badgeText,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TealPrimary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                )
                                                            }
                                                            if (unread > 0) {
                                                                Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                                                                    Text(unread.toString())
                                                                }
                                                            }
                                                        }
                                                        Text(threadTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = listOfNotNull(
                                                                latest.patientPhone.takeIf { it.isNotBlank() },
                                                                "(${threadMessages.size} ${bi(isEn, "رسالة", "msgs")})",
                                                            ).joinToString(" • "),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                        Text(
                                                            text = "${latest.senderName}: ${latest.body}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 2,
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            expandedThreadKeys = if (isExpanded) {
                                                                expandedThreadKeys - threadKey
                                                            } else {
                                                                expandedThreadKeys + threadKey
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                            .testTag("admin_expand_thread_$threadKey"),
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            if (isExpanded) bi(isEn, "إخفاء الرسائل", "Hide Msgs")
                                                            else bi(isEn, "عرض الرسائل", "View Msgs"),
                                                            maxLines = 1,
                                                        )
                                                    }
                                                    if (!isPatientClinic) {
                                                        Button(
                                                            onClick = {
                                                                onOpenChatThread(
                                                                    if (isClinicAdmin) {
                                                                        DirectChatTarget(
                                                                            threadType = ChatThreadType.CLINIC_ADMIN,
                                                                            viewerRole = MessageSenderRole.ADMIN,
                                                                            title = latest.clinicName.ifBlank { latest.doctorName },
                                                                            subtitle = latest.doctorName,
                                                                            doctorId = latest.doctorId,
                                                                        )
                                                                    } else {
                                                                        DirectChatTarget(
                                                                            threadType = ChatThreadType.PATIENT_ADMIN,
                                                                            viewerRole = MessageSenderRole.ADMIN,
                                                                            title = latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") },
                                                                            subtitle = latest.patientPhone,
                                                                            patientAccountId = latest.patientAccountId,
                                                                            patientName = latest.patientName,
                                                                            patientPhone = latest.patientPhone,
                                                                        )
                                                                    }
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(48.dp),
                                                        ) {
                                                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(bi(isEn, "فتح والرد", "Reply"), maxLines = 1)
                                                        }
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            onDeleteThread(latest.threadType, latest.patientAccountId, latest.doctorId)
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                                                        border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                                                        modifier = Modifier
                                                            .height(48.dp)
                                                            .testTag("admin_delete_thread_$threadKey"),
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(bi(isEn, "مسح المحادثة", "Clear Chat"), maxLines = 1)
                                                    }
                                                }

                                                AnimatedVisibility(visible = isExpanded) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                                        threadMessages.forEach { msg ->
                                                            Surface(
                                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                modifier = Modifier.fillMaxWidth(),
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                ) {
                                                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                        Text(
                                                                            text = msg.senderName,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = TealPrimary,
                                                                        )
                                                                        Text(
                                                                            text = msg.body,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = { onDeleteMessage(msg.id) },
                                                                        modifier = Modifier
                                                                            .size(48.dp)
                                                                            .testTag("admin_delete_msg_${msg.id}"),
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Delete,
                                                                            contentDescription = bi(isEn, "حذف الرسالة", "Delete message"),
                                                                            tint = SlotBookedBorder,
                                                                            modifier = Modifier.size(18.dp),
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                8 -> {
                    item {
                        AdminAccordionCard(
                            title = bi(
                                isEn,
                                "👨‍👩‍👧‍👦 طلبات ربط الأسرة (الزوج / الزوجة) — (${state.familyLinkRequests.size})",
                                "👨‍👩‍👧‍👦 Family Link Requests (Spouses) — (${state.familyLinkRequests.size})",
                            ),
                            subtitle = bi(
                                isEn,
                                "بانتظار المراجعة: $pendingFamilyLinksCount — اضغط لفتح أو إغلاق",
                                "Pending review: $pendingFamilyLinksCount — Tap to expand/collapse",
                            ),
                            expanded = familyLinksAccordionExpanded,
                            onToggle = { familyLinksAccordionExpanded = !familyLinksAccordionExpanded },
                            testTagName = "admin_accordion_family_links",
                        ) {
                            if (state.familyLinkRequests.isEmpty()) {
                                Text(
                                    text = bi(isEn, "لا توجد طلبات ربط أسرة مرسلة حتى الآن.", "No family link requests submitted yet."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    state.familyLinkRequests.forEach { req ->
                                        val isPending = req.status == FamilyLinkStatus.PENDING
                                        val isApproved = req.status == FamilyLinkStatus.APPROVED
                                        Surface(
                                            color = when {
                                                isApproved -> SlotAvailableBg.copy(alpha = 0.55f)
                                                isPending -> MaterialTheme.colorScheme.surface
                                                else -> SlotBookedBg.copy(alpha = 0.55f)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isApproved -> SlotAvailableBorder
                                                    isPending -> TealPrimary
                                                    else -> SlotBookedBorder
                                                },
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_family_req_${req.id}"),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${req.requesterName} (${req.requesterPhone})",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                        Text(
                                                            text = bi(isEn, "يطلب الربط مع هاتف: ${req.targetPhone}", "Target spouse phone: ${req.targetPhone}"),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TealPrimary,
                                                        )
                                                    }
                                                    Surface(
                                                        color = if (isApproved) SlotAvailableBg else if (isPending) Color(0xFFFEF3C7) else SlotBookedBg,
                                                        shape = RoundedCornerShape(8.dp),
                                                    ) {
                                                        Text(
                                                            text = when (req.status) {
                                                                FamilyLinkStatus.APPROVED -> bi(isEn, "معتمد ✓", "Approved ✓")
                                                                FamilyLinkStatus.REJECTED -> bi(isEn, "مرفوض", "Rejected")
                                                                else -> bi(isEn, "بانتظار المراجعة", "Pending")
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isApproved) SlotAvailableText else if (isPending) Color(0xFFB45309) else SlotBookedText,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (req.proofMarriageContractUrl.isNotBlank()) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                onPreviewAttachment(
                                                                    AttachmentPreviewTarget(
                                                                        path = req.proofMarriageContractUrl,
                                                                        mimeType = "image/jpeg",
                                                                        displayName = "marriage_contract.jpg",
                                                                        title = bi(isEn, "صورة قسيمة الزواج — ${req.requesterName}", "Marriage Certificate — ${req.requesterName}"),
                                                                    )
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        ) {
                                                            Text(bi(isEn, "📄 قسيمة الزواج", "📄 Marriage Cert"), style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                    if (req.proofNationalIdFrontUrl.isNotBlank()) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                onPreviewAttachment(
                                                                    AttachmentPreviewTarget(
                                                                        path = req.proofNationalIdFrontUrl,
                                                                        mimeType = "image/jpeg",
                                                                        displayName = "id_front.jpg",
                                                                        title = bi(isEn, "البطاقة القومية (وجه) — ${req.requesterName}", "National ID Front — ${req.requesterName}"),
                                                                    )
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        ) {
                                                            Text(bi(isEn, "🪪 بطاقة (وجه)", "🪪 ID Front"), style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                    if (req.proofNationalIdBackUrl.isNotBlank()) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                onPreviewAttachment(
                                                                    AttachmentPreviewTarget(
                                                                        path = req.proofNationalIdBackUrl,
                                                                        mimeType = "image/jpeg",
                                                                        displayName = "id_back.jpg",
                                                                        title = bi(isEn, "البطاقة القومية (ظهر) — ${req.requesterName}", "National ID Back — ${req.requesterName}"),
                                                                    )
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        ) {
                                                            Text(bi(isEn, "🪪 بطاقة (ظهر)", "🪪 ID Back"), style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                                if (isPending) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = { onApproveFamilyLinkRequest(req.id) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(44.dp)
                                                                .testTag("approve_family_req_${req.id}"),
                                                        ) {
                                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(bi(isEn, "اعتماد الربط ✓", "Approve ✓"))
                                                        }
                                                        OutlinedButton(
                                                            onClick = { onRejectFamilyLinkRequest(req.id, "") },
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                                                            border = BorderStroke(1.dp, SlotBookedBorder),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(44.dp)
                                                                .testTag("reject_family_req_${req.id}"),
                                                        ) {
                                                            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(bi(isEn, "رفض ✕", "Reject ✕"))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddForm) {
        DoctorFormDialog(
            initialDoctor = null,
            defaultClinicType = defaultFormClinicType,
            hospitalName = bi(isEn, state.brandConfig.hospitalNameAr, state.brandConfig.hospitalNameEn),
            isEn = isEn,
            currencyLabel = currencyLabel,
            onSave = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl ->
                onAddDoctor(name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl)
                showAddForm = false
            },
            onDismiss = { showAddForm = false },
        )
    }

    val editingDoc = doctorToEdit
    if (editingDoc != null) {
        DoctorFormDialog(
            initialDoctor = editingDoc,
            defaultClinicType = editingDoc.clinicType,
            hospitalName = bi(isEn, state.brandConfig.hospitalNameAr, state.brandConfig.hospitalNameEn),
            isEn = isEn,
            currencyLabel = currencyLabel,
            onSave = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl ->
                onUpdateDoctor(editingDoc.id, name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass, extraSpecs, nameEn, clinicEn, locEn, driveUrl, secondaryDriveUrl)
                doctorToEdit = null
            },
            onDismiss = { doctorToEdit = null },
        )
    }

    val deletingDoc = doctorToDelete
    if (deletingDoc != null) {
        AlertDialog(
            onDismissRequest = { doctorToDelete = null },
            title = { Text(bi(isEn, "تأكيد الحذف النهائي", "Confirm Permanent Deletion")) },
            text = {
                Text(
                    bi(
                        isEn,
                        "هل أنت متأكد من حذف ${deletingDoc.doctorName} (${deletingDoc.clinicName}) نهائياً من المنصة؟",
                        "Delete ${deletingDoc.doctorName} (${deletingDoc.clinicName}) permanently?",
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDoctor(deletingDoc.id)
                        doctorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                ) {
                    Text(bi(isEn, "حذف نهائي", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { doctorToDelete = null }) {
                    Text(bi(isEn, "تراجع", "Cancel"))
                }
            },
        )
    }

    if (showAddLabForm) {
        LabFormDialog(
            initialLab = null,
            isEn = isEn,
            onSave = { name, phone, loc, user, pass ->
                onAddLab(name, phone, loc, user, pass)
                showAddLabForm = false
            },
            onDismiss = { showAddLabForm = false },
        )
    }

    val editingLab = labToEdit
    if (editingLab != null) {
        LabFormDialog(
            initialLab = editingLab,
            isEn = isEn,
            onSave = { name, phone, loc, user, pass ->
                onUpdateLab(editingLab.id, name, phone, loc, user, pass)
                labToEdit = null
            },
            onDismiss = { labToEdit = null },
        )
    }

    val deletingLab = labToDelete
    if (deletingLab != null) {
        AlertDialog(
            onDismissRequest = { labToDelete = null },
            title = { Text(bi(isEn, "تأكيد حذف المعمل", "Confirm Lab Deletion")) },
            text = {
                Text(
                    bi(
                        isEn,
                        "هل أنت متأكد من حذف ${deletingLab.labName} من بوابة معامل التحاليل؟",
                        "Remove ${deletingLab.labName} from the medical lab portal?",
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLab(deletingLab.id)
                        labToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                ) {
                    Text(bi(isEn, "حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { labToDelete = null }) {
                    Text(bi(isEn, "تراجع", "Cancel"))
                }
            },
        )
    }

    val editingUser = userToEdit
    if (editingUser != null) {
        UserEditDialog(
            account = editingUser,
            isEn = isEn,
            onSave = { newName, newPhone, newPass ->
                onUpdateUser(editingUser.id, newName, newPhone, newPass)
                userToEdit = null
            },
            onDismiss = { userToEdit = null },
        )
    }

    val deletingUser = userToDelete
    if (deletingUser != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(bi(isEn, "تأكيد حذف حساب العميل", "Confirm Account Deletion")) },
            text = {
                Text(
                    bi(
                        isEn,
                        "هل أنت متأكد من حذف حساب العميل (${deletingUser.fullName} - ${deletingUser.phone}) وجميع أفراد الأسرة المسجلين به نهائياً؟",
                        "Permanently delete ${deletingUser.fullName} (${deletingUser.phone}) and registered family members?",
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(deletingUser.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                ) {
                    Text(bi(isEn, "حذف الحساب", "Delete Account"))
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(bi(isEn, "إلغاء", "Cancel"))
                }
            },
        )
    }
}

@Composable
private fun AdminUserManageCard(
    account: PatientAccountEntity,
    familyMembers: List<FamilyMemberEntity>,
    isEn: Boolean = false,
    onToggleVerified: () -> Unit,
    onCopyOtp: () -> Unit,
    onRegenerateOtp: () -> Unit,
    onOpenChat: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val verified = account.isPhoneVerified
    val sourceLabel = if (verified) account.verifiedBy.ifBlank { "كود OTP" } else bi(isEn, "بانتظار التفعيل", "Pending")
    val nowMillis = System.currentTimeMillis()
    val isOtpExpired = !verified && (
        account.otpCode.length != 6 ||
            account.otpExpiresAtMillis <= 0L ||
            nowMillis > account.otpExpiresAtMillis
        )
    val remMins = if (!verified && !isOtpExpired) {
        (((account.otpExpiresAtMillis - nowMillis) + 59_999L) / 60_000L).coerceAtLeast(1L)
    } else 0L
    val remHours = remMins / 60L
    val remM = remMins % 60L

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (verified) SlotAvailableBorder.copy(alpha = 0.55f) else Color(0xFFEF4444)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${account.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(bi(isEn, "📞 الهاتف: ${account.phone}", "📞 Phone: ${account.phone}"), style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    color = if (verified) SlotAvailableBg else Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = if (verified) null else BorderStroke(1.dp, Color(0xFFEF4444)),
                ) {
                    Text(
                        text = if (verified) bi(isEn, "✅ موثق", "✅ Verified") else bi(isEn, "⚠️ غير موثق", "⚠️ Unverified"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (verified) SlotAvailableText else Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (!verified) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bi(isEn, "كود OTP العشوائي (6 أرقام - صالح 24 ساعة):", "Random 6-Digit OTP (Valid 24h):"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                            )
                            Text(
                                text = account.otpCode.ifBlank { "------" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp,
                                color = TealPrimaryDark,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isOtpExpired) {
                                    bi(isEn, "⏰ منتهي الصلاحية", "⏰ Expired")
                                } else if (remHours > 0) {
                                    bi(isEn, "⏳ صالح: ${remHours}س ${remM}د", "⏳ Valid: ${remHours}h ${remM}m")
                                } else {
                                    bi(isEn, "⏳ صالح: ${remM} دقيقة", "⏳ Valid: ${remM}m")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOtpExpired) SlotBookedText else SlotAvailableText,
                            )
                            TextButton(
                                onClick = onRegenerateOtp,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("admin_user_regen_otp_${account.id}"),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(bi(isEn, "توليد كود 24س", "Regen 24h"), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "جهة التفعيل: $sourceLabel",
                            "Verification Source: $sourceLabel",
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (verified) TealPrimary else SlotBookedText,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (familyMembers.isEmpty()) {
                            bi(isEn, "أفراد الأسرة: صاحب الحساب فقط", "Family: Account holder only")
                        } else {
                            bi(
                                isEn,
                                "أفراد الأسرة (${familyMembers.size}): ${familyMembers.joinToString(" • ") { "${it.fullName} (${it.relation})" }}",
                                "Family (${familyMembers.size}): ${familyMembers.joinToString(" • ") { "${it.fullName} (${it.relation})" }}",
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!verified) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1) زر نسخ الكود ورسالة الواتساب + توليد كود جديد (24 ساعة)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onCopyOtp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D4ED8),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_user_copy_otp_${account.id}"),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = bi(isEn, "📋 نسخ الكود ورسالة الواتساب", "📋 Copy OTP & WhatsApp"),
                                maxLines = 1,
                            )
                        }
                        Button(
                            onClick = onRegenerateOtp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_user_show_regen_otp_${account.id}"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = bi(isEn, "🔄 توليد كود جديد (24 ساعة)", "🔄 New Code (24h)"),
                                maxLines = 1,
                            )
                        }
                    }

                    // 2) زر توثيق الحساب فوراً (بدون كود) + 3) زر مراسلة / إرسال تحذير
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onToggleVerified,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlotAvailableBorder,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("admin_user_verify_toggle_${account.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = bi(isEn, "✅ توثيق أوتوماتيك فوراً (بدون كود)", "✅ Verify Now (No Code)"),
                                maxLines = 1,
                            )
                        }
                        Button(
                            onClick = onOpenChat,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_user_chat_warn_${account.id}"),
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = bi(isEn, "💬 مراسلة / ⚠️ تحذير إداري", "💬 Chat / ⚠️ Warning"),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (verified) {
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("admin_user_chat_warn_${account.id}"),
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "💬 مراسلة / تحذير", "💬 Chat / Warn"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onToggleVerified,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                        border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("admin_user_verify_toggle_${account.id}"),
                    ) {
                        Text(
                            text = bi(isEn, "إلغاء التفعيل", "Unverify"),
                            maxLines = 1,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("admin_user_edit_${account.id}"),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "تعديل", "Edit"), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                    border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_user_delete_${account.id}"),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = bi(isEn, "حذف", "Delete"), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserEditDialog(
    account: PatientAccountEntity,
    isEn: Boolean = false,
    onSave: (fullName: String, phone: String, newPassword: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var fullName by remember { mutableStateOf(account.fullName) }
    var phone by remember { mutableStateOf(account.phone) }
    var newPassword by remember { mutableStateOf("") }
    val digitsOnly = { raw: String ->
        buildString(raw.length) {
            for (ch in raw) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - 0x0660)).toChar())
                    in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - 0x06F0)).toChar())
                }
            }
        }
    }
    val pinValid = newPassword.isEmpty() || newPassword.length >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bi(isEn, "تعديل بيانات العميل", "Edit Customer Details")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(bi(isEn, "الاسم الكامل *", "Full Name *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_edit_user_name"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = digitsOnly(it) },
                    label = { Text(bi(isEn, "رقم الهاتف *", "Phone Number *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_edit_user_phone"),
                )
                PasswordOutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = digitsOnly(it) },
                    label = bi(isEn, "رقم سري جديد (أرقام فقط - اختياري)", "New Numeric PIN (optional)"),
                    keyboardType = KeyboardType.NumberPassword,
                    toggleTestTag = "admin_edit_user_pass_toggle",
                    modifier = Modifier.fillMaxWidth().testTag("admin_edit_user_pass"),
                )
                if (!pinValid) {
                    Text(
                        text = bi(isEn, "الرقم السري يجب أن يكون 4 أرقام على الأقل", "PIN must be at least 4 digits"),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotBookedText,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName, phone, newPassword.takeIf { it.isNotBlank() }) },
                enabled = pinValid,
                modifier = Modifier.testTag("admin_edit_user_save"),
            ) {
                Text(bi(isEn, "حفظ التعديلات", "Save Changes"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إلغاء", "Cancel"))
            }
        },
    )
}

@Composable
private fun AdminAccordionCard(
    title: String,
    subtitle: String = "",
    expanded: Boolean,
    onToggle: () -> Unit,
    testTagName: String = "",
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().let { if (testTagName.isNotBlank()) it.testTag(testTagName) else it },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TealPrimary,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun VisitorStatTile(
    title: String,
    total: Int,
    apk: Int,
    web: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TealPrimary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TealPrimary,
            )
            Text(
                text = "(APK: $apk | Web: $web)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun AdminMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    testTagName: String = "",
    onClick: () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.45f)),
        modifier = modifier
            .clickable(onClick = onClick)
            .let { if (testTagName.isNotBlank()) it.testTag(testTagName) else it },
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TealPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AdminDoctorManageCard(
    doctor: ClinicDoctorEntity,
    isEn: Boolean = false,
    currencyLabel: String = if (isEn) "EGP" else "ج.م",
    onChatClinic: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_manage_doc_${doctor.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val docDisplayName = if (isEn && doctor.doctorNameEn.isNotBlank()) doctor.doctorNameEn else doctor.doctorName
                    val clinicDisplayName = if (isEn && doctor.clinicNameEn.isNotBlank()) doctor.clinicNameEn else doctor.clinicName
                    val allSpecsText = SpecialtyCatalog.formatDoctorSpecialties(
                        doctor.specialty,
                        doctor.additionalSpecialties,
                        isEn,
                    )
                    Text(docDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$allSpecsText — $clinicDisplayName", style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                }
                Surface(color = SlotAvailableBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (doctor.clinicType == ClinicType.HOSPITAL) bi(isEn, "مستشفى", "Hospital") else bi(isEn, "عيادة خاصة", "Private"),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlotAvailableText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (doctor.bio.isNotBlank()) {
                Text(doctor.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = bi(
                    isEn,
                    "📍 ${doctor.locationLabel} • الكشف: ${doctor.feeEgp} $currencyLabel • 🕒 ${TimeUtils.formatMinutesAr(doctor.workStartMinutes, false)} - ${TimeUtils.formatMinutesAr(doctor.workEndMinutes, false)}",
                    "📍 ${doctor.locationLabelEn.ifBlank { doctor.locationLabel }} • Fee: ${doctor.feeEgp} $currencyLabel • 🕒 ${TimeUtils.formatMinutesAr(doctor.workStartMinutes, true)} - ${TimeUtils.formatMinutesAr(doctor.workEndMinutes, true)}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(bi(isEn, "المستخدم: ${doctor.username}", "User: ${doctor.username}"), style = MaterialTheme.typography.labelMedium)
                    Text(bi(isEn, "كلمة المرور: ${doctor.password}", "Pass: ${doctor.password}"), style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onChatClinic,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .testTag("admin_doc_chat_${doctor.id}"),
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "💬 مراسلة العيادة", "Chat Clinic"), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.9f).height(46.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "تعديل", "Edit"), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                    border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.height(46.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = bi(isEn, "حذف", "Delete"), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DoctorFormDialog(
    initialDoctor: ClinicDoctorEntity?,
    defaultClinicType: String,
    hospitalName: String,
    isEn: Boolean = false,
    currencyLabel: String = if (isEn) "EGP" else "ج.م",
    onSave: (
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
        additionalSpecialties: String,
        doctorNameEn: String,
        clinicNameEn: String,
        locationLabelEn: String,
        driveWebAppUrl: String,
        secondaryDriveWebAppUrl: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = initialDoctor != null
    var doctorName by remember { mutableStateOf(initialDoctor?.doctorName ?: "") }
    var doctorNameEn by remember { mutableStateOf(initialDoctor?.doctorNameEn ?: "") }
    val initialPrimary = SpecialtyCatalog.normalizeSpecialty(initialDoctor?.specialty ?: "").ifBlank {
        SpecialtyCatalog.all.first().nameAr
    }
    var specialty by remember { mutableStateOf(initialPrimary) }
    var primaryDropdownExpanded by remember { mutableStateOf(false) }
    val selectedAdditional = remember {
        mutableStateListOf<String>().apply {
            addAll(SpecialtyCatalog.parseAdditionalSpecialties(initialDoctor?.additionalSpecialties.orEmpty()))
        }
    }
    var clinicName by remember { mutableStateOf(initialDoctor?.clinicName ?: "") }
    var clinicNameEn by remember { mutableStateOf(initialDoctor?.clinicNameEn ?: "") }
    var clinicType by remember { mutableStateOf(initialDoctor?.clinicType ?: defaultClinicType) }
    var bio by remember { mutableStateOf(initialDoctor?.bio ?: "") }
    var locationLabel by remember { mutableStateOf(initialDoctor?.locationLabel ?: "قرية الحجايزة") }
    var locationLabelEn by remember { mutableStateOf(initialDoctor?.locationLabelEn ?: "Al-Hajayzah Village") }
    var feeText by remember { mutableStateOf((initialDoctor?.feeEgp ?: 150).toString()) }
    var mapsUrl by remember { mutableStateOf(initialDoctor?.mapsUrl ?: "") }
    var driveWebAppUrl by remember { mutableStateOf(initialDoctor?.driveWebAppUrl ?: "") }
    var secondaryDriveWebAppUrl by remember { mutableStateOf(initialDoctor?.secondaryDriveWebAppUrl ?: "") }
    var startHour by remember { mutableIntStateOf((initialDoctor?.workStartMinutes ?: (17 * 60)) / 60) }
    var endHour by remember { mutableIntStateOf((initialDoctor?.workEndMinutes ?: (21 * 60)) / 60) }
    var slotDuration by remember { mutableIntStateOf(initialDoctor?.slotDurationMinutes ?: 20) }
    var username by remember { mutableStateOf(initialDoctor?.username ?: "") }
    var password by remember { mutableStateOf(initialDoctor?.password ?: "1234") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) bi(isEn, "تعديل البيانات", "Edit Clinic")
                else bi(isEn, "إضافة دكتور أو قسم جديد", "Add Doctor / Department")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text(bi(isEn, "اسم الدكتور (بالعربية) *", "Doctor Name (Arabic) *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = doctorNameEn,
                    onValueChange = { doctorNameEn = it },
                    label = { Text(bi(isEn, "اسم الدكتور بالإنجليزية (اختياري)", "Doctor Name (English, optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Primary Specialty Dropdown (Unified 18 Specialties Catalog)
                ExposedDropdownMenuBox(
                    expanded = primaryDropdownExpanded,
                    onExpandedChange = { primaryDropdownExpanded = !primaryDropdownExpanded },
                ) {
                    OutlinedTextField(
                        value = SpecialtyCatalog.displaySpecialty(specialty, isEn),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(bi(isEn, "التخصص الرئيسي *", "Primary Specialty *")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("admin_primary_specialty_dropdown"),
                    )
                    ExposedDropdownMenu(
                        expanded = primaryDropdownExpanded,
                        onDismissRequest = { primaryDropdownExpanded = false },
                    ) {
                        SpecialtyCatalog.all.forEach { specItem ->
                            DropdownMenuItem(
                                text = { Text(specItem.label(isEn)) },
                                onClick = {
                                    specialty = specItem.nameAr
                                    selectedAdditional.removeAll { it == specItem.nameAr }
                                    primaryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Text(
                    text = bi(
                        isEn,
                        "تخصصات إضافية للطبيب (اختيار متعدد - اختياري):",
                        "Additional Specialties (Multi-select, optional):",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = TealPrimary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SpecialtyCatalog.all.filter { it.nameAr != specialty }.forEach { specItem ->
                        val isSelected = specItem.nameAr in selectedAdditional
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedAdditional.remove(specItem.nameAr)
                                else selectedAdditional.add(specItem.nameAr)
                            },
                            label = {
                                Text(
                                    text = specItem.label(isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = clinicType == ClinicType.PRIVATE,
                        onClick = { clinicType = ClinicType.PRIVATE },
                        label = { Text(bi(isEn, "عيادة خاصة", "Private Clinic")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = clinicType == ClinicType.HOSPITAL,
                        onClick = { clinicType = ClinicType.HOSPITAL },
                        label = { Text(bi(isEn, "المستشفى التخصصي", "Hospital Dept")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = {
                        Text(
                            if (clinicType == ClinicType.HOSPITAL) bi(isEn, "اسم القسم بالمستشفى", "Department Name")
                            else bi(isEn, "اسم العيادة (عربي)", "Clinic Name (Arabic)")
                        )
                    },
                    placeholder = {
                        Text(
                            if (clinicType == ClinicType.HOSPITAL) "$hospitalName - ${bi(isEn, "قسم ...", "Dept ...")}"
                            else ""
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = clinicNameEn,
                    onValueChange = { clinicNameEn = it },
                    label = { Text(bi(isEn, "اسم العيادة بالإنجليزية (اختياري)", "Clinic Name (English, optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(bi(isEn, "نبذة عن الدكتور", "Doctor Bio")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = locationLabel,
                    onValueChange = { locationLabel = it },
                    label = { Text(bi(isEn, "عنوان العيادة بالتفصيل", "Clinic Address")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(bi(isEn, "سعر الكشف ($currencyLabel)", "Consultation Fee ($currencyLabel)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mapsUrl,
                    onValueChange = { mapsUrl = it },
                    label = { Text(bi(isEn, "رابط موقع العيادة على خرائط جوجل (Google Maps)", "Google Maps Link (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = driveWebAppUrl,
                    onValueChange = { driveWebAppUrl = it },
                    label = {
                        Text(
                            bi(
                                isEn,
                                "رابط تخزين Google Drive الأساسي للعيادة (Apps Script URL 1)",
                                "Clinic Primary Google Drive URL (optional)",
                            ),
                        )
                    },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_doc_drive_url_input"),
                )
                OutlinedTextField(
                    value = secondaryDriveWebAppUrl,
                    onValueChange = { secondaryDriveWebAppUrl = it },
                    label = {
                        Text(
                            bi(
                                isEn,
                                "رابط تخزين Google Drive الثانوي للعيادة (Apps Script URL 2 - اختياري)",
                                "Clinic Secondary Google Drive URL (optional)",
                            ),
                        )
                    },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_doc_secondary_drive_url_input"),
                )

                Text(
                    text = bi(isEn, "بداية العمل: ${TimeUtils.formatMinutesAr(startHour * 60, false)}", "Start: ${TimeUtils.formatMinutesAr(startHour * 60, true)}"),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(10, 12, 14, 15, 16, 17, 18)) { hr ->
                        FilterChip(
                            selected = startHour == hr,
                            onClick = { startHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60, isEn)) },
                        )
                    }
                }
                Text(
                    text = bi(isEn, "نهاية العمل: ${TimeUtils.formatMinutesAr(endHour * 60, false)}", "End: ${TimeUtils.formatMinutesAr(endHour * 60, true)}"),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(18, 19, 20, 21, 22, 23)) { hr ->
                        FilterChip(
                            selected = endHour == hr,
                            onClick = { endHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60, isEn)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30).forEach { dur ->
                        FilterChip(
                            selected = slotDuration == dur,
                            onClick = { slotDuration = dur },
                            label = { Text(bi(isEn, "$dur د", "${dur}m")) },
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = bi(isEn, "حساب تسجيل دخول الدكتور للوحة العيادة:", "Doctor Portal Login Credentials:"),
                    style = MaterialTheme.typography.titleSmall,
                    color = TealPrimary,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(bi(isEn, "اسم المستخدم أو رقم الهاتف للدكتور", "Username or Phone")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = bi(isEn, "كلمة المرور للدكتور", "Doctor Password"),
                    toggleTestTag = "admin_doc_password_toggle",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_doc_password_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        doctorName,
                        specialty,
                        clinicName,
                        clinicType,
                        locationLabel,
                        bio,
                        mapsUrl,
                        feeText.toIntOrNull() ?: 150,
                        startHour * 60,
                        endHour * 60,
                        slotDuration,
                        username,
                        password,
                        SpecialtyCatalog.serializeAdditionalSpecialties(selectedAdditional),
                        doctorNameEn,
                        clinicNameEn,
                        locationLabelEn,
                        driveWebAppUrl,
                        secondaryDriveWebAppUrl,
                    )
                }
            ) {
                Text(if (isEdit) bi(isEn, "حفظ التعديلات", "Save Changes") else bi(isEn, "إضافة ونشر فوراً", "Add & Publish"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إلغاء", "Cancel"))
            }
        },
    )
}

@Composable
private fun AdminLabManageCard(
    lab: MedicalLabEntity,
    isEn: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF4338CA).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_manage_lab_${lab.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(lab.labName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "📍 ${lab.locationLabel.ifBlank { bi(isEn, "قرية الحجايزة", "Al-Hajayzah") }}${if (lab.phone.isNotBlank()) " • ${lab.phone}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(color = Color(0xFFE0E7FF), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = bi(isEn, "معمل تحاليل", "Medical Lab"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3730A3),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(bi(isEn, "المستخدم: ${lab.username}", "User: ${lab.username}"), style = MaterialTheme.typography.labelMedium)
                    Text(bi(isEn, "كلمة المرور: ${lab.password}", "Pass: ${lab.password}"), style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "تعديل", "Edit"))
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                    border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.height(46.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "حذف", "Delete"))
                }
            }
        }
    }
}

@Composable
private fun LabFormDialog(
    initialLab: MedicalLabEntity?,
    isEn: Boolean = false,
    onSave: (labName: String, phone: String, locationLabel: String, username: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = initialLab != null
    var labName by remember { mutableStateOf(initialLab?.labName ?: "") }
    var phone by remember { mutableStateOf(initialLab?.phone ?: "") }
    var locationLabel by remember { mutableStateOf(initialLab?.locationLabel ?: "") }
    var username by remember { mutableStateOf(initialLab?.username ?: "") }
    var password by remember { mutableStateOf(initialLab?.password ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) bi(isEn, "تعديل بيانات المعمل", "Edit Medical Lab") else bi(isEn, "إضافة معمل تحاليل جديد", "Add New Medical Lab")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = labName,
                    onValueChange = { labName = it },
                    label = { Text(bi(isEn, "اسم معمل التحاليل *", "Lab Name *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lab_form_name_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(bi(isEn, "رقم هاتف المعمل", "Lab Phone")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = locationLabel,
                    onValueChange = { locationLabel = it },
                    label = { Text(bi(isEn, "العنوان بالتفصيل", "Address")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(bi(isEn, "اسم المستخدم للدخول *", "Login Username *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lab_form_username_input"),
                )
                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = bi(isEn, "كلمة المرور للدخول *", "Login Password *"),
                    toggleTestTag = "lab_form_password_toggle",
                    modifier = Modifier.fillMaxWidth().testTag("lab_form_password_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(labName, phone, locationLabel, username, password) },
                modifier = Modifier.testTag("lab_form_save_button"),
            ) {
                Text(if (isEdit) bi(isEn, "حفظ التعديلات", "Save Changes") else bi(isEn, "إضافة المعمل", "Add Lab"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إلغاء", "Cancel"))
            }
        },
    )
}

private fun computeDriveUsedGb(records: List<MedicalRecordEntity>): Double {
    if (records.isEmpty()) return 0.0
    var totalBytes = 0L
    records.forEach { rec ->
        val localLen = if (rec.attachmentPath.isNotBlank() && !rec.attachmentPath.startsWith("http", true)) {
            runCatching { java.io.File(rec.attachmentPath).length() }.getOrDefault(0L)
        } else 0L
        totalBytes += if (localLen > 0L) {
            localLen
        } else if (rec.attachmentMimeType.contains("pdf", true)) {
            3_200_000L // متوسط حجم تقرير PDF بالمنصة عند المزامنة السحابية
        } else {
            2_400_000L // متوسط حجم صورة الأشعة/التحليل
        }
    }
    return totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
}

private fun computeDrivePercent(usedGb: Double, quotaGb: Double, fileCount: Int): Int {
    if (quotaGb <= 0.0) return 1
    if (fileCount <= 0 && usedGb <= 0.0) return 1
    val pct = ((usedGb / quotaGb) * 100.0).toInt()
    return pct.coerceIn(1, 100)
}

@Composable
private fun DriveAccountUsageMeterCard(
    accountTitle: String,
    usedGb: Double,
    quotaGb: Double,
    usedPercent: Int,
    heavyFilesCount: Int,
    isLinked: Boolean,
    isEn: Boolean,
    testTagPrefix: String,
    onCleanHeavyFiles: () -> Unit,
) {
    val barColor = when {
        usedPercent >= 90 -> Color(0xFFDC2626)
        usedPercent >= 70 -> Color(0xFFD97706)
        else -> TealPrimary
    }
    val formattedUsedGb = String.format(java.util.Locale.US, "%.2f", usedGb)
    val formattedQuotaGb = String.format(java.util.Locale.US, "%.0f", quotaGb)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (usedPercent >= 90) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = accountTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (isLinked) SlotAvailableBg else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = if (isLinked) bi(isEn, "✓ مرتبط", "✓ Linked") else bi(isEn, "احتياطي", "Fallback"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLinked) SlotAvailableText else Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (usedPercent / 100f).coerceIn(0.01f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("${testTagPrefix}_progress"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$usedPercent% - $formattedUsedGb GB / $formattedQuotaGb GB",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                )
                Text(
                    text = bi(isEn, "$heavyFilesCount ملف مرفق", "$heavyFilesCount files"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (usedPercent >= 90) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "⚠️ تنبيه امتلاء التخزين: الحساب وصل إلى $usedPercent%! يُنصح بتفريغ الملفات الثقيلة الآن.",
                            "⚠️ Storage Alert: Account reached $usedPercent%! Clean heavy files now.",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            OutlinedButton(
                onClick = onCleanHeavyFiles,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("${testTagPrefix}_clean_btn"),
            ) {
                Text(
                    text = bi(isEn, "🧹 تفريغ وتنظيف الملفات الثقيلة الآن", "🧹 Clean Heavy Files Now"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

