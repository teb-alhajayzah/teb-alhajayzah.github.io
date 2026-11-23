package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark

enum class LegalModalType {
    PRIVACY_POLICY,
    TERMS_OF_USE,
}

@Composable
fun PrivacyAndTermsDialog(
    type: LegalModalType,
    isEn: Boolean = false,
    onDismiss: () -> Unit,
) {
    val isPrivacy = type == LegalModalType.PRIVACY_POLICY
    val titleAr = if (isPrivacy) "📜 سياسة الخصوصية وسرية السجلات الطبية" else "⚖️ اتفاقية وشروط الاستخدام"
    val titleEn = if (isPrivacy) "📜 Privacy Policy & Medical Records Security" else "⚖️ Terms of Use & Booking Agreement"
    val subtitleAr = if (isPrivacy) "منصة طب الحجايزة — خصوصية المريض وسرية بياناته أولويتنا القصوى" else "شروط حجز العيادات وتنظيم الخدمة الطبية في منصة طب الحجايزة"
    val subtitleEn = if (isPrivacy) "Teb Al-Hajayzah — Patient privacy and record confidentiality are our top priority" else "Terms for booking clinics and medical services on Teb Al-Hajayzah"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .testTag(if (isPrivacy) "privacy_policy_modal" else "terms_of_use_modal"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(TealPrimary, TealPrimaryDark)))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
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
                            Icon(
                                imageVector = if (isPrivacy) Icons.Filled.Security else Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEn) titleEn else titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = if (isEn) subtitleEn else subtitleAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("legal_modal_close_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = if (isEn) "Close" else "إغلاق",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Body content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (isPrivacy) {
                        LegalSectionCard(
                            icon = Icons.Filled.Lock,
                            title = if (isEn) "1. Confidentiality of Medical Records" else "1. سرية وحماية السجلات الطبية",
                            content = if (isEn) {
                                "All medical records, lab tests, prescriptions, and radiology files are strictly encrypted and confidential. Only the patient and authorized registered clinic doctors have access for treatment purposes."
                            } else {
                                "كافة السجلات الطبية والتحاليل والأشعة والروشتات مشفرة ومحمية بأعلى درجات السرية، ولا يمكن الاطلاع عليها إلا من خلال المريض شخصياً أو الأطباء المصرح لهم في العيادات لغرض المتابعة الطبية."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Shield,
                            title = if (isEn) "2. Family Privacy & Spouse Access Control" else "2. خصوصية الأسرة واستقلالية الملفات",
                            content = if (isEn) {
                                "Children's files are accessible to both parents once family link is verified. However, personal medical records of husband and wife remain strictly hidden from each other by default, and are only viewable if the individual explicitly toggles 'Allow Spouse Access'."
                            } else {
                                "ملفات الأبناء متاحة للوالدين تلقائياً بمجرد اعتماد ربط حساب الأسرة. في المقابل، تظل الملفات الشخصية للزوج والزوجة محجوبة افتراضياً عن الطرف الآخر، ولا تتاح إلا بتفعيل صريح لمفتاح 'السماح للزوج/الزوجة بالاطلاع على ملفي الطبي الشخصي' مع إمكانية إيقافه في أي لحظة."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Security,
                            title = if (isEn) "3. Permanent Medical Narrative & Cloud Storage" else "3. السجل الطبي الدائم والتخزين السحابي",
                            content = if (isEn) {
                                "Medical diagnoses, prescriptions, and test results are summarized permanently into text logs that survive indefinitely. Heavy files (PDFs/Images) are safely archived on designated Google Drive storage per clinic and platform fallback."
                            } else {
                                "يتم حفظ السجل الطبي الكتابي والتشخيصات والروشتات مدى الحياة في سجل تاريخي دائم. الملفات والمرفقات الثقيلة (PDF والأشعة) تؤرشف في مساحات Google Drive معزولة ومخصصة لكل عيادة واحتياطي المنصة العام."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Lock,
                            title = if (isEn) "4. Zero Third-Party Advertising & Data Sharing" else "4. منع الإعلانات التجارية ومشاركة البيانات",
                            content = if (isEn) {
                                "Teb Al-Hajayzah is a pure healthcare community platform. We never display commercial ads, track users for marketing, or share personal or health data with third parties."
                            } else {
                                "منصة طب الحجايزة منصة مجتمعية خدمية نقية 100%. يُمنع منعاً باتاً عرض أي إعلانات تجارية أو استغلال بيانات المرضى أو مشاركتها مع أي جهة خارجية لأغراض تجارية."
                            },
                        )
                    } else {
                        LegalSectionCard(
                            icon = Icons.Filled.Gavel,
                            title = if (isEn) "1. Booking Commitment & Slot Times" else "1. الالتزام بالحجوزات والمواعيد",
                            content = if (isEn) {
                                "Patients are expected to attend scheduled clinic appointments on time. If unable to attend, please cancel beforehand to give other community members the opportunity to book."
                            } else {
                                "يلتزم المريض بالحضور في الموعد المحجوز بالعيادة. في حال التعذر، يجب إلغاء الحجز مبكراً من خلال التطبيق أو الاتصال بالعيادة لإتاحة الفرصة لمريض آخر من أهالي القرية."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Shield,
                            title = if (isEn) "2. Account Verification & Fake Booking Ban" else "2. التوثيق لمرة واحدة وحظر الحجوزات الوهمية",
                            content = if (isEn) {
                                "Account activation (OTP) is completed once in lifetime to verify authentic phone numbers. Platform management reserves the right to immediately block numbers involved in repeated fake bookings or disruptions."
                            } else {
                                "يتم تفعيل الحساب لمرة واحدة في العمر لضمان جدية الحجوزات. تحتفظ إدارة المنصة بحق حظر أرقام الهواتف التي تقوم بحجوزات وهمية متكررة أو تسئ استخدام خدمات العيادات."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Security,
                            title = if (isEn) "3. Guidance Nature of AI Summaries" else "3. الطبيعة الطبية الاسترشادية للملخصات",
                            content = if (isEn) {
                                "AI-generated medical test summaries provide simplified, reassuring guidance in plain language. They are advisory only and must never replace direct diagnosis, consultation, and prescriptions by licensed physicians."
                            } else {
                                "التلخيصات الذكية لنتائج التحاليل تقدم إيضاحاً مبسطاً ومطمئناً للمريض باللغة العربية، وهي استرشادية بحتة ولا تغني بأي حال عن الفحص المباشر واستشارة الطبيب المعالج والالتزام بتعليماته."
                            },
                        )

                        LegalSectionCard(
                            icon = Icons.Filled.Lock,
                            title = if (isEn) "4. Family Link Proof & Administrative Review" else "4. إثبات صلة القرابة والاعتماد الإداري",
                            content = if (isEn) {
                                "Family account linking requests require submitting official marriage proof or national ID cards. Approvals are strictly audited and granted by the General Manager to prevent unauthorized data access."
                            } else {
                                "طلبات ربط حسابات الأسرة (الزوج والزوجة) تتطلب إرفاق صورة قسيمة الزواج أو بطاقات الرقم القومي لإثبات صلة القرابة، وتخضع للمراجعة والتدقيق الدقيق والاعتماد الحصري من إدارة المنصة."
                            },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = if (isEn) {
                            "All rights reserved © 2026 Teb Al-Hajayzah Platform — Serving Al-Hajayzah & Surrounding Villages (v1.0.0)"
                        } else {
                            "جميع الحقوق محفوظة © 2026 منصة طب الحجايزة — لخدمة أهالي الحجايزة والقرى المجاورة (v1.0.0)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text(if (isEn) "Close" else "فهمت وموافق")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25,
            )
        }
    }
}
