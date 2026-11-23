package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FamilyMemberEntity
import com.example.ui.bi
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.TealPrimary
import com.example.util.ChronicConditionsCatalog

data class AttachmentPreviewTarget(
    val path: String,
    val mimeType: String,
    val displayName: String,
    val title: String,
    val recordId: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientBioOverviewCard(
    member: FamilyMemberEntity,
    isEn: Boolean = false,
    isSharedFamilyMember: Boolean = false,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedKeys = remember(member.chronicConditions) {
        ChronicConditionsCatalog.parseKeys(member.chronicConditions)
    }
    val customConditions = remember(selectedKeys, member.otherChronicNotes) {
        val presetKeys = ChronicConditionsCatalog.presets.map { it.key.lowercase() }.toSet()
        buildList {
            selectedKeys.filter { it.lowercase() !in presetKeys }.forEach { add(it) }
            member.otherChronicNotes.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
        }.distinct()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.38f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("patient_bio_overview_card"),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = TealPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = bi(isEn, "البيانات الحيوية والأمراض المزمنة (نظرة عامة)", "Patient Bio & Chronic Conditions (Overview)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                        )
                        Text(
                            text = "${member.fullName} • ${member.relation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (member.nationalId.isNotBlank()) {
                            Text(
                                text = bi(isEn, "الرقم القومي: ${member.nationalId}", "National ID: ${member.nationalId}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (isSharedFamilyMember) {
                            Text(
                                text = bi(isEn, "👨‍👩‍👧‍👦 مرتبط عبر حساب الأسرة", "👨‍👩‍👧‍👦 Shared via Family Link"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("edit_patient_bio_button"),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi(isEn, "تعديل البيانات", "Edit Data"), maxLines = 1)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VitalStatBox(
                    label = bi(isEn, "السن", "Age"),
                    value = if (member.age > 0) bi(isEn, "${member.age} س", "${member.age}y") else "—",
                    accent = TealPrimary,
                    modifier = Modifier.weight(1f),
                )
                VitalStatBox(
                    label = bi(isEn, "الوزن", "Weight"),
                    value = if (member.weightKg > 0.0) {
                        val w = if (member.weightKg % 1.0 == 0.0) member.weightKg.toInt().toString() else member.weightKg.toString()
                        bi(isEn, "$w كجم", "$w kg")
                    } else "—",
                    accent = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                )
                VitalStatBox(
                    label = bi(isEn, "الطول", "Height"),
                    value = if (member.heightCm > 0.0) {
                        val h = if (member.heightCm % 1.0 == 0.0) member.heightCm.toInt().toString() else member.heightCm.toString()
                        bi(isEn, "$h سم", "$h cm")
                    } else "—",
                    accent = Color(0xFF4338CA),
                    modifier = Modifier.weight(1f),
                )
                VitalStatBox(
                    label = bi(isEn, "فصيلة الدم", "Blood"),
                    value = member.bloodType.ifBlank { "—" },
                    accent = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = bi(isEn, "الأمراض المزمنة:", "Chronic Conditions:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (selectedKeys.isEmpty() && customConditions.isEmpty()) {
                    Text(
                        text = bi(
                            isEn,
                            "لم تُسجّل أمراض مزمنة بعد — اضغط 'تعديل البيانات' للتحديث",
                            "No chronic conditions logged yet — tap 'Edit Data' to update",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        selectedKeys.forEach { key ->
                            val label = ChronicConditionsCatalog.labelForKey(key, isEn)
                            Surface(
                                color = DelayWarningBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DelayWarningBorder.copy(alpha = 0.6f)),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DelayWarningText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                        if (member.otherChronicNotes.trim().isNotEmpty()) {
                            Surface(
                                color = DelayWarningBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DelayWarningBorder.copy(alpha = 0.6f)),
                            ) {
                                Text(
                                    text = member.otherChronicNotes.trim(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DelayWarningText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (member.drugAllergies.isNotBlank()) {
                Surface(
                    color = SlotBookedBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlotBookedBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = SlotBookedBorder, modifier = Modifier.size(18.dp))
                        Text(
                            text = bi(isEn, "حساسية أدوية: ${member.drugAllergies}", "Drug Allergies: ${member.drugAllergies}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotBookedText,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPatientBioDialog(
    member: FamilyMemberEntity,
    isEn: Boolean = false,
    onSave: (
        age: Int,
        weightKg: Double,
        heightCm: Double,
        bloodType: String,
        chronicConditions: List<String>,
        drugAllergies: String,
        otherChronicNotes: String,
        nationalId: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var nationalIdText by remember(member.id) { mutableStateOf(member.nationalId) }
    var ageText by remember(member.id) { mutableStateOf(if (member.age > 0) member.age.toString() else "") }
    var weightText by remember(member.id) {
        mutableStateOf(
            if (member.weightKg > 0.0) {
                if (member.weightKg % 1.0 == 0.0) member.weightKg.toInt().toString() else member.weightKg.toString()
            } else ""
        )
    }
    var heightText by remember(member.id) {
        mutableStateOf(
            if (member.heightCm > 0.0) {
                if (member.heightCm % 1.0 == 0.0) member.heightCm.toInt().toString() else member.heightCm.toString()
            } else ""
        )
    }
    var bloodType by remember(member.id) { mutableStateOf(member.bloodType) }
    val selectedConditions = remember(member.id) {
        mutableStateListOf<String>().apply {
            addAll(ChronicConditionsCatalog.parseKeys(member.chronicConditions))
        }
    }
    var customConditionInput by remember { mutableStateOf("") }
    var drugAllergies by remember(member.id) { mutableStateOf(member.drugAllergies) }
    var otherChronicNotes by remember(member.id) { mutableStateOf(member.otherChronicNotes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "تعديل البيانات الحيوية والأمراض المزمنة", "Edit Bio Vitals & Chronic Conditions"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                OutlinedTextField(
                    value = nationalIdText,
                    onValueChange = { nationalIdText = it.filter { ch -> ch.isDigit() }.take(14) },
                    label = { Text(bi(isEn, "الرقم القومي (14 رقم - اختياري)", "National ID (14 digits - optional)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bio_national_id_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text(bi(isEn, "السن (سنوات)", "Age (yrs)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bio_age_input"),
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = { Text(bi(isEn, "الوزن (كجم)", "Weight (kg)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bio_weight_input"),
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = { Text(bi(isEn, "الطول (سم)", "Height (cm)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bio_height_input"),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Bloodtype, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Text(
                        text = bi(isEn, "فصيلة الدم:", "Blood Type:"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ChronicConditionsCatalog.bloodTypes.forEach { bt ->
                        val isSelected = bloodType == bt
                        FilterChip(
                            selected = isSelected,
                            onClick = { bloodType = if (isSelected) "" else bt },
                            label = { Text(bt, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.testTag("bio_blood_$bt"),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = DelayWarningBorder, modifier = Modifier.size(18.dp))
                    Text(
                        text = bi(isEn, "الأمراض المزمنة (اختيار سريع + إضافة):", "Chronic Conditions (Quick Select + Add):"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ChronicConditionsCatalog.presets.forEach { preset ->
                        val active = selectedConditions.any { it.equals(preset.key, ignoreCase = true) }
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) {
                                    selectedConditions.removeAll { it.equals(preset.key, ignoreCase = true) }
                                } else {
                                    selectedConditions.add(preset.key)
                                }
                            },
                            label = { Text(if (isEn) preset.labelEn else preset.labelAr) },
                            modifier = Modifier.testTag("bio_chronic_${preset.key}"),
                        )
                    }
                    selectedConditions
                        .filter { key -> ChronicConditionsCatalog.presets.none { it.key.equals(key, ignoreCase = true) } }
                        .forEach { customKey ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedConditions.remove(customKey) },
                                label = { Text("$customKey ✕") },
                            )
                        }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customConditionInput,
                        onValueChange = { customConditionInput = it },
                        label = { Text(bi(isEn, "إضافة مرض مزمن آخر", "Add other chronic condition")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            val clean = customConditionInput.trim()
                            if (clean.isNotEmpty() && selectedConditions.none { it.equals(clean, ignoreCase = true) }) {
                                selectedConditions.add(clean)
                                customConditionInput = ""
                            }
                        },
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = bi(isEn, "إضافة", "Add"))
                    }
                }

                OutlinedTextField(
                    value = drugAllergies,
                    onValueChange = { drugAllergies = it },
                    label = { Text(bi(isEn, "حساسية من أدوية معينة (مثال: البنسلين، الأسبرين...)", "Drug Allergies (e.g., Penicillin, NSAIDs...)")) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bio_drug_allergies_input"),
                )

                OutlinedTextField(
                    value = otherChronicNotes,
                    onValueChange = { otherChronicNotes = it },
                    label = { Text(bi(isEn, "أمراض مزمنة أو ملاحظات صحية أخرى (اختياري)", "Other chronic conditions / health notes (optional)")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(bi(isEn, "إلغاء", "Cancel"))
                    }
                    Button(
                        onClick = {
                            val finalConditions = selectedConditions.toMutableList()
                            customConditionInput.trim().takeIf { it.isNotEmpty() }?.let { extra ->
                                if (finalConditions.none { it.equals(extra, ignoreCase = true) }) {
                                    finalConditions.add(extra)
                                }
                            }
                            onSave(
                                ageText.toIntOrNull() ?: member.age,
                                weightText.toDoubleOrNull() ?: 0.0,
                                heightText.toDoubleOrNull() ?: 0.0,
                                bloodType,
                                finalConditions,
                                drugAllergies,
                                otherChronicNotes,
                                nationalIdText,
                            )
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("bio_save_button"),
                    ) {
                        Text(bi(isEn, "حفظ البيانات الحيوية", "Save Bio Data"))
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorPatientMedicalAlertBanner(
    member: FamilyMemberEntity?,
    patientName: String,
    isEn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val vitals = if (member != null) {
        buildList {
            if (member.age > 0) add(bi(isEn, "السن: ${member.age} س", "Age: ${member.age}y"))
            if (member.weightKg > 0.0) {
                val w = if (member.weightKg % 1.0 == 0.0) member.weightKg.toInt().toString() else member.weightKg.toString()
                add(bi(isEn, "الوزن: $w كجم", "Wt: $w kg"))
            }
            if (member.heightCm > 0.0) {
                val h = if (member.heightCm % 1.0 == 0.0) member.heightCm.toInt().toString() else member.heightCm.toString()
                add(bi(isEn, "الطول: $h سم", "Ht: $h cm"))
            }
            if (member.bloodType.isNotBlank()) add(bi(isEn, "الفصيلة: ${member.bloodType}", "Blood: ${member.bloodType}"))
        }.joinToString(" • ")
    } else ""

    val chronicText = if (member != null) {
        ChronicConditionsCatalog.formatLabels(
            rawKeys = member.chronicConditions,
            otherNotes = member.otherChronicNotes,
            isEn = isEn,
        )
    } else ""

    val allergyText = member?.drugAllergies?.trim().orEmpty()
    val hasAlert = chronicText.isNotBlank() || allergyText.isNotBlank()

    Surface(
        color = when {
            allergyText.isNotBlank() -> SlotBookedBg
            hasAlert -> DelayWarningBg
            else -> TealPrimary.copy(alpha = 0.08f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.5.dp,
            when {
                allergyText.isNotBlank() -> SlotBookedBorder
                hasAlert -> DelayWarningBorder
                else -> TealPrimary.copy(alpha = 0.35f)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("doctor_patient_medical_alert_banner"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (hasAlert) Icons.Filled.Warning else Icons.Filled.MonitorHeart,
                    contentDescription = null,
                    tint = when {
                        allergyText.isNotBlank() -> SlotBookedBorder
                        hasAlert -> DelayWarningBorder
                        else -> TealPrimary
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = bi(
                        isEn,
                        "تنبيه طبي وبيانات حيوية للمريض: $patientName",
                        "Patient Vitals & Medical Alert: $patientName",
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        allergyText.isNotBlank() -> SlotBookedText
                        hasAlert -> DelayWarningText
                        else -> TealPrimary
                    },
                )
            }
            if (vitals.isNotBlank()) {
                Text(
                    text = vitals,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (chronicText.isNotBlank()) {
                Text(
                    text = bi(isEn, "الأمراض المزمنة: $chronicText", "Chronic Conditions: $chronicText"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = DelayWarningText,
                )
            }
            if (allergyText.isNotBlank()) {
                Text(
                    text = bi(isEn, "🚨 حساسية أدوية: $allergyText", "🚨 Drug Allergies: $allergyText"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlotBookedText,
                )
            }
            if (vitals.isBlank() && !hasAlert) {
                Text(
                    text = bi(
                        isEn,
                        "لم يسجل المريض بيانات حيوية أو أمراض مزمنة أو حساسية أدوية حتى الآن.",
                        "No vitals, chronic conditions, or drug allergies recorded by patient yet.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DeleteFileConfirmationDialog(
    fileName: String,
    isEn: Boolean = false,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = Color(0xFFDC2626),
            )
        },
        title = {
            Text(
                text = bi(isEn, "تأكيد حذف الملف", "Confirm File Deletion"),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = bi(isEn, "هل أنت متأكد من حذف هذا الملف؟", "Are you sure you want to delete this file?"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (fileName.isNotBlank()) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                ),
                modifier = Modifier.testTag("confirm_delete_file_button"),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(bi(isEn, "حذف الملف", "Delete File"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إلغاء", "Cancel"))
            }
        },
    )
}
