package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.SlotBookedBg
import com.example.ui.theme.SlotBookedBorder
import com.example.ui.theme.SlotBookedText
import com.example.ui.theme.TealPrimary
import com.example.util.MedicalSummaryAnalyzer

@Composable
fun AiMedicalSummaryCard(
    patientName: String,
    records: List<MedicalRecordEntity>,
    member: FamilyMemberEntity? = null,
    isAnalyzing: Boolean = false,
    onRefreshAiSummary: (() -> Unit)? = null,
    isEn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var cardIsEn by remember(isEn) { mutableStateOf(isEn) }
    val summary = remember(member, patientName, records, cardIsEn) {
        MedicalSummaryAnalyzer.summarize(
            member = member,
            patientName = patientName,
            records = records,
            isEn = cardIsEn,
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_medical_summary_card"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = TealPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = if (cardIsEn) "AI Medical Summary" else "الملخص الطبي الذكي",
                            tint = TealPrimary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (cardIsEn) "AI Medical Summary" else "الملخص الطبي المختصر (AI Summary)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                        )
                        Text(
                            text = if (cardIsEn) {
                                "Reads uploaded PDFs, vitals, chronics & prescriptions"
                            } else {
                                "يقرأ ملفات PDF والتحاليل والبيانات الحيوية والأدوية تلقائياً"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRefreshAiSummary != null) {
                        IconButton(
                            onClick = onRefreshAiSummary,
                            enabled = !isAnalyzing,
                            modifier = Modifier.testTag("ai_summary_refresh_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = if (cardIsEn) "Re-analyze medical files" else "تحديث التحليل الطبي",
                                tint = TealPrimary,
                            )
                        }
                    }
                    FilterChip(
                        selected = cardIsEn,
                        onClick = { cardIsEn = !cardIsEn },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        label = {
                            Text(
                                text = if (cardIsEn) "AR" else "EN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                        modifier = Modifier.testTag("ai_summary_lang_toggle"),
                    )
                }
            }

            if (summary.vitalsSummary.isNotBlank() ||
                summary.chronicConditionsSummary.isNotBlank() ||
                summary.drugAllergies.isNotBlank()
            ) {
                Surface(
                    color = TealPrimary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (summary.vitalsSummary.isNotBlank()) {
                            Text(
                                text = summary.vitalsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (summary.chronicConditionsSummary.isNotBlank()) {
                            Text(
                                text = if (cardIsEn) {
                                    "Chronic: ${summary.chronicConditionsSummary}"
                                } else {
                                    "الأمراض المزمنة: ${summary.chronicConditionsSummary}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DelayWarningText,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (summary.drugAllergies.isNotBlank()) {
                            Text(
                                text = if (cardIsEn) {
                                    "🚨 Drug Allergies: ${summary.drugAllergies}"
                                } else {
                                    "🚨 حساسية أدوية: ${summary.drugAllergies}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotBookedText,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            if (isAnalyzing) {
                Surface(
                    color = TealPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (cardIsEn) {
                                "Reading attached PDF / images and updating AI medical summary..."
                            } else {
                                "جاري قراءة ملفات PDF والمرفقات وتحديث الملخص الطبي بالذكاء الاصطناعي..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        LinearProgressIndicator(
                            color = TealPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (summary.aiNarrative.isNotBlank()) {
                Surface(
                    color = Color(0xFFF0FDFA),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_gemini_narrative_box"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (cardIsEn) {
                                    "Smart Clinical Analysis (PDFs & History)"
                                } else {
                                    "القراءة والتحليل الذكي للتقارير والتحاليل"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = summary.aiNarrative,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else if (summary.aiError.isNotBlank()) {
                Surface(
                    color = SlotBookedBg.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlotBookedBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = SlotBookedBorder, modifier = Modifier.size(16.dp))
                            Text(
                                text = summary.aiError,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlotBookedText,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (onRefreshAiSummary != null) {
                            OutlinedButton(onClick = onRefreshAiSummary, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (cardIsEn) "Retry AI Analysis" else "إعادة محاولة التحليل الذكي")
                            }
                        }
                    }
                }
            }

            if (summary.totalRecords > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SummaryMiniPill(
                        label = if (cardIsEn) "Clinics" else "عيادات",
                        value = summary.platformPrescriptionsCount.toString(),
                        color = TealPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniPill(
                        label = if (cardIsEn) "External" else "خارجية",
                        value = summary.externalVisitsCount.toString(),
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniPill(
                        label = if (cardIsEn) "Labs/Files" else "معامل وأشعة",
                        value = summary.labAndImagingCount.toString(),
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                    )
                }

                if (summary.keyDiagnoses.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MedicalInformation, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (cardIsEn) "Key Diagnoses & History:" else "أبرز التشخيصات والحالة:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        summary.keyDiagnoses.take(3).forEach { diag ->
                            Text(
                                text = "• $diag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                if (summary.currentMedications.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (cardIsEn) "Current & Logged Medications:" else "الأدوية الحالية والمسجلة:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        summary.currentMedications.take(4).forEach { med ->
                            Text(
                                text = "• $med",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                if (summary.recentLabsAndImaging.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (cardIsEn) "Recent Labs & Imaging:" else "أحدث التحاليل والأشعة:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        summary.recentLabsAndImaging.take(3).forEach { labItem ->
                            Text(
                                text = "• $labItem",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else if (summary.aiNarrative.isBlank()) {
                Text(
                    text = summary.headlineSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Surface(
                color = DelayWarningBg.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DelayWarningBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = DelayWarningBorder,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp),
                    )
                    Text(
                        text = summary.disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = DelayWarningText,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}
