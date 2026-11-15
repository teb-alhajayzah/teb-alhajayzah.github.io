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
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.medications.EgyptianMedication
import com.example.data.medications.MedicationCatalog
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.TealPrimary
import com.example.util.ImageStorageHelper
import com.example.util.PrescriptionPrintHelper
import com.example.util.TimeUtils
import java.io.File

private data class SelectedRxItem(
    val uid: String,
    val tradeName: String,
    val strength: String,
    val dosage: String,
)

@Composable
fun SmartPrescriptionDialog(
    slot: AppointmentSlotEntity,
    doctor: ClinicDoctorEntity,
    brandConfig: PlatformBrandConfig,
    medications: List<EgyptianMedication>,
    isLiveRemoteMeds: Boolean,
    favoriteMedIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSaveAndComplete: (
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var diagnosis by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    // 0 = تخصص العيادة, 1 = المفضلة, 2 = الكل
    var medFilterMode by remember { mutableIntStateOf(0) }
    var selectedSpecialtyChip by remember { mutableStateOf<String?>(null) }
    var customMedName by remember { mutableStateOf("") }
    var customMedDosage by remember { mutableStateOf("قرص كل 12 ساعة") }
    var attachmentPath by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val selectedItems = remember { mutableStateListOf<SelectedRxItem>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistUriToInternalStorage(context, uri)
            if (saved != null) {
                attachmentPath = saved
            } else {
                onShowMessage("تعذر قراءة صورة الروشتة")
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
                onShowMessage("تعذر حفظ صورة الكاميرا")
            }
        }
    }

    val filteredMedications = remember(
        medications,
        searchQuery,
        medFilterMode,
        selectedSpecialtyChip,
        favoriteMedIds,
        doctor.specialty,
    ) {
        val q = searchQuery.trim()
        medications.filter { med ->
            val matchesMode = when (medFilterMode) {
                0 -> med.specialty == doctor.specialty
                1 -> med.id in favoriteMedIds
                else -> selectedSpecialtyChip == null || med.specialty == selectedSpecialtyChip
            }
            val matchesSearch = q.isEmpty() ||
                med.tradeName.contains(q, ignoreCase = true) ||
                med.activeIngredient.contains(q, ignoreCase = true) ||
                med.strength.contains(q, ignoreCase = true) ||
                med.specialty.contains(q, ignoreCase = true)
            // When user types a search query, search across all meds if desired or keep mode?
            // Searching across all specialties when query is non-empty makes finding any med instant!
            if (q.isNotEmpty() && medFilterMode != 1) matchesSearch else (matchesMode && matchesSearch)
        }
    }

    fun buildMedicationsText(): String {
        return selectedItems.joinToString("\n") { item ->
            val strengthPart = if (item.strength.isNotBlank()) " ${item.strength}" else ""
            "• ${item.tradeName}$strengthPart — ${item.dosage.ifBlank { "حسب إرشادات الطبيب" }}"
        }
    }

    fun addMedicationToRx(med: EgyptianMedication) {
        val existingIdx = selectedItems.indexOfFirst { it.uid == med.id }
        if (existingIdx >= 0) {
            onShowMessage("الدواء مضاف بالفعل إلى الروشتة")
            return
        }
        selectedItems.add(
            SelectedRxItem(
                uid = med.id,
                tradeName = med.tradeName,
                strength = med.strength,
                dosage = med.defaultDosage,
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
                            text = "الروشتة الإلكترونية الذكية",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "المريض: ${slot.patientName} • ${TimeUtils.formatMinutesAr(slot.effectiveMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

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
                                "متصل بقاعدة الأدوية المصرية (${medications.size} دواء)"
                            } else {
                                "قاعدة الأدوية المصرية المدمجة Offline (${medications.size} دواء مصنف)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLiveRemoteMeds) SlotAvailableText else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Diagnosis Field
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("التشخيص الطبي *") },
                    placeholder = { Text("مثال: التهاب بالحلق / ارتجاع مريئي / تسوس ضرس...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rx_diagnosis_input"),
                )

                // Search Egyptian Medications
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث في قاعدة الأدوية المصرية") },
                    placeholder = { Text("ابحث بالاسم التجاري أو المادة الفعالة أو التركيز...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
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
                        label = { Text("تخصص العيادة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = medFilterMode == 1,
                        onClick = { medFilterMode = 1 },
                        label = { Text("المفضلة ⭐ (${favoriteMedIds.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.testTag("rx_tab_favorites"),
                    )
                    FilterChip(
                        selected = medFilterMode == 2,
                        onClick = { medFilterMode = 2 },
                        label = { Text("كل التخصصات") },
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
                                label = { Text("الكل") },
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
                        .heightIn(min = 120.dp, max = 230.dp),
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
                                    "لا توجد أدوية في قائمة المفضلة لهذا الطبيب بعد"
                                } else {
                                    "لا توجد أدوية مطابقة لبحثك"
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
                                                contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة للمفضلة",
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
                                            Text(if (isAdded) "مضاف" else "إضافة")
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
                        text = "الأدوية المختارة بالروشتة (${selectedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (selectedItems.isEmpty()) {
                    Text(
                        text = "اضغط (إضافة) بجانب أي دواء أعلاه أو أضف دواءً مخصصاً أدناه.",
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
                                                contentDescription = "حذف الدواء",
                                                tint = SlotBookedBorder,
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = rxItem.dosage,
                                        onValueChange = { updated ->
                                            selectedItems[index] = rxItem.copy(dosage = updated)
                                        },
                                        label = { Text("الجرعة وطريقة الاستخدام") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(MedicationCatalog.dosagePresets) { preset ->
                                            AssistChip(
                                                onClick = {
                                                    selectedItems[index] = rxItem.copy(dosage = preset)
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
                            text = "إضافة دواء خارج القائمة (اختياري):",
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
                                label = { Text("اسم الدواء") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                            )
                            OutlinedTextField(
                                value = customMedDosage,
                                onValueChange = { customMedDosage = it },
                                label = { Text("الجرعة") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    if (customMedName.isNotBlank()) {
                                        selectedItems.add(
                                            SelectedRxItem(
                                                uid = "custom_${System.currentTimeMillis()}",
                                                tradeName = customMedName.trim(),
                                                strength = "",
                                                dosage = customMedDosage.trim().ifBlank { "حسب إرشادات الطبيب" },
                                            )
                                        )
                                        customMedName = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "إضافة دواء مخصص", tint = TealPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تعليمات وملاحظات للمريض (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                )

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
                                onShowMessage("لا توجد كاميرا متاحة، اختر صورة من المعرض")
                            } catch (_: Exception) {
                                onShowMessage("تعذر فتح الكاميرا")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصوير روشتة ورقية")
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
                        Text("إرفاق صورة")
                    }
                }

                if (attachmentPath.isNotEmpty()) {
                    AsyncImage(
                        model = File(attachmentPath),
                        contentDescription = "صورة الروشتة المرفقة",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                HorizontalDivider()

                // Print / PDF Action Button
                OutlinedButton(
                    onClick = {
                        val draftRecord = MedicalRecordEntity(
                            id = "preview_${System.currentTimeMillis()}",
                            familyMemberId = slot.familyMemberId,
                            patientName = slot.patientName,
                            patientPhone = slot.patientPhone,
                            doctorId = doctor.id,
                            doctorName = doctor.doctorName,
                            specialty = doctor.specialty,
                            visitDateLabel = TimeUtils.formatTodayLabelAr(),
                            diagnosis = diagnosis.ifBlank { "كشف طبي عام" },
                            medications = buildMedicationsText(),
                            notes = notes,
                            attachmentPath = attachmentPath,
                            recordType = MedicalRecordType.PRESCRIPTION,
                        )
                        PrescriptionPrintHelper.printPrescription(
                            context = context,
                            brandConfig = brandConfig,
                            record = draftRecord,
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
                    Text("طباعة الروشتة (Print / PDF)")
                }

                // Save and Send to Patient EMR Button
                Button(
                    onClick = {
                        onSaveAndComplete(
                            diagnosis,
                            buildMedicationsText(),
                            notes,
                            attachmentPath,
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
                    Text("إرسال لملف المريض وإنهاء الكشف")
                }
            }
        }
    }
}
