package com.example.util

import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

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
                aiError = "",
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
        labsAndUploads.take(8).forEach { rec ->
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            val title = localized.diagnosis.trim().ifEmpty { if (isEn) "Lab / Imaging" else "تحليل / أشعة" }
            val fileTag = rec.attachmentName.trim()
                .takeIf { it.isNotEmpty() && !title.contains(it, ignoreCase = true) }
                ?.let { " [$it]" }
                .orEmpty()
            val source = if (rec.recordType == MedicalRecordType.LAB_RESULT) rec.doctorName else {
                if (isEn) "Patient Upload" else "رفع المريض"
            }
            val noteText = localized.notes.trim()
            val noteSuffix = if (noteText.isNotBlank()) " — ${noteText.take(60)}" else ""
            labItems += "$title$fileTag ($source • ${rec.visitDateLabel})$noteSuffix"
        }
        if (labItems.size < 8) {
            (platformRx + externalVisits)
                .filter { it.attachmentPath.isNotBlank() && it.attachmentName.isNotBlank() }
                .take(8 - labItems.size)
                .forEach { rec ->
                    val label = if (isEn) "Attachment: ${rec.attachmentName} (${rec.visitDateLabel})" else "مرفق طبي: ${rec.attachmentName} (${rec.visitDateLabel})"
                    if (label !in labItems) labItems += label
                }
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
            aiError = "",
            disclaimer = disclaimerText,
        )
    }

    data class OfflineMedicalSummaryBundle(
        val summaryAr: String,
        val summaryEn: String,
    )

    data class ParsedClinicalLabFindings(
        val normalCount: Int,
        val normalHighlights: List<String>,
        val followUpPoints: List<String>,
    )

    /**
     * نظام الطوارئ الذكي (Smart Local Parser Fallback):
     * يبحث داخل النصوص المستخرجة وملاحظات التحاليل عن المؤشرات الطبية الشائعة
     * (مثل Pus cells, RBCs, Crystals, Oxalate, Glucose, Hemoglobin) ويقارنها بالنطاق الطبيعي
     * الموجود في نفس السطر أو النطاقات المرجعية المعتادة، ثم يرتب المخرجات في جزأين واضحين دون أخطاء.
     */
    fun buildOfflineFallbackBundle(
        member: FamilyMemberEntity?,
        patientName: String,
        records: List<MedicalRecordEntity>,
    ): OfflineMedicalSummaryBundle {
        val summaryAr = buildOfflineFallbackText(
            member = member,
            patientName = patientName,
            records = records,
            isEn = false,
        )
        val summaryEn = buildOfflineFallbackText(
            member = member,
            patientName = patientName,
            records = records,
            isEn = true,
        )
        return OfflineMedicalSummaryBundle(
            summaryAr = summaryAr,
            summaryEn = summaryEn,
        )
    }

    fun parseClinicalLabText(
        texts: List<String>,
        member: FamilyMemberEntity?,
        isEn: Boolean,
    ): ParsedClinicalLabFindings {
        val normals = LinkedHashSet<String>()
        val abnormals = LinkedHashSet<String>()
        val seenKeys = HashSet<String>()

        val chronicKeys = member?.chronicConditions?.let { ChronicConditionsCatalog.parseKeys(it) }.orEmpty()
        val hasDiabetes = chronicKeys.any { it.contains("diabetes", ignoreCase = true) || it.contains("سكر") }
        val hasKidney = chronicKeys.any { it.contains("kidney", ignoreCase = true) || it.contains("كل") }
        val hasBpOrHeart = chronicKeys.any {
            it.contains("hyper", ignoreCase = true) ||
                it.contains("heart", ignoreCase = true) ||
                it.contains("ضغط") ||
                it.contains("قلب")
        }

        fun addNormal(key: String, label: String) {
            if (seenKeys.add("N:$key") && normals.size < 8) {
                normals += label
            }
        }

        fun addAbnormal(key: String, label: String) {
            if (seenKeys.add("A:$key") && abnormals.size < 8) {
                abnormals += label
            }
        }

        val allLines = texts.flatMap { raw ->
            raw.replace("\r\n", "\n")
                .split('\n', ';', '|', '•')
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.length >= 3 && !isMetadataHeaderLine(it) }
        }

        // 1) Specific clinical biomarkers (with optional inline Ref Range on the same line)
        allLines.forEach { line ->
            // Pus cells / WBCs in urine/microscopy or blood
            val pusMatch = Regex(
                "(?:pus\\s*cells?|p\\.?u\\.?s\\.?\\s*cells?|leucocytes?|leukocytes?|صديد|خلايا\\s*صديدية)\\s*[:=\\-]?\\s*([0-9]+(?:\\s*[-–]\\s*[0-9]+)?|\\+{1,4}|positive|pos|present|many|heavy|moderate|few|rare|trace|nil|absent|negative|neg|none|normal)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (pusMatch != null) {
                val rawVal = pusMatch.groupValues[1].trim()
                val inlineRef = extractInlineNumericRange(line, pusMatch.range.last + 1)
                val upperRef = inlineRef?.second ?: 5.0
                val eval = evaluateSemiQuantOrRange(rawVal, lowNormal = 0.0, highNormal = upperRef)
                val refLabel = inlineRef?.let { "${formatNum(it.first)} - ${formatNum(it.second)}" } ?: "0 - 5 /HPF"
                if (eval > 0) {
                    addAbnormal(
                        "pus",
                        if (isEn) {
                            "Elevated Pus Cells ($rawVal vs Ref $refLabel): may indicate mild urinary inflammation or infection; follow up with your doctor for proper treatment."
                        } else {
                            "وجود نسبة بسيطة من خلايا الصديد في البول، وتحتاج للمتابعة مع الطبيب لوصف العلاج المناسب"
                        },
                    )
                } else if (eval == 0) {
                    val isQualNeg = isQualitativeNegative(rawVal)
                    addNormal(
                        "pus",
                        if (isEn) "Pus Cells ($rawVal)" else if (isQualNeg) "خلايا الصديد: سلبية / طبيعية ولا يوجد بها أي مشكلة" else "خلايا الصديد: طبيعية ($rawVal) ولا يوجد بها أي مشكلة",
                    )
                }
            }

            // RBCs
            val rbcMatch = Regex(
                "(?:r\\.?b\\.?c\\.?s?|red\\s*(?:blood\\s*)?cells?|كرات\\s*(?:الدم\\s*)?الحمراء|خلايا\\s*دم\\s*حمراء)\\s*[:=\\-]?\\s*([0-9]+(?:\\.[0-9]+)?(?:\\s*[-–]\\s*[0-9]+(?:\\.[0-9]+)?)?|\\+{1,4}|positive|pos|present|many|heavy|moderate|few|rare|trace|nil|absent|negative|neg|none|normal)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (rbcMatch != null) {
                val rawVal = rbcMatch.groupValues[1].trim()
                val inlineRef = extractInlineNumericRange(line, rbcMatch.range.last + 1)
                val firstNum = extractFirstNumber(rawVal)
                val isQual = firstNum == null
                val isUrineMicroscopy = line.contains("/hpf", ignoreCase = true) ||
                    rawVal.contains("-") ||
                    (inlineRef != null && inlineRef.second <= 10.0) ||
                    (inlineRef == null && firstNum != null && firstNum > 6.5) ||
                    rawVal.startsWith("+") ||
                    isQual
                if (isUrineMicroscopy) {
                    val high = inlineRef?.second ?: 3.0
                    val eval = evaluateSemiQuantOrRange(rawVal, lowNormal = 0.0, highNormal = high)
                    val refLabel = inlineRef?.let { "${formatNum(it.first)} - ${formatNum(it.second)}" } ?: "0 - 3 /HPF"
                    if (eval > 0) {
                        addAbnormal(
                            "rbc_urine",
                            if (isEn) {
                                "Elevated RBCs ($rawVal vs Ref $refLabel): microscopic red blood cells noticed; follow up with your doctor."
                            } else {
                                "وجود نسبة بسيطة من كرات الدم الحمراء في البول، وتحتاج للمتابعة مع الطبيب للاطمئنان"
                            },
                        )
                    } else if (eval == 0) {
                        val isQualNeg = isQualitativeNegative(rawVal)
                        addNormal(
                            "rbc_urine",
                            if (isEn) "Urine RBCs ($rawVal)" else if (isQualNeg) "كرات الدم الحمراء: سلبية / طبيعية ولا يوجد بها أي مشكلة" else "كرات الدم الحمراء: طبيعية ($rawVal) ولا يوجد بها أي مشكلة",
                        )
                    }
                } else if (firstNum != null) {
                    val low = inlineRef?.first ?: 3.8
                    val high = inlineRef?.second ?: 5.8
                    val refLabel = "${formatNum(low)} - ${formatNum(high)}"
                    if (firstNum < low || firstNum > high) {
                        addAbnormal(
                            "rbc_cbc",
                            if (isEn) {
                                "RBC Count out of range ($rawVal vs Ref $refLabel): discuss with your physician."
                            } else {
                                "عدد كرات الدم الحمراء خارج المعدل الطبيعي المعتاد، ويُنصح بمراجعة الطبيب للاطمئنان"
                            },
                        )
                    } else {
                        addNormal("rbc_cbc", if (isEn) "RBCs ($rawVal)" else "كرات الدم الحمراء: ضمن المعدل الطبيعي ($rawVal)")
                    }
                }
            }

            // Crystals / Oxalate / Urate / Phosphates
            val crystalMatch = Regex(
                "(?:calcium\\s*oxalate|ca\\.?\\s*oxalate|oxalate|uric\\s*acid\\s*crystals?|amorphous\\s*(?:urates?|phosphates?)|crystals?|أملاح|اوكسالات|أوكسالات|بلورات|يورات)\\s*[:=\\-]?\\s*(\\+{1,4}|positive|pos|present|many|heavy|moderate|few|rare|trace|nil|absent|negative|neg|none|normal|[0-9]+(?:\\s*[-–]\\s*[0-9]+)?)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (crystalMatch != null) {
                val rawVal = crystalMatch.groupValues[1].trim()
                val eval = evaluateSemiQuantOrRange(rawVal, lowNormal = 0.0, highNormal = 0.0)
                val isCalciumOxalate = line.contains("oxalate", ignoreCase = true) || line.contains("أوكسالات") || line.contains("اوكسالات")
                if (eval > 0) {
                    val displaySaltEn = if (isCalciumOxalate) "Calcium Oxalate Crystals" else "Urinary Crystals / Salts"
                    addAbnormal(
                        "crystals",
                        if (isEn) {
                            "$displaySaltEn detected ($rawVal vs Ref Nil): urinary salts present — drink plenty of water and review with your doctor."
                        } else if (isCalciumOxalate) {
                            "وجود نسبة أملاح (أوكسالات) في البول، ويُنصح بشرب كميات كافية من الماء ومراجعة الطبيب"
                        } else {
                            "وجود نسبة أملاح في البول، ويُنصح بشرب كميات كافية من الماء ومراجعة الطبيب"
                        },
                    )
                } else if (eval == 0) {
                    addNormal(
                        "crystals",
                        if (isEn) "Crystals ($rawVal)" else "الأملاح والبلورات: سلبية / طبيعية ولا يوجد بها أي مشكلة",
                    )
                }
            }

            // Bacteria / Epithelial / Casts
            val bactMatch = Regex(
                "(?:bacteria|bacteriuria|casts?|yeast|فطريات|بكتيريا)\\s*[:=\\-]?\\s*(\\+{1,4}|positive|pos|present|many|heavy|moderate|few|rare|trace|nil|absent|negative|neg|none|normal)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (bactMatch != null) {
                val rawVal = bactMatch.groupValues[1].trim()
                val eval = evaluateSemiQuantOrRange(rawVal, lowNormal = 0.0, highNormal = 0.0)
                if (eval > 0) {
                    addAbnormal(
                        "bacteria",
                        if (isEn) {
                            "Microbial/Bacteria finding ($rawVal vs Ref Nil): requires doctor evaluation of urine report."
                        } else {
                            "وجود بكتيريا أو مكونات بسيطة في عينة البول، وتحتاج للمتابعة مع الطبيب لوصف العلاج المناسب"
                        },
                    )
                } else if (eval == 0) {
                    addNormal("bacteria", if (isEn) "Bacteria ($rawVal)" else "البكتيريا: سلبية / طبيعية ولا يوجد بها أي مشكلة")
                }
            }

            // Hemoglobin (Hb / Hgb)
            val hbMatch = Regex(
                "(?:hemoglobin|haemoglobin|hgb|\\bhb\\b|الهيموجلوبين)\\s*[:=\\-]?\\s*(\\d{1,2}(?:\\.\\d+)?)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (hbMatch != null && !line.contains("hba1c", ignoreCase = true) && !line.contains("a1c", ignoreCase = true)) {
                val hb = hbMatch.groupValues[1].toDoubleOrNull()
                if (hb != null && hb in 2.0..25.0) {
                    val inlineRef = extractInlineNumericRange(line, hbMatch.range.last + 1)
                    val low = inlineRef?.first ?: 11.5
                    val high = inlineRef?.second ?: 17.5
                    val refLabel = "${formatNum(low)} - ${formatNum(high)}"
                    if (hb < low) {
                        addAbnormal(
                            "hb",
                            if (isEn) {
                                "Low Hemoglobin ($hb vs Ref $refLabel g/dL): below reference range — doctor review recommended for possible anemia."
                            } else {
                                "نسبة الهيموجلوبين (الأنيميا) أقل من المعدل الطبيعي وتحتاج لمتابعة الطبيب"
                            },
                        )
                    } else if (hb > high) {
                        addAbnormal(
                            "hb",
                            if (isEn) {
                                "Elevated Hemoglobin ($hb vs Ref $refLabel g/dL): slightly above range; review with doctor."
                            } else {
                                "نسبة الهيموجلوبين أعلى قليلاً من المعدل الطبيعي ($hb)، ويُنصح بمراجعة الطبيب"
                            },
                        )
                    } else {
                        addNormal("hb", if (isEn) "Hemoglobin ($hb g/dL)" else "الهيموجلوبين: ضمن المعدل الطبيعي ($hb)")
                    }
                }
            }

            // Glucose (Blood or Urine)
            val gluMatch = Regex(
                "(?:glucose|fbs|rbs|ppbs|blood\\s*sugar|urine\\s*sugar|sugar|الجلوكوز|سكر\\s*الدم|السكر)\\s*[:=\\-]?\\s*(\\d{2,3}(?:\\.\\d+)?|\\+{1,4}|trace|positive|pos|present|nil|absent|negative|neg|none|normal)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (gluMatch != null && !line.contains("hba1c", ignoreCase = true)) {
                val rawVal = gluMatch.groupValues[1].trim()
                val numVal = rawVal.toDoubleOrNull()
                if (numVal != null) {
                    val inlineRef = extractInlineNumericRange(line, gluMatch.range.last + 1)
                    val thresholdHigh = inlineRef?.second ?: extractUpperThreshold(line, gluMatch.range.last + 1) ?: 139.0
                    val low = inlineRef?.first ?: 70.0
                    val refLabel = inlineRef?.let { "${formatNum(it.first)} - ${formatNum(it.second)}" } ?: "< ${formatNum(thresholdHigh)}"
                    if (numVal > thresholdHigh || numVal < low) {
                        val chronicLinkAr = if (hasDiabetes) " (مهم جداً مع سجل مرض السكري)" else ""
                        val chronicLinkEn = if (hasDiabetes) " (important with diabetes history)" else ""
                        val dirAr = if (numVal > thresholdHigh) "ارتفاع" else "انخفاض"
                        val dirEn = if (numVal > thresholdHigh) "Elevated" else "Low"
                        addAbnormal(
                            "glucose",
                            if (isEn) {
                                "$dirEn Glucose ($numVal vs Ref $refLabel mg/dL)$chronicLinkEn: bring to your doctor's attention."
                            } else {
                                "$dirAr نسبة السكر في الدم (${formatNum(numVal)})$chronicLinkAr، وتحتاج للمتابعة مع الطبيب"
                            },
                        )
                    } else {
                        addNormal("glucose", if (isEn) "Glucose ($numVal)" else "مستوى السكر: ضمن المعدل الطبيعي (${formatNum(numVal)})")
                    }
                } else {
                    val eval = evaluateSemiQuantOrRange(rawVal, 0.0, 0.0)
                    if (eval > 0) {
                        addAbnormal(
                            "glucose_urine",
                            if (isEn) {
                                "Sugar/Glucose detected in urine ($rawVal vs Ref Nil): check with your physician."
                            } else {
                                "وجود نسبة سكر في البول، ويستلزم المتابعة مع الطبيب للاطمئنان"
                            },
                        )
                    } else if (eval == 0) {
                        addNormal("glucose_urine", if (isEn) "Urine Sugar ($rawVal)" else "سكر البول: سلبية / طبيعية ولا يوجد بها أي مشكلة")
                    }
                }
            }

            // HbA1c
            val a1cMatch = Regex("(?:hba1c|a1c|السكر\\s*التراكمي)\\s*[:=\\-]?\\s*(\\d{1,2}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(line)
            if (a1cMatch != null) {
                val a1c = a1cMatch.groupValues[1].toDoubleOrNull()
                if (a1c != null && a1c in 2.0..20.0) {
                    val inlineRef = extractInlineNumericRange(line, a1cMatch.range.last + 1)
                    val high = inlineRef?.second ?: 5.7
                    val refLabel = inlineRef?.let { "${formatNum(it.first)} - ${formatNum(it.second)}%" } ?: "4.0 - 5.7%"
                    if (a1c > high) {
                        addAbnormal(
                            "hba1c",
                            if (isEn) {
                                "Elevated HbA1c ($a1c% vs Ref $refLabel): indicates average blood sugar above normal range; review with doctor."
                            } else {
                                "ارتفاع نسبة السكر التراكمي ($a1c%) عن المعدل الطبيعي، وتحتاج للمتابعة مع الطبيب"
                            },
                        )
                    } else {
                        addNormal("hba1c", if (isEn) "HbA1c ($a1c%)" else "السكر التراكمي: ضمن المعدل الطبيعي ($a1c%)")
                    }
                }
            }

            // Albumin / Protein
            val albMatch = Regex(
                "(?:albumin|protein|الألبيومين|زلال|بروتين)\\s*[:=\\-]?\\s*(\\+{1,4}|trace|positive|pos|present|nil|absent|negative|neg|none|normal|\\d+(?:\\.\\d+)?)",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (albMatch != null) {
                val rawVal = albMatch.groupValues[1].trim()
                val eval = evaluateSemiQuantOrRange(rawVal, 0.0, 0.0)
                if (eval > 0) {
                    val linkAr = if (hasKidney || hasBpOrHeart) " (يستدعي انتباهاً خاصاً مع الضغط أو الكلى)" else ""
                    val linkEn = if (hasKidney || hasBpOrHeart) " (needs special attention with BP/kidney history)" else ""
                    addAbnormal(
                        "albumin",
                        if (isEn) {
                            "Albumin / Protein detected ($rawVal vs Ref Nil)$linkEn: physician evaluation advised."
                        } else {
                            "وجود نسبة زلال أو بروتين في البول$linkAr، وتحتاج للمتابعة مع الطبيب"
                        },
                    )
                } else if (eval == 0) {
                    addNormal("albumin", if (isEn) "Albumin/Protein ($rawVal)" else "الزلال والبروتين: سلبية / طبيعية ولا يوجد بها أي مشكلة")
                }
            }

            // Generic inline Ref-Range comparator (excludes dates, barcodes, ages, years, & admin metadata)
            val genericMatch = Regex(
                "^([A-Za-z][A-Za-z0-9\\s/\\-.()]{1,34}?)\\s*[:=\\-]?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:[A-Za-z%/^0-9µμ]+)?\\s*(?:\\(|\\[|Ref\\.?\\s*Range\\s*[:=]?)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:-|to|–|~)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:\\)|\\])?",
                RegexOption.IGNORE_CASE,
            ).find(line)
            if (genericMatch != null) {
                val testName = genericMatch.groupValues[1].trim()
                val value = genericMatch.groupValues[2].toDoubleOrNull()
                val low = genericMatch.groupValues[3].toDoubleOrNull()
                val high = genericMatch.groupValues[4].toDoubleOrNull()
                val normKey = testName.lowercase().replace(Regex("[^a-z0-9]"), "")
                val alreadyHandled = normKey.contains("pus") || normKey.contains("rbc") ||
                    normKey.contains("hb") || normKey.contains("hemoglob") || normKey.contains("haemoglob") ||
                    normKey.contains("gluc") || normKey.contains("sugar") ||
                    normKey.contains("albumin") || normKey.contains("protein") ||
                    normKey.contains("crystal") || normKey.contains("oxalate") || normKey.contains("bacteria")
                val isMetaName = isMetadataTestName(testName)
                val looksLikeDateNumbers = value != null && low != null && high != null &&
                    (value >= 1900.0 || low >= 1900.0 || high >= 1900.0 ||
                        Regex("\\b\\d{1,4}[/\\-]\\d{1,2}[/\\-](?:19|20)\\d{2}\\b").containsMatchIn(line))
                if (!alreadyHandled && !isMetaName && !looksLikeDateNumbers && testName.length >= 2 && value != null && low != null && high != null && high >= low && normKey.isNotBlank()) {
                    val refLabel = "${formatNum(low)} - ${formatNum(high)}"
                    if (value < low) {
                        addAbnormal(
                            "gen_$normKey",
                            if (isEn) {
                                "$testName is below normal ($value vs Ref $refLabel): mention to your doctor."
                            } else {
                                "انخفاض ${translateLabTermToAr(testName)} (${formatNum(value)}) أقل من المعدل الطبيعي، ويُنصح بعرضه على الطبيب"
                            },
                        )
                    } else if (value > high) {
                        addAbnormal(
                            "gen_$normKey",
                            if (isEn) {
                                "$testName is above normal ($value vs Ref $refLabel): requires physician review."
                            } else {
                                "ارتفاع ${translateLabTermToAr(testName)} (${formatNum(value)}) أعلى من المعدل الطبيعي، ويحتاج لمتابعة الطبيب"
                            },
                        )
                    } else {
                        addNormal(
                            "gen_$normKey",
                            if (isEn) "$testName ($value)" else "${translateLabTermToAr(testName)}: ضمن المعدل الطبيعي (${formatNum(value)})",
                        )
                    }
                }
            }
        }

        // Chronic condition & allergy safety reminders as follow-up points if relevant
        val allergies = member?.drugAllergies?.trim().orEmpty()
        if (allergies.isNotBlank()) {
            addAbnormal(
                "allergy_alert",
                if (isEn) {
                    "Drug Allergy Precaution: Recorded allergy to ($allergies) — always remind the doctor before prescribing."
                } else {
                    "تنبيه حساسية الأدوية: المريض لديه حساسية مسجلة تجاه ($allergies)؛ يجب تنبيه الطبيب قبل وصف أي علاج."
                },
            )
        }

        return ParsedClinicalLabFindings(
            normalCount = normals.size,
            normalHighlights = normals.toList(),
            followUpPoints = abnormals.toList(),
        )
    }

    /**
     * مستخرج خفيف وآمن لنصوص ملفات PDF المخزنة محلياً في الأندرويد (يدعم التدفقات المضغوطة FlateDecode والنصوص المباشرة).
     */
    fun extractReadablePdfText(filePath: String): String {
        if (filePath.isBlank()) return ""
        val file = File(filePath)
        if (!file.exists() || !file.isFile || file.length() <= 0L || file.length() > 3 * 1024 * 1024L) {
            return ""
        }
        return try {
            val rawBytes = file.readBytes()
            val iso = String(rawBytes, Charsets.ISO_8859_1)
            val collected = StringBuilder()

            // Extract uncompressed PDF text blocks first
            extractPdfTextOperators(iso, collected)

            // Inflate FlateDecode streams if compressed
            var searchIdx = 0
            var streamsProcessed = 0
            while (streamsProcessed < 12 && collected.length < 16000) {
                val streamStart = iso.indexOf("stream", searchIdx)
                if (streamStart < 0) break
                val dataStart = when {
                    iso.startsWith("\r\n", streamStart + 6) -> streamStart + 8
                    iso.startsWith("\n", streamStart + 6) || iso.startsWith("\r", streamStart + 6) -> streamStart + 7
                    else -> {
                        searchIdx = streamStart + 6
                        continue
                    }
                }
                val streamEnd = iso.indexOf("endstream", dataStart)
                if (streamEnd < 0) break
                val sliceLen = (streamEnd - dataStart).coerceAtMost(512 * 1024)
                if (sliceLen > 4) {
                    val decompressed = tryInflateBytes(rawBytes, dataStart, sliceLen)
                    if (decompressed != null) {
                        extractPdfTextOperators(String(decompressed, Charsets.ISO_8859_1), collected)
                    }
                }
                searchIdx = streamEnd + 9
                streamsProcessed++
            }
            collected.toString().take(12000)
        } catch (_: Exception) {
            ""
        }
    }

    private fun tryInflateBytes(src: ByteArray, offset: Int, length: Int): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(src, offset, length)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var guard = 0
            while (!inflater.finished() && guard < 128) {
                val count = inflater.inflate(buf)
                if (count == 0) break
                out.write(buf, 0, count)
                guard++
            }
            inflater.end()
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPdfTextOperators(content: String, out: StringBuilder) {
        val btBlocks = Regex("BT([\\s\\S]{1,12000}?)ET").findAll(content)
        for (block in btBlocks) {
            val body = block.groupValues[1]
            val literals = Regex("\\(([^()]{1,240})\\)").findAll(body)
            val lineParts = mutableListOf<String>()
            for (lit in literals) {
                val clean = lit.groupValues[1]
                    .replace("\\(", "(")
                    .replace("\\)", ")")
                    .replace("\\\\", "\\")
                    .replace(Regex("[\\u0000-\\u001F]"), " ")
                    .trim()
                if (clean.isNotEmpty()) lineParts += clean
            }
            if (lineParts.isNotEmpty()) {
                out.append(lineParts.joinToString(" ")).append('\n')
            }
        }
    }

    fun stripMetadataLines(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.replace("\r\n", "\n")
            .split('\n')
            .filterNot { line ->
                val cleaned = line.replace(Regex("\\s+"), " ").trim()
                cleaned.isNotEmpty() && isMetadataHeaderLine(cleaned)
            }
            .joinToString("\n")
    }

    private fun isMetadataHeaderLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        val headerKeywords = listOf(
            "patient name", "patient id", "patient no", "sample date", "report date", "reporting date",
            "reported on", "reported at", "request date", "request id", "request no", "ordered date",
            "collection date", "collection time", "printed on", "printed at", "print date",
            "referred by", "ref. by", "ref by", "ref. no", "ref no", "doctor name", "physician",
            "lab name", "laboratory", "branch", "accession", "specimen", "barcode", "serial",
            "receipt", "invoice", "gender", "sex:", "age:", "age ", "dob", "date of birth",
            "national id", "passport", "page ", "tel:", "phone:", "mobile:", "address:",
            "اسم المريض", "تاريخ التقرير", "تاريخ الطلب", "تاريخ الطباعة", "تاريخ العينة",
            "تاريخ التحليل", "اسم المعمل", "الطبيب المعالج", "رقم الهاتف", "العنوان",
            "رقم العينة", "رقم الطلب", "كود المريض", "الباركود", "السن:", "العمر:", "النوع:",
        )
        if (headerKeywords.any { lower.contains(it) }) return true
        // Ignore lines starting with common admin labels like "Date:", "ID:", "Request:", "Reporting:"
        if (Regex("^(?:date|reporting|reported|request|ordered|received|registered|sample|specimen|accession|barcode|id|code|age|gender|sex|name|patient|doctor|dr\\.?|time|tel|mobile|fax)\\b\\s*[:\\-#/]", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
            return true
        }
        // Ignore pure date / timestamp lines (e.g., 19/11/2026 or 2026-11-19 10:30 AM)
        val withoutDates = lower
            .replace(Regex("\\b\\d{1,4}[/\\-]\\d{1,2}[/\\-](?:19|20)\\d{2}\\b"), "")
            .replace(Regex("\\b(?:19|20)\\d{2}[/\\-]\\d{1,2}[/\\-]\\d{1,2}\\b"), "")
            .replace(Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:am|pm)?\\b"), "")
            .replace(Regex("[\\s\\-/:.,]+"), "")
        if (withoutDates.isEmpty()) return true
        return false
    }

    private fun isMetadataTestName(testName: String): Boolean {
        val lower = testName.lowercase().trim()
        return Regex(
            "\\b(?:date|report|reporting|reported|request|requested|order|ordered|collect|collection|sample|specimen|accession|barcode|serial|receipt|invoice|age|gender|sex|dob|birth|patient|doctor|lab|page|tel|phone|mobile|address|time|year|month|day|code|id|no)\\b|\\b20(?:2[0-9]|3[0-9])\\b",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(lower)
    }

    private fun isQualitativeNegative(rawVal: String): Boolean {
        return rawVal.trim().lowercase() in setOf("nil", "absent", "negative", "neg", "none", "normal")
    }

    private fun translateLabTermToAr(testName: String): String {
        val lower = testName.lowercase().trim()
        return when {
            lower.contains("wbc") || lower.contains("leuk") || lower.contains("leuc") || lower.contains("white") -> "كرات الدم البيضاء"
            lower.contains("plt") || lower.contains("platelet") -> "الصفائح الدموية"
            lower.contains("creatin") -> "وظائف الكلى (الكرياتينين)"
            lower.contains("urea") || lower.contains("bun") -> "وظائف الكلى (البولينا)"
            lower.contains("uric") -> "حمض البوليك (النقرس)"
            lower.contains("alt") || lower.contains("sgpt") || lower.contains("ast") || lower.contains("sgot") -> "إنزيمات الكبد"
            lower.contains("bilirubin") -> "نسبة الصفراء"
            lower.contains("cholest") -> "نسبة الكوليسترول"
            lower.contains("triglycer") -> "الدهون الثلاثية"
            lower.contains("tsh") || lower.contains("thyroid") -> "هرمون الغدة الدرقية"
            lower.contains("vit") && lower.contains("d") -> "فيتامين د"
            lower.contains("calcium") || lower == "ca" -> "نسبة الكالسيوم"
            lower.contains("iron") || lower == "fe" -> "نسبة الحديد"
            lower.contains("ferritin") -> "مخزون الحديد"
            lower.contains("esr") -> "سرعة الترسيب"
            lower.contains("crp") -> "معامل الالتهاب"
            lower.contains("specific gravity") || lower == "sg" -> "كثافة البول"
            lower == "ph" -> "درجة حموضة البول"
            lower.contains("epithel") -> "الخلايا الطلائية في البول"
            lower.contains("mucus") -> "المخاط في البول"
            lower.contains("ketone") -> "الأسيتون في البول"
            lower.contains("sodium") || lower == "na" -> "الصوديوم"
            lower.contains("potassium") || lower == "k" -> "البوتاسيوم"
            else -> "أحد مؤشرات التحليل"
        }
    }

    private fun extractInlineNumericRange(line: String, startIndex: Int): Pair<Double, Double>? {
        val tail = if (startIndex in 0 until line.length) line.substring(startIndex) else line
        // Avoid interpreting dates like 19/11/2026 or 11-2026 as reference ranges
        val sanitizedTail = tail
            .replace(Regex("\\b\\d{1,4}[/\\-]\\d{1,2}[/\\-](?:19|20)\\d{2}\\b"), " ")
            .replace(Regex("\\b(?:19|20)\\d{2}[/\\-]\\d{1,2}[/\\-]\\d{1,2}\\b"), " ")
        val m = Regex("(?:\\(|\\[|ref\\.?\\s*range\\s*[:=]?)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:-|to|–|~)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
            .find(sanitizedTail) ?: return null
        val low = m.groupValues[1].toDoubleOrNull() ?: return null
        val high = m.groupValues[2].toDoubleOrNull() ?: return null
        if (low >= 1900.0 || high >= 1900.0) return null
        return if (high >= low) (low to high) else null
    }

    private fun extractUpperThreshold(line: String, startIndex: Int): Double? {
        val tail = if (startIndex in 0 until line.length) line.substring(startIndex) else line
        val m = Regex("(?:<|<=|up\\s*to)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(tail) ?: return null
        return m.groupValues[1].toDoubleOrNull()
    }

    private fun extractFirstNumber(raw: String): Double? {
        val m = Regex("\\d+(?:\\.\\d+)?").find(raw) ?: return null
        return m.value.toDoubleOrNull()
    }

    /** Returns >0 if abnormal/elevated, 0 if normal, -1 if indeterminate. */
    private fun evaluateSemiQuantOrRange(rawVal: String, lowNormal: Double, highNormal: Double): Int {
        val lower = rawVal.trim().lowercase()
        if (lower in setOf("nil", "absent", "negative", "neg", "none", "normal")) return 0
        if (lower.startsWith("+") || lower in setOf("positive", "pos", "present", "many", "heavy", "moderate", "few", "rare", "trace")) {
            return if (lower == "few" || lower == "rare" || lower == "trace") 1 else 2
        }
        val rangeNums = Regex("\\d+(?:\\.\\d+)?").findAll(lower).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (rangeNums.isEmpty()) return -1
        val maxVal = rangeNums.maxOrNull() ?: return -1
        val minVal = rangeNums.minOrNull() ?: return -1
        return if (maxVal > highNormal || minVal < lowNormal) 1 else 0
    }

    private fun formatNum(d: Double): String {
        return if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
    }

    private fun buildOfflineFallbackText(
        member: FamilyMemberEntity?,
        patientName: String,
        records: List<MedicalRecordEntity>,
        isEn: Boolean,
    ): String {
        val sorted = records.sortedByDescending { it.createdAtMillis }
        val resolvedName = member?.fullName?.takeIf { it.isNotBlank() } ?: patientName.ifBlank {
            if (isEn) "Patient" else "المريض"
        }

        val vitalsParts = buildList {
            if (member != null && member.age > 0) {
                add(if (isEn) "Age: ${member.age} yrs" else "السن: ${member.age} سنة")
            }
            if (member != null && member.weightKg > 0.0) {
                val w = if (member.weightKg % 1.0 == 0.0) member.weightKg.toInt().toString() else member.weightKg.toString()
                add(if (isEn) "Weight: $w kg" else "الوزن: $w كجم")
            }
            if (member != null && member.heightCm > 0.0) {
                val h = if (member.heightCm % 1.0 == 0.0) member.heightCm.toInt().toString() else member.heightCm.toString()
                add(if (isEn) "Height: $h cm" else "الطول: $h سم")
            }
            if (member != null && member.bloodType.isNotBlank()) {
                add(if (isEn) "Blood Type: ${member.bloodType}" else "فصيلة الدم: ${member.bloodType}")
            }
        }

        val chronicLabels = if (member != null) {
            ChronicConditionsCatalog.formatLabels(
                rawKeys = member.chronicConditions,
                otherNotes = member.otherChronicNotes,
                isEn = isEn,
            )
        } else ""
        val allergyText = member?.drugAllergies?.trim().orEmpty()

        val clinicalTextSources = mutableListOf<String>()
        sorted.forEach { rec ->
            if (rec.notes.isNotBlank()) clinicalTextSources += rec.notes
            if (rec.diagnosis.isNotBlank()) clinicalTextSources += rec.diagnosis
            if (rec.attachmentPath.isNotBlank() && ImageStorageHelper.isPdfAttachment(rec.attachmentPath, rec.attachmentMimeType)) {
                val extractedPdf = extractReadablePdfText(rec.attachmentPath)
                if (extractedPdf.isNotBlank()) clinicalTextSources += extractedPdf
            }
        }
        val parsedLab = parseClinicalLabText(clinicalTextSources, member, isEn)

        val uploadedLabs = sorted.filter {
            it.recordType == MedicalRecordType.LAB_RESULT ||
                it.recordType == MedicalRecordType.PATIENT_UPLOAD ||
                it.attachmentPath.isNotBlank() ||
                it.attachmentName.isNotBlank()
        }.take(8).map { rec ->
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            val title = localized.diagnosis.trim().ifBlank { rec.attachmentName.ifBlank { if (isEn) "Uploaded Lab / File" else "تحليل / مرفق مرفوع" } }
            val fileSuffix = rec.attachmentName.trim()
                .takeIf { it.isNotEmpty() && !title.contains(it, ignoreCase = true) }
                ?.let { " ($it)" }
                .orEmpty()
            val datePart = rec.visitDateLabel.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
            "• $title$fileSuffix$datePart"
        }.distinct()

        val meds = LinkedHashSet<String>()
        sorted.filter {
            it.recordType == MedicalRecordType.PRESCRIPTION || it.recordType == MedicalRecordType.EXTERNAL_VISIT
        }.forEach { rec ->
            val localized = BilingualMedicalTranslator.resolveForDisplay(rec, showEnglish = isEn)
            localized.medications
                .split('\n')
                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                .filter { it.isNotEmpty() }
                .forEach { medLine ->
                    if (meds.size < 8) meds += "• $medLine"
                }
        }

        val generalStatusText = when {
            parsedLab.normalCount > 0 && parsedLab.followUpPoints.isEmpty() -> {
                val normalsStr = parsedLab.normalHighlights.take(4).joinToString(" • ")
                if (isEn) {
                    "Most readable indicators (${parsedLab.normalCount}) are within normal reference limits: $normalsStr."
                } else {
                    "معظم مؤشرات التحليل المقروءة (${parsedLab.normalCount}) ضمن المعدل الطبيعي: $normalsStr."
                }
            }
            parsedLab.normalCount > 0 && parsedLab.followUpPoints.isNotEmpty() -> {
                val normalsStr = parsedLab.normalHighlights.take(3).joinToString(" • ")
                if (isEn) {
                    "${parsedLab.normalCount} indicators are within normal limits ($normalsStr), alongside specific points below needing doctor attention."
                } else {
                    "يوجد ${parsedLab.normalCount} مؤشرات ضمن المعدل الطبيعي ($normalsStr)، مع وجود النقاط التالية التي تحتاج لمراجعة الطبيب."
                }
            }
            parsedLab.followUpPoints.isNotEmpty() -> {
                if (isEn) {
                    "Specific laboratory indicators or chronic-condition precautions were detected for physician review."
                } else {
                    "تم رصد بعض المؤشرات أو التنبيهات الصحية التي تستدعي متابعة الطبيب المعالج."
                }
            }
            else -> {
                if (isEn) {
                    "Clinical bio profile, chronic conditions, and uploaded files for $resolvedName are organized below for doctor review."
                } else {
                    "البيانات الحيوية والأمراض المزمنة والملفات المرفوعة لـ $resolvedName منظمة وجاهزة لمراجعة الطبيب."
                }
            }
        }

        val numberedFollowUps = if (parsedLab.followUpPoints.isNotEmpty()) {
            parsedLab.followUpPoints.mapIndexed { idx, pt -> "${idx + 1}. $pt" }.joinToString("\n")
        } else {
            if (isEn) {
                "1. No abnormal numeric flags were detected in the readable text; show the original attached report to your treating doctor."
            } else {
                "1. لم تُرصد قيم غير طبيعية في النص المقروء، ويُنصح بعرض أصل التقرير المرفق على الطبيب للمراجعة."
            }
        }

        return buildString {
            append(
                if (isEn) "📋 Structured Medical Summary — $resolvedName\n"
                else "📋 الملخص الطبي المنظم — $resolvedName\n",
            )
            append("\n")
            append(if (isEn) "✅ Overall Status & Normal Values:\n" else "✅ الحالة العامة والقيم الطبيعية:\n")
            append(generalStatusText)

            append("\n\n")
            append(if (isEn) "📌 Points Requiring Physician Follow-Up:\n" else "📌 نقاط تحتاج لمتابعة أو انتباه الطبيب:\n")
            append(numberedFollowUps)

            append("\n\n")
            append(if (isEn) "📊 Vitals & Bio:\n" else "📊 البيانات الحيوية:\n")
            append(
                if (vitalsParts.isNotEmpty()) vitalsParts.joinToString(" • ")
                else if (isEn) "No bio measurements logged yet." else "لم تُسجّل قياسات حيوية بعد.",
            )

            append("\n\n")
            append(if (isEn) "🩺 Chronic Conditions & Allergies:\n" else "🩺 الأمراض المزمنة والحساسية:\n")
            if (chronicLabels.isNotBlank()) {
                append(chronicLabels)
            } else {
                append(if (isEn) "No chronic conditions logged." else "لا توجد أمراض مزمنة مسجلة.")
            }
            if (allergyText.isNotBlank()) {
                append("\n")
                append(if (isEn) "⚠️ Drug Allergies: $allergyText" else "⚠️ حساسية أدوية: $allergyText")
            }

            append("\n\n")
            append(if (isEn) "🧪 Uploaded Lab Reports & Files (${uploadedLabs.size}):\n" else "🧪 التحاليل والمرفقات المرفوعة (${uploadedLabs.size}):\n")
            if (uploadedLabs.isNotEmpty()) {
                append(uploadedLabs.joinToString("\n"))
            } else {
                append(if (isEn) "No uploaded lab reports yet." else "لا توجد ملفات تحاليل مرفوعة حالياً.")
            }

            append("\n\n")
            append(if (isEn) "💊 Current & Recorded Medications (${meds.size}):\n" else "💊 الأدوية الحالية والمسجلة (${meds.size}):\n")
            if (meds.isNotEmpty()) {
                append(meds.joinToString("\n"))
            } else {
                append(if (isEn) "No active medications recorded." else "لا توجد أدوية حالية مسجلة.")
            }
        }.trim()
    }

    /**
     * يُنشئ فوراً التدوين الكتابي الدائم (بالعربية المبسطة والإنجليزية) لأي حدث طبي:
     * - التحاليل والأشعة المرفوعة (LAB_RESULT / PATIENT_UPLOAD)
     * - الكشوفات والروشتات والزيارات الخارجية (PRESCRIPTION / EXTERNAL_VISIT) مع سبب الوصف ودواعي الاستخدام
     */
    fun buildPermanentLogNarrative(
        record: MedicalRecordEntity,
        member: FamilyMemberEntity?,
        providerLabel: String,
    ): Pair<String, String> {
        val ts = record.createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val dateTimeAr = TimeUtils.formatActualDateTimeLabel(ts, isEn = false)
        val dateTimeEn = TimeUtils.formatActualDateTimeLabel(ts, isEn = true)
        val resolvedProviderAr = providerLabel.ifBlank {
            record.doctorName.ifBlank {
                if (record.recordType == MedicalRecordType.PATIENT_UPLOAD) "رفع المريض / الأسرة" else "منصة طب الحجايزة"
            }
        }
        val resolvedProviderEn = providerLabel.ifBlank {
            record.doctorName.ifBlank {
                if (record.recordType == MedicalRecordType.PATIENT_UPLOAD) "Patient / Family Upload" else "Teb Al-Hajayzah"
            }
        }

        val isLabOrUpload = record.recordType == MedicalRecordType.LAB_RESULT ||
            record.recordType == MedicalRecordType.PATIENT_UPLOAD

        if (isLabOrUpload) {
            val sources = buildList {
                if (record.notes.isNotBlank()) add(record.notes)
                if (record.diagnosis.isNotBlank()) add(record.diagnosis)
                if (record.attachmentPath.isNotBlank() && ImageStorageHelper.isPdfAttachment(record.attachmentPath, record.attachmentMimeType)) {
                    val extracted = extractReadablePdfText(record.attachmentPath)
                    if (extracted.isNotBlank()) add(extracted)
                }
            }
            val parsedAr = parseClinicalLabText(sources, member, isEn = false)
            val parsedEn = parseClinicalLabText(sources, member, isEn = true)
            val fileTitle = record.diagnosis.trim().ifBlank {
                record.attachmentName.trim().ifBlank { "تحليل / أشعة طبية" }
            }
            val cleanTitleAr = translateFileTitleToPlainAr(fileTitle)
            val normalsAr = parsedAr.normalHighlights.take(4).joinToString("، ")
            val followUpsAr = parsedAr.followUpPoints.take(5).joinToString("؛ ")
            val narrativeAr = buildString {
                append("بتاريخ $dateTimeAr، تم تسجيل وتوثيق ($cleanTitleAr) عبر ($resolvedProviderAr).")
                when {
                    parsedAr.normalCount > 0 && parsedAr.followUpPoints.isEmpty() -> {
                        append(" تُظهر القيم المقروءة حالة مطمئنة وضمن المعدل الطبيعي ($normalsAr).")
                    }
                    parsedAr.followUpPoints.isNotEmpty() -> {
                        if (normalsAr.isNotBlank()) {
                            append(" مؤشرات ضمن الطبيعي: $normalsAr.")
                        }
                        append(" نقاط تستدعي مراجعة الطبيب: $followUpsAr.")
                    }
                    else -> {
                        val safeNotes = stripMetadataLines(record.notes).trim()
                        if (safeNotes.isNotBlank() && !Regex("[A-Za-z]{4,}").containsMatchIn(safeNotes)) {
                            append(" تفاصيل التقرير المرفق: $safeNotes.")
                        } else {
                            append(" تم حفظ التقرير في السجل الطبي الدائم لمراجعته مع الطبيب المختص.")
                        }
                    }
                }
                append(" (${PatientMedicalSummary.STANDARD_DISCLAIMER})")
            }.trim()

            val normalsEn = parsedEn.normalHighlights.take(4).joinToString(", ")
            val followUpsEn = parsedEn.followUpPoints.take(5).joinToString("; ")
            val narrativeEn = buildString {
                append("On $dateTimeEn, ($fileTitle) was documented via ($resolvedProviderEn).")
                when {
                    parsedEn.normalCount > 0 && parsedEn.followUpPoints.isEmpty() -> {
                        append(" Readable clinical values appear reassuring and within normal limits ($normalsEn).")
                    }
                    parsedEn.followUpPoints.isNotEmpty() -> {
                        if (normalsEn.isNotBlank()) append(" Normal indicators: $normalsEn.")
                        append(" Points for physician follow-up: $followUpsEn.")
                    }
                    else -> {
                        append(" Archived permanently in the medical record for treating physician review.")
                    }
                }
                append(" (${PatientMedicalSummary.STANDARD_DISCLAIMER_EN})")
            }.trim()

            return narrativeAr to narrativeEn
        }

        // PRESCRIPTION or EXTERNAL_VISIT
        val localizedAr = BilingualMedicalTranslator.resolveForDisplay(record, showEnglish = false)
        val localizedEn = BilingualMedicalTranslator.resolveForDisplay(record, showEnglish = true)
        val specialtyAr = record.specialty.ifBlank { "كشف طبي" }
        val specialtyEn = SpecialtyCatalog.displaySpecialty(specialtyAr, isEn = true)
        val diagAr = localizedAr.diagnosis.trim().ifBlank { "متابعة طبية عامة" }
        val diagEn = localizedEn.diagnosis.trim().ifBlank { diagAr }
        val medsListAr = localizedAr.medications.lines().map { it.trim().removePrefix("•").trim() }.filter { it.isNotBlank() }
        val medsListEn = localizedEn.medications.lines().map { it.trim().removePrefix("•").trim() }.filter { it.isNotBlank() }
        val notesAr = stripMetadataLines(localizedAr.notes).trim()
        val notesEn = stripMetadataLines(localizedEn.notes).trim()

        val indicationAr = buildMedicationIndicationSummary(diagAr, specialtyAr, medsListAr, notesAr, isEn = false)
        val indicationEn = buildMedicationIndicationSummary(diagEn, specialtyEn, medsListEn, notesEn, isEn = true)

        val narrativeAr = buildString {
            append("📅 التاريخ والوقت الفعلي: $dateTimeAr\n")
            append("👨‍⚕️ الطبيب / الجهة: $resolvedProviderAr ($specialtyAr)\n")
            append("🩺 التشخيص: $diagAr\n")
            append("💊 الأدوية والجرعات الموصوفة: ")
            if (medsListAr.isNotEmpty()) {
                append(medsListAr.joinToString(" • "))
            } else {
                append("لم تُدرج أدوية في هذه الزيارة")
            }
            append("\n🎯 سبب الوصف ودواعي الاستخدام: $indicationAr")
            if (notesAr.isNotBlank()) {
                append("\n📝 توصيات الطبيب: $notesAr")
            }
        }.trim()

        val narrativeEn = buildString {
            append("📅 Date & Time: $dateTimeEn\n")
            append("👨‍⚕️ Physician / Provider: $resolvedProviderEn ($specialtyEn)\n")
            append("🩺 Diagnosis: $diagEn\n")
            append("💊 Prescribed Medications & Dosages: ")
            if (medsListEn.isNotEmpty()) {
                append(medsListEn.joinToString(" • "))
            } else {
                append("No medications listed for this visit")
            }
            append("\n🎯 Indication / Reason for Prescribing: $indicationEn")
            if (notesEn.isNotBlank()) {
                append("\n📝 Doctor Notes: $notesEn")
            }
        }.trim()

        return narrativeAr to narrativeEn
    }

    private fun translateFileTitleToPlainAr(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("urine") || lower.contains("بول") -> "تحليل بول شامل"
            lower.contains("cbc") || lower.contains("صورة دم") || lower.contains("blood") -> "تحليل صورة دم كاملة"
            lower.contains("hba1c") || lower.contains("sugar") || lower.contains("glucose") || lower.contains("سكر") -> "تحليل السكر"
            lower.contains("x-ray") || lower.contains("xray") || lower.contains("أشعة") || lower.contains("ct") || lower.contains("mri") || lower.contains("sonar") || lower.contains("سونا") -> "تقرير أشعة وفحص تصويري"
            Regex("[A-Za-z]{4,}").containsMatchIn(raw) && !raw.any { it in '\u0600'..'\u06FF' } -> "تقرير تحليل أو أشعة طبية"
            else -> raw
        }
    }

    private fun buildMedicationIndicationSummary(
        diagnosis: String,
        specialty: String,
        meds: List<String>,
        notes: String,
        isEn: Boolean,
    ): String {
        if (isEn) {
            return buildString {
                append("Prescribed to manage $diagnosis within $specialty care")
                if (meds.isNotEmpty()) {
                    append(" (${meds.size} medication(s) aligned with the documented diagnosis")
                    if (notes.isNotBlank()) append(" and follow-up plan")
                    append(")")
                }
                append(".")
            }
        }
        val medHints = meds.mapNotNull { medLine ->
            val lower = medLine.lowercase()
            when {
                lower.contains("augmentin") || lower.contains("hibiotic") || lower.contains("أموكسيسيلين") || lower.contains("أوجمنتين") || lower.contains("هايبوتيك") || lower.contains("مضاد") -> "مضاد حيوي لعلاج العدوى البكتيرية"
                lower.contains("controloc") || lower.contains("nexium") || lower.contains("كونترولوك") || lower.contains("نيكسيوم") || lower.contains("جافيسكون") -> "حماية المعدة وتقليل الحموضة والارتجاع"
                lower.contains("cetal") || lower.contains("brufen") || lower.contains("cataflam") || lower.contains("سيتال") || lower.contains("بروفين") || lower.contains("كتافلام") || lower.contains("مسكن") -> "تسكين الألم وخفض الحرارة والالتهاب"
                lower.contains("dimra") || lower.contains("myolgin") || lower.contains("ديمرا") || lower.contains("ميولجين") -> "باسط للعضلات ومسكن لآلام العظام"
                lower.contains("glucophage") || lower.contains("amaryl") || lower.contains("جلوكوفاج") || lower.contains("أماريل") -> "تنظيم مستوى السكر في الدم"
                lower.contains("concor") || lower.contains("norvasc") || lower.contains("كونكور") || lower.contains("نورفاسك") -> "تنظيم ضغط الدم ودعم القلب"
                lower.contains("folic") || lower.contains("osteocare") || lower.contains("vidrop") || lower.contains("فوليك") || lower.contains("أوستيوكير") || lower.contains("فيتامين") -> "دعم التغذية والفيتامينات والمعادن الأساسية"
                lower.contains("zyrtec") || lower.contains("farcolin") || lower.contains("زيرتك") || lower.contains("فاركولين") -> "تخفيف الحساسية وتحسين التنفس"
                else -> null
            }
        }.distinct()

        return buildString {
            append("علاج ومتابعة ($diagnosis) في تخصص ($specialty)")
            if (medHints.isNotEmpty()) {
                append(" — يشمل: ${medHints.joinToString("، ")}")
            }
            if (notes.isNotBlank()) {
                append(" وفق خطة الطبيب ($notes)")
            }
        }.trim()
    }
}
