package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TebFcmService : FirebaseMessagingService() {

    companion object {
        private const val PREFS_NAME = "teb_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_ERROR = "fcm_error"

        @Volatile
        var currentFcmToken: String = ""
            private set

        @Volatile
        var currentFcmError: String = ""
            private set

        fun updateCachedToken(context: Context, token: String) {
            val clean = token.trim()
            if (clean.isEmpty()) return
            currentFcmToken = clean
            currentFcmError = ""
            runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FCM_TOKEN, clean)
                    .remove(KEY_FCM_ERROR)
                    .apply()
            }
        }

        fun getCachedToken(context: Context): String {
            if (currentFcmToken.isNotBlank()) return currentFcmToken
            val saved = runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_FCM_TOKEN, "")
                    .orEmpty()
                    .trim()
            }.getOrDefault("")
            if (saved.isNotEmpty()) {
                currentFcmToken = saved
            }
            return saved
        }

        fun updateFcmError(context: Context, errorMessage: String) {
            val clean = errorMessage.trim()
            currentFcmError = clean
            runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FCM_ERROR, clean)
                    .apply()
            }
        }

        fun getCachedError(context: Context): String {
            if (currentFcmError.isNotBlank()) return currentFcmError
            val saved = runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_FCM_ERROR, "")
                    .orEmpty()
                    .trim()
            }.getOrDefault("")
            if (saved.isNotEmpty()) {
                currentFcmError = saved
            }
            return saved
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateCachedToken(applicationContext, token)
        Log.d("TebFcmService", "New FCM Token received for independent project: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "طب الحجايزة"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "لديك إشعار جديد من منصة طب الحجايزة"

        TebNotificationHelper.showHighPriorityNotification(
            context = applicationContext,
            title = title,
            body = body,
        )
    }
}
