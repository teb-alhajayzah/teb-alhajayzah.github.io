package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.util.ChronicConditionsCatalog
import com.example.util.ImageStorageHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class GeminiMedicalSummaryResult(
    val summaryAr: String,
    val summaryEn: String,
)

class GeminiMedicalAnalyzer {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        "generalStatusAr" to Schema.string(
                            "Part 1 in simple patient-friendly Arabic ONLY (الحالة العامة والقيم الطبيعية): Write 100% pure Arabic with ZERO English words or medical abbreviations (never write Absent, Nil, Ref, Pus cells, RBCs, Oxalate, Hemoglobin, Hb, etc.). Describe normal/negative values calmly as 'سلبية / طبيعية ولا يوجد بها أي مشكلة' and ignore all dates, reporting/request IDs, ages, genders, and barcode numbers.",
                        ),
                        "followUpPointsAr" to Schema.string(
                            "Part 2 in simple patient-friendly Arabic ONLY (نقاط تحتاج لمتابعة أو انتباه الطبيب): Numbered list formatted as 1. ...\\n2. ...\\n3. ... with ZERO English words, units, or abbreviations. Use reassuring simple Arabic (e.g. 'وجود نسبة بسيطة من خلايا الصديد في البول، وتحتاج للمتابعة مع الطبيب لوصف العلاج المناسب', 'وجود نسبة أملاح (أوكسالات) في البول، ويُنصح بشرب كميات كافية من الماء ومراجعة الطبيب', 'نسبة الهيموجلوبين (الأنيميا) أقل من المعدل الطبيعي وتحتاج لمتابعة الطبيب'). Ignore all dates/IDs/barcodes. Never diagnose definitively or prescribe drugs.",
                        ),
                        "generalStatusEn" to Schema.string(
                            "Part 1 in simple patient-friendly English (Overall Status & Normal Values): State clearly whether most indicators are within normal limits and ignore all dates, request/reporting IDs, barcodes, age/gender lines.",
                        ),
                        "followUpPointsEn" to Schema.string(
                            "Part 2 in English (Points Requiring Physician Follow-Up): Numbered list ONLY formatted as 1. ...\\n2. ...\\n3. ... highlighting abnormal values, pus cells, RBCs, crystals, or chronic-condition/allergy alerts WITHOUT definitive diagnosis or prescribing medications.",
                        ),
                    ),
                )
            },
        )
    }

    suspend fun analyzeMemberHistoryAndFiles(
        member: FamilyMemberEntity,
        records: List<MedicalRecordEntity>,
        focusRecordId: String? = null,
    ): Result<GeminiMedicalSummaryResult> = withContext(Dispatchers.IO) {
        try {
            val promptText = buildClinicalPrompt(member, records)

            // Prioritize focused record first, then latest records that have readable PDF/image files under 18MB.
            val orderedRecords = buildList {
                if (!focusRecordId.isNullOrBlank()) {
                    records.firstOrNull { it.id == focusRecordId }?.let { add(it) }
                }
                records.sortedByDescending { it.createdAtMillis }.forEach { rec ->
                    if (none { it.id == rec.id }) add(rec)
                }
            }

            val inlineFiles = mutableListOf<Triple<ByteArray, String, String>>()
            for (rec in orderedRecords) {
                if (inlineFiles.size >= 2) break
                val path = rec.attachmentPath
                if (path.isBlank()) continue
                val file = File(path)
                if (!file.exists() || !file.isFile) continue
                val sizeBytes = file.length()
                if (sizeBytes <= 0L || sizeBytes > MAX_INLINE_FILE_BYTES) continue
                val resolvedMime = when {
                    ImageStorageHelper.isPdfAttachment(path, rec.attachmentMimeType) -> "application/pdf"
                    rec.attachmentMimeType.startsWith("image/", ignoreCase = true) -> rec.attachmentMimeType
                    path.lowercase().endsWith(".png") -> "image/png"
                    else -> "image/jpeg"
                }
                val bytes = runCatching { file.readBytes() }.getOrNull() ?: continue
                inlineFiles += Triple(bytes, resolvedMime, rec.attachmentName.ifBlank { file.name })
            }

            val inputContent = content {
                text(promptText)
                inlineFiles.forEach { (bytes, mime, name) ->
                    text("Attached medical document ($name) — ignore lab header/date/patient metadata and interpret ONLY the clinical test values & reference ranges:")
                    inlineData(bytes, mime)
                }
            }

            val response = withTimeout(60_000L) {
                model.generateContent(inputContent)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("لم يُرجع نموذج التحليل الطبي استجابة نصية صالحة"),
                )
            }
            Result.success(parseStructuredResponse(rawJson))
        } catch (ce: CancellationException) {
            throw ce
        } catch (te: TimeoutCancellationException) {
            Log.e(TAG, "Gemini medical analysis timed out", te)
            Result.failure(IllegalStateException("انتهت مهلة تحليل الملف الطبي"))
        } catch (ex: Exception) {
            Log.e(TAG, "Gemini medical analysis failed", ex)
            Result.failure(
                IllegalStateException(
                    ex.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "تعذر تحليل الملف الطبي عبر الذكاء الاصطناعي",
                ),
            )
        }
    }

    /**
     * نقطة تكامل مباشرة لواجهة الويب داخل تطبيق الأندرويد (AndroidAiBridge):
     * تستقبل بيانات المريض الحيوية، الأمراض المزمنة، النصوص المستخرجة من PDF، والمرفقات (PDF / صور)
     * وترسلها إلى Firebase AI Logic (Gemini) لتعود بملخص منسق في جزأين بالعربية والإنجليزية.
     */
    suspend fun analyzeWebPayloadJson(payloadJson: String): Result<GeminiMedicalSummaryResult> =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(payloadJson)
                val memberObj = root.optJSONObject("member") ?: JSONObject()
                val fullName = memberObj.optString("fullName").ifBlank { "المريض" }
                val age = memberObj.optInt("age", 0)
                val weightKg = memberObj.optDouble("weightKg", 0.0)
                val heightCm = memberObj.optDouble("heightCm", 0.0)
                val bloodType = memberObj.optString("bloodType").trim()
                val chronicsRaw = memberObj.opt("chronicConditions")
                val chronicKeys = when (chronicsRaw) {
                    is JSONArray -> (0 until chronicsRaw.length()).mapNotNull {
                        chronicsRaw.optString(it).takeIf(String::isNotBlank)
                    }
                    is String -> ChronicConditionsCatalog.parseKeys(chronicsRaw)
                    else -> emptyList()
                }
                val otherChronicNotes = memberObj.optString("otherChronicNotes").trim()
                val drugAllergies = memberObj.optString("drugAllergies").trim()
                val chronicLabels = ChronicConditionsCatalog.formatLabels(
                    rawKeys = ChronicConditionsCatalog.encodeKeys(chronicKeys),
                    otherNotes = otherChronicNotes,
                    isEn = false,
                )

                val vitals = buildList {
                    add("الاسم: $fullName")
                    if (age > 0) add("السن: $age سنة")
                    if (weightKg > 0.0 && !weightKg.isNaN()) add("الوزن: ${weightKg.toInt()} كجم")
                    if (heightCm > 0.0 && !heightCm.isNaN()) add("الطول: ${heightCm.toInt()} سم")
                    if (bloodType.isNotBlank()) add("فصيلة الدم: $bloodType")
                }.joinToString(" | ")

                val recordsArr = root.optJSONArray("records") ?: JSONArray()
                val focusId = root.optString("focusRecordId").trim()
                val recordsList = (0 until recordsArr.length()).mapNotNull { recordsArr.optJSONObject(it) }
                val orderedRecords = buildList {
                    if (focusId.isNotBlank()) {
                        recordsList.firstOrNull { it.optString("id") == focusId }?.let { add(it) }
                    }
                    recordsList.sortedByDescending { it.optLong("createdAtMillis", 0L) }.forEach { obj ->
                        if (none { it.optString("id") == obj.optString("id") }) add(obj)
                    }
                }

                val historyBlock = if (orderedRecords.isEmpty()) {
                    "لا توجد زيارات أو تحاليل مسجلة حتى الآن."
                } else {
                    orderedRecords.take(8).joinToString("\n\n") { rec ->
                        val extracted = com.example.util.MedicalSummaryAnalyzer.stripMetadataLines(
                            rec.optString("extractedText").trim().take(4000),
                        )
                        val meds = rec.optString("medications").trim().replace("\n", "; ")
                        val notes = com.example.util.MedicalSummaryAnalyzer.stripMetadataLines(rec.optString("notes").trim())
                        val attName = rec.optString("attachmentName").trim()
                        buildString {
                            append("- [${rec.optString("recordType")}] ${rec.optString("doctorName")} | العنوان/التشخيص: ${rec.optString("diagnosis")}")
                            if (meds.isNotBlank()) append(" | الأدوية: $meds")
                            if (notes.isNotBlank()) append(" | ملاحظات: $notes")
                            if (attName.isNotBlank()) append(" | مرفق: $attName")
                            if (extracted.isNotBlank()) {
                                append("\n  النص المستخرج من التقرير:\n  $extracted")
                            }
                        }
                    }
                }

                val inlineFiles = mutableListOf<Triple<ByteArray, String, String>>()
                for (rec in orderedRecords) {
                    if (inlineFiles.size >= 2) break
                    val dataUrl = rec.optString("attachmentDataUrl").trim()
                    if (dataUrl.isBlank()) continue
                    val commaIdx = dataUrl.indexOf(',')
                    val pureB64 = if (commaIdx >= 0) dataUrl.substring(commaIdx + 1) else dataUrl
                    val mimeFromPrefix = if (commaIdx > 5) {
                        dataUrl.substring(5, commaIdx).substringBefore(';').lowercase()
                    } else ""
                    val rawMime = rec.optString("attachmentMimeType").trim().lowercase()
                    val fileName = rec.optString("attachmentName").ifBlank { "medical_report" }
                    val resolvedMime = when {
                        mimeFromPrefix == "application/pdf" || rawMime == "application/pdf" || fileName.lowercase().endsWith(".pdf") -> "application/pdf"
                        mimeFromPrefix.startsWith("image/") -> mimeFromPrefix
                        rawMime.startsWith("image/") -> rawMime
                        fileName.lowercase().endsWith(".png") -> "image/png"
                        else -> "image/jpeg"
                    }
                    val bytes = runCatching { Base64.decode(pureB64, Base64.DEFAULT) }.getOrNull() ?: continue
                    if (bytes.isEmpty() || bytes.size > MAX_INLINE_FILE_BYTES) continue
                    inlineFiles += Triple(bytes, resolvedMime, fileName)
                }

                val promptText = buildClinicalInstructionHeader(
                    vitals = vitals,
                    chronicLabels = chronicLabels,
                    drugAllergies = drugAllergies,
                    historyBlock = historyBlock,
                )

                val inputContent = content {
                    text(promptText)
                    inlineFiles.forEach { (bytes, mime, name) ->
                        text("Attached medical report ($name) — extract actual clinical parameter values and compare against reference ranges:")
                        inlineData(bytes, mime)
                    }
                }

                val response = withTimeout(60_000L) {
                    model.generateContent(inputContent)
                }
                val rawJson = response.text?.trim().orEmpty()
                if (rawJson.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Empty Gemini response"))
                }
                Result.success(parseStructuredResponse(rawJson))
            } catch (ce: CancellationException) {
                throw ce
            } catch (ex: Exception) {
                Log.w(TAG, "Gemini web payload analysis failed", ex)
                Result.failure(ex)
            }
        }

    private fun parseStructuredResponse(rawJson: String): GeminiMedicalSummaryResult {
        val cleaned = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val obj = JSONObject(cleaned)

        val generalAr = sanitizeArabicPlainLanguage(obj.optString("generalStatusAr").trim())
        val rawFollowUpAr = sanitizeArabicPlainLanguage(obj.optString("followUpPointsAr").trim())
        val generalEn = obj.optString("generalStatusEn").trim()
        val rawFollowUpEn = obj.optString("followUpPointsEn").trim()

        val followUpAr = ensureNumberedPoints(rawFollowUpAr)
        val followUpEn = ensureNumberedPoints(rawFollowUpEn)

        val fullAr = buildString {
            if (generalAr.isNotBlank()) {
                append("✅ الحالة العامة والقيم الطبيعية:\n")
                append(generalAr)
            }
            if (followUpAr.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("📌 نقاط تحتاج لمتابعة أو انتباه الطبيب:\n")
                append(followUpAr)
            }
        }.trim()

        val fullEn = buildString {
            if (generalEn.isNotBlank()) {
                append("✅ Overall Status & Normal Values:\n")
                append(generalEn)
            }
            if (followUpEn.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("📌 Points Requiring Physician Follow-Up:\n")
                append(followUpEn)
            }
        }.trim()

        return GeminiMedicalSummaryResult(
            summaryAr = fullAr.ifBlank { fullEn },
            summaryEn = fullEn.ifBlank { fullAr },
        )
    }

    private fun ensureNumberedPoints(raw: String): String {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return ""
        return lines.mapIndexed { idx, line ->
            val stripped = line
                .replace(Regex("^(?:\\d+[.)\\-:]|[•\\-*📌⚠️])\\s*"), "")
                .trim()
            "${idx + 1}. $stripped"
        }.joinToString("\n")
    }

    private fun buildClinicalPrompt(
        member: FamilyMemberEntity,
        records: List<MedicalRecordEntity>,
    ): String {
        val chronicList = ChronicConditionsCatalog.formatLabels(
            rawKeys = member.chronicConditions,
            otherNotes = member.otherChronicNotes,
            isEn = false,
        )
        val vitals = buildList {
            add("الاسم: ${member.fullName}")
            add("السن: ${member.age} سنة")
            if (member.weightKg > 0.0) add("الوزن: ${member.weightKg.toInt()} كجم")
            if (member.heightCm > 0.0) add("الطول: ${member.heightCm.toInt()} سم")
            if (member.bloodType.isNotBlank()) add("فصيلة الدم: ${member.bloodType}")
        }.joinToString(" | ")

        val historyBlock = if (records.isEmpty()) {
            "لا توجد زيارات أو تحاليل مسجلة حتى الآن."
        } else {
            records.sortedByDescending { it.createdAtMillis }
                .take(8)
                .joinToString("\n") { rec ->
                    "- [${rec.recordType}] ${rec.visitDateLabel} | ${rec.doctorName} | التشخيص/العنوان: ${rec.diagnosis}" +
                        (if (rec.medications.isNotBlank()) " | الأدوية: ${rec.medications.replace("\n", "; ")}" else "") +
                        (if (rec.notes.isNotBlank()) " | ملاحظات: ${rec.notes}" else "") +
                        (if (rec.attachmentName.isNotBlank()) " | مرفق: ${rec.attachmentName}" else "")
                }
        }

        return buildClinicalInstructionHeader(
            vitals = vitals,
            chronicLabels = chronicList,
            drugAllergies = member.drugAllergies,
            historyBlock = historyBlock,
        )
    }

    private fun sanitizeArabicPlainLanguage(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace(Regex("\\b(?:Ca\\.?\\s*Oxalate|Calcium\\s*Oxalate)\\b", RegexOption.IGNORE_CASE), "أملاح (أوكسالات)")
            .replace(Regex("\\bOxalate\\b", RegexOption.IGNORE_CASE), "أوكسالات")
            .replace(Regex("\\bPus\\s*cells?\\b", RegexOption.IGNORE_CASE), "خلايا الصديد")
            .replace(Regex("\\bR\\.?B\\.?C\\.?s?\\b", RegexOption.IGNORE_CASE), "كرات الدم الحمراء")
            .replace(Regex("\\b(?:Hemoglobin|Haemoglobin|Hb|Hgb)\\b", RegexOption.IGNORE_CASE), "الهيموجلوبين")
            .replace(Regex("\\bHbA1c\\b", RegexOption.IGNORE_CASE), "السكر التراكمي")
            .replace(Regex("\\b(?:Absent|Nil|Negative)\\b", RegexOption.IGNORE_CASE), "سلبية / طبيعية")
            .replace(Regex("\\bRef\\.?\\s*(?:Range)?\\b", RegexOption.IGNORE_CASE), "المعدل الطبيعي")
            .replace(Regex("/HPF\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(?:mg/dL|g/dL|mmol/L|U/L)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun buildClinicalInstructionHeader(
        vitals: String,
        chronicLabels: String,
        drugAllergies: String,
        historyBlock: String,
    ): String {
        return """
            أنت مساعد طبي ذكي لمنصة "طب الحجايزة" متخصص في تفسير تقارير التحاليل والأشعة للمريض بلغة عربية بسيطة ومطمئنة جداً.
            المطلوب قراءة البيانات الحيوية للمريض (السن والأمراض المزمنة وحساسية الأدوية) وجميع القيم السريرية الفعلية الموجودة في النص المستخرج أو ملفات الـ PDF/الصور المرفقة وتلخيصها للمريض في نقطتين رئيسيتين: الحالة العامة، ونقاط المتابعة المرقمة (1، 2، 3).

            قواعد صارمة لا تقبل الاستثناء:
            1. تجاهل تماماً أي أرقام أو سطور مرتبطة بالتواريخ أو الأكواد الإدارية أو الترويسة مثل: (Date, Reporting Date, Request, Age, Gender, Sex, ID, Barcode, Specimen, Accession, 2026) واسم المعمل أو رقم الهاتف أو العنوان، وممنوع نهائياً اعتبار أي تاريخ أو رقم باركود نتيجة طبية مرتفعة أو منخفضة.
            2. اللغة العربية المبسطة والمطمئنة فقط في الحقول العربية (generalStatusAr و followUpPointsAr):
               - ممنوع منعاً باتاً كتابة أي كلمات أو مصطلحات إنجليزية أو اختصارات لاتينية للمريض في الملخص العربي (ممنوع كتابة: Absent أو Nil أو Ref أو Pus cells أو RBCs أو Oxalate أو Hemoglobin أو Hb أو HbA1c أو mg/dL).
               - التزم بهذا الأسلوب المبسط والمطمئن:
                 * إذا كانت خلايا الصديد مرتفعة: اكتب "وجود نسبة بسيطة من خلايا الصديد في البول، وتحتاج للمتابعة مع الطبيب لوصف العلاج المناسب".
                 * إذا وجدت أملاح أوكسالات: اكتب "وجود نسبة أملاح (أوكسالات) في البول، ويُنصح بشرب كميات كافية من الماء ومراجعة الطبيب".
                 * إذا كانت النتيجة سلبية أو غائبة: اكتب "سلبية / طبيعية ولا يوجد بها أي مشكلة".
                 * إذا كان الهيموجلوبين منخفضاً: اكتب "نسبة الهيموجلوبين (الأنيميا) أقل من المعدل الطبيعي وتحتاج لمتابعة الطبيب".
            3. قسّم الملخص دائماً إلى جزأين واضحين داخل JSON:
               - الجزء الأول (generalStatusAr / generalStatusEn): الحالة العامة والقيم الطبيعية — اذكر بوضوح القيم الطبيعية والسلبية بأسلوب مطمئن.
               - الجزء الثاني (followUpPointsAr / followUpPointsEn): نقاط تحتاج لمتابعة أو انتباه الطبيب — في نقاط مرقمة إلزامياً:
                 1. ...
                 2. ...
                 3. ...
                 مع ربطها بالأمراض المزمنة وحساسية الأدوية المسجلة.
            4. ممنوع إعطاء تشخيص مرضي قاطع أو وصف أي دواء أو جرعة علاجية؛ اكتفِ بالشرح التبسيطي المريح والتوجيه لمراجعة الطبيب المختص.
            5. إذا كانت جميع القيم طبيعية ولا توجد أمراض مزمنة حرجة، اذكر ذلك بوضوح في الجزء الأول واكتب في الجزء الثاني: "1. جميع المؤشرات المقروءة ضمن النطاق الطبيعي المعتاد، ويُنصح بعرض أصل التقرير على الطبيب في المتابعة الدورية."
            6. لا تخترع أي رقم غير موجود في النص أو المرفق.

            البيانات الحيوية للمريض: $vitals
            الأمراض المزمنة: ${chronicLabels.ifBlank { "لا توجد أمراض مزمنة مسجلة" }}
            حساسية الأدوية: ${drugAllergies.ifBlank { "لا توجد حساسية أدوية مسجلة" }}

            السجل الطبي والنصوص المستخرجة:
            $historyBlock
        """.trimIndent()
    }

    companion object {
        private const val TAG = "GeminiMedicalAnalyzer"
        private const val MAX_INLINE_FILE_BYTES = 18 * 1024 * 1024L
    }
}
