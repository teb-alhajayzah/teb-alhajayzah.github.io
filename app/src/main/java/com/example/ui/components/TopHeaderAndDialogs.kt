package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.config.PlatformBrandConfig
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.PatientAccountEntity
import com.example.ui.AppRoleMode
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import java.io.File

@Composable
fun TebTopHeader(
    brandConfig: PlatformBrandConfig,
    roleMode: AppRoleMode,
    unreadAlertsCount: Int,
    activeAccount: PatientAccountEntity?,
    onSwitchRole: (AppRoleMode) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onOpenConfig: () -> Unit,
) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TealPrimary, TealPrimaryDark),
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Image(
                        painter = painterResource(id = brandConfig.logoRes),
                        contentDescription = brandConfig.platformNameAr,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = brandConfig.platformNameAr,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = brandConfig.villageNameAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("notifications_button"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadAlertsCount > 0) {
                                    Badge(containerColor = Color(0xFFEF4444)) {
                                        Text(
                                            text = unreadAlertsCount.toString(),
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "الإشعارات والتنبيهات",
                                tint = Color.White,
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("account_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = activeAccount?.fullName ?: "حسابي والعائلة",
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = onOpenSuperAdmin,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("super_admin_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "إدارة المنصة (Admin)",
                            tint = if (roleMode == AppRoleMode.SUPER_ADMIN) Color(0xFFFDE047) else Color.White,
                        )
                    }

                    IconButton(
                        onClick = onOpenConfig,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("platform_config_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "إعدادات المنصة الموحدة",
                            tint = Color.White,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RoleModePillButton(
                        selected = roleMode == AppRoleMode.PATIENT,
                        label = AppRoleMode.PATIENT.labelAr,
                        icon = Icons.Filled.LocalHospital,
                        testTag = "switch_to_patient_mode",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchRole(AppRoleMode.PATIENT) },
                    )
                    RoleModePillButton(
                        selected = roleMode == AppRoleMode.DOCTOR_ADMIN,
                        label = AppRoleMode.DOCTOR_ADMIN.labelAr,
                        icon = Icons.Filled.MedicalServices,
                        testTag = "switch_to_doctor_mode",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchRole(AppRoleMode.DOCTOR_ADMIN) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleModePillButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TealPrimary else Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TealPrimary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AccountAndFamilyDialog(
    activeAccount: PatientAccountEntity?,
    familyMembers: List<FamilyMemberEntity>,
    activeMemberId: String,
    onSelectMember: (String) -> Unit,
    onAddFamilyMember: (name: String, relation: String, age: Int, phone: String) -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (name: String, phone: String, nationalId: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf("الابن") }
    var newMemberAge by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf(activeAccount?.phone ?: "") }

    var loginIdentifier by remember { mutableStateOf(activeAccount?.phone ?: "01012345678") }
    var loginPassword by remember { mutableStateOf("") }

    var regName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regNationalId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "الحساب وأفراد العائلة",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                if (activeAccount != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الحساب النشط: ${activeAccount.fullName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "الهاتف: ${activeAccount.phone}  •  الرقم القومي: ${activeAccount.nationalId.ifBlank { "غير مضاف" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("أفراد العائلة") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("تسجيل الدخول") },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("حساب جديد") },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "اختر الفرد الذي تحجز أو تستعرض ملفه الطبي، أو أضف فرداً جديداً:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            familyMembers.forEach { member ->
                                val isSelected = member.id == activeMemberId
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        }
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectMember(member.id)
                                            onDismiss()
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = member.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Text(
                                                text = "${member.relation} • ${member.age} سنة",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (isSelected) {
                                            Text(
                                                text = "نشط ✓",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "إضافة فرد جديد من العائلة",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            OutlinedTextField(
                                value = newMemberName,
                                onValueChange = { newMemberName = it },
                                label = { Text("الاسم الكامل") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = newMemberRelation,
                                    onValueChange = { newMemberRelation = it },
                                    label = { Text("صلة القرابة (الأب/الأم/الابن)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f),
                                )
                                OutlinedTextField(
                                    value = newMemberAge,
                                    onValueChange = { newMemberAge = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("العمر") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f),
                                )
                            }
                            OutlinedTextField(
                                value = newMemberPhone,
                                onValueChange = { newMemberPhone = it },
                                label = { Text("رقم الهاتف") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    onAddFamilyMember(
                                        newMemberName,
                                        newMemberRelation,
                                        newMemberAge.toIntOrNull() ?: 25,
                                        newMemberPhone.ifBlank { activeAccount?.phone ?: "" },
                                    )
                                    newMemberName = ""
                                    newMemberAge = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("add_family_member_button"),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة إلى العائلة")
                            }
                        }

                        1 -> {
                            Text(
                                text = "سجّل الدخول برقم الهاتف أو الرقم القومي (كلمة المرور الافتراضية للحساب التجريبي: 1234)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = { loginIdentifier = it },
                                label = { Text("رقم الهاتف أو الرقم القومي") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("كلمة المرور") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    onLogin(loginIdentifier, loginPassword)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                            ) {
                                Text("دخول")
                            }
                        }

                        2 -> {
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("الاسم ثلاثي") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text("رقم الهاتف") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = regNationalId,
                                onValueChange = { regNationalId = it },
                                label = { Text("الرقم القومي (اختياري)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("كلمة المرور") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    onRegister(regName, regPhone, regNationalId, regPassword)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                            ) {
                                Text("إنشاء الحساب والدخول فوراً")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformConfigDialog(
    currentConfig: PlatformBrandConfig,
    onSave: (
        platformName: String,
        villageName: String,
        hospitalName: String,
        apkDownloadUrl: String,
        medsApiUrl: String,
        adminUsername: String,
        adminPassword: String,
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var platformName by remember { mutableStateOf(currentConfig.platformNameAr) }
    var villageName by remember { mutableStateOf(currentConfig.villageNameAr) }
    var hospitalName by remember { mutableStateOf(currentConfig.hospitalNameAr) }
    var apkUrl by remember { mutableStateOf(currentConfig.apkDownloadUrl) }
    var medsApiUrl by remember { mutableStateOf(currentConfig.medicationsApiUrl) }
    var adminUser by remember { mutableStateOf(currentConfig.adminUsername) }
    var adminPass by remember { mutableStateOf(currentConfig.adminPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات المنصة الموحدة (Config)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "يتم سحب اسم المنصة، المستشفى، رابط تحميل الـ APK، وبيانات الإدارة العليا من الإعدادات الموحدة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = platformName,
                    onValueChange = { platformName = it },
                    label = { Text("اسم المنصة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = villageName,
                    onValueChange = { villageName = it },
                    label = { Text("اسم القرية / المركز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("اسم المستشفى الرئيسي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apkUrl,
                    onValueChange = { apkUrl = it },
                    label = { Text("رابط تحميل APK المباشر (GitHub Releases)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = medsApiUrl,
                    onValueChange = { medsApiUrl = it },
                    label = { Text("رابط API الأدوية المصرية (workers.dev - اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "بيانات دخول الإدارة العليا (${currentConfig.adminOwnerName}):",
                    style = MaterialTheme.typography.titleSmall,
                    color = TealPrimary,
                )
                OutlinedTextField(
                    value = adminUser,
                    onValueChange = { adminUser = it },
                    label = { Text("اسم مستخدم الإدارة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = adminPass,
                    onValueChange = { adminPass = it },
                    label = { Text("كلمة مرور الإدارة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(platformName, villageName, hospitalName, apkUrl, medsApiUrl, adminUser, adminPass)
                    onDismiss()
                }
            ) {
                Text("حفظ وتطبيق")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    }
                ) {
                    Text("استعادة الافتراضي")
                }
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        },
    )
}

@Composable
fun NotificationsDialog(
    notifications: List<DelayNotificationEntity>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("تنبيهات تأخير المواعيد", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (notifications.isEmpty()) {
                    Text(
                        text = "لا توجد تنبيهات تأخير حالياً. جميع العيادات تعمل في مواعيدها المحددة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.height(300.dp),
                    ) {
                        items(notifications, key = { it.id }) { alert ->
                            Surface(
                                color = DelayWarningBg,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DelayWarningBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = DelayWarningBorder,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = alert.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DelayWarningText,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("حسناً")
                }
            }
        }
    }
}

@Composable
fun FullImagePreviewDialog(
    imagePath: String,
    title: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.92f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إغلاق", color = Color.White)
                    }
                }
            }
        }
    }
}
