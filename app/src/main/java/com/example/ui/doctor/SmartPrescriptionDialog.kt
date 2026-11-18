package com.example.ui.doctor

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.config.PlatformBrandConfig
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.medications.EgyptianMedication
import com.example.data.medications.MedicationCatalog
import com.example.ui.bi
import com.example.ui.components.DoctorPatientMedicalAlertBanner
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.TealPrimary
import com.example.util.BilingualMedicalTranslator
import com.example.util.ImageStorageHelper
import com.example.util.PrescriptionPrintHelper
import com.example.util.SpecialtyCatalog
import com.example.util.TimeUtils
import java.io.File

private data class SelectedRxItem(
    val uid: String,
    val tradeName: String,
    val strength: String,
    val dosage: String,
    val dosageEn: String,
)

@Composable
fun SmartPrescriptionDialog(
    slot: AppointmentSlotEntity,
    doctor: ClinicDoctorEntity,
    brandConfig: PlatformBrandConfig,
    medications: List<EgyptianMedication>,
    isLiveRemoteMeds: Boolean,
    favoriteMedIds: Set<String>,
    patientMember: FamilyMemberEntity? = null,
    isEn: Boolean = false,
    onToggleFavorite: (String) -> Unit,
    onSaveAndComplete: (
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
        diagnosisEn: String,
        medicationsEn: String,
        notesEn: String,
        aiReportAr: String,
        aiReportEn: String,
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var diagnosis by remember { mutableStateOf("") }
    var diagnosisEn by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var notesEn by remember { mutableStateOf("") }
    var aiReportAr by remember { mutableStateOf("") }
    var aiReportEn by remember { mutableStateOf("") }
    var showBilingualSection by remember { mutableStateOf(false) }
    var printAsEnglish by remember { mutableStateOf(isEn) }

    var searchQuery by remember { mutableStateOf("") }
    // 0 = تخصص العيادة, 1 = المفضلة, 2 = الكل
    var medFilterMode by remember { mutableIntStateOf(0) }
    var selectedSpecialtyChip by remember { mutableStateOf<String?>(null) }
    var customMedName by remember { mutableStateOf("") }
    var customMedDosage by remember { mutableStateOf(bi(isEn, "قرص كل 12 ساعة", "1 tab every 12h")) }
    var attachmentPath by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val selectedItems = remember { mutableStateListOf<SelectedRxItem>() }

    val doctorMedCatalogKeys = remember(doctor.specialty, doctor.additionalSpecialties) {
        SpecialtyCatalog.medicationCatalogKeysForDoctor(doctor)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistUriToInternalStorage(context, uri)
            if (saved != null) {
                attachmentPath = saved
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة صورة الروشتة", "Could not read prescription image"))
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = tempCameraFile
        if (success && file != null) {
            val saved = ImageStorageHelper.persistFileToInternalStorage(context, file)
            if (saved != null) {
                attachmentPath = saved
            } else {
                onShowMessage(bi(isEn, "تعذر حفظ صورة الكاميرا", "Could not save camera photo"))
            }
        }
    }

    val filteredMedications = remember(
        medications,
        searchQuery,
        medFilterMode,
        selectedSpecialtyChip,
        favoriteMedIds,
        doctorMedCatalogKeys,
    ) {
        val q = searchQuery.trim()
        medications.filter { med ->
            val matchesMode = when (medFilterMode) {
                0 -> doctorMedCatalogKeys.isEmpty() || med.specialty in doctorMedCatalogKeys
                1 -> med.id in favoriteMedIds
                else -> selectedSpecialtyChip == null || med.specialty == selectedSpecialtyChip
            }
            val matchesSearch = q.isEmpty() ||
                med.tradeName.contains(q, ignoreCase = true) ||
                med.activeIngredient.contains(q, ignoreCase = true) ||
                med.strength.contains(q, ignoreCase = true) ||
                med.specialty.contains(q, ignoreCase = true)
            if (q.isNotEmpty() && medFilterMode != 1) matchesSearch else (matchesMode && matchesSearch)
        }
    }

    fun buildMedicationsText(): String {
        return selectedItems.joinToString("\n") { item ->
            val strengthPart = if (item.strength.isNotBlank()) " ${item.strength}" else ""
            val arDose = BilingualMedicalTranslator.toArabicDosage(item.dosage.ifBlank { "حسب إرشادات الطبيب" })
            "• ${item.tradeName}$strengthPart — $arDose"
        }
    }

    fun buildMedicationsTextEn(): String {
        return selectedItems.joinToString("\n") { item ->
            val strengthPart = if (item.strength.isNotBlank()) " ${item.strength}" else ""
            val rawLine = "• ${item.tradeName}$strengthPart — ${item.dosageEn.ifBlank { BilingualMedicalTranslator.toEnglishDosage(item.dosage) }}"
            BilingualMedicalTranslator.translateMedicationsLineToEnglish(rawLine)
        }
    }

    fun generateBilingualDraft() {
        val medsAr = buildMedicationsText()
        val bundle = BilingualMedicalTranslator.buildBilingualBundle(
            patientName = slot.patientName,
            doctorName = doctor.doctorName,
            specialty = doctor.specialty,
            diagnosisInput = diagnosis.ifBlank { diagnosisEn },
            medicationsInput = medsAr,
            notesInput = notes.ifBlank { notesEn },
            existingDiagnosisEn = diagnosisEn,
            existingMedicationsEn = "",
            existingNotesEn = notesEn,
        )
        if (diagnosis.isBlank()) diagnosis = bundle.diagnosisAr
        diagnosisEn = bundle.diagnosisEn
        if (notes.isBlank() && bundle.notesAr.isNotBlank()) notes = bundle.notesAr
        if (notesEn.isBlank() && bundle.notesEn.isNotBlank()) notesEn = bundle.notesEn
        aiReportAr = bundle.aiReportAr
        aiReportEn = bundle.aiReportEn
        selectedItems.forEachIndexed { idx, item ->
            val translatedEn = BilingualMedicalTranslator.toEnglishDosage(item.dosage)
            selectedItems[idx] = item.copy(dosageEn = translatedEn)
        }
        showBilingualSection = true
        onShowMessage(
            bi(
                isEn,
                "تم توليد الروشتة والتقرير الطبي باللغتين (AR + EN) بنجاح — يمكنك مراجعتهما قبل الإرسال",
                "Bilingual AR + EN Rx and Medical Report generated — review or edit before sending",
            )
        )
    }

    fun addMedicationToRx(med: EgyptianMedication) {
        val existingIdx = selectedItems.indexOfFirst { it.uid == med.id }
        if (existingIdx >= 0) {
            onShowMessage(bi(isEn, "الدواء مضاف بالفعل إلى الروشتة", "Medication already added"))
            return
        }
        selectedItems.add(
            SelectedRxItem(
                uid = med.id,
                tradeName = med.tradeName,
                strength = med.strength,
                dosage = med.defaultDosage,
                dosageEn = BilingualMedicalTranslator.toEnglishDosage(med.defaultDosage),
            )
        )
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "الروشتة الإلكترونية الذكية (AR / EN)", "Smart Bilingual Prescription (AR / EN)"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "المريض: ${slot.patientName} • ${TimeUtils.formatMinutesAr(slot.effectiveMinutes, false)}",
                                "Patient: ${slot.patientName} • ${TimeUtils.formatMinutesAr(slot.effectiveMinutes, true)}",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                // Medical Alert Banner (Bio Vitals, Chronic Conditions & Drug Allergies)
                DoctorPatientMedicalAlertBanner(
                    member = patientMember,
                    patientName = slot.patientName,
                    isEn = isEn,
                )

                // Egyptian Medications DB Status Badge
                Surface(
                    color = if (isLiveRemoteMeds) SlotAvailableBg else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isLiveRemoteMeds) SlotAvailableBorder else TealPrimary.copy(alpha = 0.35f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiveRemoteMeds) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (isLiveRemoteMeds) SlotAvailableBorder else TealPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (isLiveRemoteMeds) {
                                bi(isEn, "متصل بقاعدة الأدوية المصرية (${medications.size} دواء)", "Connected to Egyptian Meds API (${medications.size} drugs)")
                            } else {
                                bi(isEn, "قاعدة الأدوية المصرية المدمجة Offline (${medications.size} دواء مصنف)", "Built-in Egyptian Meds Database (${medications.size} drugs)")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLiveRemoteMeds) SlotAvailableText else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Diagnosis Field (Arabic or English)
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text(bi(isEn, "التشخيص الطبي (بالعربية أو الإنجليزية) *", "Medical Diagnosis (AR or EN) *")) },
                    placeholder = { Text(bi(isEn, "مثال: التهاب بالحلق / Acute Tonsillitis / متابعة حمل...", "e.g., Acute pharyngitis / GERD...")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rx_diagnosis_input"),
                )

                // Search Egyptian Medications
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(bi(isEn, "بحث في قاعدة الأدوية المصرية", "Search Egyptian Medications")) },
                    placeholder = { Text(bi(isEn, "ابحث بالاسم التجاري أو المادة الفعالة أو التركيز...", "Search trade name, ingredient, or strength...")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = bi(isEn, "مسح البحث", "Clear search"))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rx_med_search_input"),
                )

                // Mode Tabs: Clinic Specialty / Favorites / All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = medFilterMode == 0,
                        onClick = { medFilterMode = 0 },
                        label = { Text(bi(isEn, "تخصص العيادة", "Specialty")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = medFilterMode == 1,
                        onClick = { medFilterMode = 1 },
                        label = { Text(bi(isEn, "المفضلة ⭐ (${favoriteMedIds.size})", "Favorites ⭐ (${favoriteMedIds.size})")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.testTag("rx_tab_favorites"),
                    )
                    FilterChip(
                        selected = medFilterMode == 2,
                        onClick = { medFilterMode = 2 },
                        label = { Text(bi(isEn, "كل التخصصات", "All")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }

                if (medFilterMode == 2) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSpecialtyChip == null,
                                onClick = { selectedSpecialtyChip = null },
                                label = { Text(bi(isEn, "الكل", "All")) },
                            )
                        }
                        items(MedicationCatalog.specialtiesOrder) { spec ->
                            FilterChip(
                                selected = selectedSpecialtyChip == spec,
                                onClick = {
                                    selectedSpecialtyChip = if (selectedSpecialtyChip == spec) null else spec
                                },
                                label = { Text(spec) },
                            )
                        }
                    }
                }

                // Catalog list
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp),
                ) {
                    if (filteredMedications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (medFilterMode == 1) {
                                    bi(isEn, "لا توجد أدوية في قائمة المفضلة لهذا الطبيب بعد", "No favorite medications saved yet")
                                } else {
                                    bi(isEn, "لا توجد أدوية مطابقة لبحثك", "No medications match your search")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredMedications, key = { it.id }) { med ->
                                val isFav = med.id in favoriteMedIds
                                val isAdded = selectedItems.any { it.uid == med.id }
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAdded) SlotAvailableBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(
                                            onClick = { onToggleFavorite(med.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("fav_med_${med.id}"),
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarOutline,
                                                contentDescription = if (isFav) bi(isEn, "إزالة من المفضلة", "Remove favorite") else bi(isEn, "إضافة للمفضلة", "Add favorite"),
                                                tint = if (isFav) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = med.tradeName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${med.activeIngredient} • ${med.strength}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TealPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${med.form} • ${med.specialty}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { addMedicationToRx(med) },
                                            enabled = !isAdded,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("add_med_${med.id}"),
                                        ) {
                                            Icon(
                                                imageVector = if (isAdded) Icons.Filled.CheckCircle else Icons.Filled.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAdded) bi(isEn, "مضاف", "Added") else bi(isEn, "إضافة", "Add"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Prescription Builder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = TealPrimary)
                    Text(
                        text = bi(isEn, "الأدوية المختارة بالروشتة (${selectedItems.size})", "Selected Prescription Items (${selectedItems.size})"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (selectedItems.isEmpty()) {
                    Text(
                        text = bi(isEn, "اضغط (إضافة) بجانب أي دواء أعلاه أو أضف دواءً مخصصاً أدناه.", "Tap Add next to a medication above or enter a custom drug below."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedItems.forEachIndexed { index, rxItem ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
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
                                            text = "${index + 1}. ${rxItem.tradeName} ${rxItem.strength}".trim(),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(
                                            onClick = { selectedItems.removeAt(index) },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = bi(isEn, "حذف الدواء", "Remove drug"),
                                                tint = SlotBookedBorder,
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = rxItem.dosage,
                                        onValueChange = { updated ->
                                            selectedItems[index] = rxItem.copy(
                                                dosage = updated,
                                                dosageEn = BilingualMedicalTranslator.toEnglishDosage(updated),
                                            )
                                        },
                                        label = { Text(bi(isEn, "الجرعة وطريقة الاستخدام", "Dosage & Instructions")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(MedicationCatalog.dosagePresets) { preset ->
                                            AssistChip(
                                                onClick = {
                                                    selectedItems[index] = rxItem.copy(
                                                        dosage = preset,
                                                        dosageEn = BilingualMedicalTranslator.toEnglishDosage(preset),
                                                    )
                                                },
                                                label = { Text(preset) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick custom med entry
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = bi(isEn, "إضافة دواء خارج القائمة (اختياري):", "Add Custom Medication (optional):"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = customMedName,
                                onValueChange = { customMedName = it },
                                label = { Text(bi(isEn, "اسم الدواء", "Medication Name")) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                            )
                            OutlinedTextField(
                                value = customMedDosage,
                                onValueChange = { customMedDosage = it },
                                label = { Text(bi(isEn, "الجرعة", "Dosage")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    if (customMedName.isNotBlank()) {
                                        val finalDosage = customMedDosage.trim().ifBlank { bi(isEn, "حسب إرشادات الطبيب", "As directed") }
                                        selectedItems.add(
                                            SelectedRxItem(
                                                uid = "custom_${System.currentTimeMillis()}",
                                                tradeName = customMedName.trim(),
                                                strength = "",
                                                dosage = finalDosage,
                                                dosageEn = BilingualMedicalTranslator.toEnglishDosage(finalDosage),
                                            )
                                        )
                                        customMedName = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = bi(isEn, "إضافة دواء مخصص", "Add custom drug"), tint = TealPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(bi(isEn, "تعليمات وملاحظات للمريض (اختياري)", "Notes for Patient (optional)")) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Bilingual AI Generator Banner & Editable AR/EN Section
                Button(
                    onClick = { generateBilingualDraft() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rx_generate_bilingual_ai"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        bi(
                            isEn,
                            "توليد الروشتة والتقرير باللغتين (AR + EN)",
                            "Auto-Generate Bilingual AR + EN Rx & Report",
                        )
                    )
                }

                if (showBilingualSection) {
                    Surface(
                        color = TealPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "مراجعة النسخة الثنائية اللغة (عربي مبسط للمريض / إنجليزي دقيق للصيدلي):",
                                    "Review Bilingual Output (Simplified Arabic / Clinical English):",
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            OutlinedTextField(
                                value = diagnosisEn,
                                onValueChange = { diagnosisEn = it },
                                label = { Text("Clinical Diagnosis (EN)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = notesEn,
                                onValueChange = { notesEn = it },
                                label = { Text("Clinical Notes (EN)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = aiReportAr,
                                onValueChange = { aiReportAr = it },
                                label = { Text("التقرير الطبي المبسط للمريض (AR)") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = aiReportEn,
                                onValueChange = { aiReportEn = it },
                                label = { Text("Clinical Medical Report (EN)") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Optional Paper Rx Camera / Gallery Attachment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                tempCameraFile = file
                                cameraLauncher.launch(uri)
                            } catch (_: ActivityNotFoundException) {
                                onShowMessage(bi(isEn, "لا توجد كاميرا متاحة، اختر صورة من المعرض", "No camera available; pick from gallery"))
                            } catch (_: Exception) {
                                onShowMessage(bi(isEn, "تعذر فتح الكاميرا", "Could not open camera"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "تصوير روشتة ورقية", "Capture Paper Rx"))
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "إرفاق صورة", "Attach Image"))
                    }
                }

                if (attachmentPath.isNotEmpty()) {
                    AsyncImage(
                        model = File(attachmentPath),
                        contentDescription = bi(isEn, "صورة الروشتة المرفقة", "Attached prescription image"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                HorizontalDivider()

                // Print language toggle + Print / PDF Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = !printAsEnglish,
                        onClick = { printAsEnglish = false },
                        label = { Text("طباعة (AR)") },
                    )
                    FilterChip(
                        selected = printAsEnglish,
                        onClick = { printAsEnglish = true },
                        label = { Text("Print (EN)") },
                    )
                }

                OutlinedButton(
                    onClick = {
                        val medsAr = buildMedicationsText()
                        val medsEn = buildMedicationsTextEn()
                        val bundle = BilingualMedicalTranslator.buildBilingualBundle(
                            patientName = slot.patientName,
                            doctorName = doctor.doctorName,
                            specialty = doctor.specialty,
                            diagnosisInput = diagnosis.ifBlank { diagnosisEn.ifBlank { "كشف طبي عام" } },
                            medicationsInput = medsAr,
                            notesInput = notes,
                            existingDiagnosisEn = diagnosisEn,
                            existingMedicationsEn = medsEn,
                            existingNotesEn = notesEn,
                        )
                        val draftRecord = MedicalRecordEntity(
                            id = "preview_${System.currentTimeMillis()}",
                            familyMemberId = slot.familyMemberId,
                            patientName = slot.patientName,
                            patientPhone = slot.patientPhone,
                            doctorId = doctor.id,
                            doctorName = doctor.doctorName,
                            specialty = doctor.specialty,
                            visitDateLabel = TimeUtils.formatTodayLabelAr(printAsEnglish),
                            diagnosis = bundle.diagnosisAr,
                            medications = bundle.medicationsAr.ifBlank { medsAr },
                            notes = bundle.notesAr,
                            attachmentPath = attachmentPath,
                            recordType = MedicalRecordType.PRESCRIPTION,
                            diagnosisEn = bundle.diagnosisEn,
                            medicationsEn = bundle.medicationsEn,
                            notesEn = bundle.notesEn,
                            aiReportAr = aiReportAr.ifBlank { bundle.aiReportAr },
                            aiReportEn = aiReportEn.ifBlank { bundle.aiReportEn },
                        )
                        PrescriptionPrintHelper.printPrescription(
                            context = context,
                            brandConfig = brandConfig,
                            record = draftRecord,
                            isEnglish = printAsEnglish,
                            onError = onShowMessage,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rx_print_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (printAsEnglish) "Print Prescription & Report (EN PDF)"
                        else "طباعة الروشتة والتقرير (AR Print / PDF)"
                    )
                }

                // Save and Send to Patient EMR Button
                Button(
                    onClick = {
                        val medsAr = buildMedicationsText()
                        val medsEn = buildMedicationsTextEn()
                        onSaveAndComplete(
                            diagnosis,
                            medsAr,
                            notes,
                            attachmentPath,
                            diagnosisEn,
                            medsEn,
                            notesEn,
                            aiReportAr,
                            aiReportEn,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("rx_save_send_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bi(isEn, "إرسال لملف المريض وإنهاء الكشف", "Send to Patient File & Complete"))
                }
            }
        }
    }
}
