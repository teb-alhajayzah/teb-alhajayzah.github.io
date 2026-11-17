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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MedicalRecordEntity
import com.example.ui.theme.DelayWarningBg
import com.example.ui.theme.DelayWarningBorder
import com.example.ui.theme.DelayWarningText
import com.example.ui.theme.TealPrimary
import com.example.util.MedicalSummaryAnalyzer

@Composable
fun AiMedicalSummaryCard(
    patientName: String,
    records: List<MedicalRecordEntity>,
    isEn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val summary = remember(patientName, records, isEn) {
        MedicalSummaryAnalyzer.summarize(patientName, records, isEn = isEn)
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
                            contentDescription = if (isEn) "AI Medical Summary" else "الملخص الطبي الذكي",
                            tint = TealPrimary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (isEn) "AI Medical Summary" else "الملخص الطبي المختصر (AI Summary)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                        )
                        Text(
                            text = if (isEn) "Auto-generated from visits, prescriptions & lab reports" else "تحليل تلقائي للزيارات والأدوية ونتائج التحاليل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (summary.totalRecords > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SummaryMiniPill(
                        label = if (isEn) "Clinics" else "عيادات",
                        value = summary.platformPrescriptionsCount.toString(),
                        color = TealPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniPill(
                        label = if (isEn) "External" else "خارجية",
                        value = summary.externalVisitsCount.toString(),
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniPill(
                        label = if (isEn) "Labs/Files" else "معامل وأشعة",
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
                                if (isEn) "Key Diagnoses & History:" else "أبرز التشخيصات والحالة:",
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
                                if (isEn) "Current & Logged Medications:" else "الأدوية الحالية والمسجلة:",
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
                                if (isEn) "Recent Labs & Imaging:" else "أحدث التحاليل والأشعة:",
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
            } else {
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
