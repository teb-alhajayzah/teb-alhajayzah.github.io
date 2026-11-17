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
        const val STANDARD_DISCLAIMER_EN =
            "Medical Notice: This summary is auto-generated to assist the treating physician and does not replace direct clinical examination or original reports."
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

    val EXTERNAL_SPECIALTIES_EN = listOf(
        "Dermatology" to "جلدية",
        "OB/GYN" to "نساء وتوليد",
        "Cardiology" to "قلب وأوعية دموية",
        "Dentistry" to "أسنان",
        "Pediatrics" to "أطفال",
        "Internal Medicine" to "باطنة",
        "Orthopedics" to "عظام",
        "ENT" to "أنف وأذن وحنجرة",
        "Ophthalmology" to "عيون",
        "General Surgery" to "جراحة",
        "Urology" to "مسالك بولية",
        "Other Specialties" to "تخصصات أخرى",
    )

    fun summarize(
        patientName: String,
        records: List<MedicalRecordEntity>,
        isEn: Boolean = false,
    ): PatientMedicalSummary {
        val fallbackName = if (isEn) "Patient" else "المريض"
        val disclaimerText = if (isEn) PatientMedicalSummary.STANDARD_DISCLAIMER_EN else PatientMedicalSummary.STANDARD_DISCLAIMER
        if (records.isEmpty()) {
            return PatientMedicalSummary(
                totalRecords = 0,
                platformPrescriptionsCount = 0,
                externalVisitsCount = 0,
                labAndImagingCount = 0,
                keyDiagnoses = emptyList(),
                currentMedications = emptyList(),
                recentLabsAndImaging = emptyList(),
                headlineSummary = if (isEn) {
                    "No medical records yet for ${patientName.ifBlank { fallbackName }}. Log an external visit or lab report to generate an automatic summary."
                } else {
                    "لا توجد بيانات طبية مسجلة بعد لـ ${patientName.ifBlank { fallbackName }}. أضف زيارة طبيب أو نتيجة تحليل ليتم تحليلها تلقائياً."
                },
                disclaimer = disclaimerText,
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
                val sourceTag = if (rec.recordType == MedicalRecordType.EXTERNAL_VISIT) {
                    if (isEn) "External" else "خارجي"
                } else {
                    if (isEn) "Platform" else "المنصة"
                }
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
            val title = rec.diagnosis.trim().ifEmpty { if (isEn) "Lab / Imaging" else "تحليل / أشعة" }
            val source = if (rec.recordType == MedicalRecordType.LAB_RESULT) rec.doctorName else {
                if (isEn) "Patient Upload" else "رفع المريض"
            }
            val noteSuffix = if (rec.notes.isNotBlank()) " — ${rec.notes.take(60)}" else ""
            labItems += "$title ($source • ${rec.visitDateLabel})$noteSuffix"
        }

        val nameLabel = patientName.ifBlank { fallbackName }
        val parts = mutableListOf<String>()
        parts += if (isEn) {
            "Summary for $nameLabel (${sorted.size} records)"
        } else {
            "ملخص الحالة لـ $nameLabel (${sorted.size} سجل طبي)"
        }
        if (diagnoses.isNotEmpty()) {
            parts += if (isEn) {
                "Key Diagnoses: ${diagnoses.take(2).joinToString(" | ")}"
            } else {
                "أبرز التشخيصات المسجلة: ${diagnoses.take(2).joinToString(" | ")}"
            }
        }
        if (medLines.isNotEmpty()) {
            parts += if (isEn) {
                "Medications (${medLines.size}): ${medLines.take(3).joinToString(" • ")}"
            } else {
                "العلاجات الحالية/الأخيرة (${medLines.size}): ${medLines.take(3).joinToString(" • ")}"
            }
        }
        if (labItems.isNotEmpty()) {
            parts += if (isEn) {
                "Labs & Imaging (${labsAndUploads.size}): ${labItems.take(2).joinToString(" | ")}"
            } else {
                "التحاليل والأشعة المرفقة (${labsAndUploads.size}): ${labItems.take(2).joinToString(" | ")}"
            }
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
            disclaimer = disclaimerText,
        )
    }
}
