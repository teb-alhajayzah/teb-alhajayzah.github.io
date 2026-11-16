package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TebDao {

    @Query("SELECT COUNT(*) FROM clinic_doctors")
    suspend fun countDoctors(): Int

    @Query("SELECT * FROM patient_accounts")
    fun observeAllAccounts(): Flow<List<PatientAccountEntity>>

    @Query("SELECT * FROM patient_accounts WHERE nationalId = :nationalId LIMIT 1")
    suspend fun findAccountByNationalId(nationalId: String): PatientAccountEntity?

    @Query("SELECT * FROM patient_accounts WHERE phone = :phone LIMIT 1")
    suspend fun findAccountByPhone(phone: String): PatientAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PatientAccountEntity)

    @Query("UPDATE patient_accounts SET isPhoneVerified = 1 WHERE id = :accountId")
    suspend fun markAccountPhoneVerified(accountId: String)

    @Query("SELECT * FROM family_members WHERE accountId = :accountId ORDER BY id ASC")
    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMembers(members: List<FamilyMemberEntity>)

    @Query("SELECT * FROM clinic_doctors ORDER BY clinicType ASC, doctorName ASC")
    fun observeDoctors(): Flow<List<ClinicDoctorEntity>>

    @Query("SELECT * FROM clinic_doctors WHERE id = :doctorId LIMIT 1")
    suspend fun getDoctorById(doctorId: String): ClinicDoctorEntity?

    @Query("SELECT * FROM clinic_doctors WHERE username = :username LIMIT 1")
    suspend fun findDoctorByUsername(username: String): ClinicDoctorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: ClinicDoctorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<ClinicDoctorEntity>)

    @Update
    suspend fun updateDoctor(doctor: ClinicDoctorEntity)

    @Query("DELETE FROM clinic_doctors WHERE id = :doctorId")
    suspend fun deleteDoctorById(doctorId: String)

    @Query("DELETE FROM doctor_favorite_meds WHERE doctorId = :doctorId")
    suspend fun deleteFavoritesForDoctor(doctorId: String)

    @Query("UPDATE clinic_doctors SET currentDelayMinutes = currentDelayMinutes + :additionalDelay WHERE id = :doctorId")
    suspend fun addDoctorDelay(doctorId: String, additionalDelay: Int)

    @Query("UPDATE clinic_doctors SET workStartMinutes = :startMin, workEndMinutes = :endMin, slotDurationMinutes = :durationMin, currentDelayMinutes = 0 WHERE id = :doctorId")
    suspend fun updateDoctorScheduleConfig(doctorId: String, startMin: Int, endMin: Int, durationMin: Int)

    @Query("SELECT * FROM appointment_slots ORDER BY startMinutes ASC")
    fun observeAllSlots(): Flow<List<AppointmentSlotEntity>>

    @Query("SELECT * FROM appointment_slots WHERE doctorId = :doctorId ORDER BY startMinutes ASC")
    fun observeSlotsForDoctor(doctorId: String): Flow<List<AppointmentSlotEntity>>

    @Query("SELECT * FROM appointment_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: String): AppointmentSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<AppointmentSlotEntity>)

    @Update
    suspend fun updateSlot(slot: AppointmentSlotEntity)

    @Query("DELETE FROM appointment_slots WHERE doctorId = :doctorId")
    suspend fun deleteSlotsForDoctor(doctorId: String)

    @Query("UPDATE appointment_slots SET delayMinutes = delayMinutes + :additionalDelay WHERE doctorId = :doctorId AND status != :completedStatus")
    suspend fun shiftUpcomingSlotsDelay(
        doctorId: String,
        additionalDelay: Int,
        completedStatus: String = SlotStatus.COMPLETED,
    )

    @Transaction
    suspend fun replaceDoctorSchedule(
        doctorId: String,
        startMin: Int,
        endMin: Int,
        durationMin: Int,
        newSlots: List<AppointmentSlotEntity>,
    ) {
        updateDoctorScheduleConfig(doctorId, startMin, endMin, durationMin)
        deleteSlotsForDoctor(doctorId)
        insertSlots(newSlots)
    }

    @Transaction
    suspend fun deleteDoctorCascade(doctorId: String) {
        deleteSlotsForDoctor(doctorId)
        deleteFavoritesForDoctor(doctorId)
        deleteDoctorById(doctorId)
    }

    @Query("SELECT * FROM medical_records ORDER BY createdAtMillis DESC")
    fun observeAllMedicalRecords(): Flow<List<MedicalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecords(records: List<MedicalRecordEntity>)

    @Query("SELECT * FROM delay_notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<DelayNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DelayNotificationEntity)

    @Query("UPDATE delay_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("SELECT * FROM doctor_favorite_meds")
    fun observeAllDoctorFavorites(): Flow<List<DoctorFavoriteMedEntity>>

    @Query("SELECT COUNT(*) FROM doctor_favorite_meds WHERE doctorId = :doctorId AND medicationId = :medicationId")
    suspend fun isMedicationFavorite(doctorId: String, medicationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteMed(fav: DoctorFavoriteMedEntity)

    @Query("DELETE FROM doctor_favorite_meds WHERE doctorId = :doctorId AND medicationId = :medicationId")
    suspend fun deleteFavoriteMed(doctorId: String, medicationId: String)

    @Query("SELECT * FROM blocked_phones ORDER BY blockedAtMillis DESC")
    fun observeBlockedPhones(): Flow<List<BlockedPhoneEntity>>

    @Query("SELECT COUNT(*) FROM blocked_phones WHERE phone = :phone")
    suspend fun isPhoneBlocked(phone: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedPhone(blocked: BlockedPhoneEntity)

    @Query("DELETE FROM blocked_phones WHERE phone = :phone")
    suspend fun unblockPhone(phone: String)
}
