package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.bi
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.TealPrimary
import com.example.util.TebNotificationHelper

@Composable
fun NotificationStatusCard(
    isEn: Boolean = false,
    tagPrefix: String = "notif_card",
    onShowMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember {
        mutableStateOf(TebNotificationHelper.hasNotificationPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = TebNotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isGranted = granted || TebNotificationHelper.hasNotificationPermission(context)
        if (isGranted) {
            TebNotificationHelper.ensureNotificationChannel(context)
            onShowMessage(
                bi(
                    isEn,
                    "تم تفعيل إشعارات الخلفية والتذكير بالمواعيد بنجاح ✓",
                    "Background notifications & reminders enabled ✓",
                )
            )
        } else {
            TebNotificationHelper.openAppNotificationSettings(context)
            onShowMessage(
                bi(
                    isEn,
                    "يرجى السماح بالإشعارات من إعدادات التطبيق ليصلك التنبيه في الخلفية",
                    "Please allow notifications in Android App Settings",
                )
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) SlotAvailableBg.copy(alpha = 0.55f) else DelayWarningBg.copy(alpha = 0.7f),
        ),
        border = BorderStroke(
            1.2.dp,
            if (isGranted) SlotAvailableBorder.copy(alpha = 0.65f) else DelayWarningBorder.copy(alpha = 0.75f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_root"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (isGranted) SlotAvailableText else DelayWarningText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = bi(isEn, "🔔 حالة الإشعارات والتنبيهات", "🔔 Notification & Alert Status"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) SlotAvailableText else DelayWarningText,
                    )
                }
                Surface(
                    color = if (isGranted) SlotAvailableBorder else DelayWarningBorder,
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Text(
                        text = if (isGranted) bi(isEn, "مفعّلة ✓", "Enabled ✓") else bi(isEn, "غير مفعّلة", "Disabled"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                text = bi(
                    isEn,
                    "تصلك إشعارات الخلفية للرسائل ونتائج التحاليل والتذكير قبل المواعيد (قبل بساعتين/ساعة ونصف ساعة) حتى عند إغلاق التطبيق.",
                    "Receive background alerts for messages, lab results, and automatic reminders (2h/1h and 30m before appointments) even when closed.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isGranted) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                TebNotificationHelper.isRuntimeNotificationPermissionRequired(context)
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                TebNotificationHelper.openAppNotificationSettings(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("${tagPrefix}_enable_button"),
                    ) {
                        Text(bi(isEn, "تفعيل الإشعارات الآن", "Enable Notifications"))
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            TebNotificationHelper.isRuntimeNotificationPermissionRequired(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val sent = TebNotificationHelper.showHighPriorityNotification(
                                context = context,
                                title = bi(isEn, "إشعار تجريبي — طب الحجايزة", "Test Alert — Teb Al-Hajayzah"),
                                body = bi(
                                    isEn,
                                    "تعمل إشعارات الخلفية والتذكير بالمواعيد والرسائل بنجاح مع الصوت والتنبيه ✓",
                                    "Background alerts, appointment reminders, and sounds are working properly ✓",
                                ),
                            )
                            isGranted = TebNotificationHelper.hasNotificationPermission(context)
                            if (sent) {
                                onShowMessage(
                                    bi(
                                        isEn,
                                        "تم إرسال إشعار تجريبي إلى شريط الإشعارات بنجاح ✓",
                                        "Test notification sent to status bar ✓",
                                    )
                                )
                            } else {
                                TebNotificationHelper.openAppNotificationSettings(context)
                                onShowMessage(
                                    bi(
                                        isEn,
                                        "يرجى منح إذن الإشعارات من إعدادات الجهاز أولاً",
                                        "Please grant notification permission in device settings first",
                                    )
                                )
                            }
                        }
                    },
                    border = BorderStroke(1.dp, TealPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("${tagPrefix}_test_button"),
                ) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = bi(isEn, "🚀 إرسال إشعار تجريبي", "🚀 Send Test Alert"),
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
