package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.local.PermanentLogEventType
import com.example.data.local.PermanentMedicalLogEntity
import com.example.ui.bi
import com.example.ui.theme.SlotAvailableBg
import com.example.ui.theme.SlotAvailableBorder
import com.example.ui.theme.SlotAvailableText
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.util.TimeUtils

@Composable
fun PermanentMedicalLogSection(
    patientName: String,
    logs: List<PermanentMedicalLogEntity>,
    isEn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var newestFirst by remember { mutableStateOf(true) }
    val orderedLogs = remember(logs, newestFirst) {
        if (newestFirst) logs.sortedByDescending { it.eventTimestampMillis }
        else logs.sortedBy { it.eventTimestampMillis }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFF0D9488).copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("permanent_medical_log_section"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = Color(0xFF0D9488).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HistoryEdu,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = bi(
                                isEn,
                                "📜 السجل الطبي التاريخي الدائم (مدى الحياة)",
                                "📜 Permanent Lifetime Medical Log",
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimaryDark,
                        )
                        Text(
                            text = bi(
                                isEn,
                                "توثيق كتابي فوري ودائم لـ $patientName — محفوظ مدى الحياة حتى بعد حذف ملفات PDF أو الصور",
                                "Instant permanent narrative for $patientName — preserved forever even after deleting heavy PDFs or images",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = SlotAvailableBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlotAvailableBorder.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = SlotAvailableText,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = bi(
                                isEn,
                                "محفوظ تلقائياً (${orderedLogs.size})",
                                "Auto-Locked (${orderedLogs.size})",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlotAvailableText,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                FilterChip(
                    selected = !newestFirst,
                    onClick = { newestFirst = !newestFirst },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = {
                        Text(
                            text = if (newestFirst) {
                                bi(isEn, "الأحدث أولاً", "Newest First")
                            } else {
                                bi(isEn, "الأقدم أولاً", "Oldest First")
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("permanent_log_sort_toggle"),
                )
            }

            if (orderedLogs.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = bi(
                            isEn,
                            "لم يتم تسجيل أحداث طبية بعد. بمجرد رفع تحليل أو أشعة أو إتمام كشف طبي، سيُحوّل تلقائياً إلى تدوين تاريخي دائم هنا.",
                            "No permanent medical log entries yet. Any lab upload, clinic prescription, or external visit will be documented here permanently.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            } else {
                orderedLogs.forEachIndexed { idx, entry ->
                    if (idx > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                    PermanentMedicalLogEntryCard(entry = entry, isEn = isEn)
                }
            }
        }
    }
}

@Composable
private fun PermanentMedicalLogEntryCard(
    entry: PermanentMedicalLogEntity,
    isEn: Boolean,
) {
    val (badgeLabel, badgeBg, badgeFg, icon) = when (entry.eventType) {
        PermanentLogEventType.LAB_REPORT -> Quadruple(
            bi(isEn, "نتيجة معمل تحاليل", "Lab Report"),
            Color(0xFFEEF2FF),
            Color(0xFF3730A3),
            Icons.Filled.Science,
        )
        PermanentLogEventType.PATIENT_UPLOAD -> Quadruple(
            bi(isEn, "تحليل / أشعة مرفوعة", "Uploaded Lab / Scan"),
            Color(0xFFECFDF5),
            Color(0xFF065F46),
            Icons.Filled.UploadFile,
        )
        PermanentLogEventType.EXTERNAL_VISIT -> Quadruple(
            bi(isEn, "زيارة طبيب خارجي", "External Doctor Visit"),
            Color(0xFFFFFBEB),
            Color(0xFF92400E),
            Icons.Filled.LocalHospital,
        )
        else -> Quadruple(
            bi(isEn, "كشف وروشتة عيادة", "Clinic Visit & Rx"),
            Color(0xFFF0FDFA),
            Color(0xFF0F766E),
            Icons.Filled.MedicalServices,
        )
    }

    val dateTimeText = if (entry.eventTimestampMillis > 0L) {
        TimeUtils.formatActualDateTimeLabel(entry.eventTimestampMillis, isEn = isEn)
    } else {
        entry.eventDateTimeLabel
    }
    val narrative = if (isEn) {
        entry.narrativeEn.ifBlank { entry.narrativeAr }
    } else {
        entry.narrativeAr.ifBlank { entry.narrativeEn }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permanent_log_item_${entry.id}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (entry.isAiGenerated) {
                    Surface(
                        color = TealPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = bi(isEn, "توثيق ذكي", "AI Verified"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimaryDark,
                            )
                        }
                    }
                }
            }

            if (dateTimeText.isNotBlank()) {
                Text(
                    text = "🕒 $dateTimeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (entry.title.isNotBlank() || entry.providerName.isNotBlank()) {
                Text(
                    text = buildString {
                        if (entry.title.isNotBlank()) append(entry.title)
                        if (entry.providerName.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(entry.providerName)
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimaryDark,
                )
            }

            Text(
                text = narrative,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
