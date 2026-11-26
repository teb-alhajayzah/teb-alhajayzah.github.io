package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import kotlin.math.abs

data class AppointmentReminderTrigger(
    val kind: String,
    val requestCode: Int,
    val triggerAtMillis: Long,
    val title: String,
    val body: String,
)

object TebNotificationHelper {

    const val CHANNEL_ID = "teb_alhajayzah_alerts_v1"
    const val CHANNEL_NAME_AR = "تنبيهات وإشعارات طب الحجايزة"
    const val EXTRA_TITLE = "extra_notif_title"
    const val EXTRA_BODY = "extra_notif_body"
    const val EXTRA_NOTIFICATION_ID = "extra_notif_id"

    private const val KIND_EARLY_2H = "REMIND_2H"
    private const val KIND_EARLY_1H = "REMIND_1H"
    private const val KIND_PRE_30M = "REMIND_30M"
    private const val KIND_CUSTOM = "REMIND_CUSTOM"

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isRuntimeNotificationPermissionRequired(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
    }

    fun openAppNotificationSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME_AR,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "تذكيرات المواعيد ورسائل العيادات ونتائج التحاليل"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSound, audioAttrs)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showHighPriorityNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
    ): Boolean {
        if (title.isBlank() && body.isBlank()) return false
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return runCatching {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title.ifBlank { "طب الحجايزة" })
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSound)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    fun computeReminderTriggersMillis(
        slotId: String,
        doctorName: String,
        clinicName: String,
        effectiveMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<AppointmentReminderTrigger> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, (effectiveMinutes / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (effectiveMinutes % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val apptMillis = cal.timeInMillis
        if (apptMillis <= nowMillis) return emptyList()

        val timeLabel = TimeUtils.formatMinutesAr(effectiveMinutes, isEn = false)
        val placeLabel = clinicName.ifBlank { doctorName.ifBlank { "العيادة" } }
        val docLabel = doctorName.ifBlank { placeLabel }
        val out = mutableListOf<AppointmentReminderTrigger>()

        val twoHoursBefore = apptMillis - 120L * 60_000L
        val oneHourBefore = apptMillis - 60L * 60_000L
        val thirtyMinBefore = apptMillis - 30L * 60_000L

        if (twoHoursBefore > nowMillis) {
            out += AppointmentReminderTrigger(
                kind = KIND_EARLY_2H,
                requestCode = reminderRequestCode(slotId, KIND_EARLY_2H),
                triggerAtMillis = twoHoursBefore,
                title = "تذكير بموعدك الطبي بعد ساعتين",
                body = "لديك كشف في $placeLabel ($docLabel) اليوم الساعة $timeLabel — يرجى الاستعداد والحضور قبل الموعد بوقت كافٍ.",
            )
        } else if (oneHourBefore > nowMillis) {
            out += AppointmentReminderTrigger(
                kind = KIND_EARLY_1H,
                requestCode = reminderRequestCode(slotId, KIND_EARLY_1H),
                triggerAtMillis = oneHourBefore,
                title = "تذكير بموعدك الطبي بعد ساعة",
                body = "تذكير: موعد الكشف في $placeLabel ($docLabel) اليوم الساعة $timeLabel.",
            )
        }

        if (thirtyMinBefore > nowMillis) {
            out += AppointmentReminderTrigger(
                kind = KIND_PRE_30M,
                requestCode = reminderRequestCode(slotId, KIND_PRE_30M),
                triggerAtMillis = thirtyMinBefore,
                title = "اقترب موعد كشفك خلال 30 دقيقة",
                body = "موعدك مع $docLabel ($placeLabel) الساعة $timeLabel قد اقترب — نتمنى لك السلامة.",
            )
        }

        return out
    }

    fun scheduleAppointmentReminders(
        context: Context,
        slotId: String,
        doctorName: String,
        clinicName: String,
        effectiveMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        ensureNotificationChannel(context)
        cancelAppointmentReminders(context, slotId)
        val triggers = computeReminderTriggersMillis(
            slotId = slotId,
            doctorName = doctorName,
            clinicName = clinicName,
            effectiveMinutes = effectiveMinutes,
            nowMillis = nowMillis,
        )
        triggers.forEach { trigger ->
            scheduleAlarmInternal(
                context = context,
                requestCode = trigger.requestCode,
                triggerAtMillis = trigger.triggerAtMillis,
                title = trigger.title,
                body = trigger.body,
            )
        }
    }

    fun scheduleCustomReminder(
        context: Context,
        slotId: String,
        title: String,
        body: String,
        triggerAtMillis: Long,
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        ensureNotificationChannel(context)
        scheduleAlarmInternal(
            context = context,
            requestCode = reminderRequestCode(slotId, KIND_CUSTOM),
            triggerAtMillis = triggerAtMillis,
            title = title,
            body = body,
        )
    }

    fun cancelAppointmentReminders(context: Context, slotId: String) {
        val alarmManager = runCatching {
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        }.getOrNull() ?: return

        listOf(KIND_EARLY_2H, KIND_EARLY_1H, KIND_PRE_30M, KIND_CUSTOM).forEach { kind ->
            runCatching {
                val reqCode = reminderRequestCode(slotId, kind)
                val intent = Intent(context, TebAppointmentReminderReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
                if (pi != null) {
                    alarmManager.cancel(pi)
                    pi.cancel()
                }
            }
        }
    }

    fun scheduleBackgroundSyncAlarm(context: Context) {
        runCatching {
            ensureNotificationChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TebBackgroundSyncReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                910_045,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val intervalMs = AlarmManager.INTERVAL_FIFTEEN_MINUTES
            val firstTriggerAt = System.currentTimeMillis() + 120_000L
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                firstTriggerAt,
                intervalMs,
                pi,
            )
        }
    }

    private fun scheduleAlarmInternal(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        body: String,
    ) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TebAppointmentReminderReceiver::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }
    }

    private fun reminderRequestCode(slotId: String, kind: String): Int =
        abs("${slotId}_$kind".hashCode()) and 0x7FFFFFFF
}

class TebAppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TebNotificationHelper.EXTRA_TITLE)
            ?: "تذكير بموعدك الطبي — طب الحجايزة"
        val body = intent.getStringExtra(TebNotificationHelper.EXTRA_BODY)
            ?: "اقترب موعد الكشف الطبي الخاص بك."
        val notifId = intent.getIntExtra(
            TebNotificationHelper.EXTRA_NOTIFICATION_ID,
            (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
        )
        TebNotificationHelper.showHighPriorityNotification(
            context = context,
            title = title,
            body = body,
            notificationId = notifId,
        )
    }
}
