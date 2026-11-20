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

    @Query("SELECT * FROM patient_accounts ORDER BY fullName ASC")
    suspend fun getAllAccountsOnce(): List<PatientAccountEntity>

    @Query("SELECT * FROM patient_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): PatientAccountEntity?

    @Query("SELECT * FROM patient_accounts WHERE nationalId = :nationalId LIMIT 1")
    suspend fun findAccountByNationalId(nationalId: String): PatientAccountEntity?

    @Query("SELECT * FROM patient_accounts WHERE phone = :phone LIMIT 1")
    suspend fun findAccountByPhone(phone: String): PatientAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PatientAccountEntity)

    @Query("UPDATE patient_accounts SET isPhoneVerified = 1, verifiedBy = :verifiedBy, verifiedAtMillis = :verifiedAtMillis, otpUsed = 1 WHERE id = :accountId")
    suspend fun markAccountPhoneVerified(
        accountId: String,
        verifiedBy: String = "كود OTP",
        verifiedAtMillis: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE patient_accounts SET isPhoneVerified = :isVerified, verifiedBy = :verifiedBy, verifiedAtMillis = :verifiedAtMillis, otpUsed = :otpUsed WHERE id = :accountId")
    suspend fun setAccountVerificationStatus(
        accountId: String,
        isVerified: Boolean,
        verifiedBy: String,
        verifiedAtMillis: Long,
        otpUsed: Boolean = isVerified,
    )

    @Query("UPDATE patient_accounts SET otpCode = :otpCode, otpCreatedAtMillis = :createdAtMillis, otpExpiresAtMillis = :expiresAtMillis, otpUsed = 0 WHERE id = :accountId")
    suspend fun updateAccountOtp(
        accountId: String,
        otpCode: String,
        createdAtMillis: Long,
        expiresAtMillis: Long,
    )

    @Query("UPDATE patient_accounts SET fullName = :fullName, phone = :phone, password = :password WHERE id = :accountId")
    suspend fun updateAccountDetails(
        accountId: String,
        fullName: String,
        phone: String,
        password: String,
    )

    @Query("UPDATE family_members SET fullName = :fullName, phone = :phone WHERE accountId = :accountId AND relation LIKE 'نفسي%'")
    suspend fun updateSelfMemberDetails(accountId: String, fullName: String, phone: String)

    @Query("DELETE FROM family_members WHERE accountId = :accountId")
    suspend fun deleteFamilyMembersForAccount(accountId: String)

    @Query("DELETE FROM patient_accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Transaction
    suspend fun deleteAccountCascade(accountId: String) {
        deleteFamilyMembersForAccount(accountId)
        deleteAccountById(accountId)
    }

    @Query("SELECT * FROM family_members ORDER BY id ASC")
    fun observeAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE accountId = :accountId ORDER BY id ASC")
    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE accountId = :accountId ORDER BY id ASC")
    suspend fun getFamilyMembersOnce(accountId: String): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :memberId LIMIT 1")
    suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMembers(members: List<FamilyMemberEntity>)

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

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

    @Query("SELECT * FROM medical_records WHERE id = :recordId LIMIT 1")
    suspend fun getMedicalRecordById(recordId: String): MedicalRecordEntity?

    @Query("SELECT * FROM medical_records WHERE familyMemberId = :memberId OR (:patientName != '' AND patientName = :patientName) ORDER BY createdAtMillis DESC")
    suspend fun getMedicalRecordsForMemberOnce(memberId: String, patientName: String): List<MedicalRecordEntity>

    @Query("SELECT COUNT(*) FROM medical_records WHERE attachmentPath = :path")
    suspend fun countRecordsUsingAttachmentPath(path: String): Int

    @Query("UPDATE medical_records SET attachmentPath = '', attachmentMimeType = '', attachmentName = '' WHERE id = :recordId")
    suspend fun clearMedicalRecordAttachment(recordId: String)

    @Query("DELETE FROM medical_records WHERE id = :recordId")
    suspend fun deleteMedicalRecordById(recordId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecords(records: List<MedicalRecordEntity>)

    @Query("UPDATE clinic_doctors SET chatEnabled = :chatEnabled WHERE id = :doctorId")
    suspend fun updateDoctorChatEnabled(doctorId: String, chatEnabled: Boolean)

    @Query("SELECT * FROM delay_notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<DelayNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DelayNotificationEntity): Long

    @Query("UPDATE delay_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("UPDATE delay_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationReadById(notificationId: Long)

    @Query("SELECT * FROM direct_messages ORDER BY createdAtMillis ASC")
    fun observeAllDirectMessages(): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity)

    @Query(
        "UPDATE direct_messages SET isReadByPatient = 1 " +
            "WHERE threadType = :threadType AND patientAccountId = :patientAccountId AND doctorId = :doctorId"
    )
    suspend fun markThreadReadByPatient(threadType: String, patientAccountId: String, doctorId: String)

    @Query(
        "UPDATE direct_messages SET isReadByClinic = 1 " +
            "WHERE threadType = :threadType AND doctorId = :doctorId AND patientAccountId = :patientAccountId"
    )
    suspend fun markThreadReadByClinic(threadType: String, doctorId: String, patientAccountId: String)

    @Query(
        "UPDATE direct_messages SET isReadByAdmin = 1 " +
            "WHERE threadType = :threadType AND patientAccountId = :patientAccountId AND doctorId = :doctorId"
    )
    suspend fun markThreadReadByAdmin(threadType: String, patientAccountId: String, doctorId: String)

    @Query("UPDATE direct_messages SET isReadByPatient = 1 WHERE patientAccountId = :patientAccountId AND (threadType = 'PATIENT_CLINIC' OR threadType = 'PATIENT_ADMIN')")
    suspend fun markAllPatientMessagesRead(patientAccountId: String)

    @Query("UPDATE direct_messages SET isReadByClinic = 1 WHERE doctorId = :doctorId AND (threadType = 'PATIENT_CLINIC' OR threadType = 'CLINIC_ADMIN')")
    suspend fun markAllClinicMessagesRead(doctorId: String)

    @Query("UPDATE direct_messages SET isReadByAdmin = 1 WHERE threadType = 'PATIENT_ADMIN' OR threadType = 'CLINIC_ADMIN'")
    suspend fun markAllAdminMessagesRead()

    @Query("SELECT * FROM doctor_favorite_meds")
    fun observeAllDoctorFavorites(): Flow<List<DoctorFavoriteMedEntity>>

    @Query("SELECT COUNT(*) FROM doctor_favorite_meds WHERE doctorId = :doctorId AND medicationId = :medicationId")
    suspend fun isMedicationFavorite(doctorId: String, medicationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteMed(favorite: DoctorFavoriteMedEntity)

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

    @Query("SELECT * FROM activation_otp_codes ORDER BY createdAtMillis DESC")
    fun observeAllActivationCodes(): Flow<List<ActivationOtpCodeEntity>>

    @Query("SELECT * FROM activation_otp_codes")
    suspend fun getAllActivationCodesOnce(): List<ActivationOtpCodeEntity>

    @Query("SELECT * FROM activation_otp_codes WHERE code = :code LIMIT 1")
    suspend fun getActivationCode(code: String): ActivationOtpCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivationCodes(codes: List<ActivationOtpCodeEntity>)

    @Query(
        "UPDATE activation_otp_codes SET isUsed = 1, usedByName = :usedByName, usedByPhone = :usedByPhone, usedAtMillis = :usedAtMillis WHERE code = :code AND isUsed = 0"
    )
    suspend fun markActivationCodeUsed(
        code: String,
        usedByName: String,
        usedByPhone: String,
        usedAtMillis: Long,
    ): Int

    @Transaction
    suspend fun redeemActivationCodeForAccount(
        code: String,
        accountId: String,
        usedByName: String,
        usedByPhone: String,
        usedAtMillis: Long,
    ): Boolean {
        val updatedRows = markActivationCodeUsed(code, usedByName, usedByPhone, usedAtMillis)
        if (updatedRows <= 0) return false
        markAccountPhoneVerified(accountId, "كود OTP", usedAtMillis)
        return true
    }

    @Query("SELECT * FROM medical_labs ORDER BY labName ASC")
    fun observeLabs(): Flow<List<MedicalLabEntity>>

    @Query("SELECT * FROM medical_labs WHERE id = :labId LIMIT 1")
    suspend fun getLabById(labId: String): MedicalLabEntity?

    @Query("SELECT * FROM medical_labs WHERE username = :username LIMIT 1")
    suspend fun findLabByUsername(username: String): MedicalLabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLab(lab: MedicalLabEntity)

    @Update
    suspend fun updateLab(lab: MedicalLabEntity)

    @Query("DELETE FROM medical_labs WHERE id = :labId")
    suspend fun deleteLabById(labId: String)
}
