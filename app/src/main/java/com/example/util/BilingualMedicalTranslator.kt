package com.example.util

import com.example.data.local.MedicalRecordEntity

data class BilingualPrescriptionBundle(
    val diagnosisAr: String,
    val diagnosisEn: String,
    val medicationsAr: String,
    val medicationsEn: String,
    val notesAr: String,
    val notesEn: String,
    val aiReportAr: String,
    val aiReportEn: String,
)

data class LocalizedMedicalRecordView(
    val diagnosis: String,
    val medications: String,
    val notes: String,
    val aiReport: String,
    val specialty: String,
)

object BilingualMedicalTranslator {

    private val diagnosisPairs: List<Pair<String, String>> = listOf(
        "التهاب حاد باللوزتين" to "Acute Tonsillitis",
        "التهاب بالحلق" to "Acute Pharyngitis (Sore Throat)",
        "نزلة برد" to "Common Cold / Upper Respiratory Tract Infection",
        "انفلونزا" to "Seasonal Influenza",
        "التهاب شعبي" to "Acute Bronchitis",
        "حساسية صدر" to "Bronchial Asthma / Allergic Airway Disease",
        "نزلة معوية بسيطة" to "Mild Acute Gastroenteritis",
        "نزلة معوية" to "Acute Gastroenteritis",
        "ارتجاع مريئي" to "Gastroesophageal Reflux Disease (GERD)",
        "التهاب بالمعدة" to "Gastritis",
        "قولون عصبي" to "Irritable Bowel Syndrome (IBS)",
        "متابعة حمل" to "Routine Antenatal Follow-up",
        "متابعة الحمل" to "Antenatal Care Follow-up",
        "تأخر إنجاب" to "Infertility / Fertility Evaluation",
        "تكيس مبايض" to "Polycystic Ovary Syndrome (PCOS)",
        "أنيميا" to "Iron Deficiency Anemia",
        "فقر دم" to "Anemia",
        "ارتفاع ضغط الدم" to "Hypertension",
        "ضغط دم مرتفع" to "Essential Hypertension",
        "مرض السكري" to "Diabetes Mellitus",
        "السكري" to "Diabetes Mellitus Follow-up",
        "التهاب مسالك بولية" to "Urinary Tract Infection (UTI)",
        "خشونة بالركبة" to "Knee Osteoarthritis",
        "ألم أسفل الظهر" to "Low Back Pain / Lumbar Strain",
        "التهاب مفاصل" to "Arthritis",
        "تسوس أسنان" to "Dental Caries",
        "تسوس ضرس" to "Molar Dental Caries",
        "التهاب اللثة" to "Gingivitis",
        "حساسية جلدية" to "Allergic Dermatitis",
        "إكزيما" to "Eczema / Atopic Dermatitis",
        "التهاب بالعين" to "Conjunctivitis",
        "التهاب بالأذن الوسطى" to "Acute Otitis Media",
        "التهاب الجيوب الأنفية" to "Acute Sinusitis",
        "كشف طبي عام" to "General Medical Consultation",
        "متابعة لدى طبيب خارجي" to "External Specialist Follow-up",
        "متابعة دورية" to "Routine Clinical Follow-up",
    )

    private val phrasePairs: List<Pair<String, String>> = listOf(
        "كيس على نصف كوب ماء عند اللزوم" to "1 sachet in half glass of water as needed",
        "قرص قبل الفطار بنصف ساعة" to "1 tablet 30 min before breakfast",
        "قرص قبل الأكل بنصف ساعة 3 مرات" to "1 tablet 30 min before meals 3 times daily",
        "قرص بعد الأكل كل 12 ساعة لمدة 5 أيام" to "1 tablet after meals every 12 hours for 5 days",
        "قرص كل 12 ساعة لمدة 7 أيام" to "1 tablet every 12 hours for 7 days",
        "قرص كل 12 ساعة لمدة 5 أيام" to "1 tablet every 12 hours for 5 days",
        "قرص كل 8 ساعات بعد الأكل لمدة 5 أيام" to "1 tablet every 8 hours after meals for 5 days",
        "قرص كل 8 ساعات بعد الأكل عند اللزوم" to "1 tablet every 8 hours after meals as needed",
        "قرص كل 8 ساعات بعد الأكل" to "1 tablet every 8 hours after meals",
        "قرص كل 8 ساعات" to "1 tablet every 8 hours",
        "قرص كل 12 ساعة" to "1 tablet every 12 hours",
        "قرص كل 6 ساعات" to "1 tablet every 6 hours",
        "قرص صباحاً يومياً" to "1 tablet once daily in the morning",
        "قرص مساءً يومياً" to "1 tablet once daily in the evening",
        "قرص بعد الغداء يومياً" to "1 tablet daily after lunch",
        "قرص بعد وجبة الغداء يومياً" to "1 tablet daily after lunch",
        "قرص قبل الفطار يومياً" to "1 tablet daily before breakfast",
        "قرص يومياً في نفس الموعد" to "1 tablet once daily at the same time",
        "قرص واحد يومياً لمدة 7 أيام" to "1 tablet once daily for 7 days",
        "كبسولة كل 6 ساعات لمدة 5 أيام" to "1 capsule every 6 hours for 5 days",
        "كبسولة كل 8 ساعات لمدة 5 أيام" to "1 capsule every 8 hours for 5 days",
        "كبسولة كل 8 ساعات بعد الأكل" to "1 capsule every 8 hours after meals",
        "كبسولة بعد الغداء بساعتين يومياً" to "1 capsule daily 2 hours after lunch",
        "كبسولة بعد الوجبة الرئيسية يومياً" to "1 capsule daily after main meal",
        "كبسولة مساءً يومياً" to "1 capsule once daily in the evening",
        "5 سم كل 12 ساعة لمدة 7 أيام" to "5 ml every 12 hours for 7 days",
        "5 سم كل 8 ساعات بعد الأكل لخفض الحرارة" to "5 ml every 8 hours after meals for fever",
        "5 سم كل 6 ساعات عند اللزوم" to "5 ml every 6 hours as needed",
        "5 سم 3 مرات يومياً" to "5 ml 3 times daily",
        "5 سم كل 12 ساعة" to "5 ml every 12 hours",
        "5 سم كل 8 ساعات" to "5 ml every 8 hours",
        "5 نقط مساءً يومياً" to "5 drops once daily in the evening",
        "جلسة بخار صباحاً ومساءً" to "Nebulizer session morning and evening",
        "قطرة في العين المصابة 3 مرات يومياً" to "1 drop in affected eye 3 times daily",
        "قطرة كل 6 ساعات لترطيب العين" to "1 drop every 6 hours for eye lubrication",
        "قطرة 3 مرات يومياً لمدة 7 أيام" to "1 drop 3 times daily for 7 days",
        "قطرة 3 مرات يومياً" to "1 drop 3 times daily",
        "بختان في كل فتحة أنف مرة يومياً" to "2 sprays in each nostril once daily",
        "دهان موضعي مرتين يومياً على موضع الألم" to "Apply topically twice daily to painful area",
        "دهان موضعي مرتين يومياً لمدة 5 أيام" to "Apply topically twice daily for 5 days",
        "دهان موضعي 3 مرات يومياً" to "Apply topically 3 times daily",
        "دهان موضعي مرتين يومياً" to "Apply topically twice daily",
        "مضمضة مرتين يومياً بعد غسل الأسنان" to "Mouthwash twice daily after brushing",
        "ملعقة كبيرة بعد الأكل وعند اللزوم" to "1 tablespoon after meals and as needed",
        "مرة واحدة مساءً لمدة 3 أيام" to "Once nightly for 3 days",
        "مرة واحدة يومياً بعد وجبة دسمة" to "Once daily after a main meal",
        "قبل الفطار بنصف ساعة" to "30 minutes before breakfast",
        "بعد الأكل مباشرة" to "Immediately after meals",
        "لمدة 5 أيام" to "for 5 days",
        "لمدة 7 أيام" to "for 7 days",
        "لمدة أسبوعين" to "for 2 weeks",
        "عند اللزوم" to "as needed",
        "حسب إرشادات الطبيب" to "As directed by physician",
        "حسب الإرشادات" to "As directed",
    )

    private val notePairs: List<Pair<String, String>> = listOf(
        "الراحة التامة وشرب سوائل دافئة" to "Complete rest and adequate warm fluids.",
        "الراحة وشرب سوائل دافئة" to "Adequate rest and warm fluids.",
        "شرب سوائل بكثرة" to "Maintain good oral hydration with plenty of fluids.",
        "الالتزام بمواعيد الدواء ومراجعة الطبيب عند اللزوم" to "Adhere strictly to medication schedule and follow up if symptoms persist.",
        "متابعة بعد أسبوع" to "Follow-up visit in 1 week.",
        "تجنب الأطعمة الدسمة والحارة" to "Avoid spicy, fried, and fatty foods.",
        "الالتزام بالراحة وتجنب المجهود" to "Rest and avoid strenuous physical activity.",
        "قياس ضغط الدم يومياً" to "Monitor blood pressure daily.",
    )

    fun containsArabic(text: String): Boolean = text.any { it in '\u0600'..'\u06FF' }

    fun toEnglishDiagnosis(input: String): String {
        val clean = input.trim()
        if (clean.isEmpty()) return ""
        if (!containsArabic(clean)) return clean
        diagnosisPairs.firstOrNull { clean.equals(it.first, ignoreCase = true) }?.let { return it.second }
        diagnosisPairs.firstOrNull { clean.contains(it.first, ignoreCase = true) }?.let { match ->
            val rest = clean.replace(match.first, "").trim(' ', '-', '،', ',')
            return if (rest.isEmpty()) match.second else "${match.second} ($rest)"
        }
        return "Clinical Assessment: $clean"
    }

    fun toArabicDiagnosis(input: String): String {
        val clean = input.trim()
        if (clean.isEmpty()) return ""
        if (containsArabic(clean)) return clean
        diagnosisPairs.firstOrNull { clean.equals(it.second, ignoreCase = true) }?.let { return it.first }
        diagnosisPairs.firstOrNull { clean.contains(it.second, ignoreCase = true) }?.let { return it.first }
        return "تقييم طبي: $clean"
    }

    fun toEnglishDosage(dosage: String): String {
        val clean = dosage.trim()
        if (clean.isEmpty()) return "As directed by physician"
        if (!containsArabic(clean)) return clean
        var translated = clean
        phrasePairs.forEach { (ar, en) ->
            if (translated.contains(ar)) {
                translated = translated.replace(ar, en)
            }
        }
        translated = translated
            .replace("مجم", "mg")
            .replace("مكجم", "mcg")
            .replace("جم", "g")
            .replace("سم", "ml")
            .replace("مل", "ml")
            .replace("قرص", "tablet")
            .replace("كبسولة", "capsule")
            .replace("شراب", "syrup")
            .replace("قطرة", "drops")
            .replace("ساعات", "hours")
            .replace("ساعة", "hour")
            .replace("أيام", "days")
            .replace("يومياً", "daily")
            .replace("صباحاً", "morning")
            .replace("مساءً", "evening")
            .replace("بعد الأكل", "after meals")
            .replace("قبل الأكل", "before meals")
        return translated.trim()
    }

    fun toArabicDosage(dosage: String): String {
        val clean = dosage.trim()
        if (clean.isEmpty()) return "حسب إرشادات الطبيب"
        if (containsArabic(clean)) return clean
        var translated = clean
        phrasePairs.forEach { (ar, en) ->
            if (translated.contains(en, ignoreCase = true)) {
                translated = translated.replace(en, ar, ignoreCase = true)
            }
        }
        translated = translated
            .replace("every 12 hours", "كل 12 ساعة", ignoreCase = true)
            .replace("every 8 hours", "كل 8 ساعات", ignoreCase = true)
            .replace("every 6 hours", "كل 6 ساعات", ignoreCase = true)
            .replace("1 tablet", "قرص", ignoreCase = true)
            .replace("1 capsule", "كبسولة", ignoreCase = true)
            .replace("after meals", "بعد الأكل", ignoreCase = true)
            .replace("before breakfast", "قبل الفطار", ignoreCase = true)
            .replace("as needed", "عند اللزوم", ignoreCase = true)
        return translated.trim()
    }

    fun translateMedicationsLineToEnglish(line: String): String {
        val stripped = line.trim().removePrefix("•").removePrefix("-").trim()
        if (stripped.isEmpty()) return ""
        val parts = stripped.split("—", limit = 2)
        val rawDrug = parts[0].trim()
        val rawDosage = parts.getOrNull(1)?.trim().orEmpty()

        // Extract English name inside parentheses when formatted as "كونترولوك (Controloc) 40 مجم"
        val parenMatch = Regex("\\(([A-Za-z0-9\\s\\-]+)\\)").find(rawDrug)
        val cleanDrugEn = if (parenMatch != null) {
            val enBrand = parenMatch.groupValues[1].trim()
            val afterParen = rawDrug.substring(parenMatch.range.last + 1).trim()
            val normalizedStrength = afterParen
                .replace("مجم/5 مل", "mg/5ml")
                .replace("مجم/مل", "mg/ml")
                .replace("مجم", "mg")
                .replace("مكجم", "mcg")
                .replace("جم", "g")
                .replace("وحدة", "IU")
            "$enBrand ${normalizedStrength}".trim()
        } else {
            rawDrug
                .replace("شراب", "Syrup")
                .replace("أقراص", "Tablets")
                .replace("كبسول", "Capsules")
                .replace("كريم", "Cream")
                .replace("قطرة", "Drops")
                .replace("مجم", "mg")
                .replace("جم", "g")
                .replace("مل", "ml")
        }

        val dosageEn = toEnglishDosage(rawDosage.ifBlank { "حسب إرشادات الطبيب" })
        return "• $cleanDrugEn — $dosageEn"
    }

    fun translateMedicationsText(raw: String, toEnglish: Boolean): String {
        if (raw.isBlank()) return ""
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n") { line ->
                if (toEnglish) {
                    translateMedicationsLineToEnglish(line)
                } else {
                    val stripped = line.removePrefix("•").removePrefix("-").trim()
                    val parts = stripped.split("—", limit = 2)
                    val drugPart = parts[0].trim()
                    val dosagePart = parts.getOrNull(1)?.trim().orEmpty()
                    if (dosagePart.isNotEmpty()) {
                        "• $drugPart — ${toArabicDosage(dosagePart)}"
                    } else {
                        "• $drugPart"
                    }
                }
            }
    }

    fun toEnglishNotes(notes: String): String {
        val clean = notes.trim()
        if (clean.isEmpty()) return ""
        if (!containsArabic(clean)) return clean
        notePairs.firstOrNull { clean.equals(it.first, ignoreCase = true) }?.let { return it.second }
        var result = clean
        notePairs.forEach { (ar, en) ->
            result = result.replace(ar, en)
        }
        return if (containsArabic(result)) "Clinical Note: $result" else result
    }

    fun toArabicNotes(notes: String): String {
        val clean = notes.trim()
        if (clean.isEmpty()) return ""
        if (containsArabic(clean)) return clean
        notePairs.firstOrNull { clean.equals(it.second, ignoreCase = true) }?.let { return it.first }
        return "ملاحظات الطبيب: $clean"
    }

    fun buildBilingualBundle(
        patientName: String,
        doctorName: String,
        specialty: String,
        diagnosisInput: String,
        medicationsInput: String,
        notesInput: String,
        existingDiagnosisEn: String = "",
        existingMedicationsEn: String = "",
        existingNotesEn: String = "",
    ): BilingualPrescriptionBundle {
        val cleanDiag = diagnosisInput.trim().ifBlank { "كشف طبي عام" }
        val diagAr = toArabicDiagnosis(cleanDiag)
        val diagEn = existingDiagnosisEn.trim().ifBlank { toEnglishDiagnosis(cleanDiag) }

        val medsAr = translateMedicationsText(medicationsInput, toEnglish = false)
            .ifBlank { medicationsInput.trim() }
        val medsEn = existingMedicationsEn.trim().ifBlank {
            translateMedicationsText(medicationsInput, toEnglish = true)
        }

        val notesAr = toArabicNotes(notesInput)
        val notesEn = existingNotesEn.trim().ifBlank { toEnglishNotes(notesInput) }

        val specAr = SpecialtyCatalog.displaySpecialty(specialty, isEn = false)
        val specEn = SpecialtyCatalog.displaySpecialty(specialty, isEn = true)

        val medCount = medsAr.lines().count { it.isNotBlank() }
        val patientLabelAr = patientName.ifBlank { "المريض" }
        val patientLabelEn = patientName.ifBlank { "Patient" }

        val aiReportAr = buildString {
            appendLine("التقرير الطبي المبسط للمريض ($patientLabelAr):")
            appendLine("• الطبيب المعالج: $doctorName ($specAr)")
            appendLine("• التشخيص الطبي: $diagAr")
            if (medCount > 0) {
                appendLine("• الخطة العلاجية: تم وصف $medCount صنف دوائي بالجرعات المحددة بالروشتة.")
            } else {
                appendLine("• الخطة العلاجية: متابعة إكلينيكية بدون أدوية مضافة حالياً.")
            }
            if (notesAr.isNotBlank()) {
                appendLine("• إرشادات مهمة: $notesAr")
            }
            append("• تنبيه: يرجى الالتزام بالجرعات وعدم إيقاف العلاج قبل استشارة الطبيب.")
        }.trim()

        val aiReportEn = buildString {
            appendLine("Clinical Medical Report ($patientLabelEn):")
            appendLine("• Attending Physician: $doctorName ($specEn)")
            appendLine("• Assessment / Diagnosis: $diagEn")
            if (medCount > 0) {
                appendLine("• Therapeutic Plan: $medCount prescribed medication(s) as detailed in Rx.")
            } else {
                appendLine("• Therapeutic Plan: Clinical observation / conservative management.")
            }
            if (notesEn.isNotBlank()) {
                appendLine("• Patient Instructions: $notesEn")
            }
            append("• Clinical Note: Adhere to prescribed regimen and seek review if symptoms worsen.")
        }.trim()

        return BilingualPrescriptionBundle(
            diagnosisAr = diagAr,
            diagnosisEn = diagEn,
            medicationsAr = medsAr,
            medicationsEn = medsEn,
            notesAr = notesAr,
            notesEn = notesEn,
            aiReportAr = aiReportAr,
            aiReportEn = aiReportEn,
        )
    }

    fun resolveForDisplay(
        record: MedicalRecordEntity,
        showEnglish: Boolean,
    ): LocalizedMedicalRecordView {
        val bundle = buildBilingualBundle(
            patientName = record.patientName,
            doctorName = record.doctorName,
            specialty = record.specialty,
            diagnosisInput = record.diagnosis,
            medicationsInput = record.medications,
            notesInput = record.notes,
            existingDiagnosisEn = record.diagnosisEn,
            existingMedicationsEn = record.medicationsEn,
            existingNotesEn = record.notesEn,
        )
        return if (showEnglish) {
            LocalizedMedicalRecordView(
                diagnosis = record.diagnosisEn.ifBlank { bundle.diagnosisEn },
                medications = record.medicationsEn.ifBlank { bundle.medicationsEn },
                notes = record.notesEn.ifBlank { bundle.notesEn },
                aiReport = record.aiReportEn.ifBlank { bundle.aiReportEn },
                specialty = SpecialtyCatalog.displaySpecialty(record.specialty, isEn = true),
            )
        } else {
            LocalizedMedicalRecordView(
                diagnosis = bundle.diagnosisAr,
                medications = bundle.medicationsAr.ifBlank { record.medications },
                notes = bundle.notesAr.ifBlank { record.notes },
                aiReport = record.aiReportAr.ifBlank { bundle.aiReportAr },
                specialty = SpecialtyCatalog.displaySpecialty(record.specialty, isEn = false),
            )
        }
    }
}
