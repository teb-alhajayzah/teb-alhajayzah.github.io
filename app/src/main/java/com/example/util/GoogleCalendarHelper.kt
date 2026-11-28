package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.ClinicDoctorEntity
import com.example.ui.bi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GoogleCalendarHelper {

    fun resolveSlotStartMillis(
        slot: AppointmentSlotEntity,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Long {
        if (slot.appointmentTimestamp > 0L) {
            return slot.appointmentTimestamp
        }
        val effMinutes = ((slot.effectiveMinutes % 1440) + 1440) % 1440
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo")).apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, effMinutes / 60)
            set(Calendar.MINUTE, effMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatUtcGoogleCalendarStamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(millis))
    }

    fun buildAppointmentTitle(
        doctor: ClinicDoctorEntity?,
        isEn: Boolean,
    ): String {
        val docName = if (isEn && !doctor?.doctorNameEn.isNullOrBlank()) {
            doctor!!.doctorNameEn
        } else {
            doctor?.doctorName.orEmpty().ifBlank { bi(isEn, "عيادة طب الحجايزة", "Teb Al-Hajayzah Clinic") }
        }
        val clinicName = if (isEn && !doctor?.clinicNameEn.isNullOrBlank()) {
            doctor!!.clinicNameEn
        } else {
            doctor?.clinicName.orEmpty()
        }
        return if (clinicName.isNotBlank()) {
            bi(isEn, "موعد طبي: $docName - $clinicName", "Medical Appointment: $docName - $clinicName")
        } else {
            bi(isEn, "موعد طبي: $docName", "Medical Appointment: $docName")
        }
    }

    fun buildAppointmentDetails(
        slot: AppointmentSlotEntity,
        doctor: ClinicDoctorEntity?,
        isEn: Boolean,
    ): String {
        val docName = if (isEn && !doctor?.doctorNameEn.isNullOrBlank()) {
            doctor!!.doctorNameEn
        } else {
            doctor?.doctorName.orEmpty()
        }
        val clinicName = if (isEn && !doctor?.clinicNameEn.isNullOrBlank()) {
            doctor!!.clinicNameEn
        } else {
            doctor?.clinicName.orEmpty()
        }
        val formattedTime = TimeUtils.formatMinutesAr(slot.effectiveMinutes, isEn)
        val lines = mutableListOf<String>()
        if (slot.patientName.isNotBlank()) {
            lines += bi(isEn, "المريض: ${slot.patientName}", "Patient: ${slot.patientName}")
        }
        if (docName.isNotBlank()) {
            lines += bi(isEn, "الطبيب: $docName", "Doctor: $docName")
        }
        if (clinicName.isNotBlank()) {
            lines += bi(isEn, "العيادة: $clinicName", "Clinic: $clinicName")
        }
        lines += bi(isEn, "التاريخ والوقت: ${slot.dateLabel} — $formattedTime", "Date & Time: ${slot.dateLabel} — $formattedTime")
        if (slot.delayMinutes > 0) {
            lines += bi(
                isEn,
                "ملاحظة: يوجد تأخير (+${slot.delayMinutes} دقيقة)",
                "Note: Schedule delay (+${slot.delayMinutes} min)",
            )
        }
        val mapUrl = doctor?.mapsUrl.orEmpty()
        if (mapUrl.isNotBlank()) {
            lines += bi(isEn, "الموقع على الخريطة: $mapUrl", "Google Maps: $mapUrl")
        }
        lines += bi(isEn, "تم الحجز عبر منصة طب الحجايزة", "Booked via Teb Al-Hajayzah Platform")
        return lines.joinToString("\n")
    }

    fun buildGoogleCalendarUrl(
        slot: AppointmentSlotEntity,
        doctor: ClinicDoctorEntity?,
        isEn: Boolean,
    ): String {
        val durationMinutes = (doctor?.slotDurationMinutes ?: 20).coerceAtLeast(10)
        val startMillis = resolveSlotStartMillis(slot)
        val endMillis = startMillis + durationMinutes * 60_000L
        val location = if (isEn && !doctor?.locationLabelEn.isNullOrBlank()) {
            doctor!!.locationLabelEn
        } else {
            doctor?.locationLabel.orEmpty().ifBlank { doctor?.clinicName.orEmpty() }
        }
        val uri = Uri.parse("https://calendar.google.com/calendar/render")
            .buildUpon()
            .appendQueryParameter("action", "TEMPLATE")
            .appendQueryParameter("text", buildAppointmentTitle(doctor, isEn))
            .appendQueryParameter(
                "dates",
                "${formatUtcGoogleCalendarStamp(startMillis)}/${formatUtcGoogleCalendarStamp(endMillis)}",
            )
            .appendQueryParameter("details", buildAppointmentDetails(slot, doctor, isEn))
            .appendQueryParameter("location", location)
            .appendQueryParameter("ctz", "Africa/Cairo")
            .build()
        return uri.toString()
    }

    fun openGoogleCalendarForAppointment(
        context: Context,
        slot: AppointmentSlotEntity,
        doctor: ClinicDoctorEntity?,
        isEn: Boolean,
    ): Boolean {
        val calendarUrl = buildGoogleCalendarUrl(slot, doctor, isEn)
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        } catch (_: Exception) {
            try {
                val durationMinutes = (doctor?.slotDurationMinutes ?: 20).coerceAtLeast(10)
                val startMillis = resolveSlotStartMillis(slot)
                val endMillis = startMillis + durationMinutes * 60_000L
                val location = if (isEn && !doctor?.locationLabelEn.isNullOrBlank()) {
                    doctor!!.locationLabelEn
                } else {
                    doctor?.locationLabel.orEmpty().ifBlank { doctor?.clinicName.orEmpty() }
                }
                val insertIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    putExtra(CalendarContract.Events.TITLE, buildAppointmentTitle(doctor, isEn))
                    putExtra(CalendarContract.Events.DESCRIPTION, buildAppointmentDetails(slot, doctor, isEn))
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(insertIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
