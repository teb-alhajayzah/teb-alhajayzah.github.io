package com.example.ui.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.SlotStatus
import com.example.ui.TebUiState
import com.example.ui.bi
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.OnTealContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import com.example.util.SpecialtyCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class AnalyticsScope {
    CLINIC,
    PLATFORM,
}

private enum class AnalyticsChartFilter {
    ALL,
    SPECIALTY,
    AGE,
}

private data class AgeBracketStat(
    val id: String,
    val shortLabel: String,
    val labelAr: String,
    val labelEn: String,
    val count: Int,
    val percentage: Int,
    val color: Color,
)

private data class SpecialtyDemandStat(
    val specialtyAr: String,
    val specialtyEn: String,
    val count: Int,
    val percentage: Int,
    val color: Color,
)

private data class DoctorAnalyticsSnapshot(
    val monthLabelAr: String,
    val monthLabelEn: String,
    val totalUniquePatients: Int,
    val knownAgePatients: Int,
    val unknownAgePatients: Int,
    val topAgeBracket: AgeBracketStat?,
    val ageBrackets: List<AgeBracketStat>,
    val totalMonthlySpecialtyRequests: Int,
    val topSpecialties: List<SpecialtyDemandStat>,
)

private val SpecialtyPiePalette = listOf(
    Color(0xFF0D7377),
    Color(0xFF2563EB),
    Color(0xFF16A34A),
    Color(0xFFD97706),
    Color(0xFF7C3AED),
    Color(0xFFDC2626),
    Color(0xFF0891B2),
    Color(0xFF4F46E5),
)

private val AgeBarPalette = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFF0D7377),
    Color(0xFFF59E0B),
    Color(0xFF7C3AED),
)

private fun extractAgeFromEgyptianNationalId(nationalId: String, nowCal: Calendar): Int? {
    val digits = nationalId.filter { it.isDigit() }
    if (digits.length != 14) return null
    val centuryCode = digits[0]
    val century = when (centuryCode) {
        '2' -> 1900
        '3' -> 2000
        else -> return null
    }
    val year = century + (digits.substring(1, 3).toIntOrNull() ?: return null)
    val month = digits.substring(3, 5).toIntOrNull() ?: return null
    val day = digits.substring(5, 7).toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null

    var age = nowCal.get(Calendar.YEAR) - year
    val curMonth = nowCal.get(Calendar.MONTH) + 1
    val curDay = nowCal.get(Calendar.DAY_OF_MONTH)
    if (curMonth < month || (curMonth == month && curDay < day)) {
        age -= 1
    }
    return if (age in 0..120) age else null
}

private fun isInMonth(
    timestampMillis: Long,
    targetYear: Int,
    targetMonth: Int,
    fallbackCurrentMonth: Boolean,
    tz: TimeZone,
): Boolean {
    if (timestampMillis <= 0L) return fallbackCurrentMonth
    val cal = Calendar.getInstance(tz).apply { timeInMillis = timestampMillis }
    return cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth
}

private fun computeDoctorAnalytics(
    state: TebUiState,
    activeDoctor: ClinicDoctorEntity,
    scope: AnalyticsScope,
): DoctorAnalyticsSnapshot {
    val cairoTz = TimeZone.getTimeZone("Africa/Cairo")
    val nowCal = Calendar.getInstance(cairoTz)
    val currentYear = nowCal.get(Calendar.YEAR)
    val currentMonth = nowCal.get(Calendar.MONTH)
    val monthLabelAr = SimpleDateFormat("MMMM yyyy", Locale("ar", "EG")).apply {
        timeZone = cairoTz
    }.format(Date(nowCal.timeInMillis))
    val monthLabelEn = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
        timeZone = cairoTz
    }.format(Date(nowCal.timeInMillis))

    val membersById = state.allFamilyMembers.associateBy { it.id }
    val accountsById = state.allAccounts.associateBy { it.id }
    val doctorsById = state.doctors.associateBy { it.id }

    fun normalizePhone(phone: String): String = phone.filter { it.isDigit() }.let {
        if (it.startsWith("20") && it.length > 10) "0${it.drop(2)}" else it
    }

    val membersByPhone = state.allFamilyMembers
        .filter { it.phone.isNotBlank() }
        .associateBy { normalizePhone(it.phone) }
    val accountsByPhone = state.allAccounts
        .filter { it.phone.isNotBlank() }
        .associateBy { normalizePhone(it.phone) }

    fun resolvePatientAge(
        familyMemberId: String,
        patientPhone: String,
    ): Int? {
        val member = membersById[familyMemberId]
            ?: membersByPhone[normalizePhone(patientPhone)]
        if (member != null) {
            if (member.age in 1..120) return member.age
            extractAgeFromEgyptianNationalId(member.nationalId, nowCal)?.let { return it }
            val parentAcc = accountsById[member.accountId]
            if (parentAcc != null) {
                extractAgeFromEgyptianNationalId(parentAcc.nationalId, nowCal)?.let { return it }
            }
        }
        val directAcc = accountsByPhone[normalizePhone(patientPhone)]
        if (directAcc != null) {
            extractAgeFromEgyptianNationalId(directAcc.nationalId, nowCal)?.let { return it }
        }
        return null
    }

    val scopedSlots = state.allSlots.filter { slot ->
        slot.status != SlotStatus.AVAILABLE &&
            (scope == AnalyticsScope.PLATFORM || slot.doctorId == activeDoctor.id)
    }
    val scopedRecords = state.medicalRecords.filter { rec ->
        rec.recordType != MedicalRecordType.LAB_RESULT &&
            (scope == AnalyticsScope.PLATFORM || rec.doctorId == activeDoctor.id)
    }

    val patientAgesByKey = linkedMapOf<String, Int?>()
    fun registerPatient(familyMemberId: String, phone: String, name: String) {
        val cleanPhone = normalizePhone(phone)
        val key = when {
            familyMemberId.isNotBlank() -> "fm:$familyMemberId"
            cleanPhone.isNotBlank() -> "ph:$cleanPhone"
            name.isNotBlank() -> "nm:${name.trim().lowercase()}"
            else -> return
        }
        val resolved = resolvePatientAge(familyMemberId, phone)
        val existing = patientAgesByKey[key]
        if (existing == null || resolved != null) {
            patientAgesByKey[key] = resolved ?: existing
        }
    }

    scopedSlots.forEach { slot ->
        registerPatient(slot.familyMemberId, slot.patientPhone, slot.patientName)
    }
    scopedRecords.forEach { rec ->
        registerPatient(rec.familyMemberId, rec.patientPhone, rec.patientName)
    }
    if (scope == AnalyticsScope.PLATFORM) {
        state.allFamilyMembers.forEach { member ->
            registerPatient(member.id, member.phone, member.fullName)
        }
    }

    var c0to12 = 0
    var c13to25 = 0
    var c26to45 = 0
    var c46to60 = 0
    var c60Plus = 0
    var unknownAge = 0

    patientAgesByKey.values.forEach { age ->
        when {
            age == null -> unknownAge++
            age <= 12 -> c0to12++
            age <= 25 -> c13to25++
            age <= 45 -> c26to45++
            age <= 60 -> c46to60++
            else -> c60Plus++
        }
    }

    val knownTotal = c0to12 + c13to25 + c26to45 + c46to60 + c60Plus
    fun pct(count: Int, total: Int): Int =
        if (total <= 0) 0 else ((count * 100f) / total).toInt().coerceIn(0, 100)

    val ageBrackets = listOf(
        AgeBracketStat("0_12", "0-12", "أطفال (0-12 سنة)", "Children (0-12)", c0to12, pct(c0to12, knownTotal), AgeBarPalette[0]),
        AgeBracketStat("13_25", "13-25", "شباب (13-25 سنة)", "Youth (13-25)", c13to25, pct(c13to25, knownTotal), AgeBarPalette[1]),
        AgeBracketStat("26_45", "26-45", "بالغون (26-45 سنة)", "Adults (26-45)", c26to45, pct(c26to45, knownTotal), AgeBarPalette[2]),
        AgeBracketStat("46_60", "46-60", "متوسط العمر (46-60 سنة)", "Middle-Aged (46-60)", c46to60, pct(c46to60, knownTotal), AgeBarPalette[3]),
        AgeBracketStat("60_plus", "+60", "كبار السن (+60 سنة)", "Seniors (+60)", c60Plus, pct(c60Plus, knownTotal), AgeBarPalette[4]),
    )
    val topBracket = ageBrackets.filter { it.count > 0 }.maxByOrNull { it.count }

    // Monthly Specialty Demand
    val specialtyCounts = linkedMapOf<String, Int>()
    fun recordSpecialtyDemand(rawSpecialty: String) {
        val clean = SpecialtyCatalog.findByNameOrAlias(rawSpecialty)?.nameAr ?: rawSpecialty.trim()
        if (clean.isNotBlank()) {
            specialtyCounts[clean] = (specialtyCounts[clean] ?: 0) + 1
        }
    }

    scopedSlots.forEach { slot ->
        val slotTs = when {
            slot.bookedAtMillis > 0L -> slot.bookedAtMillis
            slot.appointmentTimestamp > 0L -> slot.appointmentTimestamp
            else -> 0L
        }
        if (isInMonth(slotTs, currentYear, currentMonth, fallbackCurrentMonth = true, tz = cairoTz)) {
            val doc = doctorsById[slot.doctorId]
            if (doc != null) {
                val specs = SpecialtyCatalog.allDoctorSpecialties(doc.specialty, doc.additionalSpecialties)
                if (specs.isNotEmpty()) {
                    recordSpecialtyDemand(specs.first())
                } else {
                    recordSpecialtyDemand(doc.specialty)
                }
            }
        }
    }

    scopedRecords.forEach { rec ->
        if (isInMonth(rec.createdAtMillis, currentYear, currentMonth, fallbackCurrentMonth = false, tz = cairoTz)) {
            val docSpec = doctorsById[rec.doctorId]?.specialty.orEmpty()
            recordSpecialtyDemand(rec.specialty.ifBlank { docSpec })
        }
    }

    val totalSpecialtyRequests = specialtyCounts.values.sum()
    val topSpecialties = specialtyCounts.entries
        .sortedByDescending { it.value }
        .take(8)
        .mapIndexed { idx, entry ->
            val catItem = SpecialtyCatalog.findByNameOrAlias(entry.key)
            SpecialtyDemandStat(
                specialtyAr = catItem?.nameAr ?: entry.key,
                specialtyEn = catItem?.nameEn ?: entry.key,
                count = entry.value,
                percentage = pct(entry.value, totalSpecialtyRequests),
                color = SpecialtyPiePalette[idx % SpecialtyPiePalette.size],
            )
        }

    return DoctorAnalyticsSnapshot(
        monthLabelAr = monthLabelAr,
        monthLabelEn = monthLabelEn,
        totalUniquePatients = patientAgesByKey.size,
        knownAgePatients = knownTotal,
        unknownAgePatients = unknownAge,
        topAgeBracket = topBracket,
        ageBrackets = ageBrackets,
        totalMonthlySpecialtyRequests = totalSpecialtyRequests,
        topSpecialties = topSpecialties,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorAnalyticsSection(
    state: TebUiState,
    activeDoctor: ClinicDoctorEntity,
    isEn: Boolean,
) {
    var selectedScope by remember { mutableStateOf(AnalyticsScope.PLATFORM) }
    var selectedChartFilter by remember { mutableStateOf(AnalyticsChartFilter.ALL) }

    val snapshot = remember(
        selectedScope,
        activeDoctor.id,
        state.allSlots,
        state.medicalRecords,
        state.allFamilyMembers,
        state.allAccounts,
        state.doctors,
    ) {
        computeDoctorAnalytics(state, activeDoctor, selectedScope)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_analytics_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Controls & KPI Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.padding(8.dp).size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "لوحة الإحصائيات البيانية", "Clinic Analytics & Insights"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "بيانات حية لشهر ${snapshot.monthLabelAr}",
                                "Live metrics for ${snapshot.monthLabelEn}",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Scope Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedScope == AnalyticsScope.PLATFORM,
                        onClick = { selectedScope = AnalyticsScope.PLATFORM },
                        label = {
                            Text(
                                text = bi(isEn, "كامل المنصة", "All Clinics"),
                                maxLines = 1,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = OnTealContainer,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("analytics_scope_platform"),
                    )
                    FilterChip(
                        selected = selectedScope == AnalyticsScope.CLINIC,
                        onClick = { selectedScope = AnalyticsScope.CLINIC },
                        label = {
                            Text(
                                text = bi(isEn, "عيادتي الحالية", "My Clinic"),
                                maxLines = 1,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealContainer,
                            selectedLabelColor = OnTealContainer,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("analytics_scope_clinic"),
                    )
                }

                // Chart Filter Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedChartFilter == AnalyticsChartFilter.ALL,
                        onClick = { selectedChartFilter = AnalyticsChartFilter.ALL },
                        label = { Text(bi(isEn, "عرض الكل", "Show All")) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("analytics_filter_all"),
                    )
                    FilterChip(
                        selected = selectedChartFilter == AnalyticsChartFilter.SPECIALTY,
                        onClick = { selectedChartFilter = AnalyticsChartFilter.SPECIALTY },
                        label = { Text(bi(isEn, "تخصصات الشهر (Pie)", "Specialties (Pie)")) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("analytics_filter_specialty"),
                    )
                    FilterChip(
                        selected = selectedChartFilter == AnalyticsChartFilter.AGE,
                        onClick = { selectedChartFilter = AnalyticsChartFilter.AGE },
                        label = { Text(bi(isEn, "الفئات العمرية (Bar)", "Age Groups (Bar)")) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("analytics_filter_age"),
                    )
                }

                // KPI Summary Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnalyticsKpiCard(
                        icon = Icons.Filled.Groups,
                        value = snapshot.totalUniquePatients.toString(),
                        label = bi(isEn, "إجمالي المرضى", "Total Patients"),
                        containerColor = TealContainer,
                        contentColor = OnTealContainer,
                        modifier = Modifier.weight(1f),
                    )
                    AnalyticsKpiCard(
                        icon = Icons.Filled.LocalHospital,
                        value = snapshot.totalMonthlySpecialtyRequests.toString(),
                        label = bi(isEn, "حجوزات الشهر", "Month Bookings"),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    AnalyticsKpiCard(
                        icon = Icons.Filled.PieChart,
                        value = snapshot.topAgeBracket?.shortLabel ?: "—",
                        label = bi(isEn, "الفئة الأكثر زيارة", "Top Age Group"),
                        containerColor = MintContainer,
                        contentColor = OnMintContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 1. Specialty Distribution Pie Chart Card (Current Month)
        if (selectedChartFilter == AnalyticsChartFilter.ALL || selectedChartFilter == AnalyticsChartFilter.SPECIALTY) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_specialty_chart_card"),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = bi(isEn, "توزيع حجوزات المرضى حسب التخصصات الطبية (الشهر الحالي)", "Monthly Patient Bookings by Medical Specialty (Pie Chart)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "مخطط دائري لشهر ${snapshot.monthLabelAr} (${snapshot.totalMonthlySpecialtyRequests} حجز/كشف)",
                                "Pie breakdown for ${snapshot.monthLabelEn} (${snapshot.totalMonthlySpecialtyRequests} bookings)",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (snapshot.topSpecialties.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "لا توجد حجوزات أو زيارات مسجلة خلال الشهر الحالي في هذا النطاق بعد.",
                                    "No bookings or visits recorded for the current month in this scope yet.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val totalReq = snapshot.totalMonthlySpecialtyRequests.coerceAtLeast(1)
                            val outlineColor = MaterialTheme.colorScheme.surface
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.size(180.dp)) {
                                    var startAngle = -90f
                                    val pieSize = Size(size.width, size.height)
                                    snapshot.topSpecialties.forEach { spec ->
                                        val sweep = (spec.count.toFloat() / totalReq.toFloat()) * 360f
                                        drawArc(
                                            color = spec.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = true,
                                            topLeft = Offset.Zero,
                                            size = pieSize,
                                        )
                                        if (snapshot.topSpecialties.size > 1) {
                                            drawArc(
                                                color = outlineColor,
                                                startAngle = startAngle,
                                                sweepAngle = sweep,
                                                useCenter = true,
                                                topLeft = Offset.Zero,
                                                size = pieSize,
                                                style = Stroke(width = 2.dp.toPx()),
                                            )
                                        }
                                        startAngle += sweep
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            snapshot.topSpecialties.forEachIndexed { idx, spec ->
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
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(spec.color),
                                        )
                                        Text(
                                            text = "${idx + 1}. ${if (isEn) spec.specialtyEn else spec.specialtyAr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bi(
                                            isEn,
                                            "${spec.count} حجز (${spec.percentage}%)",
                                            "${spec.count} (${spec.percentage}%)",
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = spec.color,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Patient Age Distribution Bar Chart Card (0-12, 13-25, 26-45, 46-60, +60)
        if (selectedChartFilter == AnalyticsChartFilter.ALL || selectedChartFilter == AnalyticsChartFilter.AGE) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_age_chart_card"),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = bi(isEn, "توزيع المرضى حسب الفئات العمرية (Bar Chart)", "Patients by Age Groups (Bar Chart)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "الفئات: 0-12، 13-25، 26-45، 46-60، +60 (${snapshot.knownAgePatients} مريضاً)",
                                "Brackets: 0-12, 13-25, 26-45, 46-60, +60 (${snapshot.knownAgePatients} patients)",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (snapshot.knownAgePatients == 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "لا تتوفر أعمار مسجلة لمرضى هذا النطاق بعد. ستظهر الأعمدة البيانية تلقائياً بمجرد تسجيل حجوزات بأعمار المرضى.",
                                    "No patient ages recorded in this scope yet. The bar chart updates once patients book with ages.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    } else {
                        val maxCount = (snapshot.ageBrackets.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                        val gridColor = MaterialTheme.colorScheme.outlineVariant
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                snapshot.ageBrackets.forEach { bracket ->
                                    Text(
                                        text = bracket.count.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = bracket.color,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                            ) {
                                val chartHeight = size.height
                                val slotCount = snapshot.ageBrackets.size.coerceAtLeast(1)
                                val colWidth = size.width / slotCount.toFloat()
                                val barWidth = (colWidth * 0.48f).coerceAtMost(42.dp.toPx())
                                val cornerPx = 8.dp.toPx()

                                // Subtle horizontal grid lines
                                for (step in 1..4) {
                                    val y = chartHeight - (chartHeight * (step / 4f))
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.6f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }
                                // Baseline
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, chartHeight),
                                    end = Offset(size.width, chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                )

                                snapshot.ageBrackets.forEachIndexed { idx, bracket ->
                                    val ratio = bracket.count.toFloat() / maxCount.toFloat()
                                    val barHeight = if (bracket.count > 0) {
                                        (chartHeight * 0.92f * ratio).coerceAtLeast(10.dp.toPx())
                                    } else {
                                        4.dp.toPx()
                                    }
                                    val leftX = (idx * colWidth) + (colWidth - barWidth) / 2f
                                    val topY = chartHeight - barHeight
                                    drawRoundRect(
                                        color = if (bracket.count > 0) bracket.color else gridColor.copy(alpha = 0.45f),
                                        topLeft = Offset(leftX, topY),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                snapshot.ageBrackets.forEach { bracket ->
                                    Text(
                                        text = bracket.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            snapshot.ageBrackets.forEach { bracket ->
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
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(bracket.color),
                                        )
                                        Text(
                                            text = if (isEn) bracket.labelEn else bracket.labelAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${bracket.count} (${bracket.percentage}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bracket.color,
                                    )
                                }
                            }
                        }
                    }

                    if (snapshot.unknownAgePatients > 0) {
                        Text(
                            text = bi(
                                isEn,
                                "ملاحظة: يوجد ${snapshot.unknownAgePatients} مريضاً بدون عمر مسجل حالياً.",
                                "Note: ${snapshot.unknownAgePatients} patient(s) currently have no recorded age.",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
