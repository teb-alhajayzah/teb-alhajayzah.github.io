package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.PlatformBrandConfig
import com.example.data.local.ActivationOtpCodeEntity
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.TealPrimary
import com.example.util.ActivationCodePrintHelper

fun LazyListScope.otpCodeGeneratorItems(
    context: Context,
    brandConfig: PlatformBrandConfig,
    activationCodes: List<ActivationOtpCodeEntity>,
    onGenerateBatch: (Int) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val availableCount = activationCodes.count { !it.isUsed }
    val usedCount = activationCodes.count { it.isUsed }

    item(key = "otp_generator_controls") {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = TealPrimary)
                    Text(
                        text = "مولد أكواد التفعيل ذات الاستخدام الواحد (OTP)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "يُستخدم كل كود مرة واحدة فقط لتفعيل حساب مريض جديد ويحترق تلقائياً بعد الاستخدام لمنع الحجوزات الوهمية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = SlotAvailableBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = availableCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = SlotAvailableText,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "متاح للاستخدام",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlotAvailableText,
                            )
                        }
                    }
                    Surface(
                        color = SlotBookedBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = usedCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = SlotBookedText,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "مستخدم / منتهي",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlotBookedText,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onGenerateBatch(10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("otp_generate_10_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توليد 10 أكواد جديدة", maxLines = 1)
                    }
                    Button(
                        onClick = { onGenerateBatch(20) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("otp_generate_20_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توليد 20 كوداً جديداً", maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = {
                        ActivationCodePrintHelper.printAvailableCodes(
                            context = context,
                            brandConfig = brandConfig,
                            codes = activationCodes,
                            onError = onShowMessage,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("otp_print_codes_button"),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة الأكواد المتاحة (${availableCount})")
                }
            }
        }
    }

    if (activationCodes.isEmpty()) {
        item(key = "otp_empty_state") {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "لا توجد أكواد تفعيل حالياً. اضغط على زر التوليد أعلاه لإنشاء دفعة أكواد جديدة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        items(activationCodes, key = { it.code }) { item ->
            val isUsed = item.isUsed
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUsed) SlotBookedBg.copy(alpha = 0.45f) else SlotAvailableBg.copy(alpha = 0.45f),
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUsed) SlotBookedBorder.copy(alpha = 0.55f) else SlotAvailableBorder.copy(alpha = 0.65f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_code_row_${item.code}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = item.code,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp,
                                    ),
                                    color = if (isUsed) SlotBookedText else SlotAvailableText,
                                )
                            }
                            Surface(
                                color = if (isUsed) SlotBookedBorder else SlotAvailableBorder,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = if (isUsed) "مستخدم / منتهي" else "متاح للاستخدام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }

                        if (isUsed) {
                            Text(
                                text = "استخدمه: ${item.usedByName.ifBlank { "مريض" }} (${item.usedByPhone.ifBlank { "بدون رقم" }})",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotBookedText,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = "جاهز للتسليم عبر واتساب أو ورقياً في العيادة",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotAvailableText,
                            )
                        }
                    }

                    if (!isUsed) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("OTP Code", item.code))
                                onShowMessage("تم نسخ الكود ${item.code} بنجاح لإرساله عبر واتساب")
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("copy_otp_${item.code}"),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الكود", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكود")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "مستخدم",
                            tint = SlotBookedBorder,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}
