package com.example.ui.patient

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.SlotStatus
import com.example.ui.PatientTab
import com.example.ui.TebUiState
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
fun PatientHomeScreen(
    state: TebUiState,
    onSelectTab: (PatientTab) -> Unit,
    onSelectSpecialty: (String?) -> Unit,
    onSelectFamilyMember: (String) -> Unit,
    onOpenAddFamilyDialog: () -> Unit,
    onOpenDoctorBooking: (String) -> Unit,
    onCancelSlot: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activeMember = state.activeMember
    val familyIds = remember(state.familyMembers) { state.familyMembers.map { it.id }.toSet() }
    val familyNames = remember(state.familyMembers) { state.familyMembers.map { it.fullName }.toSet() }

    val myBookedSlots = remember(state.allSlots, familyIds, familyNames) {
        state.allSlots.filter { slot ->
            slot.status == SlotStatus.BOOKED &&
                (slot.familyMemberId in familyIds || slot.patientName in familyNames)
        }.sortedBy { it.effectiveMinutes }
    }

    val latestDelay = state.notifications.firstOrNull()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                    ) {
                        Image(
                            painter = painterResource(id = state.brandConfig.heroBannerRes),
                            contentDescription = state.brandConfig.hospitalNameAr,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xD9084C4F),
                                            Color(0x990D7377),
                                            Color(0x330D7377),
                                        )
                                    )
                                )
                                .padding(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "أهلاً بك، ${activeMember?.fullName ?: state.activeAccount?.fullName ?: "مريضنا الكريم"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                )
                                Text(
                                    text = state.brandConfig.taglineAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.94f),
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        text = "احجز موعدك في ثوانٍ بدون انتظار • ${state.brandConfig.hospitalNameAr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (latestDelay != null) {
                item {
                    Surface(
                        color = DelayWarningBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, DelayWarningBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "تنبيه تأخير",
                                tint = DelayWarningBorder,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = latestDelay.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DelayWarningText,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "الحجز والملف الطبي باسم:",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = onOpenAddFamilyDialog) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة فرد عائلة")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.familyMembers, key = { it.id }) { member ->
                            val selected = member.id == activeMember?.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFamilyMember(member.id) },
                                label = {
                                    Text(
                                        text = "${member.fullName.substringBefore(" ")} (${member.relation.substringBefore(" ")})",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "الأقسام الرئيسية",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeSectionTile(
                            title = "العيادات الخاصة",
                            subtitle = "${state.privateClinics.size} عيادات متاحة",
                            icon = Icons.Filled.MedicalServices,
                            containerColor = Color(0xFFE0F2F1),
                            accentColor = Color(0xFF0D7377),
                            testTag = "section_private_clinics",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(PatientTab.PRIVATE_CLINICS) },
                        )
                        HomeSectionTile(
                            title = "المستشفى التخصصي",
                            subtitle = "${state.hospitalClinics.size} أقسام طبية",
                            icon = Icons.Filled.LocalHospital,
                            containerColor = Color(0xFFE3F2FD),
                            accentColor = Color(0xFF0F5E8C),
                            testTag = "section_hospital",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(PatientTab.HOSPITAL) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeSectionTile(
                            title = "التخصصات الطبية",
                            subtitle = "${state.specialties.size} تخصصات",
                            icon = Icons.Filled.MedicalInformation,
                            containerColor = Color(0xFFECFDF5),
                            accentColor = Color(0xFF15803D),
                            testTag = "section_specialties",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectSpecialty(null)
                                onSelectTab(PatientTab.SPECIALTIES)
                            },
                        )
                        HomeSectionTile(
                            title = "ملفي الطبي",
                            subtitle = "الروشتات والتحاليل",
                            icon = Icons.Filled.FolderShared,
                            containerColor = Color(0xFFFFF7ED),
                            accentColor = Color(0xFFC2410C),
                            testTag = "section_medical_file",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(PatientTab.MEDICAL_FILE) },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "مواعيد العائلة المحجوزة اليوم (${myBookedSlots.size})",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (myBookedSlots.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = "لا توجد حجوزات قادمة اليوم",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "اختر العيادات الخاصة أو المستشفى التخصصي لحجز موعدك بضغطة واحدة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(myBookedSlots, key = { it.id }) { slot ->
                    val doctor = state.doctors.firstOrNull { it.id == slot.doctorId }
                    if (doctor != null) {
                        MyBookingCard(
                            slot = slot,
                            doctor = doctor,
                            onOpenClinic = { onOpenDoctorBooking(doctor.id) },
                            onCancel = { onCancelSlot(slot.id) },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "أطباء متاحون الآن في ${state.brandConfig.villageNameAr}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            items(state.doctors.take(4), key = { it.id }) { doctor ->
                val doctorSlots = state.slotsForDoctor(doctor.id)
                DoctorDirectoryCard(
                    doctor = doctor,
                    availableCount = doctorSlots.count { it.status == SlotStatus.AVAILABLE },
                    bookedCount = doctorSlots.count { it.status == SlotStatus.BOOKED },
                    onBookClick = { onOpenDoctorBooking(doctor.id) },
                )
            }

            // بطاقة التحميل المباشر وتحديث تطبيق طب الحجايزة (الإصدار v1.0.0) في أسفل الصفحة الرئيسية
            item {
                ApkDownloadCard(
                    platformName = state.brandConfig.platformNameAr,
                    versionLabel = state.brandConfig.appVersionLabel,
                    onDownloadClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(state.brandConfig.apkDownloadUrl),
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            onShowMessage("تعذر فتح رابط التحميل: ${state.brandConfig.apkDownloadUrl}")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ApkDownloadCard(
    platformName: String,
    versionLabel: String,
    onDownloadClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apk_download_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تحميل وتحديث تطبيق $platformName (الإصدار $versionLabel)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "تحميل مباشر لنسخة الأندرويد الرسمية المحدّثة تلقائياً من GitHub Releases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apk_direct_download_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تحميل مباشر للتطبيق (APK)",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HomeSectionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MyBookingCard(
    slot: AppointmentSlotEntity,
    doctor: ClinicDoctorEntity,
    onOpenClinic: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
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
                    Text(
                        text = doctor.doctorName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${doctor.specialty} • ${doctor.clinicName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = TimeUtils.formatMinutesAr(slot.effectiveMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            if (slot.delayMinutes > 0) {
                Text(
                    text = "تم تعديل الموعد (+${slot.delayMinutes} دقيقة تأخير عن ${TimeUtils.formatMinutesAr(slot.startMinutes)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = DelayWarningBorder,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "المريض: ${slot.patientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel) {
                        Text("إلغاء", color = SlotBookedBorder)
                    }
                    Button(
                        onClick = onOpenClinic,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("تفاصيل العيادة")
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicsDirectoryScreen(
    state: TebUiState,
    title: String,
    clinicTypeFilter: String?,
    showSpecialtyChips: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectSpecialty: (String?) -> Unit,
    onOpenDoctorBooking: (String) -> Unit,
) {
    val filteredDoctors = remember(
        state.doctors,
        clinicTypeFilter,
        state.selectedSpecialty,
        state.searchQuery,
    ) {
        state.doctors.filter { doc ->
            val matchesType = clinicTypeFilter == null || doc.clinicType == clinicTypeFilter
            val matchesSpec = !showSpecialtyChips ||
                state.selectedSpecialty == null ||
                doc.specialty == state.selectedSpecialty
            val q = state.searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                doc.doctorName.contains(q, ignoreCase = true) ||
                doc.specialty.contains(q, ignoreCase = true) ||
                doc.clinicName.contains(q, ignoreCase = true)
            matchesType && matchesSpec && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("ابحث باسم الدكتور أو التخصص أو العيادة...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showSpecialtyChips) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedSpecialty == null,
                                onClick = { onSelectSpecialty(null) },
                                label = { Text("كل التخصصات") },
                            )
                        }
                        items(state.specialties) { spec ->
                            FilterChip(
                                selected = state.selectedSpecialty == spec,
                                onClick = {
                                    onSelectSpecialty(if (state.selectedSpecialty == spec) null else spec)
                                },
                                label = { Text(spec) },
                            )
                        }
                    }
                }
            }

            if (filteredDoctors.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد عيادات مطابقة للبحث",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            } else {
                items(filteredDoctors, key = { it.id }) { doctor ->
                    val doctorSlots = state.slotsForDoctor(doctor.id)
                    DoctorDirectoryCard(
                        doctor = doctor,
                        availableCount = doctorSlots.count { it.status == SlotStatus.AVAILABLE },
                        bookedCount = doctorSlots.count { it.status == SlotStatus.BOOKED },
                        onBookClick = { onOpenDoctorBooking(doctor.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorDirectoryCard(
    doctor: ClinicDoctorEntity,
    availableCount: Int,
    bookedCount: Int,
    onBookClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onBookClick)
            .testTag("doctor_card_${doctor.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (doctor.clinicType == ClinicType.HOSPITAL) {
                                Icons.Filled.LocalHospital
                            } else {
                                Icons.Filled.MedicalServices
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = doctor.doctorName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = doctor.specialty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = doctor.clinicName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFEF9C3),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFCA8A04),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = doctor.rating.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF854D0E),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = doctor.locationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الكشف: ${doctor.feeEgp} ج.م",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (doctor.currentDelayMinutes > 0) {
                Surface(
                    color = DelayWarningBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "تنبيه: العيادة متأخرة حالياً ${doctor.currentDelayMinutes} دقيقة",
                        style = MaterialTheme.typography.labelMedium,
                        color = DelayWarningText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = SlotAvailableBg,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "متاح: $availableCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlotAvailableText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Surface(
                        color = SlotBookedBg,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "محجوز: $bookedCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlotBookedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }

                Button(
                    onClick = onBookClick,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("عرض المواعيد")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorBookingScreen(
    doctor: ClinicDoctorEntity,
    slots: List<AppointmentSlotEntity>,
    familyMembers: List<FamilyMemberEntity>,
    activeMember: FamilyMemberEntity?,
    onSelectFamilyMember: (String) -> Unit,
    onBookSlot: (slotId: String, member: FamilyMemberEntity) -> Unit,
    onCancelSlot: (slotId: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var slotPendingConfirm by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    val familyIds = remember(familyMembers) { familyMembers.map { it.id }.toSet() }
    val familyNames = remember(familyMembers) { familyMembers.map { it.fullName }.toSet() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("booking_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }
                    Text(
                        text = "الحجز اللحظي للمواعيد",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = doctor.doctorName, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${doctor.specialty} — ${doctor.clinicName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "العنوان: ${doctor.locationLabel}  •  سعر الكشف: ${doctor.feeEgp} ج.م",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مواعيد العمل: ${TimeUtils.formatMinutesAr(doctor.workStartMinutes)} إلى ${TimeUtils.formatMinutesAr(doctor.workEndMinutes)} (كل ${doctor.slotDurationMinutes} دقيقة)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (doctor.currentDelayMinutes > 0) {
                            Surface(
                                color = DelayWarningBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "تحديث: العيادة متأخرة ${doctor.currentDelayMinutes} دقيقة والأوقات الظاهرة معدّلة تلقائياً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DelayWarningText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "احجز الموعد لصالح:",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(familyMembers, key = { it.id }) { member ->
                            val selected = member.id == activeMember?.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFamilyMember(member.id) },
                                label = { Text("${member.fullName} (${member.relation.substringBefore(" ")})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LegendChip(
                        color = SlotAvailableBg,
                        borderColor = SlotAvailableBorder,
                        textColor = SlotAvailableText,
                        label = "متاح للحجز",
                    )
                    LegendChip(
                        color = SlotBookedBg,
                        borderColor = SlotBookedBorder,
                        textColor = SlotBookedText,
                        label = "محجوز (مغلق)",
                    )
                    LegendChip(
                        color = SlotCompletedBg,
                        borderColor = SlotCompletedBorder,
                        textColor = SlotCompletedText,
                        label = "تم الكشف",
                    )
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    slots.forEach { slot ->
                        val isMine = slot.status == SlotStatus.BOOKED &&
                            (slot.familyMemberId in familyIds || slot.patientName in familyNames)

                        val bgColor = when (slot.status) {
                            SlotStatus.AVAILABLE -> SlotAvailableBg
                            SlotStatus.BOOKED -> SlotBookedBg
                            else -> SlotCompletedBg
                        }
                        val borderColor = when (slot.status) {
                            SlotStatus.AVAILABLE -> SlotAvailableBorder
                            SlotStatus.BOOKED -> SlotBookedBorder
                            else -> SlotCompletedBorder
                        }
                        val textColor = when (slot.status) {
                            SlotStatus.AVAILABLE -> SlotAvailableText
                            SlotStatus.BOOKED -> SlotBookedText
                            else -> SlotCompletedText
                        }
                        val statusText = when {
                            slot.status == SlotStatus.AVAILABLE -> "متاح"
                            isMine -> "محجوز لك"
                            slot.status == SlotStatus.BOOKED -> "محجوز"
                            else -> "تم الكشف"
                        }

                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(min = 104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = slot.status == SlotStatus.AVAILABLE) {
                                    slotPendingConfirm = slot
                                }
                                .testTag("slot_tile_${slot.id}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = when (slot.status) {
                                        SlotStatus.AVAILABLE -> Icons.Filled.CheckCircle
                                        SlotStatus.BOOKED -> Icons.Filled.EventBusy
                                        else -> Icons.Filled.CheckCircle
                                    },
                                    contentDescription = statusText,
                                    tint = borderColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = TimeUtils.formatMinutesAr(slot.effectiveMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor,
                                )
                                if (isMine) {
                                    Text(
                                        text = "إلغاء الحجز",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlotBookedBorder,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onCancelSlot(slot.id) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingSlot = slotPendingConfirm
    if (pendingSlot != null && activeMember != null) {
        AlertDialog(
            onDismissRequest = { slotPendingConfirm = null },
            title = { Text("تأكيد حجز الموعد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الطبيب: ${doctor.doctorName}")
                    Text("العيادة: ${doctor.clinicName}")
                    Text("الموعد: ${TimeUtils.formatMinutesAr(pendingSlot.effectiveMinutes)}")
                    Text("اسم المريض: ${activeMember.fullName}")
                    Text(
                        text = "سيتم إغلاق الموعد فوراً باللون الأحمر عند جميع المستخدمين لمنع التضارب.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBookSlot(pendingSlot.id, activeMember)
                        slotPendingConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                    modifier = Modifier.testTag("confirm_booking_button"),
                ) {
                    Text("تأكيد الحجز الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { slotPendingConfirm = null }) {
                    Text("تراجع")
                }
            },
        )
    }
}

@Composable
private fun LegendChip(
    color: Color,
    borderColor: Color,
    textColor: Color,
    label: String,
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
