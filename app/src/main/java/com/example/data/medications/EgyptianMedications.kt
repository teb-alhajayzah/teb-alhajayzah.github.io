package com.example.data.medications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EgyptianMedication(
    val id: String,
    val tradeName: String,
    val activeIngredient: String,
    val strength: String,
    val specialty: String,
    val form: String,
    val defaultDosage: String,
)

object MedicationCatalog {

    val specialtiesOrder = listOf(
        "باطنة وجهاز هضمي",
        "أطفال وحديثي الولادة",
        "عظام ومفاصل",
        "نساء وتوليد",
        "أسنان",
        "قلب وأوعية دموية",
        "رمد وجراحة عيون",
        "أنف وأذن وحنجرة",
        "جلدية وتجميل",
    )

    val dosagePresets = listOf(
        "قرص كل 12 ساعة",
        "قرص كل 8 ساعات",
        "قرص صباحاً يومياً",
        "قبل الفطار بنصف ساعة",
        "بعد الأكل مباشرة",
        "5 سم كل 12 ساعة",
        "5 سم كل 8 ساعات",
        "كيس على نصف كوب ماء عند اللزوم",
        "قطرة 3 مرات يومياً",
        "دهان موضعي مرتين يومياً",
        "لمدة 5 أيام",
        "لمدة 7 أيام",
        "لمدة أسبوعين",
    )

    val builtInMedications: List<EgyptianMedication> = listOf(
        // باطنة وجهاز هضمي
        EgyptianMedication("med_controloc", "كونترولوك (Controloc)", "بانتوبرازول / Pantoprazole", "40 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص قبل الفطار بنصف ساعة"),
        EgyptianMedication("med_nexium", "نيكسيوم (Nexium)", "إيسوميبرازول / Esomeprazole", "40 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص قبل الفطار بنصف ساعة"),
        EgyptianMedication("med_gaviscon", "جافيسكون (Gaviscon)", "ألجينات الصوديوم", "شراب معلق", "باطنة وجهاز هضمي", "شراب", "ملعقة كبيرة بعد الأكل وعند اللزوم"),
        EgyptianMedication("med_motilium", "موتيليوم (Motilium)", "دومبيريدون / Domperidone", "10 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص قبل الأكل بنصف ساعة 3 مرات"),
        EgyptianMedication("med_antinal", "أنتينال (Antinal)", "نيفوروكسازيد / Nifuroxazide", "200 مجم", "باطنة وجهاز هضمي", "كبسول", "كبسولة كل 6 ساعات لمدة 5 أيام"),
        EgyptianMedication("med_colospasmin", "كولوسبازمين فورت", "ميبيفيرين / Mebeverine", "135 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص قبل الأكل بنصف ساعة 3 مرات"),
        EgyptianMedication("med_glucophage", "جلوكوفاج (Glucophage)", "ميتفورمين / Metformin", "1000 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص بعد وجبة الغداء يومياً"),
        EgyptianMedication("med_amaryl", "أماريل (Amaryl)", "جليميبرايد / Glimepiride", "2 مجم", "باطنة وجهاز هضمي", "أقراص", "قرص قبل الفطار يومياً"),

        // أطفال وحديثي الولادة
        EgyptianMedication("med_augmentin_ped", "أوجمنتين شراب (Augmentin)", "أموكسيسيلين + كلافولانيك", "457 مجم/5 مل", "أطفال وحديثي الولادة", "شراب", "5 سم كل 12 ساعة لمدة 7 أيام"),
        EgyptianMedication("med_hibiotic_ped", "هايبوتيك شراب (Hibiotic)", "أموكسيسيلين + كلافولانيك", "457 مجم/5 مل", "أطفال وحديثي الولادة", "شراب", "5 سم كل 12 ساعة لمدة 7 أيام"),
        EgyptianMedication("med_brufen_ped", "بروفين شراب (Brufen)", "إيبوبروفين / Ibuprofen", "100 مجم/5 مل", "أطفال وحديثي الولادة", "شراب", "5 سم كل 8 ساعات بعد الأكل لخفض الحرارة"),
        EgyptianMedication("med_cetal_ped", "سيتال شراب (Cetal)", "باراسيتامول / Paracetamol", "250 مجم/5 مل", "أطفال وحديثي الولادة", "شراب", "5 سم كل 6 ساعات عند اللزوم"),
        EgyptianMedication("med_oplex_n", "أوبلكس إن (Oplex-N)", "أوكسوميمازين + جوافينيزين", "شراب مهدئ للسعال", "أطفال وحديثي الولادة", "شراب", "5 سم 3 مرات يومياً"),
        EgyptianMedication("med_zyrtec_drops", "زيرتك نقط (Zyrtec)", "سيتريزين / Cetirizine", "10 مجم/مل", "أطفال وحديثي الولادة", "نقط فم", "5 نقط مساءً يومياً"),
        EgyptianMedication("med_farcolin", "فاركولين محلول (Farcolin)", "سالبوتامول / Salbutamol", "5 مجم/مل", "أطفال وحديثي الولادة", "جلسات بخار", "جلسة بخار صباحاً ومساءً"),

        // عظام ومفاصل
        EgyptianMedication("med_cataflam", "كتافلام (Cataflam)", "ديكلوفيناك بوتاسيوم", "50 مجم", "عظام ومفاصل", "أقراص", "قرص بعد الأكل كل 12 ساعة لمدة 5 أيام"),
        EgyptianMedication("med_dimra", "ديمرا (Dimra)", "ديكلوفيناك + ميثوكاربامول", "50/500 مجم", "عظام ومفاصل", "أقراص", "قرص كل 8 ساعات بعد الأكل"),
        EgyptianMedication("med_myolgin", "ميولجين (Myolgin)", "باراسيتامول + كلورزوكسازون", "300/250 مجم", "عظام ومفاصل", "كبسول", "كبسولة كل 8 ساعات بعد الأكل"),
        EgyptianMedication("med_alphintern", "ألفينترن (Alphintern)", "كيموتربسين + تربسين", "300 وحدة", "عظام ومفاصل", "أقراص", "قرص قبل الأكل بساعة 3 مرات يومياً"),
        EgyptianMedication("med_osteocare", "أوستيوكير (Osteocare)", "كالسيوم + ماغنيسيوم + د3", "400 مجم", "عظام ومفاصل", "أقراص", "قرص بعد الغداء يومياً"),
        EgyptianMedication("med_vidrop", "فيدروب / بون وان (One-Alpha)", "فيتامين د3 / كوليكالسيفيرول", "2800 وحدة", "عظام ومفاصل", "نقط / كبسول", "مرة واحدة يومياً بعد وجبة دسمة"),
        EgyptianMedication("med_voltaren_gel", "فولتارين جل (Voltaren Emulgel)", "ديكلوفيناك صوديوم", "1%", "عظام ومفاصل", "جل موضعي", "دهان موضعي مرتين يومياً على موضع الألم"),

        // نساء وتوليد
        EgyptianMedication("med_folic_acid", "فوليك أسيد (Folic Acid)", "حمض الفوليك", "5 مجم", "نساء وتوليد", "أقراص", "قرص صباحاً يومياً"),
        EgyptianMedication("med_ferrotron", "فيروترون (Ferrotron)", "حديد + فيتامينات ومعادن", "كبسولات حديد", "نساء وتوليد", "كبسول", "كبسولة بعد الغداء بساعتين يومياً"),
        EgyptianMedication("med_duphaston", "دوفاستون (Duphaston)", "ديدروجستيرون / Dydrogesterone", "10 مجم", "نساء وتوليد", "أقراص", "قرص كل 12 ساعة حسب الإرشادات"),
        EgyptianMedication("med_utrogestan", "يوتروجيستان (Utrogestan)", "بروجستيرون طبيعي", "200 مجم", "نساء وتوليد", "كبسول", "كبسولة مساءً يومياً"),
        EgyptianMedication("med_pregnacare", "بريجناكير (Pregnacare)", "مالتي فيتامين للحمل", "كبسول متكامل", "نساء وتوليد", "كبسول", "كبسولة بعد الوجبة الرئيسية يومياً"),
        EgyptianMedication("med_gyno_daktarin", "جينو دكتارين (Gyno-Daktarin)", "ميكونازول نترات", "400 مجم", "نساء وتوليد", "لبوس مهبلي", "مرة واحدة مساءً لمدة 3 أيام"),

        // أسنان
        EgyptianMedication("med_augmentin_1g", "أوجمنتين (Augmentin)", "أموكسيسيلين + كلافولانيك", "1 جم", "أسنان", "أقراص", "قرص كل 12 ساعة لمدة 7 أيام"),
        EgyptianMedication("med_flagyl_500", "فلاجيل (Flagyl)", "ميترونيدازول / Metronidazole", "500 مجم", "أسنان", "أقراص", "قرص كل 8 ساعات بعد الأكل لمدة 5 أيام"),
        EgyptianMedication("med_catafast", "كتافاست فوار (Catafast)", "ديكلوفيناك بوتاسيوم", "50 مجم", "أسنان", "أكياس فوارة", "كيس على نصف كوب ماء عند اللزوم"),
        EgyptianMedication("med_dalacin_c", "دالاسين سي (Dalacin C)", "كليندامايسين / Clindamycin", "300 مجم", "أسنان", "كبسول", "كبسولة كل 8 ساعات لمدة 5 أيام"),
        EgyptianMedication("med_brufen_600", "بروفين (Brufen)", "إيبوبروفين / Ibuprofen", "600 مجم", "أسنان", "أقراص", "قرص كل 8 ساعات بعد الأكل عند اللزوم"),
        EgyptianMedication("med_hexitol", "هيكسيتول مضمضة (Hexitol)", "كلورهيكسيدين / Chlorhexidine", "0.125%", "أسنان", "مضمضة فم", "مضمضة مرتين يومياً بعد غسل الأسنان"),

        // قلب وأوعية دموية
        EgyptianMedication("med_concor_5", "كونكور (Concor)", "بيسوبرولول / Bisoprolol", "5 مجم", "قلب وأوعية دموية", "أقراص", "قرص صباحاً يومياً"),
        EgyptianMedication("med_aspocid_75", "أسبوسيد (Aspocid)", "أسبرين أطفال / Acetylsalicylic", "75 مجم", "قلب وأوعية دموية", "أقراص", "قرص بعد الغداء يومياً"),
        EgyptianMedication("med_plavix_75", "بلافيكس (Plavix)", "كلوبيدوجريل / Clopidogrel", "75 مجم", "قلب وأوعية دموية", "أقراص", "قرص يومياً في نفس الموعد"),
        EgyptianMedication("med_lipitor_20", "ليبيتور (Lipitor)", "أتورفاستاتين / Atorvastatin", "20 مجم", "قلب وأوعية دموية", "أقراص", "قرص مساءً يومياً"),

        // رمد وجراحة عيون
        EgyptianMedication("med_tobradex", "توبراديكس قطرة (Tobradex)", "توبراميسين + ديكساميثازون", "0.3% / 0.1%", "رمد وجراحة عيون", "قطرة عين", "قطرة في العين المصابة 3 مرات يومياً"),
        EgyptianMedication("med_systane", "سيستان ألترا (Systane Ultra)", "مرطب للعين", "قطرة مرطبة", "رمد وجراحة عيون", "قطرة عين", "قطرة كل 6 ساعات لترطيب العين"),
        EgyptianMedication("med_vigamox", "فيجاموكس (Vigamox)", "موكسيفلوكساسين", "0.5%", "رمد وجراحة عيون", "قطرة عين", "قطرة 3 مرات يومياً لمدة 7 أيام"),

        // أنف وأذن وحنجرة
        EgyptianMedication("med_tavanic", "تافانيك (Tavanic)", "ليفوفلوكساسين / Levofloxacin", "500 مجم", "أنف وأذن وحنجرة", "أقراص", "قرص واحد يومياً لمدة 7 أيام"),
        EgyptianMedication("med_clarinase", "كلارينيز (Clarinase)", "لوراتادين + سودوإيفيدرين", "5/120 مجم", "أنف وأذن وحنجرة", "أقراص", "قرص كل 12 ساعة لمدة 5 أيام"),
        EgyptianMedication("med_nasonex", "نازونكس بخاخ (Nasonex)", "موميتازون فيورات", "50 مكجم", "أنف وأذن وحنجرة", "بخاخ أنف", "بختان في كل فتحة أنف مرة يومياً"),

        // جلدية وتجميل
        EgyptianMedication("med_fusiderm", "فيوسيدرم كريم (Fusiderm)", "حمض الفيوسيديك", "2%", "جلدية وتجميل", "كريم موضعي", "دهان موضعي 3 مرات يومياً"),
        EgyptianMedication("med_betaderm", "بيتاديرم كريم (Betaderm)", "بيتاميثازون فاليرات", "0.1%", "جلدية وتجميل", "كريم موضعي", "دهان موضعي مرتين يومياً لمدة 5 أيام"),
        EgyptianMedication("med_telfast_180", "تلفاست (Telfast)", "فيكسوفينادين / Fexofenadine", "180 مجم", "جلدية وتجميل", "أقراص", "قرص مساءً يومياً للحساسية"),
    )
}

data class MedicationFetchResult(
    val medications: List<EgyptianMedication>,
    val isLiveRemote: Boolean,
)

object EgyptianMedicationService {
    suspend fun fetchMedications(apiUrl: String): MedicationFetchResult = withContext(Dispatchers.IO) {
        val cleanUrl = apiUrl.trim()
        if (cleanUrl.isNotEmpty() && (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://"))) {
            try {
                val connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Accept", "application/json")
                }
                val code = connection.responseCode
                if (code in 200..299) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseMedicationsJson(body)
                    if (parsed.isNotEmpty()) {
                        return@withContext MedicationFetchResult(parsed, isLiveRemote = true)
                    }
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }
        MedicationFetchResult(MedicationCatalog.builtInMedications, isLiveRemote = false)
    }

    private fun parseMedicationsJson(raw: String): List<EgyptianMedication> {
        val list = mutableListOf<EgyptianMedication>()
        val trimmed = raw.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            obj.optJSONArray("medications") ?: obj.optJSONArray("data") ?: return emptyList()
        }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val trade = item.optString("tradeName").ifBlank { item.optString("name") }
            if (trade.isBlank()) continue
            list += EgyptianMedication(
                id = item.optString("id").ifBlank { "remote_$i" },
                tradeName = trade,
                activeIngredient = item.optString("activeIngredient").ifBlank { item.optString("generic", "مادة فعالة") },
                strength = item.optString("strength").ifBlank { item.optString("concentration", "") },
                specialty = item.optString("specialty").ifBlank { "باطنة وجهاز هضمي" },
                form = item.optString("form").ifBlank { "أقراص" },
                defaultDosage = item.optString("defaultDosage").ifBlank { "حسب إرشادات الطبيب" },
            )
        }
        return list
    }
}
