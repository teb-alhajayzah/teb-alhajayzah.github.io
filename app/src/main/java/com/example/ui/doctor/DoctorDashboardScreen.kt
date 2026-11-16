package com.example.ui.doctor

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BookingSource
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.SlotStatus
import com.example.ui.TebUiState
import com.example.ui.components.PasswordOutlinedTextField
import com.example.ui.components.otpCodeGeneratorItems
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
import com.example.util.TimeUtils

@Composable
fun DoctorLoginGateScreen(
    adminOwnerName: String,
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
                            contentDescription = "تسجيل دخول الأطباء",
                            tint = TealPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "تسجيل دخول الأطباء والعيادات",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "أدخل اسم المستخدم أو رقم الهاتف وكلمة المرور الخاصة بعيادتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("اسم المستخدم أو رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doctor_login_identifier"),
                )

                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "كلمة المرور",
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
                    Text("دخول إلى لوحة العيادة")
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "تُنشأ حسابات الأطباء وتُدار من لوحة الإدارة العليا ($adminOwnerName). مثال تجريبي: 01001001001 / 1234",
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
    onDelayDoctor: (doctorId: String, delayMinutes: Int) -> Unit,
    onGenerateSchedule: (doctorId: String, startMin: Int, endMin: Int, durationMin: Int) -> Unit,
    onCompleteVisit: (slotId: String, diagnosis: String, medications: String, notes: String, attachmentPath: String) -> Unit,
    onToggleFavoriteMed: (doctorId: String, medicationId: String) -> Unit,
    onGenerateOtpBatch: (count: Int, generatedBy: String) -> Unit,
    onPreviewImage: (path: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activeDoctor = state.loggedInDoctor ?: return
    val slots = remember(state.allSlots, activeDoctor.id) {
        state.slotsForDoctor(activeDoctor.id)
    }

    val availableCount = slots.count { it.status == SlotStatus.AVAILABLE }
    val bookedCount = slots.count { it.status == SlotStatus.BOOKED }
    val completedCount = slots.count { it.status == SlotStatus.COMPLETED }

    var selectedTab by remember { mutableIntStateOf(0) }
    var slotForManualBooking by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotForPrescription by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotForEmrView by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }

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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "لوحة تحكم عيادة ${activeDoctor.doctorName}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "إدارة جدول المواعيد، الحجوزات الهاتفية، تنبيهات التأخير، والروشتة الإلكترونية.",
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
                        Text("خروج")
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("جدول المواعيد والعيادة") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("أكواد التفعيل (OTP)") },
                        modifier = Modifier.testTag("doctor_otp_codes_tab"),
                    )
                }
            }

            if (selectedTab == 1) {
                otpCodeGeneratorItems(
                    context = context,
                    brandConfig = state.brandConfig,
                    activationCodes = state.activationCodes,
                    onGenerateBatch = { count -> onGenerateOtpBatch(count, activeDoctor.doctorName) },
                    onShowMessage = onShowMessage,
                )
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeDoctor.doctorName,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Text(
                                        text = "${activeDoctor.clinicName} • ${TimeUtils.formatMinutesAr(activeDoctor.workStartMinutes)} - ${TimeUtils.formatMinutesAr(activeDoctor.workEndMinutes)}",
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
                                            text = "تأخير: +${activeDoctor.currentDelayMinutes} د",
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
                                StatPill("محجوز", bookedCount.toString(), SlotBookedBg, SlotBookedText, Modifier.weight(1f))
                                StatPill("متاح", availableCount.toString(), SlotAvailableBg, SlotAvailableText, Modifier.weight(1f))
                                StatPill("تم الكشف", completedCount.toString(), SlotCompletedBg, SlotCompletedText, Modifier.weight(1f))
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
                                    Text("تأخير 15 دقيقة")
                                }
                                OutlinedButton(
                                    onClick = { onDelayDoctor(activeDoctor.id, 30) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("delay_30_min_button"),
                                ) {
                                    Text("تأخير 30 دقيقة")
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
                                Text("توليد جدول المواعيد التلقائي (كل 15 / 20 / 30 دقيقة)")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "قائمة مواعيد اليوم (${slots.size})",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                items(slots, key = { it.id }) { slot ->
                    DoctorSlotCard(
                        slot = slot,
                        onManualBook = { slotForManualBooking = slot },
                        onViewEmr = { slotForEmrView = slot },
                        onWritePrescription = { slotForPrescription = slot },
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
        SmartPrescriptionDialog(
            slot = rxSlot,
            doctor = activeDoctor,
            brandConfig = state.brandConfig,
            medications = state.medications,
            isLiveRemoteMeds = state.isLiveMedicationsApi,
            favoriteMedIds = state.favoriteMedicationIdsForDoctor(activeDoctor.id),
            onToggleFavorite = { medId -> onToggleFavoriteMed(activeDoctor.id, medId) },
            onSaveAndComplete = { diagnosis, medicationsText, notes, attachmentPath ->
                onCompleteVisit(rxSlot.id, diagnosis, medicationsText, notes, attachmentPath)
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
        PatientEmrViewerDialog(
            patientName = emrSlot.patientName,
            brandConfig = state.brandConfig,
            records = patientRecords,
            onPreviewImage = onPreviewImage,
            onShowMessage = onShowMessage,
            onDismiss = { slotForEmrView = null },
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
    onManualBook: () -> Unit,
    onViewEmr: () -> Unit,
    onWritePrescription: () -> Unit,
    onCancel: () -> Unit,
) {
    val bgColor = when (slot.status) {
        SlotStatus.AVAILABLE -> SlotAvailableBg.copy(alpha = 0.65f)
        SlotStatus.BOOKED -> SlotBookedBg.copy(alpha = 0.55f)
        else -> SlotCompletedBg.copy(alpha = 0.7f)
    }
    val borderColor = when (slot.status) {
        SlotStatus.AVAILABLE -> SlotAvailableBorder
        SlotStatus.BOOKED -> SlotBookedBorder
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Text(
                            text = TimeUtils.formatMinutesAr(slot.effectiveMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = borderColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Column {
                        Text(
                            text = when (slot.status) {
                                SlotStatus.AVAILABLE -> "موعد متاح للحجز"
                                SlotStatus.BOOKED -> slot.patientName
                                else -> "${slot.patientName} (تم الكشف ✓)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (slot.status != SlotStatus.AVAILABLE) {
                            val sourceTag = if (slot.bookingSource == BookingSource.PHONE_WALKIN) "حجز هاتفي/حضور" else "حجز عبر التطبيق"
                            Text(
                                text = "${slot.patientPhone} • $sourceTag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
                        Text("إضافة حجز هاتفي / حضور مباشر")
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
                            Text("ملف المريض")
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
                            Text("كتابة روشتة")
                        }
                    }
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("إلغاء الحجز وإتاحته", color = SlotBookedBorder)
                    }
                }

                SlotStatus.COMPLETED -> {
                    OutlinedButton(
                        onClick = onViewEmr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عرض الملف الطبي والروشتة")
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
    onConfirm: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل حجز هاتفي / مباشر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${doctor.doctorName} • الساعة ${TimeUtils.formatMinutesAr(slot.effectiveMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المريض ثلاثي") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_name_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
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
                Text("تأكيد وإغلاق الموعد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}

@Composable
private fun ScheduleGeneratorDialog(
    doctor: ClinicDoctorEntity,
    onConfirm: (startMin: Int, endMin: Int, durationMin: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var startHour by remember { mutableIntStateOf(doctor.workStartMinutes / 60) }
    var endHour by remember { mutableIntStateOf(doctor.workEndMinutes / 60) }
    var duration by remember { mutableIntStateOf(doctor.slotDurationMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توليد جدول المواعيد التلقائي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "تحديد فترة العمل ومدة كل كشف لتقسيم المواعيد تلقائياً:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("بداية العيادة: ${TimeUtils.formatMinutesAr(startHour * 60)}", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(14, 15, 16, 17, 18)) { hr ->
                        FilterChip(
                            selected = startHour == hr,
                            onClick = { startHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60)) },
                        )
                    }
                }

                Text("نهاية العيادة: ${TimeUtils.formatMinutesAr(endHour * 60)}", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(19, 20, 21, 22, 23)) { hr ->
                        FilterChip(
                            selected = endHour == hr,
                            onClick = { endHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60)) },
                        )
                    }
                }

                Text("مدة الكشف: كل $duration دقيقة", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30).forEach { dur ->
                        FilterChip(
                            selected = duration == dur,
                            onClick = { duration = dur },
                            label = { Text("$dur د") },
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
                Text("توليد المواعيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}

@Composable
private fun PatientEmrViewerDialog(
    patientName: String,
    brandConfig: com.example.config.PlatformBrandConfig,
    records: List<MedicalRecordEntity>,
    onPreviewImage: (path: String, title: String) -> Unit,
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
                            text = "الملف الطبي للمريض",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = patientName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TealPrimary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (records.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "لا توجد زيارات سابقة أو تحاليل مرفوعة لهذا المريض حتى الآن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(440.dp),
                    ) {
                        items(records, key = { it.id }) { rec ->
                            MedicalRecordTimelineCard(
                                record = rec,
                                brandConfig = brandConfig,
                                onPreviewImage = onPreviewImage,
                                onShowMessage = onShowMessage,
                            )
                        }
                    }
                }
            }
        }
    }
}
