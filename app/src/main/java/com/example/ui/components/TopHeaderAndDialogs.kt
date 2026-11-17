package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.config.PlatformBrandConfig
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.PatientAccountEntity
import com.example.ui.AppLanguage
import com.example.ui.AppRoleMode
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
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import kotlinx.coroutines.delay
import java.io.File
import java.net.URLEncoder

@Composable
fun TebTopHeader(
    brandConfig: PlatformBrandConfig,
    appLanguage: AppLanguage = AppLanguage.AR,
    roleMode: AppRoleMode,
    unreadAlertsCount: Int,
    activeAccount: PatientAccountEntity?,
    isRefreshing: Boolean = false,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSwitchRole: (AppRoleMode) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    val isEn = appLanguage == AppLanguage.EN
    var langMenuExpanded by remember { mutableStateOf(false) }

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
                .padding(horizontal = 10.dp, vertical = 10.dp),
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
                        contentDescription = bi(isEn, brandConfig.platformNameAr, brandConfig.platformNameEn),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = bi(isEn, brandConfig.platformNameAr, brandConfig.platformNameEn),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                        )
                        Text(
                            text = bi(isEn, brandConfig.villageNameAr, brandConfig.villageNameEn),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Compact AR / EN Dropdown Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { langMenuExpanded = true }
                                .testTag("language_toggle_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = appLanguage.code,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = bi(isEn, "تغيير اللغة", "Change Language"),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false },
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = lang.displayLabel,
                                            fontWeight = if (lang == appLanguage) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    trailingIcon = {
                                        if (lang == appLanguage) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = TealPrimary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                    onClick = {
                                        langMenuExpanded = false
                                        onSelectLanguage(lang)
                                    },
                                    modifier = Modifier.testTag("lang_option_${lang.code.lowercase()}"),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(width = 40.dp, height = 48.dp)
                            .testTag("refresh_button"),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = bi(isEn, "تحديث البيانات والمواعيد", "Refresh data & appointments"),
                                tint = Color.White,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(width = 40.dp, height = 48.dp)
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
                                contentDescription = bi(isEn, "الإشعارات والتنبيهات", "Notifications"),
                                tint = Color.White,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .size(width = 40.dp, height = 48.dp)
                            .testTag("account_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = activeAccount?.fullName ?: bi(isEn, "حسابي والعائلة", "Account & Family"),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
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
                        label = AppRoleMode.PATIENT.label(isEn),
                        icon = Icons.Filled.LocalHospital,
                        testTag = "switch_to_patient_mode",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchRole(AppRoleMode.PATIENT) },
                    )
                    RoleModePillButton(
                        selected = roleMode == AppRoleMode.DOCTOR_ADMIN,
                        label = AppRoleMode.DOCTOR_ADMIN.label(isEn),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
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
    otpWhatsappPhone: String = "01141271027",
    appVersionLabel: String,
    isEn: Boolean = false,
    onSelectMember: (String) -> Unit,
    onAddFamilyMember: (name: String, relation: String, age: Int, phone: String) -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (name: String, phone: String, nationalId: String, password: String) -> Unit,
    onLogoutPatient: () -> Unit = {},
    onVerifyOtp: (code: String) -> Unit,
    onTriggerHiddenAdmin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember(activeAccount?.id) {
        mutableIntStateOf(if (activeAccount == null) 1 else 0)
    }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf(bi(isEn, "الابن", "Son")) }
    var newMemberAge by remember { mutableStateOf("") }
    var newMemberPhone by remember(activeAccount?.phone) { mutableStateOf(activeAccount?.phone ?: "") }

    var loginIdentifier by remember(activeAccount?.phone) { mutableStateOf(activeAccount?.phone ?: "") }
    var loginPassword by remember { mutableStateOf("") }

    var regName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regNationalId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }

    var otpInput by remember { mutableStateOf("") }
    var versionTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeAccount?.isPhoneVerified) {
        if (activeAccount != null && !activeAccount.isPhoneVerified) {
            selectedTab = 0
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
                        text = bi(isEn, "حسابي وأفراد العائلة", "Account & Family Members"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                if (activeAccount != null) {
                    val isVerified = activeAccount.isPhoneVerified
                    Surface(
                        color = if (isVerified) MaterialTheme.colorScheme.primaryContainer else SlotBookedBg,
                        shape = RoundedCornerShape(14.dp),
                        border = if (!isVerified) BorderStroke(1.dp, SlotBookedBorder) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isVerified) Icons.Filled.VerifiedUser else Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer else SlotBookedText,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bi(isEn, "الحساب النشط: ${activeAccount.fullName}", "Active Account: ${activeAccount.fullName}"),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer else SlotBookedText,
                                    )
                                    Text(
                                        text = bi(
                                            isEn,
                                            "الهاتف: ${activeAccount.phone}  •  ${if (isVerified) "موثق ✓" else "بانتظار كود التفعيل (OTP)"}",
                                            "Phone: ${activeAccount.phone}  •  ${if (isVerified) "Verified ✓" else "Pending OTP Activation"}",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else SlotBookedText,
                                    )
                                }
                                TextButton(onClick = onLogoutPatient) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = bi(isEn, "تسجيل الخروج", "Logout"),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(bi(isEn, "خروج", "Logout"))
                                }
                            }

                            if (!isVerified) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DelayWarningBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = bi(isEn, "تفعيل الحساب لإتمام الحجز (OTP)", "Activate Account for Booking (OTP)"),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TealPrimary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(
                                            text = bi(
                                                isEn,
                                                "Send your details on WhatsApp to receive a single-use 6-digit activation code, or obtain it directly from the clinic secretary / doctor:",
                                                "أرسل بياناتك عبر واتساب لأخذ كود التفعيل المكون من 6 أرقام (صالح للاستخدام مرة واحدة)، أو استلمه مباشرة من سكرتيرة العيادة / الطبيب:",
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                        val cleanOtpDigits = otpWhatsappPhone.filter { it.isDigit() }.ifBlank { "01141271027" }
                                        val waOtpPhone = if (cleanOtpDigits.startsWith("2")) cleanOtpDigits else "2$cleanOtpDigits"
                                        val waMessage = "مرحباً إدارة طب الحجايزة، أرجو إرسال كود تفعيل حسابي - الاسم: ${activeAccount.fullName} - الرقم: ${activeAccount.phone}"

                                        Button(
                                            onClick = {
                                                val encoded = URLEncoder.encode(waMessage, "UTF-8")
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://wa.me/$waOtpPhone?text=$encoded"),
                                                )
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SlotAvailableBorder,
                                                contentColor = Color.White,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("otp_whatsapp_request_button"),
                                        ) {
                                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                bi(
                                                    isEn,
                                                    "أرسل بياناتك على واتساب لأخذ كود التفعيل (OTP)",
                                                    "Send details on WhatsApp for OTP code",
                                                )
                                            )
                                        }

                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { ch ->
                                                otpInput = ch.filter { it.isDigit() }.take(6)
                                            },
                                            label = { Text(bi(isEn, "أدخل كود التفعيل (6 أرقام)", "Enter 6-digit OTP Code")) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("otp_code_input"),
                                        )

                                        Button(
                                            onClick = { onVerifyOtp(otpInput) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("otp_verify_submit"),
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(bi(isEn, "تأكيد وتفعيل الحساب", "Verify & Activate Account"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(bi(isEn, "أفراد العائلة", "Family"), maxLines = 1) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(bi(isEn, "تسجيل الدخول", "Login"), maxLines = 1) },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(bi(isEn, "حساب جديد", "Register"), maxLines = 1) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (activeAccount == null) {
                                Text(
                                    text = bi(
                                        isEn,
                                        "يرجى تسجيل الدخول أو إنشاء حساب جديد أولاً لإدارة أفراد الأسرة.",
                                        "Please login or create an account first to manage family members.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = bi(
                                        isEn,
                                        "اختر الفرد الذي تحجز أو تستعرض ملفه الطبي، أو أضف فرداً جديداً:",
                                        "Select the family member to book for or view their file, or add a new member:",
                                    ),
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
                                                    text = bi(
                                                        isEn,
                                                        "${member.relation} • ${member.age} سنة",
                                                        "${member.relation} • ${member.age} yrs",
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            if (isSelected) {
                                                Text(
                                                    text = bi(isEn, "نشط ✓", "Active ✓"),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = bi(isEn, "إضافة فرد جديد من العائلة", "Add Family Member"),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                OutlinedTextField(
                                    value = newMemberName,
                                    onValueChange = { newMemberName = it },
                                    label = { Text(bi(isEn, "الاسم الكامل", "Full Name")) },
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
                                        label = { Text(bi(isEn, "صلة القرابة (الأب/الأم/الابن)", "Relation")) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                    )
                                    OutlinedTextField(
                                        value = newMemberAge,
                                        onValueChange = { newMemberAge = it.filter { ch -> ch.isDigit() } },
                                        label = { Text(bi(isEn, "العمر", "Age")) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f),
                                    )
                                }
                                OutlinedTextField(
                                    value = newMemberPhone,
                                    onValueChange = { newMemberPhone = it },
                                    label = { Text(bi(isEn, "رقم الهاتف", "Phone Number")) },
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
                                            newMemberPhone.ifBlank { activeAccount.phone },
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
                                    Text(bi(isEn, "إضافة إلى العائلة", "Add Member"))
                                }
                            }
                        }

                        1 -> {
                            Text(
                                text = bi(
                                    isEn,
                                    "سجّل الدخول برقم الهاتف أو الرقم القومي المسجل:",
                                    "Login using your registered phone or national ID:",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = { loginIdentifier = it },
                                label = { Text(bi(isEn, "رقم الهاتف أو الرقم القومي", "Phone or National ID")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PasswordOutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = bi(isEn, "كلمة المرور", "Password"),
                                toggleTestTag = "login_password_toggle",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
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
                                Text(bi(isEn, "دخول", "Login"))
                            }
                        }

                        2 -> {
                            Text(
                                text = bi(
                                    isEn,
                                    "يتطلب تفعيل الحساب الجديد إدخال رمز تحقق (OTP) مكون من 6 أرقام عبر الهاتف.",
                                    "New accounts require a 6-digit single-use OTP activation code.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text(bi(isEn, "الاسم ثلاثي", "Full Name")) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input"),
                            )
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text(bi(isEn, "رقم الهاتف", "Phone Number")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_phone_input"),
                            )
                            OutlinedTextField(
                                value = regNationalId,
                                onValueChange = { regNationalId = it },
                                label = { Text(bi(isEn, "الرقم القومي (اختياري)", "National ID (Optional)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PasswordOutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = bi(isEn, "كلمة المرور", "Password"),
                                toggleTestTag = "reg_password_toggle",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input"),
                            )
                            Button(
                                onClick = {
                                    onRegister(regName, regPhone, regNationalId, regPassword)
                                    selectedTab = 0
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("reg_submit_button"),
                            ) {
                                Text(bi(isEn, "إنشاء الحساب والمتابعة للتفعيل (OTP)", "Create Account & Activate (OTP)"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bi(isEn, "إصدار التطبيق: $appVersionLabel", "App Version: $appVersionLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                versionTapCount += 1
                                if (versionTapCount >= 5) {
                                    versionTapCount = 0
                                    onDismiss()
                                    onTriggerHiddenAdmin()
                                }
                            }
                            .padding(vertical = 8.dp)
                            .testTag("hidden_version_trigger"),
                    )
                }
            }
        }
    }
}

@Composable
fun AdminPinLockDialog(
    ownerName: String,
    failedAttempts: Int,
    lockoutUntilMillis: Long,
    isEn: Boolean = false,
    onSubmitPin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pinInput by remember { mutableStateOf("") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isLocked = lockoutUntilMillis > nowMillis

    LaunchedEffect(lockoutUntilMillis) {
        while (lockoutUntilMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
        nowMillis = System.currentTimeMillis()
    }

    val remainingSeconds = ((lockoutUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val remMins = remainingSeconds / 60L
    val remSecs = remainingSeconds % 60L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = TealPrimary)
                Text(bi(isEn, "الدخول المحمي للإدارة", "Protected Admin Access"))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = bi(isEn, "بوابة خاصة بالإدارة العليا ($ownerName)", "Super Admin Portal ($ownerName)"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isLocked) {
                    Surface(
                        color = SlotBookedBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlotBookedBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = bi(
                                isEn,
                                "تم قفل البوابة مؤقتاً لمدة 15 دقيقة بعد 3 محاولات خاطئة.\nالوقت المتبقي: ${remMins} د : ${remSecs.toString().padStart(2, '0')} ث",
                                "Portal temporarily locked for 15 minutes after 3 failed attempts.\nRemaining: ${remMins}m : ${remSecs.toString().padStart(2, '0')}s",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotBookedText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                } else {
                    PasswordOutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = bi(isEn, "الرقم السري للوحة التحكم (Admin PIN)", "Admin PIN"),
                        keyboardType = KeyboardType.NumberPassword,
                        toggleTestTag = "admin_pin_toggle",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input"),
                    )
                    if (failedAttempts > 0) {
                        Text(
                            text = bi(
                                isEn,
                                "تنبيه: محاولات خاطئة ($failedAttempts من 3) قبل القفل الأمني لمدة 15 دقيقة.",
                                "Warning: Failed attempts ($failedAttempts of 3) before 15-minute lockout.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlotBookedBorder,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitPin(pinInput) },
                enabled = !isLocked && pinInput.isNotBlank(),
                modifier = Modifier.testTag("admin_pin_submit"),
            ) {
                Text(bi(isEn, "تحقق ودخول", "Verify & Enter"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(bi(isEn, "إغلاق", "Close"))
            }
        },
    )
}

@Composable
fun NotificationsDialog(
    notifications: List<DelayNotificationEntity>,
    isEn: Boolean = false,
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
                    Text(
                        bi(isEn, "تنبيهات تأخير المواعيد", "Appointment Delay Alerts"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (notifications.isEmpty()) {
                    Text(
                        text = bi(
                            isEn,
                            "لا توجد تنبيهات تأخير حالياً. جميع العيادات تعمل في مواعيدها المحددة.",
                            "No delay alerts right now. All clinics are running on schedule.",
                        ),
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
                    Text(bi(isEn, "حسناً", "OK"))
                }
            }
        }
    }
}

@Composable
fun FullImagePreviewDialog(
    imagePath: String,
    title: String,
    isEn: Boolean = false,
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
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(bi(isEn, "إغلاق", "Close"), color = Color.White)
                    }
                }
            }
        }
    }
}
