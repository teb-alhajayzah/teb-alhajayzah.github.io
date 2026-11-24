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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.local.MedicalLabEntity
import com.example.data.local.SlotStatus
import com.example.ui.PatientTab
import com.example.ui.TebUiState
import com.example.ui.bi
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
import com.example.util.SpecialtyCatalog
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
    onRescheduleSlot: (fromSlotId: String, toSlotId: String) -> Unit = { _, _ -> },
    onOpenOtpAccountDialog: () -> Unit,
    onChatWithClinic: (String) -> Unit = {},
    onCheckAppUpdate: () -> Unit = {},
    onChatWithAdmin: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTermsOfUse: () -> Unit = {},
    onShowMessage: (String) -> Unit,
) {
    var slotToReschedule by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    val context = LocalContext.current
    val isEn = state.isEn
    val activeMember = state.activeMember
    val familyIds = remember(state.familyMembers) { state.familyMembers.map { it.id }.toSet() }
    val familyNames = remember(state.familyMembers) { state.familyMembers.map { it.fullName }.toSet() }
    val platformTitle = bi(isEn, state.brandConfig.platformNameAr, state.brandConfig.platformNameEn)
    val villageLabel = bi(isEn, state.brandConfig.villageNameAr, state.brandConfig.villageNameEn)
    val hospitalLabel = bi(isEn, state.brandConfig.hospitalNameAr, state.brandConfig.hospitalNameEn)
    val taglineLabel = bi(isEn, state.brandConfig.taglineAr, state.brandConfig.taglineEn)
    val currencyLabel = bi(isEn, state.brandConfig.currencyLabelAr, state.brandConfig.currencyLabelEn)

    val myBookedSlots = remember(state.allSlots, familyIds, familyNames) {
        state.allSlots.filter { slot ->
            slot.status == SlotStatus.BOOKED &&
                (slot.familyMemberId in familyIds || slot.patientName in familyNames)
        }.sortedBy { it.effectiveMinutes }
    }

    val latestDelay = state.visibleNotificationsForCurrentRole().firstOrNull()
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedHomeQuery = homeSearchQuery.trim().lowercase()

    val quickMatchedSpecialties = remember(normalizedHomeQuery) {
        if (normalizedHomeQuery.isBlank()) {
            emptyList()
        } else {
            SpecialtyCatalog.all.filter { spec ->
                spec.nameAr.lowercase().contains(normalizedHomeQuery) ||
                    spec.nameEn.lowercase().contains(normalizedHomeQuery)
            }
        }
    }
    val quickMatchedDoctors = remember(normalizedHomeQuery, state.doctors) {
        if (normalizedHomeQuery.isBlank()) {
            emptyList()
        } else {
            state.doctors.filter { doc ->
                val allSpecs = (
                    SpecialtyCatalog.formatDoctorSpecialties(doc.specialty, doc.additionalSpecialties, isEn = false) +
                        " " +
                        SpecialtyCatalog.formatDoctorSpecialties(doc.specialty, doc.additionalSpecialties, isEn = true)
                    ).lowercase()
                doc.doctorName.lowercase().contains(normalizedHomeQuery) ||
                    doc.doctorNameEn.lowercase().contains(normalizedHomeQuery) ||
                    doc.clinicName.lowercase().contains(normalizedHomeQuery) ||
                    doc.clinicNameEn.lowercase().contains(normalizedHomeQuery) ||
                    allSpecs.contains(normalizedHomeQuery)
            }
        }
    }
    val quickMatchedLabs = remember(normalizedHomeQuery, state.labs) {
        if (normalizedHomeQuery.isBlank()) {
            emptyList()
        } else {
            state.labs.filter { lab ->
                lab.labName.lowercase().contains(normalizedHomeQuery) ||
                    lab.locationLabel.lowercase().contains(normalizedHomeQuery) ||
                    lab.phone.lowercase().contains(normalizedHomeQuery)
            }
        }
    }

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
                            contentDescription = hospitalLabel,
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
                            val displayName = activeMember?.fullName ?: state.activeAccount?.fullName
                            val greetingText = if (displayName.isNullOrBlank()) {
                                bi(isEn, "أهلاً بك في منصة $platformTitle", "Welcome to $platformTitle")
                            } else {
                                bi(isEn, "أهلاً بك، $displayName", "Welcome, $displayName")
                            }
                            Column(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = greetingText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                )
                                Text(
                                    text = taglineLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.94f),
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        text = bi(
                                            isEn,
                                            "احجز موعدك في ثوانٍ بدون انتظار • $hospitalLabel",
                                            "Book instantly with zero wait • $hospitalLabel",
                                        ),
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

            // شريط البحث السريع بالصفحة الرئيسية (الأطباء والتخصصات ومعامل التحاليل)
            item {
                OutlinedTextField(
                    value = homeSearchQuery,
                    onValueChange = { homeSearchQuery = it },
                    placeholder = {
                        Text(
                            text = bi(
                                isEn,
                                "ابحث باسم الطبيب، التخصص، أو معمل التحاليل...",
                                "Search by doctor name, specialty, or medical lab...",
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TealPrimary)
                    },
                    trailingIcon = {
                        if (homeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeSearchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = bi(isEn, "مسح البحث", "Clear search"),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_search_input"),
                )
            }

            if (normalizedHomeQuery.isNotBlank()) {
                if (quickMatchedSpecialties.isEmpty() && quickMatchedDoctors.isEmpty() && quickMatchedLabs.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "لا توجد نتائج مطابقة للبحث \"$homeSearchQuery\"",
                                    "No matching doctors, specialties, or labs for \"$homeSearchQuery\"",
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                        }
                    }
                } else {
                    if (quickMatchedSpecialties.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = bi(isEn, "تخصصات مطابقة (${quickMatchedSpecialties.size})", "Matching Specialties (${quickMatchedSpecialties.size})"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TealPrimary,
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(quickMatchedSpecialties, key = { it.nameAr }) { spec ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                onSelectSpecialty(spec.nameAr)
                                                onSelectTab(PatientTab.SPECIALTIES)
                                            },
                                            label = { Text(if (isEn) spec.nameEn else spec.nameAr) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (quickMatchedDoctors.isNotEmpty()) {
                        item {
                            Text(
                                text = bi(isEn, "الأطباء والعيادات (${quickMatchedDoctors.size})", "Doctors & Clinics (${quickMatchedDoctors.size})"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TealPrimary,
                            )
                        }
                        items(quickMatchedDoctors, key = { "qs_doc_${it.id}" }) { doctor ->
                            val doctorSlots = state.slotsForDoctor(doctor.id)
                            DoctorDirectoryCard(
                                doctor = doctor,
                                availableCount = doctorSlots.count { it.status == SlotStatus.AVAILABLE },
                                bookedCount = doctorSlots.count { it.status == SlotStatus.BOOKED },
                                isEn = isEn,
                                currencyLabel = currencyLabel,
                                canChatWithClinic = state.canPatientChatWith(doctor),
                                onChatWithClinic = { onChatWithClinic(doctor.id) },
                                onBookClick = { onOpenDoctorBooking(doctor.id) },
                            )
                        }
                    }
                    if (quickMatchedLabs.isNotEmpty()) {
                        item {
                            Text(
                                text = bi(isEn, "معامل التحاليل (${quickMatchedLabs.size})", "Medical Labs (${quickMatchedLabs.size})"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4F46E5),
                            )
                        }
                        items(quickMatchedLabs, key = { "qs_lab_${it.id}" }) { lab ->
                            HomeLabSearchCard(
                                lab = lab,
                                isEn = isEn,
                                onCallLab = {
                                    val digits = lab.phone.filter { it.isDigit() }
                                    if (digits.isNotBlank()) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                                        } catch (_: Exception) {
                                            onShowMessage(lab.phone)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (state.brandConfig.homeAnnouncementAr.isNotBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.MedicalInformation,
                                contentDescription = bi(isEn, "إعلان المنصة", "Platform Notice"),
                                tint = TealPrimary,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.brandConfig.homeAnnouncementAr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            if (state.activeAccount != null && !state.activeAccount.isPhoneVerified) {
                item {
                    Surface(
                        color = SlotBookedBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, SlotBookedBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenOtpAccountDialog),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = bi(isEn, "تأكيد OTP مطلوب", "OTP verification required"),
                                tint = SlotBookedBorder,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = bi(
                                    isEn,
                                    "حسابك بانتظار التفعيل برمز التحقق (OTP). اضغط هنا لإدخال الكود وتفعيل الحجز.",
                                    "Your account awaits OTP activation. Tap here to enter your 6-digit code and unlock booking.",
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SlotBookedText,
                            )
                        }
                    }
                }
            }

            if (latestDelay != null) {
                val isWarning = latestDelay.senderLabel.contains("تحذير") || latestDelay.message.contains("تحذير")
                val cardBg = if (isWarning) Color(0xFFFEF2F2) else DelayWarningBg
                val cardBorder = if (isWarning) Color(0xFFEF4444) else DelayWarningBorder
                val cardText = if (isWarning) Color(0xFF991B1B) else DelayWarningText
                item {
                    Surface(
                        color = cardBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = if (isWarning) "تحذير إداري" else "تنبيه تأخير",
                                tint = cardBorder,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isWarning || latestDelay.senderLabel.isNotBlank()) {
                                    Text(
                                        text = if (isWarning) bi(isEn, "⚠️ رسالة تحذير إداري من المدير العام", "⚠️ Admin Warning from Platform Management") else latestDelay.senderLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWarning) Color(0xFFDC2626) else TealPrimary,
                                    )
                                }
                                Text(
                                    text = latestDelay.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = cardText,
                                )
                                if (isWarning && state.activeAccount != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = onChatWithAdmin,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        Text(bi(isEn, "💬 الرد على الإدارة الآن", "Reply to Admin"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
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
                            text = bi(isEn, "الحجز والملف الطبي باسم:", "Booking & Medical File For:"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = onOpenAddFamilyDialog) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bi(isEn, "إضافة فرد عائلة", "Add Family"))
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
                    text = bi(isEn, "الأقسام الرئيسية", "Main Sections"),
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
                            title = bi(isEn, "العيادات الخاصة", "Private Clinics"),
                            subtitle = bi(isEn, "${state.privateClinics.size} عيادات متاحة", "${state.privateClinics.size} clinics"),
                            icon = Icons.Filled.MedicalServices,
                            containerColor = Color(0xFFE0F2F1),
                            accentColor = Color(0xFF0D7377),
                            testTag = "section_private_clinics",
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(PatientTab.PRIVATE_CLINICS) },
                        )
                        HomeSectionTile(
                            title = bi(isEn, "المستشفى التخصصي", "Specialty Hospital"),
                            subtitle = bi(isEn, "${state.hospitalClinics.size} أقسام طبية", "${state.hospitalClinics.size} depts"),
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
                            title = bi(isEn, "التخصصات الطبية", "Specialties"),
                            subtitle = bi(isEn, "${state.specialties.size} تخصصات", "${state.specialties.size} specialties"),
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
                            title = bi(isEn, "ملفي الطبي", "My Medical File"),
                            subtitle = bi(isEn, "الروشتات والتحاليل", "Rx & Lab Results"),
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
                    text = bi(isEn, "مواعيد العائلة المحجوزة اليوم (${myBookedSlots.size})", "Today's Family Bookings (${myBookedSlots.size})"),
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
                                text = bi(isEn, "لا توجد حجوزات قادمة اليوم", "No upcoming bookings today"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = bi(
                                    isEn,
                                    "اختر العيادات الخاصة أو المستشفى التخصصي لحجز موعدك بضغطة واحدة.",
                                    "Choose Private Clinics or the Specialty Hospital to book in one tap.",
                                ),
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
                            isEn = isEn,
                            onOpenClinic = { onOpenDoctorBooking(doctor.id) },
                            onReschedule = { slotToReschedule = slot },
                            onCancel = { onCancelSlot(slot.id) },
                        )
                    }
                }
            }

            item {
                Text(
                    text = bi(isEn, "أطباء متاحون الآن في $villageLabel", "Doctors Available Now in $villageLabel"),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (state.doctors.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp),
                            )
                            Text(
                                text = bi(isEn, "لا توجد عيادات أو أقسام مضافة حالياً", "No clinics or departments added yet"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = bi(
                                    isEn,
                                    "يمكن للإدارة إضافة العيادات والأطباء من لوحة إدارة المنصة لتظهر هنا فوراً.",
                                    "Administrators can add real clinics and doctors from the admin dashboard to appear here immediately.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(state.doctors.take(4), key = { it.id }) { doctor ->
                    val doctorSlots = state.slotsForDoctor(doctor.id)
                    DoctorDirectoryCard(
                        doctor = doctor,
                        availableCount = doctorSlots.count { it.status == SlotStatus.AVAILABLE },
                        bookedCount = doctorSlots.count { it.status == SlotStatus.BOOKED },
                        isEn = isEn,
                        currencyLabel = currencyLabel,
                        canChatWithClinic = state.canPatientChatWith(doctor),
                        onChatWithClinic = { onChatWithClinic(doctor.id) },
                        onBookClick = { onOpenDoctorBooking(doctor.id) },
                    )
                }
            }

            // تذييل التواصل وطلب انضمام عيادة + صندوق تثبيت التطبيق والتحقق من التحديثات + شريط الحقوق
            item {
                FooterContactAndRightsSection(
                    platformName = platformTitle,
                    supportPhone = state.brandConfig.supportPhone,
                    versionLabel = state.brandConfig.appVersionLabel,
                    isEn = isEn,
                    onWhatsAppClick = {
                        val digits = state.brandConfig.supportPhone.filter { it.isDigit() }
                        val waDigits = if (digits.startsWith("2")) digits else "2$digits"
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waDigits"))
                            )
                        } catch (_: Exception) {
                            onShowMessage(bi(isEn, "تعذر فتح واتساب على هذا الجهاز", "Could not open WhatsApp on this device"))
                        }
                    },
                    onCallClick = {
                        val digits = state.brandConfig.supportPhone.filter { it.isDigit() }
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
                            )
                        } catch (_: Exception) {
                            onShowMessage(bi(isEn, "رقم التواصل: ${state.brandConfig.supportPhone}", "Support phone: ${state.brandConfig.supportPhone}"))
                        }
                    },
                    onCheckAppUpdate = onCheckAppUpdate,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onOpenTermsOfUse = onOpenTermsOfUse,
                    onSharePlatform = {
                        val shareText = bi(
                            isEn,
                            "احجز موعدك الطبي وتابع ملفك الصحي وملفات أسرتك بسهولة عبر منصة ${state.brandConfig.platformNameAr}:\nhttps://teb-alhajayzah.github.io",
                            "Book clinic appointments and manage your family medical records easily on ${state.brandConfig.platformNameEn}:\nhttps://teb-alhajayzah.github.io",
                        )
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, platformTitle)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    bi(isEn, "مشاركة منصة طب الحجايزة", "Share Teb Al-Hajayzah"),
                                ),
                            )
                        } catch (_: Exception) {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/?text=${Uri.encode(shareText)}"),
                                    ),
                                )
                            } catch (_: Exception) {
                                onShowMessage(shareText)
                            }
                        }
                    },
                )
            }
        }
    }

    val homeRescheduleSlot = slotToReschedule
    if (homeRescheduleSlot != null) {
        val doctorOpenSlots = state.slotsForDoctor(homeRescheduleSlot.doctorId)
            .filter { it.status == SlotStatus.AVAILABLE }
        RescheduleSlotPickerDialog(
            currentSlot = homeRescheduleSlot,
            availableSlots = doctorOpenSlots,
            isPatient = true,
            isEn = isEn,
            onPickNewSlot = { targetSlotId ->
                onRescheduleSlot(homeRescheduleSlot.id, targetSlotId)
                slotToReschedule = null
            },
            onDismiss = { slotToReschedule = null },
        )
    }
}

@Composable
private fun HomeLabSearchCard(
    lab: MedicalLabEntity,
    isEn: Boolean,
    onCallLab: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, Color(0xFF4F46E5).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_search_lab_${lab.id}"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E7FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, tint = Color(0xFF4F46E5))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = lab.labName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (lab.locationLabel.isNotBlank()) {
                    Text(
                        text = lab.locationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = bi(isEn, "معمل تحاليل معتمد", "Certified Medical Lab"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4F46E5),
                )
            }
            if (lab.phone.isNotBlank()) {
                OutlinedButton(
                    onClick = onCallLab,
                    modifier = Modifier.height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "اتصال", "Call"))
                }
            }
        }
    }
}

@Composable
private fun FooterContactAndRightsSection(
    platformName: String,
    supportPhone: String,
    versionLabel: String,
    isEn: Boolean = false,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTermsOfUse: () -> Unit = {},
    onSharePlatform: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = bi(isEn, "التواصل وطلب انضمام عيادة جديدة", "Contact & New Clinic Registration"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = bi(
                        isEn,
                        "للدكاترة والمرضى: تواصل مباشرة لتسجيل العيادات أو الاستفسار",
                        "For doctors & patients: contact us directly to register clinics or get support",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onWhatsAppClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("footer_whatsapp_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(bi(isEn, "واتساب مباشر", "Direct WhatsApp"))
                    }
                    OutlinedButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("footer_call_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(supportPhone)
                    }
                }
                OutlinedButton(
                    onClick = onSharePlatform,
                    border = BorderStroke(1.dp, TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("footer_share_platform_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bi(
                            isEn,
                            "📤 مشاركة منصة طب الحجايزة مع الأهل",
                            "📤 Share Teb Al-Hajayzah Platform with Family",
                        ),
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // صندوق تطبيق طب الحجايزة (Android) - فوق سطر حقوق النشر مباشرة
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apk_download_card"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF059669)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "تطبيق طب الحجايزة (Android)", "Teb Al-Hajayzah App (Android)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "التحقق المباشر من التحديثات وتثبيت النسخة الرسمية",
                                "Direct update verification & official release installation",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = onCheckAppUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apk_direct_download_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "🔄 تثبيت التطبيق / التحقق من التحديثات ($versionLabel)",
                            "🔄 Install App / Check Updates ($versionLabel)",
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.testTag("footer_privacy_policy_button"),
            ) {
                Text(
                    text = bi(isEn, "📜 سياسة الخصوصية", "📜 Privacy Policy"),
                    style = MaterialTheme.typography.labelLarge,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(
                onClick = onOpenTermsOfUse,
                modifier = Modifier.testTag("footer_terms_button"),
            ) {
                Text(
                    text = bi(isEn, "⚖️ اتفاقية وشروط الاستخدام", "⚖️ Terms of Use"),
                    style = MaterialTheme.typography.labelLarge,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = bi(
                isEn,
                "جميع الحقوق محفوظة © 2026 منصة $platformName - لخدمة أهالي الحجايزة والقرى المجاورة",
                "All Rights Reserved © 2026 $platformName - Serving Al-Hajayzah & Nearby Villages",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
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
    isEn: Boolean = false,
    onOpenClinic: () -> Unit,
    onReschedule: () -> Unit = {},
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
                    val docName = if (isEn && doctor.doctorNameEn.isNotBlank()) doctor.doctorNameEn else doctor.doctorName
                    val clinName = if (isEn && doctor.clinicNameEn.isNotBlank()) doctor.clinicNameEn else doctor.clinicName
                    val specsLabel = SpecialtyCatalog.formatDoctorSpecialties(
                        doctor.specialty,
                        doctor.additionalSpecialties,
                        isEn,
                    )
                    Text(
                        text = docName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "$specsLabel • $clinName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            if (slot.delayMinutes > 0) {
                Text(
                    text = bi(
                        isEn,
                        "تم تعديل الموعد (+${slot.delayMinutes} دقيقة تأخير عن ${TimeUtils.formatMinutesAr(slot.startMinutes, false)})",
                        "Rescheduled (+${slot.delayMinutes} min delay from ${TimeUtils.formatMinutesAr(slot.startMinutes, true)})",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = DelayWarningBorder,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = bi(isEn, "المريض: ${slot.patientName}", "Patient: ${slot.patientName}"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("patient_cancel_slot_${slot.id}"),
                    ) {
                        Text(bi(isEn, "إلغاء", "Cancel"), color = SlotBookedBorder)
                    }
                    OutlinedButton(
                        onClick = onReschedule,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("patient_reschedule_slot_${slot.id}"),
                    ) {
                        Text(bi(isEn, "تعديل الموعد", "Reschedule"), style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onOpenClinic,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(bi(isEn, "العيادة", "Clinic"))
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
    onChatWithClinic: (String) -> Unit = {},
    onOpenDoctorBooking: (String) -> Unit,
) {
    val isEn = state.isEn
    val currencyLabel = bi(isEn, state.brandConfig.currencyLabelAr, state.brandConfig.currencyLabelEn)
    val filteredDoctors = remember(
        state.doctors,
        clinicTypeFilter,
        state.selectedSpecialty,
        state.searchQuery,
    ) {
        val selectedSpec = state.selectedSpecialty
        state.doctors.filter { doc ->
            val matchesType = clinicTypeFilter == null || doc.clinicType == clinicTypeFilter
            val matchesSpec = !showSpecialtyChips ||
                selectedSpec == null ||
                SpecialtyCatalog.doctorMatchesSpecialty(doc, selectedSpec)
            val q = state.searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                doc.doctorName.contains(q, ignoreCase = true) ||
                doc.doctorNameEn.contains(q, ignoreCase = true) ||
                doc.specialty.contains(q, ignoreCase = true) ||
                doc.additionalSpecialties.contains(q, ignoreCase = true) ||
                doc.clinicName.contains(q, ignoreCase = true) ||
                doc.clinicNameEn.contains(q, ignoreCase = true)
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
                    placeholder = { Text(bi(isEn, "ابحث باسم الدكتور أو التخصص أو العيادة...", "Search doctor, specialty, or clinic...")) },
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
                                label = { Text(bi(isEn, "كل التخصصات", "All Specialties")) },
                            )
                        }
                        items(SpecialtyCatalog.all, key = { it.id }) { specItem ->
                            val isSelected = state.selectedSpecialty == specItem.nameAr
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSelectSpecialty(if (isSelected) null else specItem.nameAr)
                                },
                                label = { Text(specItem.label(isEn)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White,
                                ),
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
                                text = bi(isEn, "لا توجد عيادات مطابقة للبحث", "No matching clinics found"),
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
                        isEn = isEn,
                        currencyLabel = currencyLabel,
                        canChatWithClinic = state.canPatientChatWith(doctor),
                        onChatWithClinic = { onChatWithClinic(doctor.id) },
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
    isEn: Boolean = false,
    currencyLabel: String = if (isEn) "EGP" else "ج.م",
    canChatWithClinic: Boolean = false,
    onChatWithClinic: (() -> Unit)? = null,
    onBookClick: () -> Unit,
) {
    val context = LocalContext.current
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
                        val docName = if (isEn && doctor.doctorNameEn.isNotBlank()) doctor.doctorNameEn else doctor.doctorName
                        val clinName = if (isEn && doctor.clinicNameEn.isNotBlank()) doctor.clinicNameEn else doctor.clinicName
                        val specsLabel = SpecialtyCatalog.formatDoctorSpecialties(
                            doctor.specialty,
                            doctor.additionalSpecialties,
                            isEn,
                        )
                        Text(
                            text = docName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = specsLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = clinName,
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
                        text = if (isEn && doctor.locationLabelEn.isNotBlank()) doctor.locationLabelEn else doctor.locationLabel,
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
                        text = bi(isEn, "الكشف: ${doctor.feeEgp} $currencyLabel", "Fee: ${doctor.feeEgp} $currencyLabel"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (doctor.bio.isNotBlank()) {
                Text(
                    text = doctor.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (doctor.mapsUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(doctor.mapsUrl))
                            )
                        } catch (_: Exception) {
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bi(isEn, "موقع العيادة على خرائط جوجل", "View Clinic on Google Maps"),
                        style = MaterialTheme.typography.labelMedium,
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
                        text = bi(
                            isEn,
                            "تنبيه: العيادة متأخرة حالياً ${doctor.currentDelayMinutes} دقيقة",
                            "Notice: Clinic delayed by ${doctor.currentDelayMinutes} min",
                        ),
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
                            text = bi(isEn, "متاح: $availableCount", "Open: $availableCount"),
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
                            text = bi(isEn, "محجوز: $bookedCount", "Booked: $bookedCount"),
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
                    Text(bi(isEn, "عرض المواعيد", "View Slots"))
                }
            }

            if (canChatWithClinic && onChatWithClinic != null) {
                OutlinedButton(
                    onClick = onChatWithClinic,
                    border = BorderStroke(1.2.dp, TealPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("doctor_card_chat_${doctor.id}"),
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bi(isEn, "💬 مراسلة العيادة", "💬 Message Clinic"),
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                    )
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
    isAccountVerified: Boolean = false,
    isBookingSubmitting: Boolean = false,
    canChatWithClinic: Boolean = false,
    isEn: Boolean = false,
    onSelectFamilyMember: (String) -> Unit,
    onRequireAccountForBooking: () -> Unit = {},
    onChatWithClinic: (() -> Unit)? = null,
    onBookSlot: (slotId: String, member: FamilyMemberEntity) -> Unit,
    onCancelSlot: (slotId: String) -> Unit,
    onRescheduleSlot: (fromSlotId: String, toSlotId: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var slotPendingConfirm by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    var slotPendingReschedule by remember { mutableStateOf<AppointmentSlotEntity?>(null) }
    val familyIds = remember(familyMembers) { familyMembers.map { it.id }.toSet() }
    val familyNames = remember(familyMembers) { familyMembers.map { it.fullName }.toSet() }
    val currencyLabel = if (isEn) "EGP" else "ج.م"

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
                            contentDescription = bi(isEn, "رجوع", "Back"),
                        )
                    }
                    Text(
                        text = bi(isEn, "الحجز اللحظي للمواعيد", "Real-Time Slot Booking"),
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
                        val docName = if (isEn && doctor.doctorNameEn.isNotBlank()) doctor.doctorNameEn else doctor.doctorName
                        val clinName = if (isEn && doctor.clinicNameEn.isNotBlank()) doctor.clinicNameEn else doctor.clinicName
                        val locLabel = if (isEn && doctor.locationLabelEn.isNotBlank()) doctor.locationLabelEn else doctor.locationLabel
                        val specsLabel = SpecialtyCatalog.formatDoctorSpecialties(
                            doctor.specialty,
                            doctor.additionalSpecialties,
                            isEn,
                        )
                        Text(text = docName, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "$specsLabel — $clinName",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "العنوان: $locLabel  •  سعر الكشف: ${doctor.feeEgp} $currencyLabel",
                                "Address: $locLabel  •  Fee: ${doctor.feeEgp} $currencyLabel",
                            ),
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
                                text = bi(
                                    isEn,
                                    "مواعيد العمل: ${TimeUtils.formatMinutesAr(doctor.workStartMinutes, false)} إلى ${TimeUtils.formatMinutesAr(doctor.workEndMinutes, false)} (كل ${doctor.slotDurationMinutes} دقيقة)",
                                    "Hours: ${TimeUtils.formatMinutesAr(doctor.workStartMinutes, true)} to ${TimeUtils.formatMinutesAr(doctor.workEndMinutes, true)} (every ${doctor.slotDurationMinutes} min)",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (doctor.bio.isNotBlank()) {
                            Text(
                                text = doctor.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (doctor.mapsUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(doctor.mapsUrl))
                                        )
                                    } catch (_: Exception) {
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(bi(isEn, "موقع العيادة على خرائط جوجل", "View Clinic on Google Maps"))
                            }
                        }
                        if (canChatWithClinic && onChatWithClinic != null) {
                            OutlinedButton(
                                onClick = onChatWithClinic,
                                border = BorderStroke(1.2.dp, TealPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("booking_chat_clinic_button"),
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = bi(isEn, "💬 مراسلة العيادة", "💬 Message Clinic"),
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
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
                                    text = bi(
                                        isEn,
                                        "تحديث: العيادة متأخرة ${doctor.currentDelayMinutes} دقيقة والأوقات الظاهرة معدّلة تلقائياً.",
                                        "Update: Clinic delayed by ${doctor.currentDelayMinutes} min; shown times are adjusted automatically.",
                                    ),
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
                        text = bi(isEn, "احجز الموعد لصالح:", "Book Appointment For:"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (familyMembers.isEmpty() || !isAccountVerified) {
                        Surface(
                            color = DelayWarningBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DelayWarningBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onRequireAccountForBooking),
                        ) {
                            Text(
                                text = if (familyMembers.isEmpty()) {
                                    bi(
                                        isEn,
                                        "تصفح المواعيد مجاناً — لإتمام الحجز اضغط هنا لتسجيل الدخول وتفعيل الحساب بكود (OTP).",
                                        "Browse slots freely — tap here to sign in and activate with OTP to complete booking.",
                                    )
                                } else {
                                    bi(
                                        isEn,
                                        "حسابك غير مفعّل بعد — اضغط هنا لإدخال كود التفعيل (OTP) وإتمام الحجز.",
                                        "Account not verified yet — tap here to enter your OTP code and unlock booking.",
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DelayWarningText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    if (familyMembers.isNotEmpty()) {
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
                        label = bi(isEn, "متاح للحجز", "Available"),
                    )
                    LegendChip(
                        color = SlotBookedBg,
                        borderColor = SlotBookedBorder,
                        textColor = SlotBookedText,
                        label = bi(isEn, "محجوز (مغلق)", "Booked"),
                    )
                    LegendChip(
                        color = SlotCompletedBg,
                        borderColor = SlotCompletedBorder,
                        textColor = SlotCompletedText,
                        label = bi(isEn, "تم الكشف", "Completed"),
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
                            slot.status == SlotStatus.AVAILABLE -> bi(isEn, "متاح", "Open")
                            isMine -> bi(isEn, "محجوز لك", "Yours")
                            slot.status == SlotStatus.BOOKED -> bi(isEn, "محجوز", "Booked")
                            else -> bi(isEn, "تم الكشف", "Done")
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
                                    if (!isAccountVerified || activeMember == null) {
                                        onRequireAccountForBooking()
                                    } else {
                                        slotPendingConfirm = slot
                                    }
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
                                    text = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn),
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = bi(isEn, "إلغاء", "Cancel"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlotBookedBorder,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onCancelSlot(slot.id) }
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                        )
                                        Text(
                                            text = bi(isEn, "تعديل", "Move"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { slotPendingReschedule = slot }
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
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

    val pendingSlot = slotPendingConfirm
    if (pendingSlot != null && activeMember != null) {
        AlertDialog(
            onDismissRequest = { slotPendingConfirm = null },
            title = { Text(bi(isEn, "تأكيد حجز الموعد", "Confirm Appointment Booking")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(bi(isEn, "الطبيب: ${doctor.doctorName}", "Doctor: ${doctor.doctorName}"))
                    Text(bi(isEn, "العيادة: ${doctor.clinicName}", "Clinic: ${doctor.clinicName}"))
                    Text(bi(isEn, "الموعد: ${TimeUtils.formatMinutesAr(pendingSlot.effectiveMinutes, isEn)}", "Time: ${TimeUtils.formatMinutesAr(pendingSlot.effectiveMinutes, isEn)}"))
                    Text(bi(isEn, "اسم المريض: ${activeMember.fullName}", "Patient: ${activeMember.fullName}"))
                    Text(
                        text = bi(
                            isEn,
                            "سيتم إغلاق الموعد فوراً باللون الأحمر عند جميع المستخدمين لمنع التضارب.",
                            "This slot will immediately lock for all users to prevent double booking.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isBookingSubmitting,
                    onClick = {
                        if (!isBookingSubmitting) {
                            onBookSlot(pendingSlot.id, activeMember)
                            slotPendingConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                    modifier = Modifier.testTag("confirm_booking_button"),
                ) {
                    if (isBookingSubmitting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (isBookingSubmitting) bi(isEn, "جاري الحجز...", "Booking...")
                        else bi(isEn, "تأكيد الحجز الآن", "Confirm Booking")
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { slotPendingConfirm = null }) {
                    Text(bi(isEn, "تراجع", "Back"))
                }
            },
        )
    }

    val movingSlot = slotPendingReschedule
    if (movingSlot != null) {
        val openAlternatives = slots.filter { it.status == SlotStatus.AVAILABLE }.sortedBy { it.startMinutes }
        RescheduleSlotPickerDialog(
            currentSlot = movingSlot,
            availableSlots = openAlternatives,
            isPatient = true,
            isEn = isEn,
            onPickNewSlot = { targetId ->
                onRescheduleSlot(movingSlot.id, targetId)
                slotPendingReschedule = null
            },
            onDismiss = { slotPendingReschedule = null },
        )
    }
}

@Composable
fun RescheduleSlotPickerDialog(
    currentSlot: AppointmentSlotEntity,
    availableSlots: List<AppointmentSlotEntity>,
    isPatient: Boolean,
    isEn: Boolean,
    onPickNewSlot: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bi(isEn, "تعديل موعد الكشف", "Reschedule Appointment")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = bi(
                        isEn,
                        "الموعد الحالي: ${TimeUtils.formatMinutesAr(currentSlot.effectiveMinutes, false)} (${currentSlot.patientName})",
                        "Current slot: ${TimeUtils.formatMinutesAr(currentSlot.effectiveMinutes, true)} (${currentSlot.patientName})",
                    ),
                    fontWeight = FontWeight.Bold,
                )
                if (isPatient) {
                    Text(
                        text = bi(
                            isEn,
                            "ملاحظة: يسمح بإلغاء أو تعديل الموعد للمريض قبل الكشف بساعتين على الأقل.",
                            "Note: Patients may cancel or reschedule at least 2 hours before the appointment.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (availableSlots.isEmpty()) {
                    Text(
                        text = bi(isEn, "لا توجد مواعيد أخرى متاحة حالياً في نفس العيادة.", "No other open slots available right now in this clinic."),
                        color = SlotBookedBorder,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(availableSlots, key = { it.id }) { target ->
                            OutlinedButton(
                                onClick = { onPickNewSlot(target.id) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                            ) {
                                Text(
                                    text = bi(
                                        isEn,
                                        "نقل إلى الساعة ${TimeUtils.formatMinutesAr(target.effectiveMinutes, false)}",
                                        "Move to ${TimeUtils.formatMinutesAr(target.effectiveMinutes, true)}",
                                    ),
                                    color = SlotAvailableText,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إغلاق", "Close"))
            }
        },
    )
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
