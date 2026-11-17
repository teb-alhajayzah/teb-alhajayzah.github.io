package com.example.ui.lab

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalLabEntity
import com.example.ui.TebUiState
import com.example.ui.bi
import com.example.ui.components.PasswordOutlinedTextField
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.TealPrimary
import com.example.util.ImageStorageHelper
import java.io.File

@Composable
fun LabLoginGateCard(
    adminOwnerName: String,
    isEn: Boolean = false,
    onLoginLab: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
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
            border = BorderStroke(1.5.dp, Color(0xFF4338CA).copy(alpha = 0.35f)),
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
                            .background(Color(0xFFE0E7FF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = bi(isEn, "تسجيل دخول معامل التحاليل", "Medical Lab Login"),
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column {
                        Text(
                            text = bi(isEn, "بوابة معامل التحاليل (Lab Login)", "Medical Lab Portal"),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "سجّل دخولك لإرسال نتائج التحاليل (PDF أو صورة) لملف المريض مباشرة",
                                "Sign in to upload lab results (PDF or image) directly to a patient's file",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(bi(isEn, "اسم المستخدم للمعمل", "Lab Username")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lab_login_username"),
                )

                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = bi(isEn, "كلمة المرور", "Password"),
                    toggleTestTag = "lab_login_password_toggle",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lab_login_password"),
                )

                Button(
                    onClick = { onLoginLab(username, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("lab_login_submit"),
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bi(isEn, "دخول إلى بوابة المعمل", "Enter Lab Portal"))
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "تُنشأ حسابات معامل التحاليل في قرية الحجايزة من لوحة الإدارة العليا ($adminOwnerName).",
                            "Medical lab accounts are created by the Super Admin ($adminOwnerName).",
                        ),
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
fun LabDashboardScreen(
    state: TebUiState,
    activeLab: MedicalLabEntity,
    onSearchByPhone: (String) -> Unit,
    onUploadLabResult: (
        member: FamilyMemberEntity,
        accountPhone: String,
        testTitle: String,
        notes: String,
        path: String,
        mimeType: String,
        displayName: String,
        onDone: () -> Unit,
    ) -> Unit,
    onLogoutLab: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val isEn = state.isEn
    var phoneQuery by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf<Pair<FamilyMemberEntity, String>?>(null) }

    var testTitle by remember { mutableStateOf("") }
    var testNotes by remember { mutableStateOf("") }
    var attachmentPath by remember { mutableStateOf("") }
    var attachmentMime by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            if (saved != null) {
                attachmentPath = saved.path
                attachmentMime = saved.mimeType
                attachmentName = saved.displayName
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة ملف التحليل", "Could not read lab file"))
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            if (saved != null) {
                attachmentPath = saved.path
                attachmentMime = saved.mimeType
                attachmentName = saved.displayName
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { ok ->
        val file = tempCameraFile
        if (ok && file != null) {
            val saved = ImageStorageHelper.persistFileToInternalStorage(context, file)
            if (saved != null) {
                attachmentPath = saved
                attachmentMime = "image/jpeg"
                attachmentName = if (isEn) "lab_report.jpg" else "تقرير_معمل.jpg"
            }
        }
    }

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
                            text = activeLab.labName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF4338CA),
                        )
                        Text(
                            text = bi(isEn, "رفع نتائج التحاليل مباشرة لملف المريض الموحد", "Upload lab results straight to the unified patient file"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = onLogoutLab,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlotBookedBorder),
                        border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("lab_logout_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "خروج", "Sign Out"))
                    }
                }
            }

            // Patient search by phone card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFF4338CA).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = bi(isEn, "البحث عن المريض برقم الهاتف", "Find Patient by Phone"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = phoneQuery,
                                onValueChange = { phoneQuery = it },
                                label = { Text(bi(isEn, "رقم هاتف المريض", "Patient Phone")) },
                                placeholder = { Text("010xxxxxxxx") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lab_phone_search_input"),
                            )
                            Button(
                                onClick = {
                                    selectedTarget = null
                                    onSearchByPhone(phoneQuery)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("lab_phone_search_button"),
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = bi(isEn, "بحث", "Search"))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(bi(isEn, "بحث", "Search"))
                            }
                        }
                    }
                }
            }

            if (state.labSearchResults.isNotEmpty()) {
                item {
                    Text(
                        text = bi(isEn, "اختر الفرد المراد إرسال النتيجة إلى ملفه:", "Select family member to upload result for:"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(state.labSearchResults, key = { it.account.id }) { res ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = bi(isEn, "حساب: ${res.account.fullName} (${res.account.phone})", "Account: ${res.account.fullName} (${res.account.phone})"),
                                style = MaterialTheme.typography.titleSmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            res.familyMembers.forEach { member ->
                                val isSelected = selectedTarget?.first?.id == member.id
                                Surface(
                                    color = if (isSelected) SlotAvailableBg else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) SlotAvailableBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedTarget = member to res.account.phone }
                                        .testTag("lab_select_member_${member.id}"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Person, contentDescription = null, tint = TealPrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(member.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = bi(isEn, "${member.relation} • ${member.age} سنة", "${member.relation} • ${member.age} yrs"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = bi(isEn, "محدد", "Selected"), tint = SlotAvailableBorder)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val target = selectedTarget
            if (target != null) {
                item {
                    val member = target.first
                    val accountPhone = target.second
                    val isPdf = ImageStorageHelper.isPdfAttachment(attachmentPath, attachmentMime)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, SlotAvailableBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, tint = SlotAvailableBorder)
                                Text(
                                    text = bi(isEn, "إرسال نتيجة تحليل لملف: ${member.fullName}", "Upload Result for: ${member.fullName}"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            OutlinedTextField(
                                value = testTitle,
                                onValueChange = { testTitle = it },
                                label = { Text(bi(isEn, "اسم التحليل أو الفحص *", "Test / Scan Title *")) },
                                placeholder = { Text(bi(isEn, "مثال: صورة دم كاملة CBC / وظائف كلى", "e.g., CBC / Kidney Panel")) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lab_result_title_input"),
                            )

                            OutlinedTextField(
                                value = testNotes,
                                onValueChange = { testNotes = it },
                                label = { Text(bi(isEn, "ملاحظات المعمل أو القيم المهمة (اختياري)", "Lab notes or key values (optional)")) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { docLauncher.launch(arrayOf("application/pdf", "image/*")) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("lab_pick_pdf_button"),
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "ملف PDF / مستند", "PDF / Document"), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "صورة من المعرض", "Gallery Image"), maxLines = 1)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                        tempCameraFile = file
                                        cameraLauncher.launch(uri)
                                    } catch (_: ActivityNotFoundException) {
                                        onShowMessage(bi(isEn, "لا توجد كاميرا متاحة", "No camera available"))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(bi(isEn, "تصوير نتيجة التحليل بالكاميرا", "Take Photo with Camera"))
                            }

                            if (attachmentPath.isNotBlank()) {
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
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
                                            Text(
                                                text = bi(isEn, "جاهز للإرسال: ${attachmentName.ifBlank { "نتيجة_التحليل.pdf" }}", "Ready: ${attachmentName.ifBlank { "lab_report.pdf" }}"),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                } else {
                                    AsyncImage(
                                        model = File(attachmentPath),
                                        contentDescription = bi(isEn, "معاينة النتيجة", "Result preview"),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onUploadLabResult(
                                        member,
                                        accountPhone,
                                        testTitle,
                                        testNotes,
                                        attachmentPath,
                                        attachmentMime,
                                        attachmentName,
                                    ) {
                                        testTitle = ""
                                        testNotes = ""
                                        attachmentPath = ""
                                        attachmentMime = ""
                                        attachmentName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlotAvailableBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("lab_submit_result_button"),
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(bi(isEn, "إرسال النتيجة لملف المريض فوراً", "Send Result to Patient File"))
                            }
                        }
                    }
                }
            }
        }
    }
}
