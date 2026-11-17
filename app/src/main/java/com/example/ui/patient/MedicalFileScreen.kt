package com.example.ui.patient

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.config.PlatformBrandConfig
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.ui.TebUiState
import com.example.ui.bi
import com.example.ui.components.AiMedicalSummaryCard
import com.example.ui.theme.TealPrimary
import com.example.util.ImageStorageHelper
import com.example.util.MedicalSummaryAnalyzer
import com.example.util.PrescriptionPrintHelper
import com.example.util.TimeUtils
import java.io.File

private data class ExternalMedDraft(
    val drugName: String,
    val dosage: String,
    val duration: String,
)

@Composable
fun MedicalFileScreen(
    state: TebUiState,
    onSelectFamilyMember: (String) -> Unit,
    onOpenAccountDialog: () -> Unit,
    onUploadAttachment: (
        member: FamilyMemberEntity,
        title: String,
        notes: String,
        path: String,
        mimeType: String,
        displayName: String,
    ) -> Unit,
    onAddExternalDoctorVisit: (
        member: FamilyMemberEntity,
        doctorName: String,
        specialty: String,
        visitDateLabel: String,
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String,
        attachmentName: String,
    ) -> Unit,
    onPreviewImage: (path: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val isEn = state.isEn
    val activeMember = state.activeMember
    val records = remember(state.medicalRecords, activeMember) {
        state.recordsForMember(activeMember)
    }

    var showUploadDialog by remember { mutableStateOf(false) }
    var showExternalVisitDialog by remember { mutableStateOf(false) }
    var pendingFilePath by remember { mutableStateOf("") }
    var pendingFileMime by remember { mutableStateOf("image/jpeg") }
    var pendingFileName by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val fileOrDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            if (saved != null) {
                pendingFilePath = saved.path
                pendingFileMime = saved.mimeType
                pendingFileName = saved.displayName
                showUploadDialog = true
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة الملف أو الصورة المختارة", "Could not read selected file or image"))
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            if (saved != null) {
                pendingFilePath = saved.path
                pendingFileMime = saved.mimeType
                pendingFileName = saved.displayName
                showUploadDialog = true
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة الصورة المختارة", "Could not read selected image"))
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = tempCameraFile
        if (success && file != null) {
            val savedPath = ImageStorageHelper.persistFileToInternalStorage(context, file)
            if (savedPath != null) {
                pendingFilePath = savedPath
                pendingFileMime = "image/jpeg"
                pendingFileName = if (isEn) "attached_photo.jpg" else "صورة_مرفقة.jpg"
                showUploadDialog = true
            } else {
                onShowMessage(bi(isEn, "تعذر حفظ صورة الكاميرا", "Could not save camera photo"))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = bi(isEn, "ملفي الطبي الموحد", "Unified Medical File"),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = bi(
                            isEn,
                            "سجل العائلة الكامل للروشتات، الزيارات الخارجية، ونتائج المعامل والأشعة مع الملخص الطبي التلقائي.",
                            "Complete family history of prescriptions, external visits, and lab/imaging results with smart summary.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.activeAccount == null || activeMember == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Image(
                                painter = painterResource(id = state.brandConfig.medicalRecordIllustRes),
                                contentDescription = bi(isEn, "ملف طبي موحد", "Unified Medical Record"),
                                modifier = Modifier
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit,
                            )
                            Text(
                                text = bi(
                                    isEn,
                                    "سجّل الدخول أو أنشئ حساباً للوصول إلى ملفك الطبي",
                                    "Sign in or create an account to access your medical file",
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = bi(
                                    isEn,
                                    "يتم حفظ سجل الزيارات الخاصة ونتائج المعامل والروشتات بشكل مستقل لكل فرد في عائلتك.",
                                    "External visits, lab results, and prescriptions are saved separately for each family member.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = onOpenAccountDialog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("medical_file_open_account_button"),
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(bi(isEn, "تسجيل الدخول / إنشاء حساب", "Sign In / Create Account"))
                            }
                        }
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.familyMembers, key = { it.id }) { member ->
                            val selected = member.id == activeMember.id
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

                // AI Medical Summary card at the top of each family member's file
                item {
                    AiMedicalSummaryCard(
                        patientName = activeMember.fullName,
                        records = records,
                        isEn = isEn,
                    )
                }

                // Prominent button to log an External / Private Doctor Visit
                item {
                    Button(
                        onClick = { showExternalVisitDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB45309),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_external_visit_button"),
                    ) {
                        Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bi(isEn, "➕ تسجيل زيارة طبيب خارجي / خاص", "➕ Log External / Private Visit"),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.UploadFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = bi(isEn, "إضافة تحاليل أو أشعة (صورة أو PDF)", "Add Lab / Radiology Result (Image or PDF)"),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = bi(
                                            isEn,
                                            "تُضغط الصور تلقائياً وتظهر فوراً للأطباء عند الكشف",
                                            "Images compress automatically and become visible to your doctors during visits",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                            tempCameraFile = file
                                            cameraLauncher.launch(uri)
                                        } catch (_: ActivityNotFoundException) {
                                            onShowMessage(bi(isEn, "لا توجد كاميرا متاحة، اختر صورة من المعرض", "No camera available; pick an image from gallery"))
                                        } catch (_: Exception) {
                                            onShowMessage(bi(isEn, "تعذر فتح الكاميرا", "Could not open camera"))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("upload_lab_camera_button"),
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "كاميرا", "Camera"), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("upload_lab_gallery_button"),
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "صورة", "Image"), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        fileOrDocLauncher.launch(arrayOf("application/pdf", "image/*"))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("upload_lab_pdf_button"),
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                if (records.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = state.brandConfig.medicalRecordIllustRes),
                                    contentDescription = bi(isEn, "ملف طبي فارغ", "Empty medical record"),
                                    modifier = Modifier
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Fit,
                                )
                                Text(
                                    text = bi(isEn, "لا توجد سجلات طبية مسجلة لـ ${activeMember.fullName} بعد", "No medical records for ${activeMember.fullName} yet"),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = bi(
                                        isEn,
                                        "ستظهر هنا كل الروشتات، الزيارات الخارجية، ونتائج المعامل والتحاليل.",
                                        "All prescriptions, external visits, and lab results will appear here.",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(records, key = { it.id }) { record ->
                        MedicalRecordTimelineCard(
                            record = record,
                            brandConfig = state.brandConfig,
                            isEn = isEn,
                            onPreviewImage = onPreviewImage,
                            onShowMessage = onShowMessage,
                        )
                    }
                }
            }
        }
    }

    if (showUploadDialog && activeMember != null) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        val isPdf = ImageStorageHelper.isPdfAttachment(pendingFilePath, pendingFileMime)

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text(bi(isEn, "حفظ التحليل / الأشعة في الملف الطبي", "Save Lab / Imaging Result")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pendingFilePath.isNotEmpty()) {
                        if (isPdf) {
                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color(0xFF0369A1))
                                    Text(pendingFileName.ifBlank { bi(isEn, "ملف PDF مرفق", "Attached PDF") }, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            AsyncImage(
                                model = File(pendingFilePath),
                                contentDescription = bi(isEn, "معاينة الصورة", "Image preview"),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(bi(isEn, "اسم التحليل أو الأشعة (مثال: صورة دم كاملة)", "Test or scan title (e.g., CBC)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(bi(isEn, "ملاحظات إضافية للطبيب (اختياري)", "Extra notes for doctor (optional)")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUploadAttachment(
                            activeMember,
                            title,
                            notes,
                            pendingFilePath,
                            pendingFileMime,
                            pendingFileName,
                        )
                        showUploadDialog = false
                    }
                ) {
                    Text(bi(isEn, "حفظ في ملفي الطبي", "Save to Medical File"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text(bi(isEn, "إلغاء", "Cancel"))
                }
            },
        )
    }

    if (showExternalVisitDialog && activeMember != null) {
        ExternalDoctorVisitDialog(
            member = activeMember,
            isEn = isEn,
            onSave = { docName, specialty, dateLabel, diagnosis, medsText, notes, attPath, attMime, attName ->
                onAddExternalDoctorVisit(
                    activeMember,
                    docName,
                    specialty,
                    dateLabel,
                    diagnosis,
                    medsText,
                    notes,
                    attPath,
                    attMime,
                    attName,
                )
                showExternalVisitDialog = false
            },
            onShowMessage = onShowMessage,
            onDismiss = { showExternalVisitDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalDoctorVisitDialog(
    member: FamilyMemberEntity,
    isEn: Boolean = false,
    onSave: (
        doctorName: String,
        specialty: String,
        visitDateLabel: String,
        diagnosis: String,
        medicationsText: String,
        notes: String,
        attachmentPath: String,
        attachmentMimeType: String,
        attachmentName: String,
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var doctorName by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf(TimeUtils.formatTodayLabelAr(isEn)) }
    val specialtyOptions = if (isEn) {
        MedicalSummaryAnalyzer.EXTERNAL_SPECIALTIES_EN
    } else {
        MedicalSummaryAnalyzer.EXTERNAL_SPECIALTIES.map { it to it }
    }
    var selectedSpecialtyCanonical by remember { mutableStateOf(MedicalSummaryAnalyzer.EXTERNAL_SPECIALTIES.first()) }
    var specialtyExpanded by remember { mutableStateOf(false) }
    val selectedSpecialtyDisplay = specialtyOptions.firstOrNull { it.second == selectedSpecialtyCanonical }?.first
        ?: selectedSpecialtyCanonical
    var diagnosis by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var medNameInput by remember { mutableStateOf("") }
    var medDosageInput by remember { mutableStateOf("") }
    var medDurationInput by remember { mutableStateOf("") }
    val medDrafts = remember { mutableStateListOf<ExternalMedDraft>() }

    var attachmentPath by remember { mutableStateOf("") }
    var attachmentMime by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val extPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            if (saved != null) {
                attachmentPath = saved.path
                attachmentMime = saved.mimeType
                attachmentName = saved.displayName
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة الملف المرفق", "Could not read attached file"))
            }
        }
    }

    val extCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { ok ->
        val file = tempCameraFile
        if (ok && file != null) {
            val saved = ImageStorageHelper.persistFileToInternalStorage(context, file)
            if (saved != null) {
                attachmentPath = saved
                attachmentMime = "image/jpeg"
                attachmentName = if (isEn) "external_rx.jpg" else "روشتة_خارجية.jpg"
            }
        }
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bi(isEn, "تسجيل زيارة طبيب خارجي / خاص", "Log External / Private Doctor Visit"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = bi(isEn, "للمريض: ${member.fullName}", "Patient: ${member.fullName}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text(bi(isEn, "اسم الطبيب المعالج *", "Doctor Name *")) },
                    placeholder = { Text(bi(isEn, "مثال: د. أيمن فؤاد", "e.g., Dr. Ayman Fouad")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_doctor_name_input"),
                )

                OutlinedTextField(
                    value = visitDate,
                    onValueChange = { visitDate = it },
                    label = { Text(bi(isEn, "تاريخ الزيارة", "Visit Date")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_visit_date_input"),
                )

                ExposedDropdownMenuBox(
                    expanded = specialtyExpanded,
                    onExpandedChange = { specialtyExpanded = !specialtyExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedSpecialtyDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(bi(isEn, "تخصص الطبيب", "Specialty")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specialtyExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("ext_specialty_dropdown"),
                    )
                    ExposedDropdownMenu(
                        expanded = specialtyExpanded,
                        onDismissRequest = { specialtyExpanded = false },
                    ) {
                        specialtyOptions.forEach { (displayLabel, canonicalValue) ->
                            DropdownMenuItem(
                                text = { Text(displayLabel) },
                                onClick = {
                                    selectedSpecialtyCanonical = canonicalValue
                                    specialtyExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text(bi(isEn, "التشخيص / ملخص الكشف *", "Diagnosis / Visit Notes *")) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_diagnosis_input"),
                )

                HorizontalDivider()
                Text(
                    text = bi(isEn, "الأدوية الموصوفة في الزيارة (${medDrafts.size})", "Prescribed Medications (${medDrafts.size})"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (medDrafts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        medDrafts.forEachIndexed { idx, draft ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${draft.drugName} — ${draft.dosage}${if (draft.duration.isNotBlank()) " (${draft.duration})" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { medDrafts.removeAt(idx) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = bi(isEn, "حذف الدواء", "Remove drug"), tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = medNameInput,
                            onValueChange = { medNameInput = it },
                            label = { Text(bi(isEn, "اسم الدواء", "Medication Name")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ext_med_name_input"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = medDosageInput,
                                onValueChange = { medDosageInput = it },
                                label = { Text(bi(isEn, "الجرعة", "Dosage")) },
                                placeholder = { Text(bi(isEn, "قرص كل 12 ساعة", "1 tab every 12h")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = medDurationInput,
                                onValueChange = { medDurationInput = it },
                                label = { Text(bi(isEn, "المدة", "Duration")) },
                                placeholder = { Text(bi(isEn, "7 أيام", "7 days")) },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val cleanDrug = medNameInput.trim()
                                if (cleanDrug.isEmpty()) {
                                    onShowMessage(bi(isEn, "اكتب اسم الدواء أولاً", "Enter medication name first"))
                                } else {
                                    medDrafts.add(
                                        ExternalMedDraft(
                                            drugName = cleanDrug,
                                            dosage = medDosageInput.trim().ifBlank { bi(isEn, "حسب إرشادات الطبيب", "As directed") },
                                            duration = medDurationInput.trim(),
                                        )
                                    )
                                    medNameInput = ""
                                    medDosageInput = ""
                                    medDurationInput = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("ext_add_med_button"),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(bi(isEn, "إضافة دواء +", "Add Medication +"))
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(bi(isEn, "ملاحظات إضافية (اختياري)", "Extra notes (optional)")) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                tempCameraFile = file
                                extCamera.launch(uri)
                            } catch (_: Exception) {
                                onShowMessage(bi(isEn, "تعذر فتح الكاميرا", "Could not open camera"))
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "تصوير روشتة", "Camera Rx"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { extPicker.launch(arrayOf("image/*", "application/pdf")) },
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "صورة أو PDF", "Image / PDF"), maxLines = 1)
                    }
                }

                if (attachmentPath.isNotEmpty()) {
                    Text(
                        text = bi(isEn, "✓ تم إرفاق: ${attachmentName.ifBlank { "ملف مرفق" }}", "✓ Attached: ${attachmentName.ifBlank { "file" }}"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TealPrimary,
                    )
                }

                Button(
                    onClick = {
                        // Also include pending typed drug if user forgot clicking Add
                        val allMeds = medDrafts.toMutableList()
                        if (medNameInput.isNotBlank()) {
                            allMeds.add(
                                ExternalMedDraft(
                                    drugName = medNameInput.trim(),
                                    dosage = medDosageInput.trim().ifBlank { bi(isEn, "حسب إرشادات الطبيب", "As directed") },
                                    duration = medDurationInput.trim(),
                                )
                            )
                        }
                        val medsFormatted = allMeds.joinToString("\n") { d ->
                            val dur = if (d.duration.isNotBlank()) bi(isEn, " — المدة: ${d.duration}", " — ${d.duration}") else ""
                            "• ${d.drugName} — ${d.dosage}$dur"
                        }
                        onSave(
                            doctorName,
                            selectedSpecialtyCanonical,
                            visitDate,
                            diagnosis,
                            medsFormatted,
                            notes,
                            attachmentPath,
                            attachmentMime,
                            attachmentName,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ext_visit_save_button"),
                ) {
                    Text(bi(isEn, "حفظ الزيارة وتحديث الملخص الطبي", "Save Visit & Update Summary"))
                }
            }
        }
    }
}

@Composable
fun MedicalRecordTimelineCard(
    record: MedicalRecordEntity,
    brandConfig: PlatformBrandConfig,
    isEn: Boolean = false,
    onPreviewImage: (path: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val isUpload = record.recordType == MedicalRecordType.PATIENT_UPLOAD
    val isLabResult = record.recordType == MedicalRecordType.LAB_RESULT
    val isExternal = record.recordType == MedicalRecordType.EXTERNAL_VISIT
    val isPdf = ImageStorageHelper.isPdfAttachment(record.attachmentPath, record.attachmentMimeType)

    val accentColor = when (record.recordType) {
        MedicalRecordType.LAB_RESULT -> Color(0xFF4338CA)
        MedicalRecordType.EXTERNAL_VISIT -> Color(0xFFB45309)
        MedicalRecordType.PATIENT_UPLOAD -> Color(0xFF0284C7)
        else -> MaterialTheme.colorScheme.primary
    }

    val badgeLabel = when (record.recordType) {
        MedicalRecordType.LAB_RESULT -> bi(isEn, "نتيجة معمل", "Lab Result")
        MedicalRecordType.EXTERNAL_VISIT -> bi(isEn, "طبيب خارجي", "External Doctor")
        MedicalRecordType.PATIENT_UPLOAD -> bi(isEn, "رفع المريض", "Patient Upload")
        else -> bi(isEn, "عيادة بالمنصة", "Platform Clinic")
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = when {
                            isLabResult -> Icons.Filled.Science
                            isExternal -> Icons.Filled.LocalHospital
                            isUpload -> Icons.Filled.Description
                            else -> Icons.Filled.MedicalServices
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = record.doctorName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = record.specialty,
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = record.visitDateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = when {
                            isLabResult -> bi(isEn, "اسم التحليل / الفحص: ${record.diagnosis}", "Lab Test: ${record.diagnosis}")
                            isUpload -> bi(isEn, "العنوان: ${record.diagnosis}", "Title: ${record.diagnosis}")
                            else -> bi(isEn, "التشخيص الطبي: ${record.diagnosis}", "Diagnosis: ${record.diagnosis}")
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (record.medications.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.LocalPharmacy,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = record.medications,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (record.notes.isNotBlank()) {
                        Text(
                            text = bi(isEn, "ملاحظات: ${record.notes}", "Notes: ${record.notes}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (record.attachmentPath.isNotBlank()) {
                if (isPdf) {
                    OutlinedButton(
                        onClick = {
                            ImageStorageHelper.openAttachmentExternally(
                                context = context,
                                filePath = record.attachmentPath,
                                mimeType = record.attachmentMimeType,
                                onError = onShowMessage,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(bi(isEn, "فتح ملف PDF: ${record.attachmentName.ifBlank { "نتيجة التحليل" }}", "Open PDF: ${record.attachmentName.ifBlank { "Lab Report" }}"))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPreviewImage(record.attachmentPath, record.diagnosis) },
                    ) {
                        AsyncImage(
                            model = File(record.attachmentPath),
                            contentDescription = bi(isEn, "صورة الروشتة أو التحليل", "Prescription or lab image"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bi(isEn, "اضغط للتكبير", "Tap to zoom"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            if (record.recordType == MedicalRecordType.PRESCRIPTION || isExternal) {
                OutlinedButton(
                    onClick = {
                        PrescriptionPrintHelper.printPrescription(
                            context = context,
                            brandConfig = brandConfig,
                            record = record,
                            onError = onShowMessage,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(bi(isEn, "طباعة الروشتة (Print / PDF)", "Print Prescription (PDF)"))
                }
            }
        }
    }
}
