package com.example.util

import com.example.data.local.ClinicDoctorEntity

data class SuggestedDiagnosisItem(
    val ar: String,
    val en: String,
)

data class SuggestedSpecialtyMed(
    val id: String,
    val tradeName: String,
    val strength: String,
    val defaultDosage: String,
    val dosageEn: String,
)

object SpecialtyClinicalTemplates {

    const val CUSTOM_MED_PREFIX = "custom_med|"
    const val CUSTOM_DIAG_PREFIX = "custom_diag|"

    fun encodeCustomMedFav(tradeName: String, dosage: String): String {
        val cleanName = tradeName.replace("|", " ").trim()
        val cleanDose = dosage.replace("|", " ").trim().ifBlank { "حسب إرشادات الطبيب" }
        return "$CUSTOM_MED_PREFIX$cleanName|$cleanDose"
    }

    fun decodeCustomMedFav(key: String): Pair<String, String>? {
        if (!key.startsWith(CUSTOM_MED_PREFIX)) return null
        val rest = key.removePrefix(CUSTOM_MED_PREFIX)
        val parts = rest.split("|", limit = 2)
        val name = parts.getOrNull(0)?.trim().orEmpty()
        if (name.isEmpty()) return null
        val dose = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "حسب إرشادات الطبيب" }
        return name to dose
    }

    fun encodeCustomDiagFav(diagnosis: String): String =
        CUSTOM_DIAG_PREFIX + diagnosis.replace("|", " ").trim()

    fun decodeCustomDiagFav(key: String): String? {
        if (!key.startsWith(CUSTOM_DIAG_PREFIX)) return null
        return key.removePrefix(CUSTOM_DIAG_PREFIX).trim().takeIf { it.isNotEmpty() }
    }

    private val diagnosesBySpecialtyId: Map<String, List<SuggestedDiagnosisItem>> = mapOf(
        "obgyn" to listOf(
            SuggestedDiagnosisItem("متابعة حمل طبيعي", "Routine Antenatal Follow-up"),
            SuggestedDiagnosisItem("أنيميا الحمل", "Anemia in Pregnancy"),
            SuggestedDiagnosisItem("التهابات مهبلية", "Vaginal Infection / Vaginitis"),
            SuggestedDiagnosisItem("قيء الحمل", "Hyperemesis Gravidarum"),
            SuggestedDiagnosisItem("متابعة تبويض", "Ovulation Monitoring"),
            SuggestedDiagnosisItem("ارتفاع ضغط الحمل", "Gestational Hypertension"),
            SuggestedDiagnosisItem("تكيس المبايض وتأخر الإنجاب", "PCOS & Fertility Follow-up"),
        ),
        "pediatrics" to listOf(
            SuggestedDiagnosisItem("صفراء حديثي الولادة", "Neonatal Jaundice"),
            SuggestedDiagnosisItem("نزلة معوية", "Pediatric Gastroenteritis"),
            SuggestedDiagnosisItem("التهاب شعبي وحساسية صدر", "Bronchitis & Chest Allergy"),
            SuggestedDiagnosisItem("التهاب لوزتين وحلق", "Acute Tonsillitis & Pharyngitis"),
            SuggestedDiagnosisItem("سخونة وتسنين", "Fever & Teething"),
            SuggestedDiagnosisItem("نقص فيتامين د وكالسيوم", "Vitamin D & Calcium Deficiency"),
        ),
        "internal_medicine" to listOf(
            SuggestedDiagnosisItem("التهاب المعدة وارتجاع المريء", "Gastritis & GERD"),
            SuggestedDiagnosisItem("القولون العصبي ومغص", "Irritable Bowel Syndrome"),
            SuggestedDiagnosisItem("نزلة معوية وإسهال", "Acute Gastroenteritis"),
            SuggestedDiagnosisItem("ارتفاع ضغط الدم", "Essential Hypertension"),
            SuggestedDiagnosisItem("متابعة مرض السكر", "Diabetes Mellitus Follow-up"),
            SuggestedDiagnosisItem("أنيميا ونقص الحديد", "Iron Deficiency Anemia"),
        ),
        "orthopedics" to listOf(
            SuggestedDiagnosisItem("خشونة الركبة والمفاصل", "Knee Osteoarthritis"),
            SuggestedDiagnosisItem("ألم أسفل الظهر وعرق النسا", "Low Back Pain & Sciatica"),
            SuggestedDiagnosisItem("التهاب أوتار وشد عضلي", "Tendinitis & Muscle Spasm"),
            SuggestedDiagnosisItem("نقص فيتامين د وهشاشة عظام", "Vitamin D Deficiency & Osteopenia"),
            SuggestedDiagnosisItem("تورم والتواء مفصل", "Joint Sprain & Edema"),
        ),
        "cardiology" to listOf(
            SuggestedDiagnosisItem("ارتفاع ضغط الدم", "Hypertension Management"),
            SuggestedDiagnosisItem("اضطراب ضربات القلب", "Cardiac Arrhythmia"),
            SuggestedDiagnosisItem("قصور بالشرايين التاجية", "Ischemic Heart Disease"),
            SuggestedDiagnosisItem("ارتفاع الكوليسترول والدهون", "Dyslipidemia"),
        ),
        "ent" to listOf(
            SuggestedDiagnosisItem("التهاب اللوزتين والحلق", "Acute Tonsillopharyngitis"),
            SuggestedDiagnosisItem("التهاب الجيوب الأنفية", "Acute Sinusitis"),
            SuggestedDiagnosisItem("حساسية الأنف المزمنة", "Allergic Rhinitis"),
            SuggestedDiagnosisItem("التهاب الأذن الوسطى", "Otitis Media"),
        ),
        "dermatology" to listOf(
            SuggestedDiagnosisItem("إكزيما وحساسية جلدية", "Eczema & Contact Dermatitis"),
            SuggestedDiagnosisItem("التهاب بكتيري ودمامل", "Bacterial Skin Infection"),
            SuggestedDiagnosisItem("ارتكاريا وحكة جلدية", "Urticaria & Pruritus"),
            SuggestedDiagnosisItem("حب الشباب والتهاب البشرة", "Acne Vulgaris"),
        ),
        "ophthalmology" to listOf(
            SuggestedDiagnosisItem("التهاب ملتحمة العين", "Acute Conjunctivitis"),
            SuggestedDiagnosisItem("جفاف وحساسية العين", "Dry Eye & Ocular Allergy"),
            SuggestedDiagnosisItem("عدوى بكتيرية بالعين", "Bacterial Eye Infection"),
        ),
        "eye_care" to listOf(
            SuggestedDiagnosisItem("التهاب ملتحمة العين", "Acute Conjunctivitis"),
            SuggestedDiagnosisItem("جفاف وحساسية العين", "Dry Eye & Ocular Allergy"),
            SuggestedDiagnosisItem("فحص قاع العين والنظر", "Routine Vision & Fundus Exam"),
        ),
        "dentistry" to listOf(
            SuggestedDiagnosisItem("التهاب عصب الضرس وألم حاد", "Acute Pulpitis & Toothache"),
            SuggestedDiagnosisItem("خراج والتهاب اللثة", "Dental Abscess & Gingivitis"),
            SuggestedDiagnosisItem("متابعة ما بعد خلع الأسنان", "Post-Extraction Care"),
        ),
    )

    private val medsBySpecialtyId: Map<String, List<SuggestedSpecialtyMed>> = mapOf(
        "obgyn" to listOf(
            SuggestedSpecialtyMed("sug_folic", "حمض الفوليك (Folic Acid)", "5 مجم", "قرص صباحاً يومياً", "1 tab daily in the morning"),
            SuggestedSpecialtyMed("sug_iron_calc", "حديد وكالسيوم للحامل (فيروترون + أوستيوكير)", "كبسول + أقراص", "كبسولة حديد بعد الغداء + قرص كالسيوم مساءً", "Iron capsule after lunch + Calcium tab evening"),
            SuggestedSpecialtyMed("sug_duphaston", "مثبت حمل دوفاستون (Duphaston)", "10 مجم", "قرص كل 12 ساعة", "1 tab every 12 hours"),
            SuggestedSpecialtyMed("sug_utrogestan", "مثبت حمل يوتروجيستان (Utrogestan)", "200 مجم", "كبسولة مساءً يومياً", "1 cap nightly"),
            SuggestedSpecialtyMed("sug_pregnacare", "فيتامينات الحمل (Pregnacare)", "كبسول متكامل", "كبسولة بعد الوجبة الرئيسية", "1 cap daily after main meal"),
            SuggestedSpecialtyMed("sug_safe_abx", "مضاد حيوي آمن للحامل (Augmentin)", "1 جم", "قرص كل 12 ساعة لمدة 7 أيام", "1 tab every 12h for 7 days"),
            SuggestedSpecialtyMed("sug_gyno_dakt", "لبوس مهبلي (Gyno-Daktarin)", "400 مجم", "لبوسة مساءً لمدة 3 أيام", "1 vaginal supp nightly for 3 days"),
        ),
        "pediatrics" to listOf(
            SuggestedSpecialtyMed("sug_cetal_syr", "خافض حرارة شراب (Cetal)", "250 مجم/5 مل", "5 سم كل 6 ساعات عند اللزوم", "5 ml every 6 hours as needed"),
            SuggestedSpecialtyMed("sug_brufen_syr", "بروفين شراب للأطفال (Brufen)", "100 مجم/5 مل", "5 سم كل 8 ساعات بعد الأكل", "5 ml every 8h after meals"),
            SuggestedSpecialtyMed("sug_vidrop_drops", "نقط فيتامين د (Vidrop)", "2800 وحدة/مل", "4 نقط يومياً بالفم", "4 drops orally once daily"),
            SuggestedSpecialtyMed("sug_ors", "محلول معالجة الجفاف (Hydrosafe ORS)", "أكياس", "كيس على 200 مل ماء معقم بعد كل إسهال", "1 sachet in 200 ml water after each loose stool"),
            SuggestedSpecialtyMed("sug_farcolin", "موسع شعب (Farcolin / Ventolin)", "5 مجم/مل", "جلسة بخار صباحاً ومساءً", "Nebulizer session morning & evening"),
            SuggestedSpecialtyMed("sug_aug_ped", "أوجمنتين شراب (Augmentin)", "457 مجم/5 مل", "5 سم كل 12 ساعة لمدة 7 أيام", "5 ml every 12h for 7 days"),
            SuggestedSpecialtyMed("sug_zyrtec_ped", "زيرتك نقط للحساسية (Zyrtec)", "10 مجم/مل", "5 نقط مساءً يومياً", "5 drops nightly"),
        ),
        "internal_medicine" to listOf(
            SuggestedSpecialtyMed("sug_controloc", "كونترولوك (Controloc)", "40 مجم", "قرص قبل الفطار بنصف ساعة", "1 tab 30 min before breakfast"),
            SuggestedSpecialtyMed("sug_gaviscon", "جافيسكون شراب (Gaviscon)", "شراب معلق", "ملعقة كبيرة بعد الأكل", "1 tbsp after meals"),
            SuggestedSpecialtyMed("sug_colospasmin", "كولوسبازمين فورت", "135 مجم", "قرص قبل الأكل 3 مرات", "1 tab TID before meals"),
            SuggestedSpecialtyMed("sug_antinal", "أنتينال (Antinal)", "200 مجم", "كبسولة كل 6 ساعات لمدة 5 أيام", "1 cap every 6h for 5 days"),
            SuggestedSpecialtyMed("sug_glucophage", "جلوكوفاج (Glucophage)", "1000 مجم", "قرص بعد الغداء يومياً", "1 tab daily after lunch"),
        ),
        "orthopedics" to listOf(
            SuggestedSpecialtyMed("sug_cataflam", "كتافلام (Cataflam)", "50 مجم", "قرص بعد الأكل كل 12 ساعة", "1 tab every 12h after meals"),
            SuggestedSpecialtyMed("sug_dimra", "ديمرا باسط عضلات (Dimra)", "50/500 مجم", "قرص كل 8 ساعات بعد الأكل", "1 tab every 8h after meals"),
            SuggestedSpecialtyMed("sug_osteocare", "أوستيوكير كالسيوم (Osteocare)", "400 مجم", "قرص بعد الغداء يومياً", "1 tab daily after lunch"),
            SuggestedSpecialtyMed("sug_voltaren_gel", "فولتارين جل (Voltaren Gel)", "1%", "دهان موضعي مرتين يومياً", "Apply topically twice daily"),
        ),
        "cardiology" to listOf(
            SuggestedSpecialtyMed("sug_concor", "كونكور (Concor)", "5 مجم", "قرص صباحاً يومياً", "1 tab daily in the morning"),
            SuggestedSpecialtyMed("sug_aspocid", "أسبوسيد (Aspocid)", "75 مجم", "قرص بعد الغداء يومياً", "1 tab daily after lunch"),
            SuggestedSpecialtyMed("sug_lipitor", "ليبيتور (Lipitor)", "20 مجم", "قرص مساءً يومياً", "1 tab nightly"),
        ),
        "ent" to listOf(
            SuggestedSpecialtyMed("sug_tavanic", "تافانيك (Tavanic)", "500 مجم", "قرص يومياً لمدة 7 أيام", "1 tab daily for 7 days"),
            SuggestedSpecialtyMed("sug_clarinase", "كلارينيز (Clarinase)", "5/120 مجم", "قرص كل 12 ساعة لمدة 5 أيام", "1 tab every 12h for 5 days"),
            SuggestedSpecialtyMed("sug_nasonex", "نازونكس بخاخ (Nasonex)", "50 مكجم", "بختان في كل فتحة أنف يومياً", "2 sprays each nostril daily"),
        ),
        "dermatology" to listOf(
            SuggestedSpecialtyMed("sug_fusiderm", "فيوسيدرم كريم (Fusiderm)", "2%", "دهان موضعي 3 مرات يومياً", "Apply topically TID"),
            SuggestedSpecialtyMed("sug_betaderm", "بيتاديرم كريم (Betaderm)", "0.1%", "دهان موضعي مرتين يومياً", "Apply topically BID"),
            SuggestedSpecialtyMed("sug_telfast", "تلفاست (Telfast)", "180 مجم", "قرص مساءً يومياً", "1 tab nightly"),
        ),
        "ophthalmology" to listOf(
            SuggestedSpecialtyMed("sug_tobradex", "توبراديكس قطرة (Tobradex)", "0.3%/0.1%", "قطرة 3 مرات يومياً", "1 drop TID"),
            SuggestedSpecialtyMed("sug_systane", "سيستان ألترا (Systane Ultra)", "قطرة مرطبة", "قطرة كل 6 ساعات", "1 drop every 6 hours"),
            SuggestedSpecialtyMed("sug_vigamox", "فيجاموكس قطرة (Vigamox)", "0.5%", "قطرة 3 مرات يومياً لمدة 7 أيام", "1 drop TID for 7 days"),
        ),
        "eye_care" to listOf(
            SuggestedSpecialtyMed("sug_tobradex_e", "توبراديكس قطرة (Tobradex)", "0.3%/0.1%", "قطرة 3 مرات يومياً", "1 drop TID"),
            SuggestedSpecialtyMed("sug_systane_e", "سيستان ألترا (Systane Ultra)", "قطرة مرطبة", "قطرة كل 6 ساعات", "1 drop every 6 hours"),
        ),
        "dentistry" to listOf(
            SuggestedSpecialtyMed("sug_aug_dent", "أوجمنتين (Augmentin)", "1 جم", "قرص كل 12 ساعة لمدة 7 أيام", "1 tab every 12h for 7 days"),
            SuggestedSpecialtyMed("sug_flagyl", "فلاجيل (Flagyl)", "500 مجم", "قرص كل 8 ساعات لمدة 5 أيام", "1 tab every 8h for 5 days"),
            SuggestedSpecialtyMed("sug_catafast", "كتافاست فوار (Catafast)", "50 مجم", "كيس على نصف كوب ماء عند اللزوم", "1 sachet in water as needed"),
            SuggestedSpecialtyMed("sug_hexitol", "هيكسيتول مضمضة (Hexitol)", "0.125%", "مضمضة مرتين يومياً", "Rinse twice daily"),
        ),
    )

    private val defaultDiagnoses = listOf(
        SuggestedDiagnosisItem("كشف عام ومتابعة حالة", "General Consultation & Follow-up"),
        SuggestedDiagnosisItem("ارتفاع درجة الحرارة والتهاب حاد", "Acute Febrile Infection"),
        SuggestedDiagnosisItem("متابعة نتائج التحاليل والأشعة", "Lab & Radiology Review"),
    )

    private val defaultMeds = listOf(
        SuggestedSpecialtyMed("sug_gen_panadol", "بنادول / باراسيتامول (Paracetamol)", "500 مجم", "قرص كل 8 ساعات عند اللزوم", "1 tab every 8h as needed"),
        SuggestedSpecialtyMed("sug_gen_aug", "أوجمنتين (Augmentin)", "1 جم", "قرص كل 12 ساعة لمدة 7 أيام", "1 tab every 12h for 7 days"),
        SuggestedSpecialtyMed("sug_gen_ctrl", "كونترولوك (Controloc)", "40 مجم", "قرص قبل الفطار بنصف ساعة", "1 tab 30 min before breakfast"),
    )

    fun diagnosesForDoctor(doctor: ClinicDoctorEntity): List<SuggestedDiagnosisItem> {
        val list = mutableListOf<SuggestedDiagnosisItem>()
        SpecialtyCatalog.allDoctorSpecialties(doctor.specialty, doctor.additionalSpecialties).forEach { rawSpec ->
            val specItem = SpecialtyCatalog.findByNameOrAlias(rawSpec)
            val matched = specItem?.let { diagnosesBySpecialtyId[it.id] }
            if (matched != null) {
                matched.forEach { d ->
                    if (list.none { it.ar == d.ar }) list += d
                }
            }
        }
        return if (list.isNotEmpty()) list else defaultDiagnoses
    }

    fun medicationsForDoctor(doctor: ClinicDoctorEntity): List<SuggestedSpecialtyMed> {
        val list = mutableListOf<SuggestedSpecialtyMed>()
        SpecialtyCatalog.allDoctorSpecialties(doctor.specialty, doctor.additionalSpecialties).forEach { rawSpec ->
            val specItem = SpecialtyCatalog.findByNameOrAlias(rawSpec)
            val matched = specItem?.let { medsBySpecialtyId[it.id] }
            if (matched != null) {
                matched.forEach { m ->
                    if (list.none { it.id == m.id }) list += m
                }
            }
        }
        return if (list.isNotEmpty()) list else defaultMeds
    }
}
