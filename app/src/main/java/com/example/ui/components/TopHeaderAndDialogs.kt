package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.local.ChatThreadType
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.DirectMessageEntity
import com.example.data.local.FamilyLinkRequestEntity
import com.example.data.local.FamilyLinkStatus
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MessageSenderRole
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
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TebTopHeader(
    brandConfig: PlatformBrandConfig,
    appLanguage: AppLanguage = AppLanguage.AR,
    roleMode: AppRoleMode,
    unreadAlertsCount: Int,
    activeAccount: PatientAccountEntity?,
    isRefreshing: Boolean = false,
    hasNewDataBanner: Boolean = false,
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
                    var logoTapCount by remember { mutableIntStateOf(0) }
                    Image(
                        painter = painterResource(id = brandConfig.logoRes),
                        contentDescription = bi(isEn, brandConfig.platformNameAr, brandConfig.platformNameEn),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                logoTapCount += 1
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    onSwitchRole(AppRoleMode.SUPER_ADMIN)
                                }
                            }
                            .testTag("hidden_admin_logo_trigger"),
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

                    // Compact 🔄 تحديث Refresh Pill Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !isRefreshing, onClick = onRefresh)
                            .testTag("refresh_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = bi(isEn, "تحديث البيانات والمواعيد", "Refresh data & appointments"),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = bi(isEn, "تحديث", "Sync"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
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

            if (hasNewDataBanner) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onRefresh)
                        .testTag("live_update_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bi(isEn, "تتوفر بيانات جديدة - اضغط للتحديث", "New data available - Tap to refresh"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                        )
                    }
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

private fun filterDigitsOnly(raw: String): String {
    if (raw.isBlank()) return ""
    return buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> append(ch)
                in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - 0x0660)).toChar())
                in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - 0x06F0)).toChar())
            }
        }
    }
}

@Composable
fun AccountAndFamilyDialog(
    activeAccount: PatientAccountEntity?,
    familyMembers: List<FamilyMemberEntity>,
    activeMemberId: String,
    familyLinkRequests: List<FamilyLinkRequestEntity> = emptyList(),
    otpWhatsappPhone: String = "01141271027",
    appVersionLabel: String,
    unreadSupportCount: Int = 0,
    isEn: Boolean = false,
    onSelectMember: (String) -> Unit,
    onAddFamilyMember: (name: String, relation: String, age: Int, phone: String, nationalId: String) -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (name: String, phone: String, nationalId: String, password: String) -> Unit,
    onLogoutPatient: () -> Unit = {},
    onVerifyOtp: (code: String) -> Unit,
    onRegenerateOtp: () -> Unit = {},
    onOpenSupportChat: () -> Unit = {},
    onSubmitFamilyLinkRequest: (targetPhone: String, marriageUrl: String, idFrontUrl: String, idBackUrl: String) -> Unit = { _, _, _, _ -> },
    onSetSpouseViewPermission: (allow: Boolean) -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTermsOfUse: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember(activeAccount?.id) {
        mutableIntStateOf(if (activeAccount == null) 1 else 0)
    }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf(bi(isEn, "الابن", "Son")) }
    var newMemberAge by remember { mutableStateOf("") }
    var newMemberNationalId by remember { mutableStateOf("") }
    var newMemberPhone by remember(activeAccount?.phone) { mutableStateOf(activeAccount?.phone ?: "") }

    var spousePhoneInput by remember { mutableStateOf("") }
    var proofMarriageUrl by remember { mutableStateOf("") }
    var proofIdFrontUrl by remember { mutableStateOf("") }
    var proofIdBackUrl by remember { mutableStateOf("") }
    var activeProofSlot by remember { mutableIntStateOf(0) } // 0=marriage, 1=front, 2=back

    val proofImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageHelper.persistDocumentOrImageUri(context, uri)
            val dataUri = saved?.path?.let { ImageStorageHelper.encodeImageFileToCompactDataUri(it) }.orEmpty()
            val resolved = dataUri.ifBlank { saved?.path.orEmpty() }
            if (resolved.isNotBlank()) {
                when (activeProofSlot) {
                    0 -> proofMarriageUrl = resolved
                    1 -> proofIdFrontUrl = resolved
                    else -> proofIdBackUrl = resolved
                }
            } else {
                onShowMessage(bi(isEn, "تعذر قراءة الصورة المختارة", "Could not read selected image"))
            }
        }
    }

    var loginIdentifier by remember(activeAccount?.phone) { mutableStateOf(activeAccount?.phone ?: "") }
    var loginPassword by remember { mutableStateOf("") }

    var regName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regNationalId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }

    var otpInput by remember { mutableStateOf("") }

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
                                val verifiedSource = activeAccount.verifiedBy.ifBlank { "كود OTP" }
                                val verifiedLabelAr = if (verifiedSource == "كود OTP") {
                                    "موثق ✓ (مُفعّل عبر كود OTP)"
                                } else {
                                    "موثق ✓ (مُفعّل بواسطة: $verifiedSource)"
                                }
                                val verifiedLabelEn = if (verifiedSource == "كود OTP") {
                                    "Verified ✓ (Activated via OTP)"
                                } else {
                                    "Verified ✓ (By: $verifiedSource)"
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bi(isEn, "الحساب النشط: ${activeAccount.fullName}", "Active Account: ${activeAccount.fullName}"),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer else SlotBookedText,
                                    )
                                    Text(
                                        text = bi(
                                            isEn,
                                            "الهاتف: ${activeAccount.phone}  •  ${if (isVerified) verifiedLabelAr else "غير موثق (بانتظار التفعيل)"}",
                                            "Phone: ${activeAccount.phone}  •  ${if (isVerified) verifiedLabelEn else "Unverified (Pending Activation)"}",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else SlotBookedText,
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

                            if (isVerified) {
                                Surface(
                                    color = SlotAvailableBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlotAvailableBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth().testTag("otp_verified_confirmation"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SlotAvailableText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = bi(
                                                isEn,
                                                "تم تفعيل وتوثيق حسابك بنجاح (${activeAccount.verifiedBy.ifBlank { "كود OTP" }}). يمكنك حجز جميع العيادات الآن.",
                                                "Your account is verified & ready (${activeAccount.verifiedBy.ifBlank { "OTP" }}). You can book all clinics now.",
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SlotAvailableText,
                                        )
                                    }
                                }
                            } else {
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
                                                "أرسل بياناتك عبر واتساب لتأكيد وتفعيل حسابك، أو اطلب التفعيل المباشر من سكرتيرة العيادة / الطبيب برقم هاتفك:",
                                                "Send your details on WhatsApp to confirm & activate your account, or get activated directly at any clinic:",
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                        val nowMillis = System.currentTimeMillis()
                                        val isOtpExpired = activeAccount.otpCode.length != 6 ||
                                            activeAccount.otpExpiresAtMillis <= 0L ||
                                            nowMillis > activeAccount.otpExpiresAtMillis
                                        if (isOtpExpired) {
                                            Surface(
                                                color = SlotBookedBg,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SlotBookedBorder),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = bi(
                                                            isEn,
                                                            "⏰ انتهت صلاحية كود التفعيل (مضى أكثر من 24 ساعة). اضغط لتوليد كود جديد:",
                                                            "⏰ Your OTP code expired (> 24h). Tap below to generate a new code:",
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SlotBookedText,
                                                    )
                                                    Button(
                                                        onClick = onRegenerateOtp,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = DelayWarningBorder,
                                                            contentColor = Color.White,
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(48.dp)
                                                            .testTag("otp_regenerate_button"),
                                                    ) {
                                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(bi(isEn, "توليد كود جديد", "Generate New Code"))
                                                    }
                                                }
                                            }
                                        } else {
                                            val remMins = (((activeAccount.otpExpiresAtMillis - nowMillis) + 59_999L) / 60_000L).coerceAtLeast(1L)
                                            val remHours = remMins / 60L
                                            val remM = remMins % 60L
                                            val validityLabel = if (remHours > 0) {
                                                bi(isEn, "⏳ الكود الخاص بحسابك صالح لمدة $remHours س و $remM د", "⏳ Your account OTP is valid for ${remHours}h ${remM}m")
                                            } else {
                                                bi(isEn, "⏳ الكود الخاص بحسابك صالح لمدة $remM دقيقة", "⏳ Your account OTP is valid for ${remM}m")
                                            }
                                            Text(
                                                text = validityLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SlotAvailableText,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        val cleanOtpDigits = otpWhatsappPhone.filter { it.isDigit() }.ifBlank { "01141271027" }
                                        val waOtpPhone = if (cleanOtpDigits.startsWith("2")) cleanOtpDigits else "2$cleanOtpDigits"
                                        val waMessage = "مرحباً إدارة طب الحجايزة، قمت بالتسجيل وأرجو توثيق حسابي أو إرسال كود التفعيل - الاسم: ${activeAccount.fullName} - الهاتف: ${activeAccount.phone}"

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
                                                    "أرسل بياناتك على واتساب للتوثيق أو استلام الكود",
                                                    "Send details on WhatsApp to verify or get code",
                                                )
                                            )
                                        }

                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { ch ->
                                                val normalized = buildString {
                                                    for (c in ch) {
                                                        when (c) {
                                                            in '\u0660'..'\u0669' -> append(('0'.code + (c.code - 0x0660)).toChar())
                                                            in '\u06F0'..'\u06F9' -> append(('0'.code + (c.code - 0x06F0)).toChar())
                                                            in '0'..'9' -> append(c)
                                                        }
                                                    }
                                                }
                                                otpInput = normalized.take(6)
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

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenSupportChat()
                                },
                                border = BorderStroke(1.2.dp, TealPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("account_support_admin_chat_button"),
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (unreadSupportCount > 0) {
                                        bi(
                                            isEn,
                                            "💬 مراسلة الإدارة / الدعم الفني ($unreadSupportCount جديد)",
                                            "💬 Message Admin / Support ($unreadSupportCount new)",
                                        )
                                    } else {
                                        bi(
                                            isEn,
                                            "💬 مراسلة الإدارة / الدعم الفني",
                                            "💬 Message Admin / Support",
                                        )
                                    },
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
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
                                                        "${member.relation} • ${member.age} سنة${if (member.nationalId.isNotBlank()) " • قومي: ${member.nationalId}" else ""}",
                                                        "${member.relation} • ${member.age} yrs${if (member.nationalId.isNotBlank()) " • ID: ${member.nationalId}" else ""}",
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
                                    value = newMemberNationalId,
                                    onValueChange = { newMemberNationalId = filterDigitsOnly(it).take(14) },
                                    label = { Text(bi(isEn, "الرقم القومي (14 رقم - اختياري)", "National ID (14 digits - optional)")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
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
                                            newMemberNationalId,
                                        )
                                        newMemberName = ""
                                        newMemberAge = ""
                                        newMemberNationalId = ""
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

                                // ربط حسابات الأسرة (الزوج / الزوجة) + خيار الخصوصية
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = bi(isEn, "👨‍👩‍👧‍👦 ربط حسابات الأسرة (الزوج / الزوجة)", "👨‍👩‍👧‍👦 Family Account Linking (Spouse)"),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (activeAccount.linkedSpouseAccountId.isNotBlank()) {
                                            Surface(
                                                color = SlotAvailableBg,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SlotAvailableBorder),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = bi(
                                                            isEn,
                                                            "✓ مرتبط بحساب: ${activeAccount.linkedSpouseName} (${activeAccount.linkedSpousePhone})",
                                                            "✓ Linked with: ${activeAccount.linkedSpouseName} (${activeAccount.linkedSpousePhone})",
                                                        ),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = SlotAvailableText,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    Text(
                                                        text = bi(
                                                            isEn,
                                                            "ملفات الأبناء مشتركة تلقائياً في حسابيكما. ملفك الشخصي محجوب افتراضياً ما لم تسمح بذلك أدناه.",
                                                            "Children's files sync automatically between both parents. Your personal file stays private unless enabled below.",
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = bi(
                                                                isEn,
                                                                "🔓 السماح للزوج/الزوجة بالاطلاع على ملفي الطبي الشخصي",
                                                                "🔓 Allow spouse to view my personal medical file",
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                        Switch(
                                                            checked = activeAccount.allowSpouseViewPersonalRecords,
                                                            onCheckedChange = onSetSpouseViewPermission,
                                                            modifier = Modifier.testTag("spouse_privacy_toggle"),
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            val pendingReq = familyLinkRequests.firstOrNull {
                                                it.status == FamilyLinkStatus.PENDING &&
                                                    (it.requesterAccountId == activeAccount.id || it.targetPhone == activeAccount.phone)
                                            }
                                            if (pendingReq != null) {
                                                Surface(
                                                    color = DelayWarningBg,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, DelayWarningBorder),
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Text(
                                                        text = bi(
                                                            isEn,
                                                            "⏳ لديك طلب ربط أسرة قيد المراجعة مع (${pendingReq.targetPhone}) — بانتظار موافقة الإدارة",
                                                            "⏳ Pending link request with (${pendingReq.targetPhone}) awaiting Admin review",
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DelayWarningText,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(10.dp),
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = bi(
                                                        isEn,
                                                        "اربط حسابك بحساب الزوج/الزوجة لمشاركة ملفات الأبناء مع إرفاق قسيمة الزواج أو وجهي البطاقة:",
                                                        "Link with spouse account to share children's medical files (attach marriage certificate or ID photos):",
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                OutlinedTextField(
                                                    value = spousePhoneInput,
                                                    onValueChange = { spousePhoneInput = filterDigitsOnly(it) },
                                                    label = { Text(bi(isEn, "رقم هاتف الزوج / الزوجة المسجل *", "Spouse Phone Number *")) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("spouse_link_phone_input"),
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            activeProofSlot = 0
                                                            proofImagePicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                                    ) {
                                                        Text(
                                                            text = if (proofMarriageUrl.isNotBlank()) "✓ القسيمة" else "قسيمة الزواج",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            activeProofSlot = 1
                                                            proofImagePicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                                    ) {
                                                        Text(
                                                            text = if (proofIdFrontUrl.isNotBlank()) "✓ بطاقة وجه" else "بطاقة (وجه)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            activeProofSlot = 2
                                                            proofImagePicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                                    ) {
                                                        Text(
                                                            text = if (proofIdBackUrl.isNotBlank()) "✓ بطاقة ظهر" else "بطاقة (ظهر)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        if (spousePhoneInput.length < 6) {
                                                            onShowMessage(bi(isEn, "يرجى إدخال رقم هاتف صحيح للزوج/الزوجة", "Enter valid spouse phone"))
                                                        } else if (proofMarriageUrl.isBlank() && proofIdFrontUrl.isBlank()) {
                                                            onShowMessage(bi(isEn, "يرجى إرفاق صورة قسيمة الزواج أو البطاقة القومية للإدارة", "Attach marriage certificate or National ID"))
                                                        } else {
                                                            onSubmitFamilyLinkRequest(
                                                                spousePhoneInput,
                                                                proofMarriageUrl,
                                                                proofIdFrontUrl,
                                                                proofIdBackUrl,
                                                            )
                                                            spousePhoneInput = ""
                                                            proofMarriageUrl = ""
                                                            proofIdFrontUrl = ""
                                                            proofIdBackUrl = ""
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .testTag("submit_family_link_button"),
                                                ) {
                                                    Text(bi(isEn, "إرسال طلب الربط للإدارة للمراجعة", "Submit Link Request to Admin"))
                                                }
                                            }
                                        }
                                    }
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
                                onValueChange = { loginPassword = filterDigitsOnly(it) },
                                label = bi(isEn, "الرقم السري (أرقام فقط - PIN)", "Numeric PIN (digits only)"),
                                keyboardType = KeyboardType.NumberPassword,
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
                                onValueChange = { regPhone = filterDigitsOnly(it) },
                                label = { Text(bi(isEn, "رقم الهاتف", "Phone Number")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_phone_input"),
                            )
                            OutlinedTextField(
                                value = regNationalId,
                                onValueChange = { regNationalId = filterDigitsOnly(it).take(14) },
                                label = { Text(bi(isEn, "الرقم القومي (14 رقم - اختياري)", "National ID (14 digits - optional)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PasswordOutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = filterDigitsOnly(it) },
                                label = bi(isEn, "الرقم السري (أرقام فقط - PIN) *", "Numeric PIN (digits only) *"),
                                keyboardType = KeyboardType.NumberPassword,
                                toggleTestTag = "reg_password_toggle",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input"),
                            )
                            Button(
                                onClick = {
                                    if (regPassword.length < 4) {
                                        onShowMessage(
                                            bi(
                                                isEn,
                                                "الرقم السري يجب أن يتكون من 4 أرقام على الأقل",
                                                "Numeric PIN must be at least 4 digits",
                                            )
                                        )
                                    } else {
                                        onRegister(regName, regPhone, regNationalId, regPassword)
                                        selectedTab = 0
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("reg_submit_button"),
                            ) {
                                Text(bi(isEn, "إنشاء الحساب والمتابعة للتفعيل (OTP)", "Create Account & Activate (OTP)"))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = onOpenPrivacyPolicy,
                                    modifier = Modifier.testTag("reg_privacy_policy_button"),
                                ) {
                                    Text(
                                        text = bi(isEn, "📜 سياسة الخصوصية", "📜 Privacy Policy"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TealPrimary,
                                    )
                                }
                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(
                                    onClick = onOpenTermsOfUse,
                                    modifier = Modifier.testTag("reg_terms_button"),
                                ) {
                                    Text(
                                        text = bi(isEn, "⚖️ شروط الاستخدام", "⚖️ Terms of Use"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TealPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    NotificationStatusCard(
                        isEn = isEn,
                        tagPrefix = "account_notif",
                        onShowMessage = onShowMessage,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bi(isEn, "إصدار التطبيق: $appVersionLabel", "App Version: $appVersionLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                        onValueChange = { pinInput = filterDigitsOnly(it) },
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

data class DirectChatTarget(
    val threadType: String,
    val viewerRole: String,
    val title: String,
    val subtitle: String = "",
    val patientAccountId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val doctorId: String = "",
)

@Composable
fun NotificationsDialog(
    notifications: List<DelayNotificationEntity>,
    directMessages: List<DirectMessageEntity> = emptyList(),
    roleMode: AppRoleMode = AppRoleMode.PATIENT,
    activeAccount: PatientAccountEntity? = null,
    loggedInDoctor: ClinicDoctorEntity? = null,
    isEn: Boolean = false,
    onMarkAllRead: () -> Unit = {},
    onOpenChatThread: (DirectChatTarget) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val activeThreads = remember(directMessages, roleMode, activeAccount?.id, loggedInDoctor?.id) {
        when (roleMode) {
            AppRoleMode.PATIENT -> {
                val accId = activeAccount?.id.orEmpty()
                if (accId.isBlank()) {
                    emptyList()
                } else {
                    directMessages
                        .filter { it.patientAccountId == accId && (it.threadType == ChatThreadType.PATIENT_CLINIC || it.threadType == ChatThreadType.PATIENT_ADMIN) }
                        .groupBy { "${it.threadType}:${it.doctorId}" }
                        .values
                        .mapNotNull { msgs ->
                            val latest = msgs.maxByOrNull { it.createdAtMillis } ?: return@mapNotNull null
                            val unread = msgs.count { !it.isReadByPatient }
                            Triple(
                                latest,
                                unread,
                                if (latest.threadType == ChatThreadType.PATIENT_ADMIN) {
                                    DirectChatTarget(
                                        threadType = ChatThreadType.PATIENT_ADMIN,
                                        viewerRole = MessageSenderRole.PATIENT,
                                        title = bi(isEn, "الإدارة والدعم الفني", "Platform Admin & Support"),
                                        subtitle = bi(isEn, "محادثة مباشرة مع الإدارة", "Direct support thread"),
                                        patientAccountId = accId,
                                        patientName = activeAccount?.fullName.orEmpty(),
                                        patientPhone = activeAccount?.phone.orEmpty(),
                                    )
                                } else {
                                    DirectChatTarget(
                                        threadType = ChatThreadType.PATIENT_CLINIC,
                                        viewerRole = MessageSenderRole.PATIENT,
                                        title = latest.clinicName.ifBlank { latest.doctorName },
                                        subtitle = latest.doctorName,
                                        patientAccountId = accId,
                                        patientName = activeAccount?.fullName.orEmpty(),
                                        patientPhone = activeAccount?.phone.orEmpty(),
                                        doctorId = latest.doctorId,
                                    )
                                }
                            )
                        }
                        .sortedByDescending { it.first.createdAtMillis }
                }
            }

            AppRoleMode.DOCTOR_ADMIN -> {
                val doc = loggedInDoctor
                if (doc == null) {
                    emptyList()
                } else {
                    directMessages
                        .filter { it.doctorId == doc.id && (it.threadType == ChatThreadType.PATIENT_CLINIC || it.threadType == ChatThreadType.CLINIC_ADMIN) }
                        .groupBy { "${it.threadType}:${it.patientAccountId}" }
                        .values
                        .mapNotNull { msgs ->
                            val latest = msgs.maxByOrNull { it.createdAtMillis } ?: return@mapNotNull null
                            val unread = msgs.count { !it.isReadByClinic }
                            Triple(
                                latest,
                                unread,
                                if (latest.threadType == ChatThreadType.CLINIC_ADMIN) {
                                    DirectChatTarget(
                                        threadType = ChatThreadType.CLINIC_ADMIN,
                                        viewerRole = MessageSenderRole.CLINIC,
                                        title = bi(isEn, "الإدارة العليا للمنصة", "Super Admin Support"),
                                        subtitle = doc.clinicName,
                                        doctorId = doc.id,
                                    )
                                } else {
                                    DirectChatTarget(
                                        threadType = ChatThreadType.PATIENT_CLINIC,
                                        viewerRole = MessageSenderRole.CLINIC,
                                        title = latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") },
                                        subtitle = latest.patientPhone,
                                        patientAccountId = latest.patientAccountId,
                                        patientName = latest.patientName,
                                        patientPhone = latest.patientPhone,
                                        doctorId = doc.id,
                                    )
                                }
                            )
                        }
                        .sortedByDescending { it.first.createdAtMillis }
                }
            }

            AppRoleMode.SUPER_ADMIN -> {
                directMessages
                    .filter { it.threadType == ChatThreadType.PATIENT_ADMIN || it.threadType == ChatThreadType.CLINIC_ADMIN }
                    .groupBy { "${it.threadType}:${it.patientAccountId}:${it.doctorId}" }
                    .values
                    .mapNotNull { msgs ->
                        val latest = msgs.maxByOrNull { it.createdAtMillis } ?: return@mapNotNull null
                        val unread = msgs.count { !it.isReadByAdmin }
                        Triple(
                            latest,
                            unread,
                            if (latest.threadType == ChatThreadType.PATIENT_ADMIN) {
                                DirectChatTarget(
                                    threadType = ChatThreadType.PATIENT_ADMIN,
                                    viewerRole = MessageSenderRole.ADMIN,
                                    title = "${latest.patientName.ifBlank { bi(isEn, "مريض", "Patient") }} (${bi(isEn, "دعم فني", "Support")})",
                                    subtitle = latest.patientPhone,
                                    patientAccountId = latest.patientAccountId,
                                    patientName = latest.patientName,
                                    patientPhone = latest.patientPhone,
                                )
                            } else {
                                DirectChatTarget(
                                    threadType = ChatThreadType.CLINIC_ADMIN,
                                    viewerRole = MessageSenderRole.ADMIN,
                                    title = latest.clinicName.ifBlank { latest.doctorName },
                                    subtitle = latest.doctorName,
                                    doctorId = latest.doctorId,
                                )
                            }
                        )
                    }
                    .sortedByDescending { it.first.createdAtMillis }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        bi(isEn, "الإشعارات والرسائل", "Notifications & Messages"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (notifications.isEmpty() && activeThreads.isEmpty()) {
                    Text(
                        text = bi(
                            isEn,
                            "لا توجد إشعارات أو رسائل حالياً.",
                            "No notifications or messages right now.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.height(360.dp),
                    ) {
                        if (activeThreads.isNotEmpty()) {
                            item(key = "notif_threads_header") {
                                Text(
                                    text = bi(isEn, "المحادثات المباشرة (${activeThreads.size})", "Direct Messages (${activeThreads.size})"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(activeThreads, key = { "thread_${it.first.id}" }) { (latest, unread, target) ->
                                Surface(
                                    color = if (unread > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, if (unread > 0) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = target.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                if (unread > 0) {
                                                    Badge(containerColor = SlotBookedBorder, contentColor = Color.White) {
                                                        Text(unread.toString())
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${latest.senderName}: ${latest.body}",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onOpenChatThread(target)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(38.dp),
                                        ) {
                                            Text(bi(isEn, "رد / فتح", "Reply"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            item(key = "notif_alerts_header") {
                                Text(
                                    text = bi(isEn, "التنبيهات والإشعارات (${notifications.size})", "Alerts & Broadcasts (${notifications.size})"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DelayWarningText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            items(notifications, key = { it.id }) { alert ->
                                Surface(
                                    color = if (!alert.isRead) DelayWarningBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, if (!alert.isRead) DelayWarningBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = if (!alert.isRead) DelayWarningBorder else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (alert.senderLabel.isNotBlank()) {
                                                Text(
                                                    text = alert.senderLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary,
                                                )
                                            }
                                            Text(
                                                text = alert.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (!alert.isRead) DelayWarningText else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (notifications.isNotEmpty() || activeThreads.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("notifications_mark_all_read"),
                        ) {
                            Text(bi(isEn, "تحديد الكل كمقروء", "Mark All Read"), maxLines = 1)
                        }
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(bi(isEn, "إغلاق", "Close"))
                    }
                }
            }
        }
    }
}

@Composable
fun DirectChatDialog(
    target: DirectChatTarget,
    messages: List<DirectMessageEntity>,
    isEn: Boolean = false,
    canDeleteMessages: Boolean = false,
    onDeleteMessage: (messageId: String) -> Unit = {},
    onClearThread: (() -> Unit)? = null,
    onMarkThreadRead: () -> Unit,
    onSendMessage: (body: String, onDone: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftBody by remember { mutableStateOf("") }
    val threadMessages = remember(messages, target) {
        messages.filter { msg ->
            msg.threadType == target.threadType && when (target.threadType) {
                ChatThreadType.PATIENT_CLINIC ->
                    msg.patientAccountId == target.patientAccountId && msg.doctorId == target.doctorId
                ChatThreadType.PATIENT_ADMIN ->
                    msg.patientAccountId == target.patientAccountId
                ChatThreadType.CLINIC_ADMIN ->
                    msg.doctorId == target.doctorId
                else -> false
            }
        }.sortedBy { it.createdAtMillis }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(threadMessages.size, target) {
        onMarkThreadRead()
        if (threadMessages.isNotEmpty()) {
            listState.animateScrollToItem(threadMessages.lastIndex)
        }
    }

    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = TealPrimary)
                        Column {
                            Text(
                                text = target.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (target.subtitle.isNotBlank()) {
                                Text(
                                    text = target.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (canDeleteMessages && onClearThread != null && threadMessages.isNotEmpty()) {
                        TextButton(
                            onClick = onClearThread,
                            colors = ButtonDefaults.textButtonColors(contentColor = SlotBookedBorder),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("direct_chat_clear_all"),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bi(isEn, "مسح الكل", "Clear All"), maxLines = 1)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"))
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp),
                ) {
                    if (threadMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = bi(
                                    isEn,
                                    "ابدأ المحادثة بكتابة رسالتك بالأسفل وسيتم إشعار الطرف الآخر فوراً.",
                                    "Start the conversation below and the recipient will be notified immediately.",
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(threadMessages, key = { it.id }) { msg ->
                                val isMine = msg.senderRole == target.viewerRole
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (canDeleteMessages && isMine) {
                                        IconButton(
                                            onClick = { onDeleteMessage(msg.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("direct_chat_delete_msg_${msg.id}"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = bi(isEn, "حذف الرسالة", "Delete message"),
                                                tint = SlotBookedBorder,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (isMine) TealPrimary else Color.White,
                                        shape = RoundedCornerShape(14.dp),
                                        border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth(0.82f),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMine) Color.White.copy(alpha = 0.9f) else TealPrimary,
                                            )
                                            Text(
                                                text = msg.body,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMine) Color.White else Color(0xFF0F172A),
                                            )
                                            Text(
                                                text = timeFmt.format(Date(msg.createdAtMillis)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isMine) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (canDeleteMessages && !isMine) {
                                        IconButton(
                                            onClick = { onDeleteMessage(msg.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("direct_chat_delete_msg_${msg.id}"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = bi(isEn, "حذف الرسالة", "Delete message"),
                                                tint = SlotBookedBorder,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draftBody,
                        onValueChange = { draftBody = it },
                        placeholder = { Text(bi(isEn, "اكتب رسالتك هنا...", "Type your message...")) },
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("direct_chat_input"),
                    )
                    Button(
                        onClick = {
                            if (draftBody.isNotBlank()) {
                                val textToSend = draftBody
                                onSendMessage(textToSend) { draftBody = "" }
                            }
                        },
                        enabled = draftBody.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("direct_chat_send_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = bi(isEn, "إرسال", "Send"),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullImagePreviewDialog(
    target: AttachmentPreviewTarget,
    isEn: Boolean = false,
    onDeleteRecordFile: ((recordId: String) -> Unit)? = null,
    onShowMessage: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPdf = remember(target.path, target.mimeType) {
        ImageStorageHelper.isPdfAttachment(target.path, target.mimeType)
    }

    var pdfPages by remember(target.path) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pdfLoading by remember(target.path) { mutableStateOf(isPdf) }
    var pdfError by remember(target.path) { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var zoomScale by remember(target.path) { mutableFloatStateOf(1f) }
    var panX by remember(target.path) { mutableFloatStateOf(0f) }
    var panY by remember(target.path) { mutableFloatStateOf(0f) }
    var isFullscreen by remember(target.path) { mutableStateOf(false) }

    LaunchedEffect(target.path, isPdf) {
        if (isPdf) {
            pdfLoading = true
            pdfError = ""
            val res = ImageStorageHelper.renderPdfPagesToBitmaps(target.path)
            res.onSuccess { pages ->
                pdfPages = pages
                pdfLoading = false
            }.onFailure { err ->
                pdfError = err.localizedMessage ?: bi(isEn, "تعذر عرض صفحات PDF", "Could not render PDF pages")
                pdfLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isFullscreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(Color.Black.copy(alpha = 0.62f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.title.ifBlank { target.displayName },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (target.displayName.isNotBlank()) {
                                    Text(
                                        text = target.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = {
                                        zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                        if (zoomScale <= 1.01f) {
                                            panX = 0f
                                            panY = 0f
                                        }
                                    },
                                    modifier = Modifier.testTag("preview_zoom_out"),
                                ) {
                                    Icon(Icons.Filled.ZoomOut, contentDescription = bi(isEn, "تصغير", "Zoom Out"), tint = Color.White)
                                }
                                TextButton(
                                    onClick = {
                                        zoomScale = 1f
                                        panX = 0f
                                        panY = 0f
                                    },
                                    modifier = Modifier.testTag("preview_zoom_reset"),
                                ) {
                                    Text("${(zoomScale * 100).toInt()}%", color = Color.White)
                                }
                                IconButton(
                                    onClick = { zoomScale = (zoomScale + 0.35f).coerceAtMost(4f) },
                                    modifier = Modifier.testTag("preview_zoom_in"),
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = bi(isEn, "تكبير", "Zoom In"), tint = Color.White)
                                }
                                IconButton(
                                    onClick = { isFullscreen = true },
                                    modifier = Modifier.testTag("preview_fullscreen_toggle"),
                                ) {
                                    Icon(Icons.Filled.Fullscreen, contentDescription = bi(isEn, "ملء الشاشة", "Fullscreen"), tint = Color.White)
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Filled.Close, contentDescription = bi(isEn, "إغلاق", "Close"), tint = Color.White)
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .pointerInput(target.path) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                    zoomScale = newScale
                                    if (newScale > 1.02f) {
                                        panX += pan.x
                                        panY += pan.y
                                    } else {
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPdf) {
                            when {
                                pdfLoading -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        CircularProgressIndicator(color = TealPrimary)
                                        Text(
                                            text = bi(isEn, "جاري تجهيز صفحات ملف PDF...", "Rendering PDF pages..."),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }

                                pdfError.isNotBlank() -> {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(44.dp))
                                        Text(
                                            text = pdfError,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Button(
                                            onClick = {
                                                ImageStorageHelper.openAttachmentExternally(
                                                    context = context,
                                                    filePath = target.path,
                                                    mimeType = target.mimeType,
                                                    onError = onShowMessage,
                                                )
                                            },
                                        ) {
                                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(bi(isEn, "فتح بتطبيق PDF خارجي", "Open in external PDF viewer"))
                                        }
                                    }
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = zoomScale,
                                                scaleY = zoomScale,
                                                translationX = panX,
                                                translationY = panY,
                                            ),
                                        contentPadding = PaddingValues(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        itemsIndexed(pdfPages) { index, pageBitmap ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column {
                                                    Image(
                                                        bitmap = pageBitmap.asImageBitmap(),
                                                        contentDescription = bi(isEn, "صفحة PDF ${index + 1}", "PDF page ${index + 1}"),
                                                        contentScale = ContentScale.FillWidth,
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                    Text(
                                                        text = bi(isEn, "صفحة ${index + 1} من ${pdfPages.size}", "Page ${index + 1} of ${pdfPages.size}"),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF475569),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 6.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val decodedDataBitmap = remember(target.path) {
                                if (target.path.startsWith("data:image", ignoreCase = true)) {
                                    ImageStorageHelper.decodeDataUriToBitmap(target.path)
                                } else null
                            }
                            if (decodedDataBitmap != null) {
                                Image(
                                    bitmap = decodedDataBitmap.asImageBitmap(),
                                    contentDescription = target.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .graphicsLayer(
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = panX,
                                            translationY = panY,
                                        ),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                val modelAny: Any = if (target.path.startsWith("http://") || target.path.startsWith("https://")) {
                                    target.path
                                } else {
                                    File(target.path)
                                }
                                AsyncImage(
                                    model = modelAny,
                                    contentDescription = target.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .graphicsLayer(
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = panX,
                                            translationY = panY,
                                        ),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }

                    if (!isFullscreen) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val saved = ImageStorageHelper.saveAttachmentToDownloads(
                                                context = context,
                                                filePath = target.path,
                                                mimeType = target.mimeType,
                                                displayName = target.displayName.ifBlank { target.title },
                                            )
                                            saved.onSuccess { savedFile ->
                                                onShowMessage(
                                                    bi(
                                                        isEn,
                                                        "✓ تم تنزيل الملف في مجلد التنزيلات (Downloads/TebAlHajayzah): $savedFile",
                                                        "✓ Saved to Downloads/TebAlHajayzah: $savedFile",
                                                    )
                                                )
                                            }.onFailure { err ->
                                                onShowMessage(err.localizedMessage ?: bi(isEn, "تعذر تنزيل الملف", "Could not download file"))
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("preview_download_file_button"),
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPdf) {
                                            bi(isEn, "تحميل ملف التحليل (PDF)", "Download Lab PDF")
                                        } else {
                                            bi(isEn, "تحميل الملف", "Download File")
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (isPdf) {
                                    OutlinedButton(
                                        onClick = {
                                            ImageStorageHelper.openAttachmentExternally(
                                                context = context,
                                                filePath = target.path,
                                                mimeType = target.mimeType,
                                                onError = onShowMessage,
                                            )
                                        },
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                        modifier = Modifier.height(48.dp),
                                    ) {
                                        Icon(Icons.Filled.OpenInNew, contentDescription = bi(isEn, "فتح خارجي", "Open External"), tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }

                                val recId = target.recordId
                                if (!recId.isNullOrBlank() && onDeleteRecordFile != null) {
                                    Button(
                                        onClick = { showDeleteConfirm = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDC2626),
                                            contentColor = Color.White,
                                        ),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("preview_delete_file_button"),
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(bi(isEn, "حذف", "Delete"), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFullscreen) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { isFullscreen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("preview_fullscreen_exit"),
                        ) {
                            Icon(Icons.Filled.FullscreenExit, contentDescription = bi(isEn, "إنهاء ملء الشاشة", "Exit Fullscreen"), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(bi(isEn, "إنهاء ملء الشاشة", "Exit"), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && target.recordId != null && onDeleteRecordFile != null) {
        DeleteFileConfirmationDialog(
            fileName = target.displayName.ifBlank { target.title },
            isEn = isEn,
            onConfirmDelete = {
                showDeleteConfirm = false
                onDeleteRecordFile(target.recordId)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
