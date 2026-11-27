package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.BuildConfig
import com.example.ai.GeminiMedicalAnalyzer
import com.example.util.ImageStorageHelper
import com.example.util.TebNotificationHelper
import com.example.util.TebFcmService
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.TealPrimary

fun isDeviceOnline(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}

private fun isExternalPortalUrl(url: String, liveHost: String): Boolean {
    val lower = url.lowercase()
    if (
        lower.startsWith("whatsapp:") ||
        lower.startsWith("tel:") ||
        lower.startsWith("mailto:") ||
        lower.startsWith("geo:") ||
        lower.startsWith("intent:") ||
        lower.contains("wa.me/") ||
        lower.contains("api.whatsapp.com") ||
        lower.contains("maps.google.") ||
        lower.contains("maps.app.goo.gl") ||
        lower.contains("goo.gl/maps") ||
        lower.endsWith(".apk")
    ) {
        return true
    }
    val parsedHost = try {
        Uri.parse(url).host?.lowercase().orEmpty()
    } catch (_: Exception) {
        ""
    }
    return parsedHost.isNotBlank() &&
        liveHost.isNotBlank() &&
        parsedHost != liveHost &&
        !parsedHost.endsWith(".$liveHost")
}

private fun openExternalUri(context: Context, rawUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback if no handler installed
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebPortalScreen(
    liveUrl: String,
    isEn: Boolean,
    hasNewDataBanner: Boolean,
    onDismissBannerAndSync: () -> Unit,
    onCheckLiveUpdates: () -> Unit = {},
    onWebViewReady: (WebView?) -> Unit = {},
    onSwitchToNativeOffline: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val liveHost = remember(liveUrl) {
        try {
            Uri.parse(liveUrl).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            "teb-alhajayzah.github.io"
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val bridgeScope = rememberCoroutineScope()
    val geminiAnalyzer = remember { GeminiMedicalAnalyzer() }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val hasPerm = granted || TebNotificationHelper.hasNotificationPermission(context)
        if (hasPerm) {
            TebNotificationHelper.ensureNotificationChannel(context)
        } else {
            TebNotificationHelper.openAppNotificationSettings(context)
        }
        webViewRef?.evaluateJavascript(
            "if (window.__onAndroidNotifPermissionUpdated) window.__onAndroidNotifPermissionUpdated($hasPerm);",
            null,
        )
    }

    val noCacheHeaders = remember {
        mapOf(
            "Cache-Control" to "no-cache, no-store, must-revalidate",
            "Pragma" to "no-cache",
            "Expires" to "0",
        )
    }

    fun triggerFreshReload() {
        loadError = false
        isLoading = true
        onDismissBannerAndSync()
        webViewRef?.let { wv ->
            wv.clearCache(true)
            val bustUrl = if (liveUrl.contains("?")) {
                "$liveUrl&_sync=${System.currentTimeMillis()}"
            } else {
                "$liveUrl?_sync=${System.currentTimeMillis()}"
            }
            wv.loadUrl(bustUrl, noCacheHeaders)
        }
    }

    fun pushFcmTokenToWebView(targetView: WebView?, rawToken: String) {
        val clean = rawToken.trim()
        if (clean.isEmpty() || targetView == null) return
        val safeTokenLiteral = JSONObject.quote(clean)
        targetView.post {
            targetView.evaluateJavascript(
                "window.NATIVE_FCM_TOKEN = $safeTokenLiteral; " +
                    "window.receiveNativeFcmToken && window.receiveNativeFcmToken($safeTokenLiteral); " +
                    "window.__onNativeFcmTokenReady && window.__onNativeFcmTokenReady($safeTokenLiteral);",
                null,
            )
        }
    }

    fun fetchAndPushFcmToken(targetView: WebView?) {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.trim().orEmpty()
                    if (token.isNotEmpty()) {
                        TebFcmService.updateCachedToken(context, token)
                        pushFcmTokenToWebView(targetView ?: webViewRef, token)
                    } else {
                        TebFcmService.updateFcmError(context, "FCM returned empty token")
                    }
                } else {
                    val errMsg = task.exception?.message
                        ?: task.exception?.localizedMessage
                        ?: "FCM token retrieval failed"
                    TebFcmService.updateFcmError(context, errMsg)
                }
            }
        }.onFailure { err ->
            TebFcmService.updateFcmError(
                context,
                err.message ?: err.localizedMessage ?: "FirebaseMessaging initialization error",
            )
        }
    }

    LaunchedEffect(Unit) {
        fetchAndPushFcmToken(webViewRef)
    }

    // تحديث تلقائي فوري داخل التطبيق بمجرد رصد تعديل في version.json أو الموقع دون تدخل المستخدم
    LaunchedEffect(hasNewDataBanner) {
        if (hasNewDataBanner && webViewRef != null) {
            triggerFreshReload()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onCheckLiveUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onWebViewReady(null)
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (hasNewDataBanner) {
            Surface(
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { triggerFreshReload() }
                    .testTag("webview_live_update_banner"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bi(isEn, "تتوفر بيانات جديدة - اضغط للتحديث", "New data available - Tap to refresh"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                    )
                }
            }
        }

        if (isLoading && !loadError) {
            LinearProgressIndicator(
                color = TealPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                        isVerticalScrollBarEnabled = true
                        clearCache(true)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            allowFileAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            loadsImagesAutomatically = true
                            val baseUa = userAgentString.orEmpty()
                            if (!baseUa.contains("TebAlHajayzahAPK", ignoreCase = true)) {
                                userAgentString = "$baseUa TebAlHajayzahAPK/1.0.0".trim()
                            }
                        }
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun isInstalledApk(): Boolean = true

                                @JavascriptInterface
                                fun getAppVersion(): String = BuildConfig.VERSION_NAME

                                @JavascriptInterface
                                fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

                                @JavascriptInterface
                                fun downloadAndInstallApk(apkUrl: String) {
                                    bridgeScope.launch(Dispatchers.Main) {
                                        if (apkUrl.isNotBlank()) {
                                            openExternalUri(ctx, apkUrl)
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun clearCacheOnly() {
                                    bridgeScope.launch(Dispatchers.Main) {
                                        webViewRef?.clearCache(true)
                                    }
                                }

                                @JavascriptInterface
                                fun clearCacheAndReload() {
                                    bridgeScope.launch(Dispatchers.Main) {
                                        triggerFreshReload()
                                    }
                                }
                            },
                            "AndroidAppHost",
                        )
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun saveBase64File(dataUrl: String, mimeType: String, fileName: String) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val res = ImageStorageHelper.saveBase64ToDownloads(
                                            context = ctx,
                                            rawDataUrlOrBase64 = dataUrl,
                                            mimeType = mimeType,
                                            displayName = fileName,
                                        )
                                        res.onSuccess { savedName ->
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    ctx,
                                                    "تم حفظ الملف في التنزيلات: $savedName",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }.onFailure { err ->
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    ctx,
                                                    err.localizedMessage ?: "تعذر حفظ الملف",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            "AndroidPdfBridge",
                        )
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun isAvailable(): Boolean {
                                    return runCatching { FirebaseApp.getApps(ctx).isNotEmpty() }.getOrDefault(false)
                                }

                                @JavascriptInterface
                                fun analyzeMedicalSummary(callbackId: String, payloadJson: String) {
                                    val wv = this@apply
                                    bridgeScope.launch(Dispatchers.IO) {
                                        val firebaseReady = runCatching { FirebaseApp.getApps(ctx).isNotEmpty() }.getOrDefault(false)
                                        val responseObj = JSONObject()
                                        if (firebaseReady) {
                                            val aiRes = geminiAnalyzer.analyzeWebPayloadJson(payloadJson)
                                            aiRes.onSuccess { res ->
                                                responseObj.put("ok", true)
                                                responseObj.put("summaryAr", res.summaryAr)
                                                responseObj.put("summaryEn", res.summaryEn)
                                            }.onFailure {
                                                responseObj.put("ok", false)
                                            }
                                        } else {
                                            responseObj.put("ok", false)
                                        }
                                        val cbLiteral = JSONObject.quote(callbackId)
                                        val jsonLiteral = JSONObject.quote(responseObj.toString())
                                        withContext(Dispatchers.Main) {
                                            wv.evaluateJavascript(
                                                "if (window.__onAndroidAiSummaryResult) window.__onAndroidAiSummaryResult($cbLiteral, $jsonLiteral);",
                                                null,
                                            )
                                        }
                                    }
                                }
                            },
                            "AndroidAiBridge",
                        )
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun hasPermission(): Boolean {
                                    return TebNotificationHelper.hasNotificationPermission(ctx)
                                }

                                @JavascriptInterface
                                fun requestPermission() {
                                    bridgeScope.launch(Dispatchers.Main) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            TebNotificationHelper.isRuntimeNotificationPermissionRequired(ctx)
                                        ) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            TebNotificationHelper.openAppNotificationSettings(ctx)
                                            val grantedNow = TebNotificationHelper.hasNotificationPermission(ctx)
                                            webViewRef?.evaluateJavascript(
                                                "if (window.__onAndroidNotifPermissionUpdated) window.__onAndroidNotifPermissionUpdated($grantedNow);",
                                                null,
                                            )
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun showNotification(title: String, body: String): Boolean {
                                    return TebNotificationHelper.showHighPriorityNotification(
                                        context = ctx,
                                        title = title,
                                        body = body,
                                    )
                                }

                                @JavascriptInterface
                                fun scheduleAppointmentReminders(
                                    slotId: String,
                                    doctorName: String,
                                    clinicName: String,
                                    effectiveMinutes: Int,
                                ) {
                                    TebNotificationHelper.scheduleAppointmentReminders(
                                        context = ctx,
                                        slotId = slotId,
                                        doctorName = doctorName,
                                        clinicName = clinicName,
                                        effectiveMinutes = effectiveMinutes,
                                    )
                                }

                                @JavascriptInterface
                                fun scheduleReminderAt(
                                    slotId: String,
                                    title: String,
                                    body: String,
                                    triggerAtMillis: Long,
                                ) {
                                    TebNotificationHelper.scheduleCustomReminder(
                                        context = ctx,
                                        slotId = slotId,
                                        title = title,
                                        body = body,
                                        triggerAtMillis = triggerAtMillis,
                                    )
                                }

                                @JavascriptInterface
                                fun cancelReminder(slotId: String) {
                                    TebNotificationHelper.cancelAppointmentReminders(ctx, slotId)
                                }
                            },
                            "AndroidNotifBridge",
                        )
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun isAndroidApp(): Boolean = true

                                @JavascriptInterface
                                fun getAppVersion(): String = BuildConfig.VERSION_NAME

                                @JavascriptInterface
                                fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

                                @JavascriptInterface
                                fun getFcmToken(): String = TebFcmService.getCachedToken(ctx)

                                @JavascriptInterface
                                fun getFcmError(): String = TebFcmService.getCachedError(ctx)

                                @JavascriptInterface
                                fun refreshFcmToken() {
                                    fetchAndPushFcmToken(webViewRef)
                                }

                                @JavascriptInterface
                                fun areNotificationsEnabled(): Boolean =
                                    TebNotificationHelper.hasNotificationPermission(ctx)

                                @JavascriptInterface
                                fun isNotificationPermissionGranted(): Boolean =
                                    TebNotificationHelper.hasNotificationPermission(ctx)

                                @JavascriptInterface
                                fun showNativeNotification(title: String, body: String): Boolean =
                                    TebNotificationHelper.showHighPriorityNotification(
                                        context = ctx,
                                        title = title,
                                        body = body,
                                    )

                                @JavascriptInterface
                                fun requestNotificationPermission() {
                                    bridgeScope.launch(Dispatchers.Main) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            TebNotificationHelper.isRuntimeNotificationPermissionRequired(ctx)
                                        ) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            TebNotificationHelper.openAppNotificationSettings(ctx)
                                        }
                                    }
                                }
                            },
                            "AndroidBridge",
                        )
                        setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                            if (!url.isNullOrBlank()) {
                                if (url.startsWith("data:", ignoreCase = true)) {
                                    val guessedName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val res = ImageStorageHelper.saveBase64ToDownloads(
                                            context = ctx,
                                            rawDataUrlOrBase64 = url,
                                            mimeType = mimeType.orEmpty(),
                                            displayName = guessedName,
                                        )
                                        res.onSuccess { saved ->
                                            Toast.makeText(ctx, "تم تنزيل الملف: $saved", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else if (!url.startsWith("blob:", ignoreCase = true)) {
                                    openExternalUri(ctx, url)
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                                try {
                                    request?.grant(request.resources)
                                } catch (_: Exception) {
                                    // Ignore if already handled
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?,
                            ): Boolean {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = filePathCallback
                                return try {
                                    fileChooserLauncher.launch(arrayOf("image/*", "application/pdf"))
                                    true
                                } catch (_: Exception) {
                                    pendingFileCallback?.onReceiveValue(null)
                                    pendingFileCallback = null
                                    false
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                if (target.isNotBlank() && isExternalPortalUrl(target, liveHost)) {
                                    openExternalUri(ctx, target)
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                val cachedToken = TebFcmService.getCachedToken(ctx)
                                if (cachedToken.isNotBlank()) {
                                    pushFcmTokenToWebView(view, cachedToken)
                                } else {
                                    fetchAndPushFcmToken(view)
                                }
                                view?.postDelayed({
                                    val latestToken = TebFcmService.getCachedToken(ctx)
                                    if (latestToken.isNotBlank()) {
                                        pushFcmTokenToWebView(view, latestToken)
                                    } else {
                                        fetchAndPushFcmToken(view)
                                    }
                                }, 1500L)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = true
                                }
                            }
                        }
                        webViewRef = this
                        onWebViewReady(this)
                        val initialLiveUrl = if (liveUrl.contains("?")) {
                            "$liveUrl&_v=${System.currentTimeMillis()}"
                        } else {
                            "$liveUrl?_v=${System.currentTimeMillis()}"
                        }
                        loadUrl(initialLiveUrl, noCacheHeaders)
                    }
                },
            )

            if (loadError) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(
                                    text = bi(isEn, "تعذر الوصول للخادم المباشر", "Live Server Unavailable"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = bi(
                                        isEn,
                                        "تأكد من اتصال الإنترنت لإعادة المزامنة الحية، أو افتح التطبيق بالوضع المحلي الكامل.",
                                        "Check your internet connection to retry live sync, or open full offline native mode.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = { triggerFreshReload() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "🔄 إعادة المحاولة الآن", "🔄 Retry Live Sync"))
                                }
                                OutlinedButton(
                                    onClick = onSwitchToNativeOffline,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                                ) {
                                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bi(isEn, "فتح النسخة المحلية على الجهاز", "Open Offline App Mode"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
