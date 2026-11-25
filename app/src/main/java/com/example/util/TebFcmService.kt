package com.example.util

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TebFcmService : FirebaseMessagingService() {

    companion object {
        var currentFcmToken: String = ""
            private set
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        currentFcmToken = token
        Log.d("TebFcmService", "New FCM Token received for independent project: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "طب الحجايزة"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "لديك إشعار جديد من منصة طب الحجايزة"

        TebNotificationHelper.showHighPriorityNotification(
            context = applicationContext,
            title = title,
            body = body,
        )
    }
}
