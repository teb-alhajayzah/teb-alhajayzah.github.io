package com.example.util

import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType

data class PatientMedicalSummary(
    val totalRecords: Int,
    val platformPrescriptionsCount: Int,
    val externalVisitsCount: Int,
    val labAndImagingCount: Int,
    val keyDiagnoses: List<String>,
    val currentMedications: List<String>,
    val recentLabsAndImaging: List<String>,
    val headlineSummary: String,
    val disclaimer: String = STANDARD_DISCLAIMER,
) {
    companion object {
        const val STANDARD_DISCLAIMER =
            "تنبيه طبي قياسي: هذا الملخص مولّد آلياً للاستئناس الطبي ومساعدة الطبيب المعالج ولا يغني عن الفحص الإكلينيكي المباشر ومراجعة أصل التحاليل والروشتات."
    }
}

object MedicalSummaryAnalyzer {

    val EXTERNAL_SPECIALTIES = listOf(
        "جلدية",
        "نساء وتوليد",
        "قلب وأوعية دموية",
        "أسنان",
        "أطفال",
        "باطنة",
        "عظام",
        "أنف وأذن وحنجرة",
        "عيون",
        "جراحة",
        "مسالك بولية",
        "تخصصات أخرى",
    )

    fun summarize(patientName: String, records: List<MedicalRecordEntity>): PatientMedicalSummary {
        if (records.isEmpty()) {
            return PatientMedicalSummary(
                totalRecords = 0,
                platformPrescriptionsCount = 0,
                externalVisitsCount = 0,
                labAndImagingCount = 0,
                keyDiagnoses = emptyList(),
                currentMedications = emptyList(),
                recentLabsAndImaging = emptyList(),
                headlineSummary = "لا توجد بيانات طبية مسجلة بعد لـ ${patientName.ifBlank { "المريض" }}. أضف زيارة طبيب أو نتيجة تحليل ليتم تحليلها تلقائياً.",
            )
        }

        val sorted = records.sortedByDescending { it.createdAtMillis }
        val platformRx = sorted.filter { it.recordType == MedicalRecordType.PRESCRIPTION }
        val externalVisits = sorted.filter { it.recordType == MedicalRecordType.EXTERNAL_VISIT }
        val labsAndUploads = sorted.filter {
            it.recordType == MedicalRecordType.LAB_RESULT || it.recordType == MedicalRecordType.PATIENT_UPLOAD
        }

        val diagnoses = mutableListOf<String>()
        val seenDiag = HashSet<String>()
        (platformRx + externalVisits).forEach { rec ->
            val diagClean = rec.diagnosis.trim()
            if (diagClean.isNotEmpty() && seenDiag.add(diagClean.lowercase())) {
                val sourceTag = if (rec.recordType == MedicalRecordType.EXTERNAL_VISIT) "خارجي" else "المنصة"
                diagnoses += "$diagClean (${rec.specialty} • $sourceTag • ${rec.visitDateLabel})"
            }
        }

        val medLines = mutableListOf<String>()
        val seenMeds = HashSet<String>()
        (platformRx + externalVisits).forEach { rec ->
            rec.medications
                .split('\n')
                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                .filter { it.isNotEmpty() }
                .forEach { medLine ->
                    val normKey = medLine.lowercase().substringBefore("—").trim()
                    if (seenMeds.add(normKey)) {
                        medLines += medLine
                    }
                }
        }

        val labItems = mutableListOf<String>()
        labsAndUploads.take(5).forEach { rec ->
            val title = rec.diagnosis.trim().ifEmpty { "تحليل / أشعة" }
            val source = if (rec.recordType == MedicalRecordType.LAB_RESULT) rec.doctorName else "رفع المريض"
            val noteSuffix = if (rec.notes.isNotBlank()) " — ${rec.notes.take(60)}" else ""
            labItems += "$title ($source • ${rec.visitDateLabel})$noteSuffix"
        }

        val nameLabel = patientName.ifBlank { "المريض" }
        val parts = mutableListOf<String>()
        parts += "ملخص الحالة لـ $nameLabel (${sorted.size} سجل طبي)"
        if (diagnoses.isNotEmpty()) {
            parts += "أبرز التشخيصات المسجلة: ${diagnoses.take(2).joinToString(" | ")}"
        }
        if (medLines.isNotEmpty()) {
            parts += "العلاجات الحالية/الأخيرة (${medLines.size}): ${medLines.take(3).joinToString(" • ")}"
        }
        if (labItems.isNotEmpty()) {
            parts += "التحاليل والأشعة المرفقة (${labsAndUploads.size}): ${labItems.take(2).joinToString(" | ")}"
        }

        return PatientMedicalSummary(
            totalRecords = sorted.size,
            platformPrescriptionsCount = platformRx.size,
            externalVisitsCount = externalVisits.size,
            labAndImagingCount = labsAndUploads.size,
            keyDiagnoses = diagnoses.take(6),
            currentMedications = medLines.take(8),
            recentLabsAndImaging = labItems,
            headlineSummary = parts.joinToString("\n"),
        )
    }
}
