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
                            "Part 1 in Arabic (الحالة العامة والقيم الطبيعية): State clearly in simple patient-friendly Arabic whether most lab/clinical indicators are within normal range and summarize overall stability considering age and chronic conditions.",
                        ),
                        "followUpPointsAr" to Schema.string(
                            "Part 2 in Arabic (نقاط تحتاج لمتابعة أو انتباه الطبيب): Numbered list ONLY formatted as 1. ...\\n2. ...\\n3. ... highlighting abnormal/elevated/low lab values, pus cells, RBCs, crystals/salts, or chronic-condition/allergy alerts in simple Arabic WITHOUT giving a definitive diagnosis or prescribing any medication.",
                        ),
                        "generalStatusEn" to Schema.string(
                            "Part 1 in English (Overall Status & Normal Values): State clearly in simple English whether most indicators are within normal limits and summarize overall stability.",
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
                        val extracted = rec.optString("extractedText").trim().take(4000)
                        val meds = rec.optString("medications").trim().replace("\n", "; ")
                        val notes = rec.optString("notes").trim()
                        val attName = rec.optString("attachmentName").trim()
                        buildString {
                            append("- [${rec.optString("recordType")}] ${rec.optString("visitDateLabel")} | ${rec.optString("doctorName")} | العنوان/التشخيص: ${rec.optString("diagnosis")}")
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

        val generalAr = obj.optString("generalStatusAr").trim()
        val rawFollowUpAr = obj.optString("followUpPointsAr").trim()
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

    private fun buildClinicalInstructionHeader(
        vitals: String,
        chronicLabels: String,
        drugAllergies: String,
        historyBlock: String,
    ): String {
        return """
            أنت مساعد طبي ذكي لمنصة "طب الحجايزة" متخصص في تفسير تقارير التحاليل والأشعة والملف الطبي للمريض والطبيب.
            المطلوب قراءة البيانات الحيوية للمريض (السن والأمراض المزمنة وحساسية الأدوية) وجميع القيم الفعلية الموجودة في النص المستخرج أو ملفات الـ PDF/الصور المرفقة وتلخيصها للمريض بلغة عربية بسيطة ومنظمة (وبالإنجليزية في الحقول الإنجليزية).

            قواعد صارمة يجب الالتزام بها:
            1. تجاهل تماماً بيانات الترويسة مثل اسم المعمل أو تاريخ الطباعة أو رقم العينة أو العنوان ولا تذكرها كـ "نتائج تحليل".
            2. قسّم الملخص دائماً إلى جزأين واضحين داخل JSON:
               - الجزء الأول (generalStatusAr / generalStatusEn): الحالة العامة والقيم الطبيعية — اذكر بوضوح ما إذا كانت معظم مؤشرات التحليل ضمن المعدل الطبيعي وما هي المؤشرات السليمة.
               - الجزء الثاني (followUpPointsAr / followUpPointsEn): نقاط تحتاج لمتابعة أو انتباه الطبيب — استخرج أي قيمة غير طبيعية أو مرتفعة أو منخفضة أو وجود أملاح (مثل Oxalate/Urate/Crystals) أو خلايا صديدية (Pus cells) أو كرات دم حمراء (RBCs) أو زلال/سكر ومقارنتها بالمعدل الطبيعي في التقرير، واكتبها في نقاط مرقمة إلزامياً بصيغة:
                 1. ...
                 2. ...
                 3. ...
                 مع ربطها بالأمراض المزمنة وحساسية الأدوية المسجلة.
            3. ممنوع منعاً باتاً إعطاء تشخيص مرضي قاطع أو وصف أي دواء أو جرعة علاجية للمريض؛ اكتفِ بالشرح التبسيطي والتوجيه لمراجعة الطبيب المعالج.
            4. إذا كانت جميع القيم طبيعية ولا توجد أمراض مزمنة حرجة، اذكر ذلك بوضوح في الجزء الأول واكتب في الجزء الثاني نقطة مرقمة: "1. جميع المؤشرات المقروءة ضمن النطاق الطبيعي المعتاد، ويُنصح بعرض أصل التقرير على الطبيب في المتابعة الدورية."
            5. لا تخترع أي رقم غير موجود في النص أو المرفق.

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
