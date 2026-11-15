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
        onError: (String) -> Unit = {},
    ) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager == null) {
                        onError("خدمة الطباعة غير متوفرة على هذا الجهاز")
                        return
                    }
                    val jobName = "${brandConfig.platformNameAr} - روشتة ${record.patientName}"
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
            val html = buildOfficialPrescriptionHtml(brandConfig, record)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            onError("تعذر فتح نافذة الطباعة / PDF")
        }
    }

    private fun escapeHtml(text: String): String = TextUtils.htmlEncode(text)

    private fun buildOfficialPrescriptionHtml(
        brand: PlatformBrandConfig,
        record: MedicalRecordEntity,
    ): String {
        val medLines = record.medications
            .lines()
            .map { it.trim().removePrefix("•").trim() }
            .filter { it.isNotEmpty() }

        val medRowsHtml = if (medLines.isEmpty()) {
            "<tr><td colspan='2'>لا توجد أدوية نصية مسجلة</td></tr>"
        } else {
            medLines.mapIndexed { idx, line ->
                val parts = line.split("—", limit = 2)
                val medName = escapeHtml(parts[0].trim())
                val dosage = escapeHtml(parts.getOrNull(1)?.trim() ?: "حسب إرشادات الطبيب")
                "<tr><td>${idx + 1}. <strong>$medName</strong></td><td>$dosage</td></tr>"
            }.joinToString("\n")
        }

        val notesBlock = if (record.notes.isNotBlank()) {
            "<div class='box'><strong>تعليمات وملاحظات:</strong><br/>${escapeHtml(record.notes).replace("\n", "<br/>")}</div>"
        } else {
            ""
        }

        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <style>
                body { font-family: sans-serif; color: #132226; margin: 22px; line-height: 1.6; }
                .header { border-bottom: 3px solid #0D7377; padding-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
                .brand { color: #0D7377; font-size: 22px; font-weight: bold; }
                .sub { color: #4F646B; font-size: 13px; }
                .doc-box { text-align: left; }
                .meta { background: #F4F8F9; border: 1px solid #D0DCE0; border-radius: 10px; padding: 12px; margin: 14px 0; font-size: 14px; }
                .rx-title { color: #0D7377; font-size: 20px; font-weight: bold; margin: 14px 0 8px 0; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                th, td { border: 1px solid #D0DCE0; padding: 10px; text-align: right; font-size: 14px; }
                th { background: #E0F2F1; color: #043638; }
                .box { background: #FFFBEB; border: 1px solid #F59E0B; border-radius: 10px; padding: 10px 12px; margin-top: 14px; font-size: 13px; }
                .footer { margin-top: 32px; border-top: 1px dashed #94A3B8; padding-top: 12px; display: flex; justify-content: space-between; font-size: 12px; color: #4F646B; }
              </style>
            </head>
            <body>
              <div class="header">
                <div>
                  <div class="brand">${escapeHtml(brand.platformNameAr)}</div>
                  <div class="sub">${escapeHtml(brand.villageNameAr)} • هاتف: ${escapeHtml(brand.supportPhone)}</div>
                </div>
                <div class="doc-box">
                  <div><strong>${escapeHtml(record.doctorName)}</strong></div>
                  <div class="sub">${escapeHtml(record.specialty)}</div>
                </div>
              </div>

              <div class="meta">
                <div><strong>اسم المريض:</strong> ${escapeHtml(record.patientName)} &nbsp;&nbsp;|&nbsp;&nbsp; <strong>التاريخ:</strong> ${escapeHtml(record.visitDateLabel)}</div>
                <div><strong>التشخيص:</strong> ${escapeHtml(record.diagnosis)}</div>
              </div>

              <div class="rx-title">℞ الروشتة الطبية</div>
              <table>
                <thead>
                  <tr>
                    <th style="width: 50%;">الدواء والتركيز</th>
                    <th style="width: 50%;">الجرعة وطريقة الاستخدام</th>
                  </tr>
                </thead>
                <tbody>
                  $medRowsHtml
                </tbody>
              </table>

              $notesBlock

              <div class="footer">
                <div>صدرت إلكترونياً عبر منصة ${escapeHtml(brand.platformNameAr)} (${escapeHtml(brand.appVersionLabel)})</div>
                <div>توقيع الطبيب: ____________________</div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
