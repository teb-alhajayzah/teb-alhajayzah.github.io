package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.config.PlatformBrandConfig
import com.example.data.local.ActivationOtpCodeEntity

object ActivationCodePrintHelper {

    fun printAvailableCodes(
        context: Context,
        brandConfig: PlatformBrandConfig,
        codes: List<ActivationOtpCodeEntity>,
        onError: (String) -> Unit = {},
    ) {
        val available = codes.filter { !it.isUsed }
        if (available.isEmpty()) {
            onError("لا توجد أكواد متاحة للطباعة، قم بتوليد أكواد جديدة أولاً")
            return
        }
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager == null) {
                        onError("خدمة الطباعة غير متوفرة على هذا الجهاز")
                        return
                    }
                    val jobName = "${brandConfig.platformNameAr} - أكواد تفعيل الحساب"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .build(),
                    )
                }
            }
            val html = buildPrintableHtml(brandConfig, available)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (_: Exception) {
            onError("تعذر فتح نافذة الطباعة / PDF")
        }
    }

    private fun escapeHtml(text: String): String = TextUtils.htmlEncode(text)

    private fun buildPrintableHtml(
        brand: PlatformBrandConfig,
        availableCodes: List<ActivationOtpCodeEntity>,
    ): String {
        val cardsHtml = availableCodes.mapIndexed { idx, item ->
            """
            <div class="code-card">
              <div class="brand-mini">${escapeHtml(brand.platformNameAr)} • بطاقة تفعيل رقم ${idx + 1}</div>
              <div class="otp-code" dir="ltr">${escapeHtml(item.code)}</div>
              <div class="hint">كود استخدام واحد لتفعيل حساب المريض ومنع الحجوزات الوهمية</div>
            </div>
            """.trimIndent()
        }.joinToString("\n")

        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <style>
                body { font-family: sans-serif; color: #132226; margin: 20px; }
                .header { border-bottom: 3px solid #0D7377; padding-bottom: 10px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
                .title { color: #0D7377; font-size: 20px; font-weight: bold; }
                .sub { color: #4F646B; font-size: 13px; }
                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
                .code-card { border: 1.5px dashed #0D7377; border-radius: 12px; padding: 14px 10px; text-align: center; background: #F8FCFC; page-break-inside: avoid; }
                .brand-mini { font-size: 11px; color: #0D7377; font-weight: bold; margin-bottom: 6px; }
                .otp-code { font-size: 24px; font-weight: bold; letter-spacing: 4px; color: #14532D; margin: 6px 0; }
                .hint { font-size: 10px; color: #4F646B; line-height: 1.4; }
              </style>
            </head>
            <body>
              <div class="header">
                <div>
                  <div class="title">أكواد تفعيل المرضى — ${escapeHtml(brand.platformNameAr)}</div>
                  <div class="sub">${escapeHtml(brand.villageNameAr)} • عدد الأكواد المتاحة للطباعة: ${availableCodes.size}</div>
                </div>
                <div class="sub">${escapeHtml(brand.appVersionLabel)}</div>
              </div>
              <div class="grid">
                $cardsHtml
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
