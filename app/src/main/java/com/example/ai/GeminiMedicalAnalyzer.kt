package com.example.ai

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
                        "summaryAr" to Schema.string("Concise, clinically helpful Arabic summary linking patient vitals, chronic conditions, drug allergies, medications, and extracted lab/imaging PDF values."),
                        "summaryEn" to Schema.string("Concise English clinical summary mirroring the Arabic summary with medical terminology for doctors."),
                        "extractedFindingsAr" to Schema.string("Bullet points in Arabic of key numeric/diagnostic values extracted directly from attached PDF/image lab reports."),
                        "extractedFindingsEn" to Schema.string("Bullet points in English of key numeric/diagnostic values extracted directly from attached PDF/image lab reports."),
                        "alertsAr" to Schema.string("Critical clinical alerts in Arabic (e.g., drug allergies or abnormal lab values interacting with chronic diseases like diabetes or hypertension), or empty string if none."),
                        "alertsEn" to Schema.string("Critical clinical alerts in English, or empty string if none."),
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
                    text("Attached medical document ($name):")
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
            val parsed = parseStructuredResponse(rawJson)
            Result.success(parsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (te: TimeoutCancellationException) {
            Log.e(TAG, "Gemini medical analysis timed out", te)
            Result.failure(
                IllegalStateException("انتهت مهلة تحليل الملف الطبي عبر الإنترنت. حاول مجدداً عند توفر شبكة مستقرة."),
            )
        } catch (ex: Exception) {
            Log.e(TAG, "Gemini medical analysis failed", ex)
            Result.failure(
                IllegalStateException(
                    ex.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "تعذر تحليل الملف الطبي حالياً عبر الذكاء الاصطناعي.",
                ),
            )
        }
    }

    private fun parseStructuredResponse(rawJson: String): GeminiMedicalSummaryResult {
        val cleaned = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val obj = JSONObject(cleaned)
        val summaryAr = obj.optString("summaryAr").trim()
        val summaryEn = obj.optString("summaryEn").trim()
        val findingsAr = obj.optString("extractedFindingsAr").trim()
        val findingsEn = obj.optString("extractedFindingsEn").trim()
        val alertsAr = obj.optString("alertsAr").trim()
        val alertsEn = obj.optString("alertsEn").trim()

        val fullAr = buildString {
            if (summaryAr.isNotBlank()) append(summaryAr)
            if (findingsAr.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("📊 نتائج مستخرجة من المرفقات:\n").append(findingsAr)
            }
            if (alertsAr.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("⚠️ تنبيهات طبية هامة:\n").append(alertsAr)
            }
        }.trim()

        val fullEn = buildString {
            if (summaryEn.isNotBlank()) append(summaryEn)
            if (findingsEn.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("📊 Extracted Findings from Attachments:\n").append(findingsEn)
            }
            if (alertsEn.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("⚠️ Clinical Alerts:\n").append(alertsEn)
            }
        }.trim()

        return GeminiMedicalSummaryResult(
            summaryAr = fullAr.ifBlank { fullEn },
            summaryEn = fullEn.ifBlank { fullAr },
        )
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

        return """
            أنت مساعد طبي سريري ذكي لمنصة "طب الحجايزة" لمساعدة الأطباء والمرضى في قراءة الملخص الطبي السريع.
            قم بقراءة بيانات المريض الحيوية، والأمراض المزمنة، وحساسية الأدوية، وسجل الزيارات، وأي ملفات PDF أو صور تحاليل مرفقة (اقرأ الأرقام والقيم الفعلية داخل ملفات PDF أو الصور المرفقة مثل تحليل البول Urine أو CBC أو السكر أو الأشعة).
            ثم قدّم ملخصاً مهنياً دقيقاً ومختصراً باللغتين العربية والإنجليزية في JSON:
            - اربط النتائج غير الطبيعية في التحاليل بالأمراض المزمنة المسجلة للمريض (مثل ربط ارتفاع السكر أو الجلوكوز في البول بمرض السكر، أو الزلال/الكرياتينين بالضغط والكلى، أو وجود صديد/بكتيريا بعدوى).
            - اذكر أبرز تنبيه لحساسية الأدوية أو التفاعلات الهامة للطبيب المعالج قبل صرف علاج جديد.
            - التزم بالمصداقية: لا تخترع قيماً غير موجودة في النص أو المرفقات.

            البيانات الحيوية للمريض: $vitals
            الأمراض المزمنة: ${chronicList.ifBlank { "لا توجد أمراض مزمنة مسجلة" }}
            حساسية الأدوية: ${member.drugAllergies.ifBlank { "لا توجد حساسية أدوية مسجلة" }}

            السجل الطبي الأخير:
            $historyBlock
        """.trimIndent()
    }

    companion object {
        private const val TAG = "GeminiMedicalAnalyzer"
        private const val MAX_INLINE_FILE_BYTES = 18 * 1024 * 1024L
    }
}
