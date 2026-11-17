package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.config.PlatformBrandConfig
import com.example.data.local.MedicalRecordEntity

object PrescriptionPrintHelper {

    fun printPrescription(
        context: Context,
        brandConfig: PlatformBrandConfig,
        record: MedicalRecordEntity,
        isEnglish: Boolean = false,
        onError: (String) -> Unit = {},
    ) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager == null) {
                        onError(
                            if (isEnglish) "Printing service is unavailable on this device"
                            else "خدمة الطباعة غير متوفرة على هذا الجهاز"
                        )
                        return
                    }
                    val brandLabel = if (isEnglish) brandConfig.platformNameEn else brandConfig.platformNameAr
                    val jobName = if (isEnglish) {
                        "$brandLabel - Prescription (${record.patientName})"
                    } else {
                        "$brandLabel - روشتة ${record.patientName}"
                    }
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                            .build(),
                    )
                }
            }
            val html = buildOfficialPrescriptionHtml(brandConfig, record, isEnglish)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            onError(
                if (isEnglish) "Could not open print / PDF preview"
                else "تعذر فتح نافذة الطباعة / PDF"
            )
        }
    }

    private fun escapeHtml(text: String): String = TextUtils.htmlEncode(text)

    private fun buildOfficialPrescriptionHtml(
        brand: PlatformBrandConfig,
        record: MedicalRecordEntity,
        isEnglish: Boolean,
    ): String {
        val localized = BilingualMedicalTranslator.resolveForDisplay(record, showEnglish = isEnglish)
        val medLines = localized.medications
            .lines()
            .map { it.trim().removePrefix("•").trim() }
            .filter { it.isNotEmpty() }

        val medRowsHtml = if (medLines.isEmpty()) {
            val emptyMsg = if (isEnglish) "No text medications recorded" else "لا توجد أدوية نصية مسجلة"
            "<tr><td colspan='2'>$emptyMsg</td></tr>"
        } else {
            medLines.mapIndexed { idx, line ->
                val parts = line.split("—", limit = 2)
                val medName = escapeHtml(parts[0].trim())
                val defaultInstr = if (isEnglish) "As directed by physician" else "حسب إرشادات الطبيب"
                val dosage = escapeHtml(parts.getOrNull(1)?.trim() ?: defaultInstr)
                "<tr><td>${idx + 1}. <strong>$medName</strong></td><td>$dosage</td></tr>"
            }.joinToString("\n")
        }

        val notesBlock = if (localized.notes.isNotBlank()) {
            val notesTitle = if (isEnglish) "Instructions & Notes:" else "تعليمات وملاحظات:"
            "<div class='box'><strong>$notesTitle</strong><br/>${escapeHtml(localized.notes).replace("\n", "<br/>")}</div>"
        } else {
            ""
        }

        val aiReportBlock = if (localized.aiReport.isNotBlank()) {
            val reportTitle = if (isEnglish) "Medical Report Summary:" else "التقرير الطبي المختصر للمريض:"
            "<div class='report-box'><strong>$reportTitle</strong><br/>${escapeHtml(localized.aiReport).replace("\n", "<br/>")}</div>"
        } else {
            ""
        }

        val langAttr = if (isEnglish) "en" else "ar"
        val dirAttr = if (isEnglish) "ltr" else "rtl"
        val alignCell = if (isEnglish) "left" else "right"
        val alignDoc = if (isEnglish) "right" else "left"
        val brandTitle = if (isEnglish) brand.platformNameEn else brand.platformNameAr
        val villageSubtitle = if (isEnglish) brand.villageNameEn else brand.villageNameAr
        val phoneLabel = if (isEnglish) "Phone" else "هاتف"
        val patientLabel = if (isEnglish) "Patient Name:" else "اسم المريض:"
        val dateLabel = if (isEnglish) "Date:" else "التاريخ:"
        val diagLabel = if (isEnglish) "Diagnosis:" else "التشخيص:"
        val rxTitle = if (isEnglish) "℞ Electronic Prescription" else "℞ الروشتة الطبية"
        val colDrug = if (isEnglish) "Medication & Strength" else "الدواء والتركيز"
        val colDosage = if (isEnglish) "Dosage & Instructions" else "الجرعة وطريقة الاستخدام"
        val issuedText = if (isEnglish) {
            "Issued electronically via $brandTitle (${brand.appVersionLabel})"
        } else {
            "صدرت إلكترونياً عبر منصة $brandTitle (${brand.appVersionLabel})"
        }
        val signText = if (isEnglish) "Doctor Signature: ____________________" else "توقيع الطبيب: ____________________"

        return """
            <!DOCTYPE html>
            <html lang="$langAttr" dir="$dirAttr">
            <head>
              <meta charset="UTF-8" />
              <style>
                body { font-family: sans-serif; color: #132226; margin: 22px; line-height: 1.6; }
                .header { border-bottom: 3px solid #0D7377; padding-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
                .brand { color: #0D7377; font-size: 22px; font-weight: bold; }
                .sub { color: #4F646B; font-size: 13px; }
                .doc-box { text-align: $alignDoc; }
                .meta { background: #F4F8F9; border: 1px solid #D0DCE0; border-radius: 10px; padding: 12px; margin: 14px 0; font-size: 14px; }
                .rx-title { color: #0D7377; font-size: 20px; font-weight: bold; margin: 14px 0 8px 0; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                th, td { border: 1px solid #D0DCE0; padding: 10px; text-align: $alignCell; font-size: 14px; }
                th { background: #E0F2F1; color: #043638; }
                .box { background: #FFFBEB; border: 1px solid #F59E0B; border-radius: 10px; padding: 10px 12px; margin-top: 14px; font-size: 13px; }
                .report-box { background: #F0FDFA; border: 1px solid #0D7377; border-radius: 10px; padding: 10px 12px; margin-top: 12px; font-size: 13px; }
                .footer { margin-top: 28px; border-top: 1px dashed #94A3B8; padding-top: 12px; display: flex; justify-content: space-between; font-size: 12px; color: #4F646B; }
              </style>
            </head>
            <body>
              <div class="header">
                <div>
                  <div class="brand">${escapeHtml(brandTitle)}</div>
                  <div class="sub">${escapeHtml(villageSubtitle)} • $phoneLabel: ${escapeHtml(brand.supportPhone)}</div>
                </div>
                <div class="doc-box">
                  <div><strong>${escapeHtml(record.doctorName)}</strong></div>
                  <div class="sub">${escapeHtml(localized.specialty)}</div>
                </div>
              </div>

              <div class="meta">
                <div><strong>$patientLabel</strong> ${escapeHtml(record.patientName)} &nbsp;&nbsp;|&nbsp;&nbsp; <strong>$dateLabel</strong> ${escapeHtml(record.visitDateLabel)}</div>
                <div><strong>$diagLabel</strong> ${escapeHtml(localized.diagnosis)}</div>
              </div>

              <div class="rx-title">$rxTitle</div>
              <table>
                <thead>
                  <tr>
                    <th style="width: 50%;">$colDrug</th>
                    <th style="width: 50%;">$colDosage</th>
                  </tr>
                </thead>
                <tbody>
                  $medRowsHtml
                </tbody>
              </table>

              $notesBlock
              $aiReportBlock

              <div class="footer">
                <div>${escapeHtml(issuedText)}</div>
                <div>$signText</div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
