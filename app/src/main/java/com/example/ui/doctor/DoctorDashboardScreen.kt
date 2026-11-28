package com.example.ui.doctor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BookingSource
import com.example.data.local.ChatThreadType
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.MessageSenderRole
import com.example.data.local.NotificationAudience
import com.example.data.local.PermanentMedicalLogEntity
import com.example.data.local.SlotStatus
import kotlinx.coroutines.launch
import com.example.ui.TebUiState
import com.example.ui.bi
import com.example.ui.components.AiMedicalSummaryCard
import com.example.ui.components.AttachmentPreviewTarget
import com.example.ui.patient.RescheduleSlotPickerDialog
import com.example.ui.components.DirectChatTarget
import com.example.ui.components.DoctorPatientMedicalAlertBanner
import com.example.ui.components.NotificationStatusCard
import com.example.ui.components.PasswordOutlinedTextField
import com.example.ui.components.PermanentMedicalLogSection
import com.example.ui.patient.MedicalRecordTimelineCard
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.SlotCompletedBg
import com.example.ui.theme.SlotCompletedBorder
import com.example.ui.theme.SlotCompletedText
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.util.SpecialtyCatalog
import com.example.util.TimeUtils

@Composable
fun DoctorLoginGateScreen(
    adminOwnerName: String,
    isEn: Boolean = false,
    onLoginDoctor: (identifier: String, password: String) -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MedicalServices,
                            contentDescription = bi(isEn, "تسجيل دخول الأطباء", "Doctor Login"),
                            tint = TealPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column {
                        Text(
                            text = bi(isEn, "تسجيل دخول الأطباء والعيادات", "Doctors & Clinics Portal"),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "أدخل اسم المستخدم أو رقم الهاتف وكلمة المرور الخاصة بعيادتك",
                                "Enter your clinic username or phone number and password",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(bi(isEn, "اسم المستخدم أو رقم الهاتف", "Username or Phone")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doctor_login_identifier"),
                )

                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = bi(isEn, "كلمة المرور", "Password"),
                    toggleTestTag = "doctor_login_password_toggle",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doctor_login_password"),
                )

                Button(
                    onClick = { onLoginDoctor(identifier, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("doctor_login_submit"),
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bi(isEn, "دخول إلى لوحة العيادة", "Enter Clinic Dashboard"))
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "تُنشأ حسابات الأطباء والعيادات وتُدار مباشرة من لوحة الإدارة العليا ($adminOwnerName).",
                            "Doctor & clinic accounts are created and managed via Super Admin ($adminOwnerName).",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorDashboardScreen(
    state: TebUiState,
    onLogoutDoctor: () -> Unit,
    onBookManualPhoneSlot: (slotId: String, name: String, phone: String) -> Unit,
    onCancelSlot: (slotId: String) -> Unit,
    onRescheduleSlot: (fromSlotId: String, toSlotId: String) -> Unit = { _, _ -> },
    onUpdateSlotStatus: (slotId: String, newStatus: String) -> Unit = { _, _ -> },
    onDelayDoctor: (doctorId: String, delayMinutes: Int) -> Unit,
    onGenerateSchedule: (doctorId: String, startMin: Int, endMin: Int, durationMin: Int) -> Unit,
    onCompleteVisit: (
        slotId: String,
        diagnosis: String,
        medications: String,
        notes: String,
        attachmentPath: String,
        diagnosisEn: String,
        medicationsEn: String,
        notesEn: String,
        aiReportAr: String,
        aiReportEn: String,
    ) -> Unit,
    onToggleFavoriteMed: (doctorId: String, medicationId: String) -> Unit,
    onSearchPatientsForActivation: (phoneQuery: String) -> Unit,
    onActivatePatientByClinic: (accountId: String) -> Unit,
    onActivatePatientByClinicPhone: (phoneQuery: String) -> Unit = {},
    onSubmitFamilyLinkRequest: (
        requesterAccountId: String,
        targetPhone: String,
        marriageUrl: String,
        idFrontUrl: String,
        idBackUrl: String,
    ) -> Unit = { _, _, _, _, _ -> },
    onRefreshAiSummary: (memberId: String) -> Unit = {},
    onOpenChatThread: (DirectChatTarget) -> Unit = {},
    onSendPatientAlert: (patientAccountId: String, patientName: String, message: String, onSent: () -> Unit) -> Unit = { _, _, _, _ -> },
    onPreviewAttachment: (AttachmentPreviewTarget) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEn = state.isEn
    val activeDoctor = state.loggedInDoctor ?: return
    val slots = remember(state.allSlots, activeDoctor.id) {
        state.slotsForDoctor(activeDoctor.id)
    }

    val availableCount = slots.count { it.status == SlotStatus.AVAILABLE }
    val bookedCount = slots.count { it.status == SlotStatus.BOOKED }
    val completedCount = slots.count { it.status == SlotStatus.COMPLETED }
    val noShowCount = slots.count { it.status == SlotStatus.NO_SHOW }
    val unreadClinicMsgs = state.unreadMessagesCountForDoctor(activeDoctor.id)

    var selectedTab by remember { mutableIntStateOf(0) }
    var activationPhoneQuery by remember { mutableStateOf("") }
    var alertTargetAccountId by remember { mutableStateOf("") }
    var alertTargetName by remember { mutableStateOf("") }
    var alertDraftMessage by remember { mutableStateOf("") }
    var slotForManualBooking by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotForReschedule by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotForPrescription by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotForEmrView by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var lookupEmrTarget by remember { mutableStateOf<Pair<com.example.data.local.PatientAccountEntity, List<FamilyMemberEntity>>?>(null) }
    var lookupEmrSelectedMemberId by remember { mutableStateOf<String?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var linkRequestAccount by remember { mutableStateOf<com.example.data.local.PatientAccountEntity?>(null) }
    var quickActivationPhone by remember { mutableStateOf("") }
    val unreadAdminRepliesCount = remember(state.directMessages, activeDoctor.id) {
        state.directMessages.count {
            it.doctorId == activeDoctor.id &&
                it.threadType == ChatThreadType.CLINIC_ADMIN &&
                !it.isReadByClinic
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val docTitleName = if (isEn && activeDoctor.doctorNameEn.isNotBlank()) activeDoctor.doctorNameEn else activeDoctor.doctorName
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = bi(isEn, "لوحة تحكم عيادة $docTitleName", "$docTitleName Clinic"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "إدارة جدول المواعيد، الحجوزات الهاتفية، تنبيهات التأخير، والروشتة الإلكترونية.",
                                "Manage schedules, phone bookings, delay alerts, and smart prescriptions.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = onLogoutDoctor,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                        border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("doctor_logout_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "خروج", "Sign Out"))
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onOpenChatThread(
                            DirectChatTarget(
                                threadType = ChatThreadType.CLINIC_ADMIN,
                                viewerRole = MessageSenderRole.CLINIC,
                                title = bi(isEn, "الإدارة العليا للمنصة", "Super Admin Support"),
                                subtitle = activeDoctor.clinicName,
                                doctorId = activeDoctor.id,
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("doctor_contact_admin_btn"),
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bi(isEn, "📩 مراسلة وتواصل مع الإدارة العامة", "📩 Contact Super Admin"))
                    if (unreadAdminRepliesCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                            Text(unreadAdminRepliesCount.toString())
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SlotAvailableBg),
                    border = BorderStroke(1.5.dp, SlotAvailableBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doctor_top_quick_activate_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = SlotAvailableText,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = bi(isEn, "✅ تفعيل وتوثيق حساب مريض جديد", "✅ Instant Patient Activation"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlotAvailableText,
                            )
                        }
                        Text(
                            text = bi(
                                isEn,
                                "أدخل رقم هاتف المريض لتفعيله فوراً واعتماده باسم عيادتك دون الحاجة لكود OTP:",
                                "Enter patient phone to instantly activate under your clinic name without OTP:",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = quickActivationPhone,
                                onValueChange = { quickActivationPhone = it },
                                placeholder = { Text("01xxxxxxxxx") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("doctor_top_quick_phone_input"),
                            )
                            Button(
                                onClick = {
                                    if (quickActivationPhone.isNotBlank()) {
                                        val p = quickActivationPhone
                                        onActivatePatientByClinicPhone(p)
                                        quickActivationPhone = ""
                                    }
                                },
                                enabled = quickActivationPhone.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("doctor_top_quick_activate_btn"),
                            ) {
                                Text(bi(isEn, "توثيق الحساب فوراً ✓", "Verify Now ✓"))
                            }
                        }
                    }
                }
            }

            item {
                NotificationStatusCard(
                    isEn = isEn,
                    tagPrefix = "doctor_notif",
                    onShowMessage = onShowMessage,
                )
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 8.dp,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(bi(isEn, "جدول المواعيد", "Schedule"), maxLines = 1) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(bi(isEn, "تفعيل / ملف مريض", "Activate / EMR"), maxLines = 1) },
                        modifier = Modifier.testTag("doctor_activate_patient_tab"),
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(bi(isEn, "💬 رسائل المرضى", "💬 Messages"), maxLines = 1)
                                if (unreadClinicMsgs > 0) {
                                    Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                                        Text(unreadClinicMsgs.toString())
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("doctor_messages_tab"),
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(bi(isEn, "📊 الإحصائيات", "📊 Analytics"), maxLines = 1) },
                        modifier = Modifier.testTag("doctor_analytics_tab"),
                    )
                }
            }

            if (selectedTab == 3) {
                item(key = "doctor_analytics_panel") {
                    DoctorAnalyticsSection(
                        state = state,
                        activeDoctor = activeDoctor,
                        isEn = isEn,
                    )
                }
            } else if (selectedTab == 2) {
                val patientThreads = state.directMessages
                    .filter { it.doctorId == activeDoctor.id && it.threadType == ChatThreadType.PATIENT_CLINIC }
                    .groupBy { it.patientAccountId }
                    .values
                    .mapNotNull { list ->
                        val latest = list.maxByOrNull { it.createdAtMillis } ?: return@mapNotNull null
                        Triple(latest, list.count { !it.isReadByClinic }, list.size)
                    }
                    .sortedByDescending { it.first.createdAtMillis }

                val unreadAdminReplies = state.directMessages.count {
                    it.doctorId == activeDoctor.id &&
                        it.threadType == ChatThreadType.CLINIC_ADMIN &&
                        !it.isReadByClinic
                }

                val adminClinicAlerts = state.notifications.filter { alert ->
                    alert.targetAudience == NotificationAudience.ALL ||
                        (alert.targetAudience == NotificationAudience.CLINIC &&
                            (alert.targetId.isBlank() || alert.targetId == activeDoctor.id))
                }

                val patientAlertRecipients = buildList {
                    state.allAccounts.forEach { acc ->
                        add(Triple(acc.id, acc.fullName, acc.phone))
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = bi(isEn, "التواصل مع الإدارة العليا للمنصة", "Platform Super Admin Support"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    color = if (state.canPatientChatWith(activeDoctor)) SlotAvailableBg else SlotBookedBg,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = if (state.canPatientChatWith(activeDoctor)) {
                                            bi(isEn, "دردشة المرضى مفعّلة", "Patient Chat ON")
                                        } else {
                                            bi(isEn, "دردشة المرضى متوقفة", "Patient Chat OFF")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.canPatientChatWith(activeDoctor)) SlotAvailableText else SlotBookedText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onOpenChatThread(
                                        DirectChatTarget(
                                            threadType = ChatThreadType.CLINIC_ADMIN,
                                            viewerRole = MessageSenderRole.CLINIC,
                                            title = bi(isEn, "الإدارة العليا للمنصة", "Super Admin Support"),
                                            subtitle = activeDoctor.clinicName,
                                            doctorId = activeDoctor.id,
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("doctor_chat_admin_button"),
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (unreadAdminReplies > 0) {
                                        bi(isEn, "💬 مراسلة الإدارة العليا ($unreadAdminReplies جديد)", "💬 Message Super Admin ($unreadAdminReplies new)")
                                    } else {
                                        bi(isEn, "💬 مراسلة الإدارة العليا", "💬 Message Super Admin")
                                    }
                                )
                            }
                        }
                    }
                }

                if (adminClinicAlerts.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DelayWarningBg),
                            border = BorderStroke(1.dp, DelayWarningBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = bi(isEn, "إشعارات الإدارة الموجهة للعيادة (${adminClinicAlerts.size})", "Admin Alerts for Clinic (${adminClinicAlerts.size})"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DelayWarningText,
                                    fontWeight = FontWeight.Bold,
                                )
                                adminClinicAlerts.take(5).forEach { alert ->
                                    Text(
                                        text = "• ${alert.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DelayWarningText,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = TealPrimary)
                                Text(
                                    text = bi(isEn, "إرسال تنبيه مباشر لمريض (تأخير / تجهيز تحاليل / موعد)", "Send Direct Alert to Patient"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (patientAlertRecipients.isEmpty()) {
                                Text(
                                    text = bi(isEn, "لا يوجد مرضى مسجلون حالياً لإرسال تنبيهات مباشرة.", "No registered patients yet."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(patientAlertRecipients, key = { "rec_${it.first}" }) { (accId, name, phone) ->
                                        val isSel = alertTargetAccountId == accId
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                alertTargetAccountId = accId
                                                alertTargetName = name
                                            },
                                            label = { Text("$name ($phone)") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealPrimary,
                                                selectedLabelColor = Color.White,
                                            ),
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = alertDraftMessage,
                                    onValueChange = { alertDraftMessage = it },
                                    label = { Text(bi(isEn, "نص التنبيه للمريض", "Patient Alert Message")) },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("doctor_patient_alert_input"),
                                )
                                Button(
                                    onClick = {
                                        if (alertTargetAccountId.isBlank()) {
                                            onShowMessage(bi(isEn, "يرجى اختيار المريض أولاً", "Select a patient first"))
                                        } else {
                                            onSendPatientAlert(alertTargetAccountId, alertTargetName, alertDraftMessage) {
                                                alertDraftMessage = ""
                                            }
                                        }
                                    },
                                    enabled = alertDraftMessage.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("doctor_patient_alert_send"),
                                ) {
                                    Text(bi(isEn, "إرسال تنبيه مباشر للمريض", "Send Direct Patient Alert"))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = bi(isEn, "محادثات المرضى (${patientThreads.size})", "Patient Conversations (${patientThreads.size})"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (patientThreads.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "لا توجد رسائل واردة من المرضى حتى الآن.",
                                    "No incoming patient messages yet.",
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(patientThreads, key = { "pt_${it.first.patientAccountId}" }) { (latest, unread, total) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.2.dp, if (unread > 0) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (unread > 0) {
                                            Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                                                Text(unread.toString())
                                            }
                                        }
                                    }
                                    if (latest.patientPhone.isNotBlank()) {
                                        Text(
                                            text = "${latest.patientPhone} • ($total ${bi(isEn, "رسالة", "msgs")})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = "${latest.senderName}: ${latest.body}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onOpenChatThread(
                                            DirectChatTarget(
                                                threadType = ChatThreadType.PATIENT_CLINIC,
                                                viewerRole = MessageSenderRole.CLINIC,
                                                title = latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") },
                                                subtitle = latest.patientPhone,
                                                patientAccountId = latest.patientAccountId,
                                                patientName = latest.patientName,
                                                patientPhone = latest.patientPhone,
                                                doctorId = activeDoctor.id,
                                            )
                                        )
                                    },
                                    modifier = Modifier.height(44.dp),
                                ) {
                                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "فتح والرد", "Reply"))
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TealPrimary)
                                Text(
                                    text = bi(isEn, "تفعيل حساب مريض برقم الهاتف", "Direct Patient Activation by Phone"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = bi(
                                    isEn,
                                    "أدخل رقم هاتف المريض للبحث عن حسابه المسجل وتفعيله فوراً لكي يتمكن من الحجز بدون انتظار كود OTP (يُسجَّل التفعيل باسم عيادة ${activeDoctor.doctorName}).",
                                    "Search a registered patient by phone number and activate their account immediately without an OTP code.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = activationPhoneQuery,
                                    onValueChange = {
                                        activationPhoneQuery = it
                                        onSearchPatientsForActivation(it)
                                    },
                                    label = { Text(bi(isEn, "رقم هاتف المريض أو الاسم", "Patient Phone or Name")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doctor_activate_phone_input"),
                                )
                                Button(
                                    onClick = { onSearchPatientsForActivation(activationPhoneQuery) },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("doctor_activate_search_btn"),
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "بحث", "Search"))
                                }
                            }
                        }
                    }
                }

                if (state.clinicPatientSearchResults.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (activationPhoneQuery.trim().length < 3) {
                                    bi(isEn, "اكتب 3 أرقام على الأقل من هاتف المريض لعرض النتائج وتفعيل الحساب.", "Type at least 3 phone digits to search and activate.")
                                } else {
                                    bi(isEn, "لا توجد نتائج مطابقة لهذا الرقم حتى الآن.", "No registered patient matched this phone.")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                } else {
                    items(state.clinicPatientSearchResults, key = { it.account.id }) { result ->
                        val acc = result.account
                        val verified = acc.isPhoneVerified
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (verified) SlotAvailableBg.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(1.dp, if (verified) SlotAvailableBorder else TealPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(bi(isEn, "الهاتف: ${acc.phone}", "Phone: ${acc.phone}"), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(
                                        color = if (verified) SlotAvailableBg else SlotBookedBg,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = if (verified) {
                                                bi(isEn, "موثق ✓ (${acc.verifiedBy.ifBlank { "كود OTP" }})", "Verified ✓ (${acc.verifiedBy.ifBlank { "OTP" }})")
                                            } else {
                                                bi(isEn, "غير موثق", "Unverified")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (verified) SlotAvailableText else SlotBookedText,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                if (result.familyMembers.isNotEmpty()) {
                                    Text(
                                        text = bi(
                                            isEn,
                                            "أفراد الأسرة: ${result.familyMembers.joinToString(" • ") { "${it.fullName} (${it.relation})" }}",
                                            "Family Members: ${result.familyMembers.joinToString(" • ") { "${it.fullName} (${it.relation})" }}",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (!verified) {
                                    Button(
                                        onClick = { onActivatePatientByClinic(acc.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("doctor_activate_submit_${acc.id}"),
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(bi(isEn, "✓ تفعيل حساب المريض فوراً", "✓ Activate Patient Account Now"))
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val members = result.familyMembers.ifEmpty {
                                            state.allFamilyMembers.filter { it.accountId == acc.id }
                                        }
                                        lookupEmrTarget = acc to members
                                        lookupEmrSelectedMemberId = members.firstOrNull()?.id
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("doctor_view_account_emr_${acc.id}"),
                                ) {
                                    Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "📜 السجل الطبي التاريخي والتحاليل والروشتات", "📜 Permanent Medical Log, Labs & Rx"))
                                }

                                if (acc.linkedSpouseAccountId.isBlank()) {
                                    OutlinedButton(
                                        onClick = { linkRequestAccount = acc },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                    ) {
                                        Text(bi(isEn, "👨‍👩‍👧‍👦 رفع طلب ربط أسرة للإدارة", "👨‍👩‍👧‍👦 Submit Family Link Request to Admin"))
                                    }
                                } else {
                                    Text(
                                        text = bi(isEn, "✓ مرتبط عائلياً مع: ${acc.linkedSpouseName}", "✓ Linked with: ${acc.linkedSpouseName}"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotAvailableText,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Summary Metrics + Delay & Schedule Generator Controls
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val docDisplay = if (isEn && activeDoctor.doctorNameEn.isNotBlank()) activeDoctor.doctorNameEn else activeDoctor.doctorName
                                val clinicDisplay = if (isEn && activeDoctor.clinicNameEn.isNotBlank()) activeDoctor.clinicNameEn else activeDoctor.clinicName
                                val specsDisplay = SpecialtyCatalog.formatDoctorSpecialties(
                                    activeDoctor.specialty,
                                    activeDoctor.additionalSpecialties,
                                    isEn,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = docDisplay,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Text(
                                        text = specsDisplay,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "$clinicDisplay • ${TimeUtils.formatMinutesAr(activeDoctor.workStartMinutes, isEn)} - ${TimeUtils.formatMinutesAr(activeDoctor.workEndMinutes, isEn)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (activeDoctor.currentDelayMinutes > 0) {
                                    Surface(
                                        color = DelayWarningBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DelayWarningBorder),
                                    ) {
                                        Text(
                                            text = bi(isEn, "تأخير: +${activeDoctor.currentDelayMinutes} د", "Delay: +${activeDoctor.currentDelayMinutes}m"),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = DelayWarningText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatPill(bi(isEn, "قادم / محجوز", "Upcoming"), bookedCount.toString(), SlotBookedBg, SlotBookedText, Modifier.weight(1f))
                                StatPill(bi(isEn, "متاح", "Open"), availableCount.toString(), SlotAvailableBg, SlotAvailableText, Modifier.weight(1f))
                                StatPill(bi(isEn, "تم الكشف", "Done"), completedCount.toString(), SlotCompletedBg, SlotCompletedText, Modifier.weight(1f))
                                StatPill(bi(isEn, "تغيب", "No-Show"), noShowCount.toString(), DelayWarningBg, DelayWarningText, Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { onDelayDoctor(activeDoctor.id, 15) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DelayWarningBorder,
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("delay_15_min_button"),
                                ) {
                                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "تأخير 15 دقيقة", "+15 min Delay"))
                                }
                                OutlinedButton(
                                    onClick = { onDelayDoctor(activeDoctor.id, 30) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("delay_30_min_button"),
                                ) {
                                    Text(bi(isEn, "تأخير 30 دقيقة", "+30 min Delay"))
                                }
                            }

                            OutlinedButton(
                                onClick = { showScheduleDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_schedule_button"),
                            ) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(bi(isEn, "توليد جدول المواعيد التلقائي (كل 15 / 20 / 30 دقيقة)", "Auto-Generate Schedule (15 / 20 / 30 min)"))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = bi(isEn, "قائمة مواعيد اليوم (${slots.size})", "Today's Appointments (${slots.size})"),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                items(slots, key = { it.id }) { slot ->
                    DoctorSlotCard(
                        slot = slot,
                        isEn = isEn,
                        onManualBook = { slotForManualBooking = slot },
                        onViewEmr = { slotForEmrView = slot },
                        onWritePrescription = { slotForPrescription = slot },
                        onReschedule = { slotForReschedule = slot },
                        onUpdateStatus = { newStatus -> onUpdateSlotStatus(slot.id, newStatus) },
                        onCancel = { onCancelSlot(slot.id) },
                    )
                }
            }
        }
    }

    // Manual Phone Booking Dialog
    val manualSlot = slotForManualBooking
    if (manualSlot != null) {
        ManualPhoneBookingDialog(
            slot = manualSlot,
            doctor = activeDoctor,
            isEn = isEn,
            onConfirm = { name, phone ->
                onBookManualPhoneSlot(manualSlot.id, name, phone)
                slotForManualBooking = null
            },
            onDismiss = { slotForManualBooking = null },
        )
    }

    // Schedule Generator Dialog
    if (showScheduleDialog) {
        ScheduleGeneratorDialog(
            doctor = activeDoctor,
            isEn = isEn,
            onConfirm = { startMin, endMin, durationMin ->
                onGenerateSchedule(activeDoctor.id, startMin, endMin, durationMin)
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false },
        )
    }

    // Smart Electronic Prescription Dialog
    val rxSlot = slotForPrescription
    if (rxSlot != null) {
        val rxPatientMember = remember(state.allFamilyMembers, rxSlot) {
            state.allFamilyMembers.firstOrNull {
                (rxSlot.familyMemberId.isNotBlank() && it.id == rxSlot.familyMemberId) ||
                    (rxSlot.patientName.isNotBlank() && it.fullName == rxSlot.patientName)
            }
        }
        SmartPrescriptionDialog(
            slot = rxSlot,
            doctor = activeDoctor,
            brandConfig = state.brandConfig,
            medications = state.medications,
            isLiveRemoteMeds = state.isLiveMedicationsApi,
            favoriteMedIds = state.favoriteMedicationIdsForDoctor(activeDoctor.id),
            patientMember = rxPatientMember,
            isEn = isEn,
            onToggleFavorite = { medId -> onToggleFavoriteMed(activeDoctor.id, medId) },
            onSaveAndComplete = { diagnosis, medicationsText, notes, attachmentPath, diagEn, medsEn, notesEn, reportAr, reportEn ->
                onCompleteVisit(
                    rxSlot.id,
                    diagnosis,
                    medicationsText,
                    notes,
                    attachmentPath,
                    diagEn,
                    medsEn,
                    notesEn,
                    reportAr,
                    reportEn,
                )
                slotForPrescription = null
            },
            onShowMessage = onShowMessage,
            onDismiss = { slotForPrescription = null },
        )
    }

    // Patient EMR History Dialog
    val emrSlot = slotForEmrView
    if (emrSlot != null) {
        val patientRecords = state.recordsForPatient(
            familyMemberId = emrSlot.familyMemberId,
            patientName = emrSlot.patientName,
            patientPhone = emrSlot.patientPhone,
        )
        val emrPatientMember = remember(state.allFamilyMembers, emrSlot) {
            state.allFamilyMembers.firstOrNull {
                (emrSlot.familyMemberId.isNotBlank() && it.id == emrSlot.familyMemberId) ||
                    (emrSlot.patientName.isNotBlank() && it.fullName == emrSlot.patientName)
            }
        }
        val patientPermanentLogs = state.permanentLogsForPatient(
            familyMemberId = emrSlot.familyMemberId,
            patientName = emrSlot.patientName,
            patientPhone = emrSlot.patientPhone,
        )
        PatientEmrViewerDialog(
            patientName = emrSlot.patientName,
            patientMember = emrPatientMember,
            isAnalyzingAi = emrPatientMember?.id?.let { it in state.analyzingAiMemberIds } == true,
            brandConfig = state.brandConfig,
            records = patientRecords,
            permanentLogs = patientPermanentLogs,
            isEn = isEn,
            onRefreshAiSummary = { emrPatientMember?.id?.let(onRefreshAiSummary) },
            onPreviewAttachment = onPreviewAttachment,
            onShowMessage = onShowMessage,
            onDismiss = { slotForEmrView = null },
        )
    }

    // Account-level EMR & Permanent Medical Log Viewer from Tab 1
    val accountEmrPair = lookupEmrTarget
    if (accountEmrPair != null) {
        val (targetAcc, membersList) = accountEmrPair
        val activeEmrMember = membersList.firstOrNull { it.id == lookupEmrSelectedMemberId } ?: membersList.firstOrNull()
        val displayMemberName = activeEmrMember?.fullName ?: targetAcc.fullName
        val patientRecords = state.recordsForPatient(
            familyMemberId = activeEmrMember?.id.orEmpty(),
            patientName = displayMemberName,
            patientPhone = targetAcc.phone,
        )
        val patientPermanentLogs = state.permanentLogsForPatient(
            familyMemberId = activeEmrMember?.id.orEmpty(),
            patientName = displayMemberName,
            patientPhone = targetAcc.phone,
        )
        PatientEmrViewerDialog(
            patientName = displayMemberName,
            patientMember = activeEmrMember,
            familyMembersSwitcher = membersList,
            onSelectFamilyMember = { lookupEmrSelectedMemberId = it },
            isAnalyzingAi = activeEmrMember?.id?.let { it in state.analyzingAiMemberIds } == true,
            brandConfig = state.brandConfig,
            records = patientRecords,
            permanentLogs = patientPermanentLogs,
            isEn = isEn,
            onRefreshAiSummary = { activeEmrMember?.id?.let(onRefreshAiSummary) },
            onPreviewAttachment = onPreviewAttachment,
            onShowMessage = onShowMessage,
            onDismiss = { lookupEmrTarget = null },
        )
    }

    val clinicRescheduleSlot = slotForReschedule
    if (clinicRescheduleSlot != null) {
        val openClinicSlots = slots.filter { it.status == SlotStatus.AVAILABLE }
        RescheduleSlotPickerDialog(
            currentSlot = clinicRescheduleSlot,
            availableSlots = openClinicSlots,
            isPatient = false,
            isEn = isEn,
            onPickNewSlot = { targetSlotId ->
                onRescheduleSlot(clinicRescheduleSlot.id, targetSlotId)
                slotForReschedule = null
            },
            onDismiss = { slotForReschedule = null },
        )
    }

    val linkTargetAcc = linkRequestAccount
    if (linkTargetAcc != null) {
        var clinicSpousePhone by remember(linkTargetAcc.id) { mutableStateOf("") }
        var clinicProofMarriage by remember(linkTargetAcc.id) { mutableStateOf("") }
        var clinicProofFront by remember(linkTargetAcc.id) { mutableStateOf("") }
        var clinicProofSlot by remember { mutableIntStateOf(0) }

        val clinicProofPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri != null) {
                val saved = com.example.util.ImageStorageHelper.persistDocumentOrImageUri(context, uri)
                val encoded = saved?.path?.let { com.example.util.ImageStorageHelper.encodeImageFileToCompactDataUri(it) }.orEmpty()
                val res = encoded.ifBlank { saved?.path.orEmpty() }
                if (res.isNotBlank()) {
                    if (clinicProofSlot == 0) clinicProofMarriage = res else clinicProofFront = res
                }
            }
        }

        AlertDialog(
            onDismissRequest = { linkRequestAccount = null },
            title = { Text(bi(isEn, "رفع طلب ربط أسرة للإدارة", "Submit Family Link Request")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = bi(
                            isEn,
                            "الحساب: ${linkTargetAcc.fullName} (${linkTargetAcc.phone}). المراجعة والاعتماد النهائي تتم من المدير العام.",
                            "Account: ${linkTargetAcc.fullName} (${linkTargetAcc.phone}). Final approval is done by Super Admin.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = clinicSpousePhone,
                        onValueChange = { clinicSpousePhone = it.filter { ch -> ch.isDigit() } },
                        label = { Text(bi(isEn, "رقم هاتف الزوج / الزوجة *", "Spouse Phone *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                clinicProofSlot = 0
                                clinicProofPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (clinicProofMarriage.isNotBlank()) "✓ القسيمة" else "قسيمة الزواج")
                        }
                        OutlinedButton(
                            onClick = {
                                clinicProofSlot = 1
                                clinicProofPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (clinicProofFront.isNotBlank()) "✓ البطاقة" else "صورة البطاقة")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clinicSpousePhone.length < 6) {
                            onShowMessage(bi(isEn, "يرجى إدخال رقم هاتف الزوج/الزوجة", "Enter spouse phone"))
                        } else {
                            onSubmitFamilyLinkRequest(
                                linkTargetAcc.id,
                                clinicSpousePhone,
                                clinicProofMarriage,
                                clinicProofFront,
                                "",
                            )
                            linkRequestAccount = null
                        }
                    },
                ) {
                    Text(bi(isEn, "إرسال للإدارة", "Send to Admin"))
                }
            },
            dismissButton = {
                TextButton(onClick = { linkRequestAccount = null }) {
                    Text(bi(isEn, "إلغاء", "Cancel"))
                }
            },
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun DoctorSlotCard(
    slot: AppointmentSlotEntity,
    isEn: Boolean = false,
    onManualBook: () -> Unit,
    onViewEmr: () -> Unit,
    onWritePrescription: () -> Unit,
    onReschedule: () -> Unit = {},
    onUpdateStatus: (String) -> Unit = {},
    onCancel: () -> Unit,
) {
    val bgColor = when (slot.status) {
        SlotStatus.AVAILABLE -> SlotAvailableBg.copy(alpha = 0.65f)
        SlotStatus.BOOKED -> SlotBookedBg.copy(alpha = 0.55f)
        SlotStatus.NO_SHOW -> DelayWarningBg.copy(alpha = 0.8f)
        else -> SlotCompletedBg.copy(alpha = 0.7f)
    }
    val borderColor = when (slot.status) {
        SlotStatus.AVAILABLE -> SlotAvailableBorder
        SlotStatus.BOOKED -> SlotBookedBorder
        SlotStatus.NO_SHOW -> DelayWarningBorder
        else -> SlotCompletedBorder
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.75f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_slot_${slot.id}"),
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
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Text(
                            text = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn),
                            style = MaterialTheme.typography.titleMedium,
                            color = borderColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (slot.status) {
                                SlotStatus.AVAILABLE -> bi(isEn, "موعد متاح للحجز", "Open Slot")
                                SlotStatus.BOOKED -> slot.patientName
                                SlotStatus.NO_SHOW -> slot.patientName
                                else -> slot.patientName
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (slot.status != SlotStatus.AVAILABLE) {
                            val sourceTag = if (slot.bookingSource == BookingSource.PHONE_WALKIN) {
                                bi(isEn, "حجز هاتفي/حضور", "Phone / Walk-in")
                            } else {
                                bi(isEn, "حجز عبر التطبيق", "App Booking")
                            }
                            Text(
                                text = "${slot.patientPhone} • $sourceTag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (slot.status != SlotStatus.AVAILABLE) {
                    val statusLabel = when (slot.status) {
                        SlotStatus.BOOKED -> bi(isEn, "قادم", "Upcoming")
                        SlotStatus.NO_SHOW -> bi(isEn, "تغيب", "No-Show")
                        else -> bi(isEn, "تم الكشف", "Completed")
                    }
                    val statusIcon = when (slot.status) {
                        SlotStatus.BOOKED -> Icons.Filled.Schedule
                        SlotStatus.NO_SHOW -> Icons.Filled.Warning
                        else -> Icons.Filled.CheckCircle
                    }
                    val statusTextColor = when (slot.status) {
                        SlotStatus.BOOKED -> SlotBookedText
                        SlotStatus.NO_SHOW -> DelayWarningText
                        else -> SlotCompletedText
                    }
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.testTag("slot_status_badge_${slot.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusLabel,
                                tint = borderColor,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor,
                            )
                        }
                    }
                }
            }

            if (slot.status != SlotStatus.AVAILABLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val isUpcoming = slot.status == SlotStatus.BOOKED
                    val isCompleted = slot.status == SlotStatus.COMPLETED
                    val isNoShow = slot.status == SlotStatus.NO_SHOW
                    FilterChip(
                        selected = isUpcoming,
                        onClick = { if (!isUpcoming) onUpdateStatus(SlotStatus.BOOKED) },
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text(bi(isEn, "قادم", "Upcoming"), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SlotBookedBg,
                            selectedLabelColor = SlotBookedText,
                            selectedLeadingIconColor = SlotBookedBorder,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_upcoming_${slot.id}"),
                    )
                    FilterChip(
                        selected = isCompleted,
                        onClick = { if (!isCompleted) onUpdateStatus(SlotStatus.COMPLETED) },
                        leadingIcon = {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text(bi(isEn, "تم الكشف", "Done"), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SlotAvailableBg,
                            selectedLabelColor = SlotAvailableText,
                            selectedLeadingIconColor = SlotAvailableBorder,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_completed_${slot.id}"),
                    )
                    FilterChip(
                        selected = isNoShow,
                        onClick = { if (!isNoShow) onUpdateStatus(SlotStatus.NO_SHOW) },
                        leadingIcon = {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text(bi(isEn, "تغيب", "No-Show"), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DelayWarningBg,
                            selectedLabelColor = DelayWarningText,
                            selectedLeadingIconColor = DelayWarningBorder,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_noshow_${slot.id}"),
                    )
                }
            }

            when (slot.status) {
                SlotStatus.AVAILABLE -> {
                    Button(
                        onClick = onManualBook,
                        colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manual_book_${slot.id}"),
                    ) {
                        Icon(Icons.Filled.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(bi(isEn, "إضافة حجز هاتفي / حضور مباشر", "Add Phone / Walk-in Booking"))
                    }
                }

                SlotStatus.BOOKED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onViewEmr,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("view_emr_${slot.id}"),
                        ) {
                            Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bi(isEn, "ملف المريض", "Patient File"))
                        }
                        Button(
                            onClick = onWritePrescription,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("write_rx_${slot.id}"),
                        ) {
                            Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bi(isEn, "كتابة روشتة", "Write Rx"))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onReschedule,
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("clinic_reschedule_${slot.id}"),
                        ) {
                            Text(bi(isEn, "تعديل الموعد", "Reschedule"))
                        }
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("clinic_cancel_${slot.id}"),
                        ) {
                            Text(bi(isEn, "إلغاء الحجز وإتاحته", "Cancel & Free"), color = SlotBookedBorder)
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onViewEmr,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(bi(isEn, "عرض الملف الطبي والروشتة", "View Medical File & Rx"))
                        }
                        if (slot.status == SlotStatus.NO_SHOW) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text(bi(isEn, "إتاحة الموعد", "Free Slot"), color = SlotBookedBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualPhoneBookingDialog(
    slot: AppointmentSlotEntity,
    doctor: ClinicDoctorEntity,
    isEn: Boolean = false,
    onConfirm: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bi(isEn, "تسجيل حجز هاتفي / مباشر", "Manual Phone / Walk-in Booking")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${doctor.doctorName} • ${TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(bi(isEn, "اسم المريض ثلاثي", "Patient Full Name")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_name_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(bi(isEn, "رقم الهاتف", "Phone Number")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_phone_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone) },
                modifier = Modifier.testTag("manual_confirm_button"),
            ) {
                Text(bi(isEn, "تأكيد وإغلاق الموعد", "Confirm & Lock Slot"))
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
private fun ScheduleGeneratorDialog(
    doctor: ClinicDoctorEntity,
    isEn: Boolean = false,
    onConfirm: (startMin: Int, endMin: Int, durationMin: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var startHour by remember { mutableIntStateOf(doctor.workStartMinutes / 60) }
    var endHour by remember { mutableIntStateOf(doctor.workEndMinutes / 60) }
    var duration by remember { mutableIntStateOf(doctor.slotDurationMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bi(isEn, "توليد جدول المواعيد التلقائي", "Auto-Generate Schedule")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = bi(
                        isEn,
                        "تحديد فترة العمل ومدة كل كشف لتقسيم المواعيد تلقائياً:",
                        "Choose working hours and slot duration to generate slots:",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = bi(isEn, "بداية العيادة: ${TimeUtils.formatMinutesAr(startHour * 60, false)}", "Start Time: ${TimeUtils.formatMinutesAr(startHour * 60, true)}"),
                    style = MaterialTheme.typography.titleSmall,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(14, 15, 16, 17, 18)) { hr ->
                        FilterChip(
                            selected = startHour == hr,
                            onClick = { startHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60, isEn)) },
                        )
                    }
                }

                Text(
                    text = bi(isEn, "نهاية العيادة: ${TimeUtils.formatMinutesAr(endHour * 60, false)}", "End Time: ${TimeUtils.formatMinutesAr(endHour * 60, true)}"),
                    style = MaterialTheme.typography.titleSmall,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(19, 20, 21, 22, 23)) { hr ->
                        FilterChip(
                            selected = endHour == hr,
                            onClick = { endHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60, isEn)) },
                        )
                    }
                }

                Text(
                    text = bi(isEn, "مدة الكشف: كل $duration دقيقة", "Visit Duration: every $duration min"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30).forEach { dur ->
                        FilterChip(
                            selected = duration == dur,
                            onClick = { duration = dur },
                            label = { Text(bi(isEn, "$dur د", "${dur}m")) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startHour * 60, endHour * 60, duration) },
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(bi(isEn, "توليد المواعيد", "Generate Slots"))
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
private fun PatientEmrViewerDialog(
    patientName: String,
    patientMember: FamilyMemberEntity?,
    familyMembersSwitcher: List<FamilyMemberEntity> = emptyList(),
    onSelectFamilyMember: (String) -> Unit = {},
    isAnalyzingAi: Boolean,
    brandConfig: com.example.config.PlatformBrandConfig,
    records: List<MedicalRecordEntity>,
    permanentLogs: List<PermanentMedicalLogEntity>,
    isEn: Boolean = false,
    onRefreshAiSummary: () -> Unit,
    onPreviewAttachment: (AttachmentPreviewTarget) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "الملف الطبي للمريض", "Patient Medical File"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = patientName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TealPrimary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                if (familyMembersSwitcher.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(familyMembersSwitcher, key = { it.id }) { fm ->
                            val sel = fm.id == patientMember?.id
                            FilterChip(
                                selected = sel,
                                onClick = { onSelectFamilyMember(fm.id) },
                                label = { Text("${fm.fullName} (${fm.relation})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val sortedRecords = remember(records) {
                    records.sortedByDescending { it.createdAtMillis }
                }
                val isRxRecord: (MedicalRecordEntity) -> Boolean = { rec ->
                    rec.recordType == MedicalRecordType.PRESCRIPTION || rec.recordType == MedicalRecordType.EXTERNAL_VISIT
                }
                val rxOnlyRecords = remember(sortedRecords) {
                    sortedRecords.filter(isRxRecord)
                }
                val labOnlyRecords = remember(sortedRecords) {
                    sortedRecords.filterNot(isRxRecord)
                }
                var emrFilter by remember(patientName) {
                    mutableStateOf(if (rxOnlyRecords.isNotEmpty()) "RX" else "ALL")
                }
                val visibleRecords = remember(sortedRecords, rxOnlyRecords, labOnlyRecords, emrFilter) {
                    when (emrFilter) {
                        "RX" -> rxOnlyRecords
                        "LAB" -> labOnlyRecords
                        else -> sortedRecords
                    }
                }
                val emrListState = rememberLazyListState()
                val emrScope = rememberCoroutineScope()

                LazyColumn(
                    state = emrListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(500.dp),
                ) {
                    item(key = "emr_medical_alert") {
                        DoctorPatientMedicalAlertBanner(
                            member = patientMember,
                            patientName = patientName,
                            isEn = isEn,
                        )
                    }
                    item(key = "emr_ai_summary") {
                        AiMedicalSummaryCard(
                            patientName = patientName,
                            records = records,
                            member = patientMember,
                            isAnalyzing = isAnalyzingAi,
                            onRefreshAiSummary = if (patientMember != null) onRefreshAiSummary else null,
                            isEn = isEn,
                        )
                    }
                    if (sortedRecords.isNotEmpty()) {
                        item(key = "emr_rx_timeline_controls") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = bi(
                                            isEn,
                                            "💊 سجل الروشتات السابقة (جدول زمني)",
                                            "💊 Previous Prescriptions Timeline",
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = emrFilter == "RX",
                                                onClick = { emrFilter = "RX" },
                                                label = { Text(bi(isEn, "الروشتات (${rxOnlyRecords.size})", "Rx (${rxOnlyRecords.size})")) },
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = emrFilter == "LAB",
                                                onClick = { emrFilter = "LAB" },
                                                label = { Text(bi(isEn, "التحاليل (${labOnlyRecords.size})", "Labs (${labOnlyRecords.size})")) },
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = emrFilter == "ALL",
                                                onClick = { emrFilter = "ALL" },
                                                label = { Text(bi(isEn, "الكل (${sortedRecords.size})", "All (${sortedRecords.size})")) },
                                            )
                                        }
                                    }
                                    if (visibleRecords.isNotEmpty()) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            itemsIndexed(visibleRecords, key = { _, item -> "jump_${item.id}" }) { idx, rec ->
                                                val shortTitle = rec.diagnosis.trim().take(18).ifBlank { rec.doctorName }
                                                SuggestionChip(
                                                    onClick = {
                                                        emrScope.launch {
                                                            // Header items: alert (0), ai summary (1), controls (2), perm log (3) => records start at 4
                                                            emrListState.animateScrollToItem(4 + idx)
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            text = "${rec.visitDateLabel.ifBlank { bi(isEn, "زيارة", "Visit") }} • $shortTitle",
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "emr_permanent_log") {
                        PermanentMedicalLogSection(
                            patientName = patientName,
                            logs = permanentLogs,
                            isEn = isEn,
                        )
                    }
                    if (visibleRecords.isEmpty()) {
                        item(key = "emr_empty") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = bi(
                                        isEn,
                                        "لا توجد زيارات أو تحاليل مسجلة لهذا المريض في هذا التصنيف.",
                                        "No previous visits or lab reports in this category yet.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }
                    } else {
                        items(visibleRecords, key = { it.id }) { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 18.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (isRxRecord(rec)) TealPrimary else Color(0xFF4338CA),
                                                shape = CircleShape,
                                            ),
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    MedicalRecordTimelineCard(
                                        record = rec,
                                        brandConfig = brandConfig,
                                        isEn = isEn,
                                        onPreviewAttachment = onPreviewAttachment,
                                        onDeleteRecordFile = null,
                                        onShowMessage = onShowMessage,
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
