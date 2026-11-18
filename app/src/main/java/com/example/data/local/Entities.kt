package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object ClinicType {
    const val PRIVATE = "PRIVATE"
    const val HOSPITAL = "HOSPITAL"
}

object SlotStatus {
    const val AVAILABLE = "AVAILABLE"
    const val BOOKED = "BOOKED"
    const val COMPLETED = "COMPLETED"
}

object BookingSource {
    const val NONE = ""
    const val APP = "APP"
    const val PHONE_WALKIN = "PHONE_WALKIN"
}

object MedicalRecordType {
    const val PRESCRIPTION = "PRESCRIPTION"
    const val PATIENT_UPLOAD = "PATIENT_UPLOAD"
    const val EXTERNAL_VISIT = "EXTERNAL_VISIT"
    const val LAB_RESULT = "LAB_RESULT"
}

@Entity(tableName = "patient_accounts")
data class PatientAccountEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val nationalId: String,
    val password: String,
    val isPhoneVerified: Boolean = true,
    val verifiedBy: String = "",
    val verifiedAtMillis: Long = 0L,
    val otpCode: String = "",
    val otpCreatedAtMillis: Long = 0L,
    val otpExpiresAtMillis: Long = 0L,
    val otpUsed: Boolean = false,
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val fullName: String,
    val relation: String,
    val age: Int,
    val phone: String,
)

@Entity(tableName = "clinic_doctors")
data class ClinicDoctorEntity(
    @PrimaryKey val id: String,
    val doctorName: String,
    val specialty: String,
    val clinicName: String,
    val clinicType: String,
    val locationLabel: String,
    val feeEgp: Int,
    val workStartMinutes: Int,
    val workEndMinutes: Int,
    val slotDurationMinutes: Int,
    val currentDelayMinutes: Int = 0,
    val rating: Float = 4.8f,
    val experienceYears: Int = 12,
    val bio: String = "",
    val mapsUrl: String = "",
    val username: String = "",
    val password: String = "1234",
    val doctorNameEn: String = "",
    val clinicNameEn: String = "",
    val locationLabelEn: String = "",
    val additionalSpecialties: String = "",
)

@Entity(tableName = "appointment_slots")
data class AppointmentSlotEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val dateLabel: String = "اليوم",
    val startMinutes: Int,
    val delayMinutes: Int = 0,
    val status: String = SlotStatus.AVAILABLE,
    val patientName: String = "",
    val patientPhone: String = "",
    val familyMemberId: String = "",
    val bookingSource: String = BookingSource.NONE,
    val bookedAtMillis: Long = 0L,
) {
    val effectiveMinutes: Int
        get() = startMinutes + delayMinutes
}

@Entity(tableName = "medical_records")
data class MedicalRecordEntity(
    @PrimaryKey val id: String,
    val familyMemberId: String,
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val doctorName: String,
    val specialty: String,
    val visitDateLabel: String,
    val diagnosis: String,
    val medications: String,
    val notes: String,
    val attachmentPath: String = "",
    val attachmentMimeType: String = "",
    val attachmentName: String = "",
    val recordType: String = MedicalRecordType.PRESCRIPTION,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val diagnosisEn: String = "",
    val medicationsEn: String = "",
    val notesEn: String = "",
    val aiReportAr: String = "",
    val aiReportEn: String = "",
)

@Entity(tableName = "delay_notifications")
data class DelayNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorId: String,
    val doctorName: String,
    val clinicName: String,
    val delayMinutes: Int,
    val message: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
)

@Entity(
    tableName = "doctor_favorite_meds",
    primaryKeys = ["doctorId", "medicationId"],
)
data class DoctorFavoriteMedEntity(
    val doctorId: String,
    val medicationId: String,
)

@Entity(tableName = "blocked_phones")
data class BlockedPhoneEntity(
    @PrimaryKey val phone: String,
    val reason: String = "حظر إداري بسبب حجز وهمي / مخالفة",
    val blockedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "activation_otp_codes")
data class ActivationOtpCodeEntity(
    @PrimaryKey val code: String,
    val isUsed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val usedByName: String = "",
    val usedByPhone: String = "",
    val usedAtMillis: Long = 0L,
    val generatedBy: String = "ADMIN",
)

@Entity(tableName = "medical_labs")
data class MedicalLabEntity(
    @PrimaryKey val id: String,
    val labName: String,
    val phone: String = "",
    val locationLabel: String = "",
    val username: String,
    val password: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
