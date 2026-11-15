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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.config.PlatformBrandConfig
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.ui.TebUiState
import com.example.util.ImageStorageHelper
import com.example.util.PrescriptionPrintHelper
import java.io.File

@Composable
fun MedicalFileScreen(
    state: TebUiState,
    onSelectFamilyMember: (String) -> Unit,
    onUploadAttachment: (member: FamilyMemberEntity, title: String, notes: String, path: String) -> Unit,
    onPreviewImage: (path: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activeMember = state.activeMember
    val records = remember(state.medicalRecords, activeMember) {
        state.recordsForMember(activeMember)
    }

    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingFilePath by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.persistUriToInternalStorage(context, uri)
            if (savedPath != null) {
                pendingFilePath = savedPath
                showUploadDialog = true
            } else {
                onShowMessage("تعذر قراءة الصورة المختارة")
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
                showUploadDialog = true
            } else {
                onShowMessage("تعذر حفظ صورة الكاميرا")
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
                        text = "ملفي الطبي الموحد",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "جميع الزيارات السابقة، التشخيصات، الروشتات، وصور التحاليل والأشعة مع إمكانية الطباعة أو الحفظ PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.familyMembers, key = { it.id }) { member ->
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
                                    text = "إضافة تحاليل أو أشعة جديدة",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "ارفع صورة التحليل لتظهر فوراً لكل الأطباء عند الكشف",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                        tempCameraFile = file
                                        cameraLauncher.launch(uri)
                                    } catch (_: ActivityNotFoundException) {
                                        onShowMessage("لا توجد كاميرا متاحة، يمكنك اختيار صورة من المعرض")
                                    } catch (_: Exception) {
                                        onShowMessage("تعذر فتح الكاميرا، اختر صورة من المعرض")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("upload_lab_camera_button"),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصوير بالكاميرا")
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("من المعرض")
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
                                contentDescription = "ملف طبي فارغ",
                                modifier = Modifier
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit,
                            )
                            Text(
                                text = "لا توجد سجلات طبية مسجلة لـ ${activeMember?.fullName ?: "هذا الفرد"} بعد",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "ستظهر هنا كل الروشتات التي يكتبها الأطباء أو صور التحاليل التي ترفعها.",
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
                        onPreviewImage = onPreviewImage,
                        onShowMessage = onShowMessage,
                    )
                }
            }
        }
    }

    if (showUploadDialog && activeMember != null) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("حفظ التحليل / الأشعة في الملف الطبي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pendingFilePath.isNotEmpty()) {
                        AsyncImage(
                            model = File(pendingFilePath),
                            contentDescription = "معاينة الصورة",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم التحليل أو الأشعة (مثال: صورة دم كاملة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية للطبيب (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUploadAttachment(activeMember, title, notes, pendingFilePath)
                        showUploadDialog = false
                    }
                ) {
                    Text("حفظ في ملفي الطبي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("إلغاء")
                }
            },
        )
    }
}

@Composable
fun MedicalRecordTimelineCard(
    record: MedicalRecordEntity,
    brandConfig: PlatformBrandConfig,
    onPreviewImage: (path: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val isUpload = record.recordType == MedicalRecordType.PATIENT_UPLOAD
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isUpload) Color(0xFF0284C7).copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ),
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
                        imageVector = if (isUpload) Icons.Filled.Description else Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = if (isUpload) Color(0xFF0284C7) else MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
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
                        text = if (isUpload) "العنوان: ${record.diagnosis}" else "التشخيص الطبي: ${record.diagnosis}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (record.medications.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.LocalPharmacy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                            text = "ملاحظات: ${record.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (record.attachmentPath.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPreviewImage(record.attachmentPath, record.diagnosis) },
                ) {
                    AsyncImage(
                        model = File(record.attachmentPath),
                        contentDescription = "صورة الروشتة أو التحليل",
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
                                text = "اضغط للتكبير",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            if (!isUpload) {
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
                    Text("طباعة الروشتة (Print / PDF)")
                }
            }
        }
    }
}
