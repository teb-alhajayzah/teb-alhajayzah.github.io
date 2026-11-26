package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.NotificationAudience
import com.example.data.local.SlotStatus
import com.example.data.local.TebDatabase
import com.example.data.remote.FirestoreCloudSyncService
import com.example.data.repository.TebRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * مستقبل المزامنة والتذكير في الخلفية: يعمل حتى عند إغلاق التطبيق أو سحبه من الذاكرة
 * للتحقق من تذكيرات المواعيد (قبل ساعتين وقبل 30 دقيقة) والإشعارات السحابية الجديدة.
 */
class TebBackgroundSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(18_000L) {
                    val dao = TebDatabase.getInstance(appCtx).tebDao()
                    val repo = TebRepository(dao, appCtx)
                    val prefs = appCtx.getSharedPreferences("teb_bg_sync_prefs", Context.MODE_PRIVATE)
                    val nowMillis = System.currentTimeMillis()

                    // 1) فحص تذكيرات المواعيد المحلية (قبل ساعتين / قبل نصف ساعة)
                    val doctors = dao.observeDoctors().first().associateBy { it.id }
                    val bookedSlots = dao.getSlotsByStatusOnce(SlotStatus.BOOKED)
                    for (slot in bookedSlots) {
                        val apptMillis = if (slot.appointmentTimestamp > 0L) {
                            slot.appointmentTimestamp
                        } else {
                            repo.computeSlotAppointmentTimestamp(slot)
                        }
                        if (apptMillis <= nowMillis) continue
                        val diffMin = ((apptMillis - nowMillis) / 60_000L).toInt()
                        val doc = doctors[slot.doctorId]
                        val clinicLabel = doc?.clinicName.orEmpty().ifBlank { "العيادة" }
                        val docLabel = doc?.doctorName.orEmpty().ifBlank { clinicLabel }
                        val timeLabel = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn = false)

                        if (!slot.reminded2h && diffMin in 46..130) {
                            TebNotificationHelper.showHighPriorityNotification(
                                context = appCtx,
                                title = "تذكير بموعدك الطبي بعد ساعتين",
                                body = "لديك كشف في $clinicLabel ($docLabel) الساعة $timeLabel — يرجى الاستعداد والحضور قبل الموعد بوقت كافٍ.",
                                notificationId = abs("bg_2h_${slot.id}".hashCode()) and 0x7FFFFFFF,
                            )
                            val updated = slot.copy(appointmentTimestamp = apptMillis, reminded2h = true)
                            dao.updateSlot(updated)
                            val brand = repo.brandConfigFlow.first()
                            FirestoreCloudSyncService.upsertDocument(
                                brand,
                                "slots",
                                updated.id,
                                FirestoreCloudSyncService.slotToJson(updated),
                            )
                        } else if (!slot.reminded30m && diffMin in 1..35) {
                            TebNotificationHelper.showHighPriorityNotification(
                                context = appCtx,
                                title = "اقترب موعد كشفك خلال 30 دقيقة",
                                body = "موعدك مع $docLabel ($clinicLabel) الساعة $timeLabel قد اقترب — نتمنى لك السلامة.",
                                notificationId = abs("bg_30m_${slot.id}".hashCode()) and 0x7FFFFFFF,
                            )
                            val updated = slot.copy(appointmentTimestamp = apptMillis, reminded30m = true)
                            dao.updateSlot(updated)
                            val brand = repo.brandConfigFlow.first()
                            FirestoreCloudSyncService.upsertDocument(
                                brand,
                                "slots",
                                updated.id,
                                FirestoreCloudSyncService.slotToJson(updated),
                            )
                        }
                    }

                    // 2) مزامنة خفيفة للإشعارات السحابية مع احترام مهلة HTTP 429
                    if (FirestoreCloudSyncService.isCloudNetworkAllowed() && FirestoreCloudSyncService.canAutoSyncNow(nowMillis)) {
                        val brand = repo.brandConfigFlow.first()
                        val activeAccountId = repo.activeAccountIdFlow.first()
                        val loggedInDoctorId = repo.loggedInDoctorIdFlow.first()
                        val isAdminLogged = repo.isAdminLoggedInFlow.first()
                        val remoteNotifs = FirestoreCloudSyncService.listCollection(brand, "notifications")
                        if (!remoteNotifs.isNullOrEmpty()) {
                            val seenRaw = prefs.getStringSet("seen_bg_notif_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                            var changedSeen = false
                            remoteNotifs
                                .mapNotNull { FirestoreCloudSyncService.notificationFromJson(it) }
                                .filter { !it.isRead && (nowMillis - it.createdAtMillis) <= 12L * 60L * 60L * 1000L }
                                .forEach { n ->
                                    val key = "${n.createdAtMillis}_${n.targetAudience}_${n.targetId}_${n.message.take(24)}"
                                    val relevant = when (n.targetAudience) {
                                        NotificationAudience.ALL -> true
                                        NotificationAudience.ADMIN -> isAdminLogged
                                        NotificationAudience.CLINIC -> !loggedInDoctorId.isNullOrBlank() && (n.targetId.isBlank() || n.targetId == loggedInDoctorId)
                                        NotificationAudience.PATIENT -> activeAccountId.isNotBlank() && (n.targetId.isBlank() || n.targetId == activeAccountId)
                                        else -> false
                                    }
                                    if (relevant && key !in seenRaw) {
                                        seenRaw.add(key)
                                        changedSeen = true
                                        TebNotificationHelper.showHighPriorityNotification(
                                            context = appCtx,
                                            title = n.senderLabel.ifBlank { "إشعار جديد — طب الحجايزة" },
                                            body = n.message,
                                            notificationId = abs(key.hashCode()) and 0x7FFFFFFF,
                                        )
                                    }
                                }
                            if (changedSeen) {
                                prefs.edit().putStringSet("seen_bg_notif_ids", seenRaw.take(80).toSet()).apply()
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
