package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.SlotStatus
import com.example.ui.TebUiState
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.util.TimeUtils

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
    ) -> Unit,
    onResetPlatformSettings: () -> Unit,
    onLogoutAdmin: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var defaultFormClinicType by remember { mutableStateOf(ClinicType.PRIVATE) }
    var showAddForm by remember { mutableStateOf(false) }
    var doctorToEdit by remember { mutableStateOf<ClinicDoctorEntity?>(null) }
    var doctorToDelete by remember { mutableStateOf<ClinicDoctorEntity?>(null) }

    var manualBlockPhone by remember { mutableStateOf("") }
    var manualBlockReason by remember { mutableStateOf("حجز وهمي / مخالفة") }

    // General platform settings state
    var cfgPlatformName by remember(state.brandConfig.platformNameAr) { mutableStateOf(state.brandConfig.platformNameAr) }
    var cfgVillageName by remember(state.brandConfig.villageNameAr) { mutableStateOf(state.brandConfig.villageNameAr) }
    var cfgHospitalName by remember(state.brandConfig.hospitalNameAr) { mutableStateOf(state.brandConfig.hospitalNameAr) }
    var cfgSupportPhone by remember(state.brandConfig.supportPhone) { mutableStateOf(state.brandConfig.supportPhone) }
    var cfgAnnouncement by remember(state.brandConfig.homeAnnouncementAr) { mutableStateOf(state.brandConfig.homeAnnouncementAr) }
    var cfgApkUrl by remember(state.brandConfig.apkDownloadUrl) { mutableStateOf(state.brandConfig.apkDownloadUrl) }
    var cfgMedsApi by remember(state.brandConfig.medicationsApiUrl) { mutableStateOf(state.brandConfig.medicationsApiUrl) }
    var cfgAdminPin by remember(state.brandConfig.adminPin) { mutableStateOf(state.brandConfig.adminPin) }

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
                                    text = "لوحة التحكم الشاملة (Super Admin)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                )
                                Text(
                                    text = "إدارة كاملة للمنصة — ${state.brandConfig.adminOwnerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                )
                            }
                            Button(
                                onClick = onLogoutAdmin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TealPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("admin_logout_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("قفل وخروج")
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
                    AdminMetricBox("العيادات الخاصة", state.privateClinics.size.toString(), Modifier.weight(1f))
                    AdminMetricBox("أقسام المستشفى", state.hospitalClinics.size.toString(), Modifier.weight(1f))
                    AdminMetricBox("حجوزات اليوم", bookedSlots.size.toString(), Modifier.weight(1f))
                }
            }

            item {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الأطباء والعيادات") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("المستشفى والأقسام") },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("الحجوزات والحظر") },
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("إعدادات المنصة والـ PIN") },
                    )
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
                            Text("إضافة دكتور / عيادة خاصة جديدة")
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث بالاسم أو التخصص أو العيادة...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(privateDoctors, key = { it.id }) { doc ->
                        AdminDoctorManageCard(
                            doctor = doc,
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
                            Text("إضافة قسم / طبيب في المستشفى التخصصي")
                        }
                    }
                    items(hospitalDoctors, key = { it.id }) { doc ->
                        AdminDoctorManageCard(
                            doctor = doc,
                            onEdit = { doctorToEdit = doc },
                            onDelete = { doctorToDelete = doc },
                        )
                    }
                }

                2 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("حظر رقم هاتف مخالف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = manualBlockPhone,
                                    onValueChange = { manualBlockPhone = it },
                                    label = { Text("رقم الهاتف المراد حظره") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_block_phone_input"),
                                )
                                OutlinedTextField(
                                    value = manualBlockReason,
                                    onValueChange = { manualBlockReason = it },
                                    label = { Text("سبب الحظر") },
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
                                    Text("حظر الرقم وإلغاء حجوزاته")
                                }
                            }
                        }
                    }

                    if (state.blockedPhones.isNotEmpty()) {
                        item {
                            Text("الأرقام المحظورة (${state.blockedPhones.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(state.blockedPhones, key = { it.phone }) { blocked ->
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
                                        Text("رفع الحظر")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("جميع الحجوزات النشطة في المنصة (${bookedSlots.size})", style = MaterialTheme.typography.titleMedium)
                    }

                    if (bookedSlots.isEmpty()) {
                        item {
                            Text("لا توجد حجوزات نشطة حالياً.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(bookedSlots, key = { it.id }) { slot ->
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
                                            Text("هاتف: ${slot.patientPhone}", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "${doctor?.doctorName ?: ""} • ${doctor?.clinicName ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TealPrimary,
                                            )
                                        }
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                                            Text(
                                                text = TimeUtils.formatMinutesAr(slot.effectiveMinutes),
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
                                            Text("إلغاء الحجز")
                                        }
                                        if (slot.patientPhone.isNotBlank()) {
                                            Button(
                                                onClick = { onBlockPhone(slot.patientPhone, "حجز وهمي متكرر") },
                                                colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حظر الرقم")
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
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, tint = TealPrimary)
                                    Text("إعدادات المنصة العامة والأمان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                OutlinedTextField(
                                    value = cfgPlatformName,
                                    onValueChange = { cfgPlatformName = it },
                                    label = { Text("اسم المنصة") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgVillageName,
                                    onValueChange = { cfgVillageName = it },
                                    label = { Text("اسم القرية / المركز") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgHospitalName,
                                    onValueChange = { cfgHospitalName = it },
                                    label = { Text("اسم المستشفى الرئيسي") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgSupportPhone,
                                    onValueChange = { cfgSupportPhone = it },
                                    label = { Text("رقم التواصل والدعم الفني") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgAnnouncement,
                                    onValueChange = { cfgAnnouncement = it },
                                    label = { Text("إعلان التنبيه بالصفحة الرئيسية (اختياري)") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgApkUrl,
                                    onValueChange = { cfgApkUrl = it },
                                    label = { Text("رابط تحميل تطبيق الأندرويد المباشر (APK)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = cfgMedsApi,
                                    onValueChange = { cfgMedsApi = it },
                                    label = { Text("رابط API الأدوية (اختياري)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                OutlinedTextField(
                                    value = cfgAdminPin,
                                    onValueChange = { cfgAdminPin = it },
                                    label = { Text("الرقم السري للوحة التحكم (Admin PIN)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_change_pin_input"),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onResetPlatformSettings,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                    ) {
                                        Text("استعادة الافتراضي")
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
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("admin_save_platform_settings"),
                                    ) {
                                        Text("حفظ وتطبيق فوراً")
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
            hospitalName = state.brandConfig.hospitalNameAr,
            onSave = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass ->
                onAddDoctor(name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass)
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
            hospitalName = state.brandConfig.hospitalNameAr,
            onSave = { name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass ->
                onUpdateDoctor(editingDoc.id, name, spec, clinic, type, loc, bio, maps, fee, startMin, endMin, dur, user, pass)
                doctorToEdit = null
            },
            onDismiss = { doctorToEdit = null },
        )
    }

    val deletingDoc = doctorToDelete
    if (deletingDoc != null) {
        AlertDialog(
            onDismissRequest = { doctorToDelete = null },
            title = { Text("تأكيد الحذف النهائي") },
            text = {
                Text("هل أنت متأكد من حذف ${deletingDoc.doctorName} (${deletingDoc.clinicName}) نهائياً من المنصة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDoctor(deletingDoc.id)
                        doctorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotBookedBorder),
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { doctorToDelete = null }) {
                    Text("تراجع")
                }
            },
        )
    }
}

@Composable
private fun AdminMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminDoctorManageCard(
    doctor: ClinicDoctorEntity,
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
                    Text(doctor.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${doctor.specialty} — ${doctor.clinicName}", style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                }
                Surface(color = SlotAvailableBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = if (doctor.clinicType == ClinicType.HOSPITAL) "مستشفى" else "عيادة خاصة",
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
                text = "📍 ${doctor.locationLabel} • الكشف: ${doctor.feeEgp} ج.م • 🕒 ${TimeUtils.formatMinutesAr(doctor.workStartMinutes)} - ${TimeUtils.formatMinutesAr(doctor.workEndMinutes)}",
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
                    Text("المستخدم: ${doctor.username}", style = MaterialTheme.typography.labelMedium)
                    Text("كلمة المرور: ${doctor.password}", style = MaterialTheme.typography.labelMedium)
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
                    Text("تعديل البيانات")
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                    border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.height(46.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف")
                }
            }
        }
    }
}

@Composable
private fun DoctorFormDialog(
    initialDoctor: ClinicDoctorEntity?,
    defaultClinicType: String,
    hospitalName: String,
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
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = initialDoctor != null
    var doctorName by remember { mutableStateOf(initialDoctor?.doctorName ?: "") }
    var specialty by remember { mutableStateOf(initialDoctor?.specialty ?: "") }
    var clinicName by remember { mutableStateOf(initialDoctor?.clinicName ?: "") }
    var clinicType by remember { mutableStateOf(initialDoctor?.clinicType ?: defaultClinicType) }
    var bio by remember { mutableStateOf(initialDoctor?.bio ?: "") }
    var locationLabel by remember { mutableStateOf(initialDoctor?.locationLabel ?: "") }
    var feeText by remember { mutableStateOf((initialDoctor?.feeEgp ?: 150).toString()) }
    var mapsUrl by remember { mutableStateOf(initialDoctor?.mapsUrl ?: "") }
    var startHour by remember { mutableIntStateOf((initialDoctor?.workStartMinutes ?: (17 * 60)) / 60) }
    var endHour by remember { mutableIntStateOf((initialDoctor?.workEndMinutes ?: (21 * 60)) / 60) }
    var slotDuration by remember { mutableIntStateOf(initialDoctor?.slotDurationMinutes ?: 20) }
    var username by remember { mutableStateOf(initialDoctor?.username ?: "") }
    var password by remember { mutableStateOf(initialDoctor?.password ?: "1234") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "تعديل البيانات" else "إضافة دكتور أو قسم جديد") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("اسم الدكتور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("التخصص الطبي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = clinicType == ClinicType.PRIVATE,
                        onClick = { clinicType = ClinicType.PRIVATE },
                        label = { Text("عيادة خاصة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = clinicType == ClinicType.HOSPITAL,
                        onClick = { clinicType = ClinicType.HOSPITAL },
                        label = { Text("المستشفى التخصصي") },
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
                            if (clinicType == ClinicType.HOSPITAL) "اسم القسم بالمستشفى"
                            else "اسم العيادة"
                        )
                    },
                    placeholder = { Text(if (clinicType == ClinicType.HOSPITAL) "$hospitalName - قسم ..." else "") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("نبذة عن الدكتور") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = locationLabel,
                    onValueChange = { locationLabel = it },
                    label = { Text("عنوان العيادة بالتفصيل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("سعر الكشف (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mapsUrl,
                    onValueChange = { mapsUrl = it },
                    label = { Text("رابط موقع العيادة على خرائط جوجل (Google Maps)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("بداية العمل: ${TimeUtils.formatMinutesAr(startHour * 60)}", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(10, 12, 14, 15, 16, 17, 18)) { hr ->
                        FilterChip(
                            selected = startHour == hr,
                            onClick = { startHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60)) },
                        )
                    }
                }
                Text("نهاية العمل: ${TimeUtils.formatMinutesAr(endHour * 60)}", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(18, 19, 20, 21, 22, 23)) { hr ->
                        FilterChip(
                            selected = endHour == hr,
                            onClick = { endHour = hr },
                            label = { Text(TimeUtils.formatMinutesAr(hr * 60)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30).forEach { dur ->
                        FilterChip(
                            selected = slotDuration == dur,
                            onClick = { slotDuration = dur },
                            label = { Text("$dur د") },
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("حساب تسجيل دخول الدكتور للوحة العيادة:", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم أو رقم الهاتف للدكتور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور للدكتور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                    )
                }
            ) {
                Text(if (isEdit) "حفظ التعديلات" else "إضافة ونشر فوراً")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
