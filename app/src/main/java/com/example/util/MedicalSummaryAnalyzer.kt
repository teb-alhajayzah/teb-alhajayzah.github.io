package com.example.util

import com.example.data.local.FamilyMemberEntity
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
    val vitalsSummary: String = "",
    val chronicConditionsSummary: String = "",
    val drugAllergies: String = "",
    val aiNarrative: String = "",
    val aiUpdatedAtMillis: Long = 0L,
    val aiError: String = "",
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

    val EXTERNAL_SPECIALTIES: List<String>
        get() = SpecialtyCatalog.all.map { it.nameAr }

    val EXTERNAL_SPECIALTIES_EN: List<Pair<String, String>>
        get() = SpecialtyCatalog.all.map { it.nameEn to it.nameAr }

    fun summarize(
        patientName: String,
        records: List<MedicalRecordEntity>,
        isEn: Boolean = false,
    ): PatientMedicalSummary = summarize(
        member = null,
        patientName = patientName,
        records = records,
        isEn = isEn,
    )

    fun summarize(
        member: FamilyMemberEntity?,
        patientName: String,
        records: List<MedicalRecordEntity>,
        isEn: Boolean = false,
    ): PatientMedicalSummary {
        val resolvedName = member?.fullName?.takeIf { it.isNotBlank() } ?: patientName
        val fallbackName = if (isEn) "Patient" else "المريض"
        val disclaimerText = if (isEn) PatientMedicalSummary.STANDARD_DISCLAIMER_EN else PatientMedicalSummary.STANDARD_DISCLAIMER

        val vitalsSummary = if (member != null) {
            buildList {
                if (member.age > 0) add(if (isEn) "Age: ${member.age}y" else "السن: ${member.age} سنة")
                if (member.weightKg > 0.0) {
                    val w = if (member.weightKg % 1.0 == 0.0) member.weightKg.toInt().toString() else member.weightKg.toString()
                    add(if (isEn) "Weight: $w kg" else "الوزن: $w كجم")
                }
                if (member.heightCm > 0.0) {
                    val h = if (member.heightCm % 1.0 == 0.0) member.heightCm.toInt().toString() else member.heightCm.toString()
                    add(if (isEn) "Height: $h cm" else "الطول: $h سم")
                }
                if (member.bloodType.isNotBlank()) {
                    add(if (isEn) "Blood: ${member.bloodType}" else "فصيلة الدم: ${member.bloodType}")
                }
            }.joinToString(" • ")
        } else {
            ""
        }

        val chronicSummary = if (member != null) {
            ChronicConditionsCatalog.formatLabels(
                rawKeys = member.chronicConditions,
                otherNotes = member.otherChronicNotes,
                isEn = isEn,
            )
        } else {
            ""
        }

        val allergies = member?.drugAllergies?.trim().orEmpty()
        val aiNarrative = if (member != null) {
            val preferred = if (isEn) member.aiSummaryEn.trim() else member.aiSummaryAr.trim()
            preferred.ifBlank { if (isEn) member.aiSummaryAr.trim() else member.aiSummaryEn.trim() }
        } else {
            ""
        }
        val aiUpdatedAt = member?.aiSummaryUpdatedAtMillis ?: 0L
        val aiError = member?.aiSummaryError?.trim().orEmpty()

        if (records.isEmpty()) {
            val emptyParts = mutableListOf<String>()
            if (vitalsSummary.isNotBlank()) emptyParts += vitalsSummary
            if (chronicSummary.isNotBlank()) {
                emptyParts += if (isEn) "Chronic Conditions: $chronicSummary" else "الأمراض المزمنة: $chronicSummary"
            }
            if (allergies.isNotBlank()) {
                emptyParts += if (isEn) "Drug Allergies: $allergies" else "حساسية الأدوية: $allergies"
            }
            emptyParts += if (isEn) {
                "No medical records yet for ${resolvedName.ifBlank { fallbackName }}. Upload a lab PDF or log an external visit to analyze it automatically."
            } else {
                "لا توجد سجلات أو تحاليل مسجلة بعد لـ ${resolvedName.ifBlank { fallbackName }}. ارفع ملف تحليل PDF أو أضف زيارة ليتم تحليلها تلقائياً."
            }
            return PatientMedicalSummary(
                totalRecords = 0,
                platformPrescriptionsCount = 0,
                externalVisitsCount = 0,
                labAndImagingCount = 0,
                keyDiagnoses = emptyList(),
                currentMedications = emptyList(),
                recentLabsAndImaging = emptyList(),
                headlineSummary = emptyParts.joinToString("\n"),
                vitalsSummary = vitalsSummary,
                chronicConditionsSummary = chronicSummary,
                drugAllergies = allergies,
                aiNarrative = aiNarrative,
                aiUpdatedAtMillis = aiUpdatedAt,
                aiError = aiError,
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
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            val diagClean = localized.diagnosis.trim()
            if (diagClean.isNotEmpty() && seenDiag.add(diagClean.lowercase())) {
                val sourceTag = if (rec.recordType == MedicalRecordType.EXTERNAL_VISIT) {
                    if (isEn) "External" else "خارجي"
                } else {
                    if (isEn) "Platform" else "المنصة"
                }
                diagnoses += "$diagClean (${localized.specialty} • $sourceTag • ${rec.visitDateLabel})"
            }
        }

        val medLines = mutableListOf<String>()
        val seenMeds = HashSet<String>()
        (platformRx + externalVisits).forEach { rec ->
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            localized.medications
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
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            val title = localized.diagnosis.trim().ifEmpty { if (isEn) "Lab / Imaging" else "تحليل / أشعة" }
            val source = if (rec.recordType == MedicalRecordType.LAB_RESULT) rec.doctorName else {
                if (isEn) "Patient Upload" else "رفع المريض"
            }
            val noteText = localized.notes.trim()
            val noteSuffix = if (noteText.isNotBlank()) " — ${noteText.take(60)}" else ""
            labItems += "$title ($source • ${rec.visitDateLabel})$noteSuffix"
        }

        val nameLabel = resolvedName.ifBlank { fallbackName }
        val parts = mutableListOf<String>()
        parts += if (isEn) {
            "Summary for $nameLabel (${sorted.size} records)"
        } else {
            "ملخص الحالة لـ $nameLabel (${sorted.size} سجل طبي)"
        }
        if (vitalsSummary.isNotBlank()) parts += vitalsSummary
        if (chronicSummary.isNotBlank()) {
            parts += if (isEn) "Chronic Conditions: $chronicSummary" else "الأمراض المزمنة: $chronicSummary"
        }
        if (allergies.isNotBlank()) {
            parts += if (isEn) "Drug Allergies Alert: $allergies" else "تنبيه حساسية الأدوية: $allergies"
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
            vitalsSummary = vitalsSummary,
            chronicConditionsSummary = chronicSummary,
            drugAllergies = allergies,
            aiNarrative = aiNarrative,
            aiUpdatedAtMillis = aiUpdatedAt,
            aiError = aiError,
            disclaimer = disclaimerText,
        )
    }
}
