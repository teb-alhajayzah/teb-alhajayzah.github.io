package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

object FirebaseAppCheckHelper {

    private const val TAG = "AppCheckInit"
    private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
    private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
    private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

    fun ensureFirebaseAppInitialized(context: Context) {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = runCatching { FirebaseOptions.fromResource(context) }.getOrNull()
                    ?: FirebaseOptions.Builder()
                        .setProjectId("gen-lang-client-0514993881")
                        .setApplicationId("1:197836130786:android:97750c01e53b0977c411bb")
                        .setApiKey("AIzaSyCyZQYJ6-iV02yazYHRNJQyavNNymqrdhU")
                        .setGcmSenderId("197836130786")
                        .setStorageBucket("gen-lang-client-0514993881.firebasestorage.app")
                        .build()
                FirebaseApp.initializeApp(context, options)
            }
        }
    }

    fun configureAppCheck(context: Context, intent: Intent?) {
        try {
            ensureFirebaseAppInitialized(context)

            if (BuildConfig.DEBUG) {
                val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
                if (!debugToken.isNullOrEmpty()) {
                    val persistenceKey = FirebaseApp.getInstance().persistenceKey
                    val prefs = context.getSharedPreferences(
                        "$DEBUG_STORE_PREFIX$persistenceKey",
                        Context.MODE_PRIVATE,
                    )
                    // Synchronous commit ensures DebugAppCheckProviderFactory reads the secret immediately.
                    prefs.edit()
                        .putString(DEBUG_SECRET_KEY, debugToken)
                        .commit()
                    Log.d(TAG, "App Check debug secret configured successfully.")
                } else {
                    Log.w(TAG, "No debug token found in Intent extra.")
                }

                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } else {
                val resId = context.resources.getIdentifier(
                    RECAPTCHA_SITE_KEY_RES,
                    "string",
                    context.packageName,
                )
                if (resId != 0) {
                    val siteKey = context.getString(resId)
                    if (siteKey.isNotBlank()) {
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey),
                        )
                        Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                    } else {
                        Log.e(TAG, "recaptcha_site_key resource string is blank.")
                    }
                } else {
                    Log.e(TAG, "recaptcha_site_key string resource not found in package.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
        }
    }
}

