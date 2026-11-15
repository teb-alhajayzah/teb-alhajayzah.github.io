package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.config.AppConfig
import com.example.data.local.AppointmentSlotEntity
import com.example.data.local.BookingSource
import com.example.data.local.ClinicDoctorEntity
import com.example.data.local.ClinicType
import com.example.data.local.DelayNotificationEntity
import com.example.data.local.DoctorFavoriteMedEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedicalRecordEntity
import com.example.data.local.MedicalRecordType
import com.example.data.local.PatientAccountEntity
import com.example.data.local.SlotStatus
import com.example.data.local.TebDao
import com.example.data.medications.EgyptianMedicationService
import com.example.data.medications.MedicationFetchResult
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private val Context.tebDataStore by preferencesDataStore(name = "teb_prefs")

class TebRepository(
    private val dao: TebDao,
    private val appContext: Context,
) {
    private val seedMutex = Mutex()

    private val activeAccountKey = stringPreferencesKey("active_account_id")
    private val activeMemberKey = stringPreferencesKey("active_member_id")
    private val activeDoctorPortalKey = stringPreferencesKey("active_doctor_portal_id")

    val activeAccountIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeAccountKey] ?: DEFAULT_ACCOUNT_ID }

    val activeMemberIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeMemberKey] ?: DEFAULT_MEMBER_SELF_ID }

    val activeDoctorPortalIdFlow: Flow<String> = appContext.tebDataStore.data
        .map { prefs -> prefs[activeDoctorPortalKey] ?: "doc_batna_1" }

    val doctorsFlow: Flow<List<ClinicDoctorEntity>> = dao.observeDoctors()
    val allSlotsFlow: Flow<List<AppointmentSlotEntity>> = dao.observeAllSlots()
    val allAccountsFlow: Flow<List<PatientAccountEntity>> = dao.observeAllAccounts()
    val allMedicalRecordsFlow: Flow<List<MedicalRecordEntity>> = dao.observeAllMedicalRecords()
    val notificationsFlow: Flow<List<DelayNotificationEntity>> = dao.observeNotifications()
    val allDoctorFavoritesFlow: Flow<List<DoctorFavoriteMedEntity>> = dao.observeAllDoctorFavorites()

    fun observeFamilyMembers(accountId: String): Flow<List<FamilyMemberEntity>> =
        dao.observeFamilyMembers(accountId)

    fun observeSlotsForDoctor(doctorId: String): Flow<List<AppointmentSlotEntity>> =
        dao.observeSlotsForDoctor(doctorId)

    suspend fun setActiveMember(memberId: String) {
        appContext.tebDataStore.edit { it[activeMemberKey] = memberId }
    }

    suspend fun setActiveDoctorPortal(doctorId: String) {
        appContext.tebDataStore.edit { it[activeDoctorPortalKey] = doctorId }
    }

    suspend fun toggleFavoriteMedication(doctorId: String, medicationId: String) {
        if (dao.isMedicationFavorite(doctorId, medicationId) > 0) {
            dao.deleteFavoriteMed(doctorId, medicationId)
        } else {
            dao.insertFavoriteMed(DoctorFavoriteMedEntity(doctorId, medicationId))
        }
    }

    suspend fun loadEgyptianMedications(apiUrl: String): MedicationFetchResult {
        return EgyptianMedicationService.fetchMedications(apiUrl)
    }

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (dao.countDoctors() > 0) return

            val defaultAccount = PatientAccountEntity(
                id = DEFAULT_ACCOUNT_ID,
                fullName = "أحمد محمد السيد",
                phone = "01012345678",
                nationalId = "29005141200451",
                password = "1234",
            )
            dao.insertAccount(defaultAccount)

            val familyMembers = listOf(
                FamilyMemberEntity(
                    id = DEFAULT_MEMBER_SELF_ID,
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "أحمد محمد السيد",
                    relation = "نفسي (صاحب الحساب)",
                    age = 36,
                    phone = "01012345678",
                ),
                FamilyMemberEntity(
                    id = "mem_mother",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "سعاد حسن عبد الله",
                    relation = "الأم",
                    age = 62,
                    phone = "01012345678",
                ),
                FamilyMemberEntity(
                    id = "mem_wife",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "فاطمة علي محمود",
                    relation = "الزوجة",
                    age = 31,
                    phone = "01098765432",
                ),
                FamilyMemberEntity(
                    id = "mem_son",
                    accountId = DEFAULT_ACCOUNT_ID,
                    fullName = "كريم أحمد محمد",
                    relation = "الابن",
                    age = 9,
                    phone = "01012345678",
                ),
            )
            dao.insertFamilyMembers(familyMembers)

            val hospitalName = AppConfig.defaultBrand.hospitalNameAr
            val doctors = listOf(
                ClinicDoctorEntity(
                    id = "doc_batna_1",
                    doctorName = "د. محمد عبد الرحمن",
                    specialty = "باطنة وجهاز هضمي",
                    clinicName = "عيادة الشفاء التخصصية",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "شارع المستشفى، الحجايزة",
                    feeEgp = 150,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 21 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 15,
                ),
                ClinicDoctorEntity(
                    id = "doc_kids_1",
                    doctorName = "د. سارة محمود النجار",
                    specialty = "أطفال وحديثي الولادة",
                    clinicName = "عيادة براعم الحجايزة",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "بجوار المسجد الكبير، الحجايزة",
                    feeEgp = 130,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 11,
                ),
                ClinicDoctorEntity(
                    id = "doc_ortho_1",
                    doctorName = "د. طارق حسن الشافعي",
                    specialty = "عظام ومفاصل",
                    clinicName = "$hospitalName - قسم العظام",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأول",
                    feeEgp = 100,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 21 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 14,
                ),
                ClinicDoctorEntity(
                    id = "doc_obgyn_1",
                    doctorName = "د. نادية إبراهيم زكي",
                    specialty = "نساء وتوليد",
                    clinicName = "$hospitalName - قسم النساء",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الثاني",
                    feeEgp = 120,
                    workStartMinutes = 18 * 60,
                    workEndMinutes = 21 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 16,
                ),
                ClinicDoctorEntity(
                    id = "doc_dental_1",
                    doctorName = "د. كريم طارق المنصوري",
                    specialty = "أسنان",
                    clinicName = "عيادة الابتسامة للأسنان",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "أمام الوحدة المحلية، الحجايزة",
                    feeEgp = 150,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.9f,
                    experienceYears = 10,
                ),
                ClinicDoctorEntity(
                    id = "doc_cardio_1",
                    doctorName = "د. خالد مصطفى العطار",
                    specialty = "قلب وأوعية دموية",
                    clinicName = "$hospitalName - وحدة القلب",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأرضي",
                    feeEgp = 160,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 20 * 60,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 18,
                ),
                ClinicDoctorEntity(
                    id = "doc_eye_1",
                    doctorName = "د. هالة فؤاد سلامة",
                    specialty = "رمد وجراحة عيون",
                    clinicName = "مركز النور للعيون",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "ميدان الحجايزة الرئيسي",
                    feeEgp = 140,
                    workStartMinutes = 17 * 60 + 20,
                    workEndMinutes = 20 * 60 + 40,
                    slotDurationMinutes = 20,
                    rating = 4.7f,
                    experienceYears = 10,
                ),
                ClinicDoctorEntity(
                    id = "doc_ent_1",
                    doctorName = "د. عمرو يحيى البنا",
                    specialty = "أنف وأذن وحنجرة",
                    clinicName = "$hospitalName - عيادة 4",
                    clinicType = ClinicType.HOSPITAL,
                    locationLabel = "$hospitalName - الدور الأول",
                    feeEgp = 100,
                    workStartMinutes = 16 * 60,
                    workEndMinutes = 19 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.7f,
                    experienceYears = 9,
                ),
                ClinicDoctorEntity(
                    id = "doc_derm_1",
                    doctorName = "د. ريم عادل الدسوقي",
                    specialty = "جلدية وتجميل",
                    clinicName = "عيادة الندى للجلدية",
                    clinicType = ClinicType.PRIVATE,
                    locationLabel = "طريق المركز، الحجايزة",
                    feeEgp = 120,
                    workStartMinutes = 17 * 60,
                    workEndMinutes = 20 * 60 + 20,
                    slotDurationMinutes = 20,
                    rating = 4.8f,
                    experienceYears = 8,
                ),
            )
            dao.insertDoctors(doctors)

            // Seed default doctor favorites
            listOf(
                DoctorFavoriteMedEntity("doc_batna_1", "med_controloc"),
                DoctorFavoriteMedEntity("doc_batna_1", "med_gaviscon"),
                DoctorFavoriteMedEntity("doc_batna_1", "med_motilium"),
                DoctorFavoriteMedEntity("doc_kids_1", "med_augmentin_ped"),
                DoctorFavoriteMedEntity("doc_kids_1", "med_brufen_ped"),
                DoctorFavoriteMedEntity("doc_dental_1", "med_augmentin_1g"),
                DoctorFavoriteMedEntity("doc_dental_1", "med_catafast"),
            ).forEach { dao.insertFavoriteMed(it) }

            val allSlots = mutableListOf<AppointmentSlotEntity>()
            doctors.forEach { doc ->
                allSlots += buildSlotsForDoctor(
                    doctor = doc,
                    preBookedIndices = when (doc.id) {
                        "doc_batna_1" -> mapOf(
                            0 to Triple("محمود إبراهيم علي", "01022334455", SlotStatus.COMPLETED),
                            1 to Triple("أحمد محمد السيد", "01012345678", SlotStatus.BOOKED),
                            4 to Triple("سيد عبد الفتاح", "01144556677", SlotStatus.BOOKED),
                        )
                        "doc_kids_1" -> mapOf(
                            2 to Triple("كريم أحمد محمد", "01012345678", SlotStatus.BOOKED),
                        )
                        "doc_ortho_1" -> mapOf(
                            1 to Triple("سعاد حسن عبد الله", "01012345678", SlotStatus.BOOKED),
                            3 to Triple("حسن عبد القادر", "01288990011", SlotStatus.BOOKED),
                        )
                        else -> mapOf(
                            1 to Triple("علاء فتحي سالم", "01077889900", SlotStatus.BOOKED),
                        )
                    },
                )
            }
            dao.insertSlots(allSlots)

            val now = System.currentTimeMillis()
            val sampleRecords = listOf(
                MedicalRecordEntity(
                    id = "rec_1",
                    familyMemberId = DEFAULT_MEMBER_SELF_ID,
                    patientName = "أحمد محمد السيد",
                    patientPhone = "01012345678",
                    doctorId = "doc_batna_1",
                    doctorName = "د. محمد عبد الرحمن",
                    specialty = "باطنة وجهاز هضمي",
                    visitDateLabel = "10 نوفمبر 2026",
                    diagnosis = "التهاب بسيط بالمعدة وارتجاع مريئي",
                    medications = "• كونترولوك (Controloc) 40 مجم — قرص قبل الفطار بنصف ساعة\n• جافيسكون (Gaviscon) — ملعقة كبيرة بعد الأكل وعند اللزوم\n• موتيليوم (Motilium) 10 مجم — قرص قبل الغداء والعشاء",
                    notes = "الابتعاد عن الأطعمة الدسمة والمشروبات الغازية لمدة أسبوعين.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 5 * 86_400_000L,
                ),
                MedicalRecordEntity(
                    id = "rec_2",
                    familyMemberId = "mem_mother",
                    patientName = "سعاد حسن عبد الله",
                    patientPhone = "01012345678",
                    doctorId = "doc_cardio_1",
                    doctorName = "د. خالد مصطفى العطار",
                    specialty = "قلب وأوعية دموية",
                    visitDateLabel = "3 نوفمبر 2026",
                    diagnosis = "متابعة ضغط الدم المرتفع (مستقر)",
                    medications = "• كونكور (Concor) 5 مجم — قرص صباحاً يومياً\n• أسبوسيد (Aspocid) 75 مجم — قرص بعد الغداء",
                    notes = "قياس الضغط مرتين أسبوعياً وتقليل الملح في الطعام.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 12 * 86_400_000L,
                ),
                MedicalRecordEntity(
                    id = "rec_3",
                    familyMemberId = "mem_son",
                    patientName = "كريم أحمد محمد",
                    patientPhone = "01012345678",
                    doctorId = "doc_kids_1",
                    doctorName = "د. سارة محمود النجار",
                    specialty = "أطفال وحديثي الولادة",
                    visitDateLabel = "28 أكتوبر 2026",
                    diagnosis = "نزلة شعبية حادة مع ارتفاع طفيف في الحرارة",
                    medications = "• أوجمنتين شراب (Augmentin) 457 مجم/5 مل — 5 سم كل 12 ساعة لمدة 7 أيام\n• بروفين شراب (Brufen) 100 مجم/5 مل — عند اللزوم لخفض الحرارة\n• فاركولين محلول (Farcolin) — جلسة بخار صباحاً ومساءً",
                    notes = "شرب سوائل دافئة والراحة التامة.",
                    recordType = MedicalRecordType.PRESCRIPTION,
                    createdAtMillis = now - 18 * 86_400_000L,
                ),
            )
            dao.insertMedicalRecords(sampleRecords)
        }
    }

    private fun buildSlotsForDoctor(
        doctor: ClinicDoctorEntity,
        preBookedIndices: Map<Int, Triple<String, String, String>> = emptyMap(),
    ): List<AppointmentSlotEntity> {
        val list = mutableListOf<AppointmentSlotEntity>()
        var current = doctor.workStartMinutes
        var index = 0
        while (current < doctor.workEndMinutes) {
            val pre = preBookedIndices[index]
            val familyMemberId = when (pre?.first) {
                "أحمد محمد السيد" -> DEFAULT_MEMBER_SELF_ID
                "سعاد حسن عبد الله" -> "mem_mother"
                "فاطمة علي محمود" -> "mem_wife"
                "كريم أحمد محمد" -> "mem_son"
                else -> ""
            }
            list += AppointmentSlotEntity(
                id = "${doctor.id}_today_$index",
                doctorId = doctor.id,
                dateLabel = "اليوم",
                startMinutes = current,
                delayMinutes = 0,
                status = pre?.third ?: SlotStatus.AVAILABLE,
                patientName = pre?.first ?: "",
                patientPhone = pre?.second ?: "",
                familyMemberId = familyMemberId,
                bookingSource = if (pre != null) BookingSource.APP else BookingSource.NONE,
                bookedAtMillis = if (pre != null) System.currentTimeMillis() else 0L,
            )
            current += doctor.slotDurationMinutes
            index++
        }
        return list
    }

    suspend fun bookSlot(
        slotId: String,
        patientName: String,
        patientPhone: String,
        familyMemberId: String,
        source: String,
    ): Result<Unit> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الموعد غير موجود"))
        if (slot.status != SlotStatus.AVAILABLE) {
            return Result.failure(IllegalStateException("عذراً، تم حجز هذا الموعد للتو"))
        }
        dao.updateSlot(
            slot.copy(
                status = SlotStatus.BOOKED,
                patientName = patientName.trim(),
                patientPhone = patientPhone.trim(),
                familyMemberId = familyMemberId,
                bookingSource = source,
                bookedAtMillis = System.currentTimeMillis(),
            )
        )
        return Result.success(Unit)
    }

    suspend fun cancelSlot(slotId: String) {
        val slot = dao.getSlotById(slotId) ?: return
        if (slot.status == SlotStatus.BOOKED) {
            dao.updateSlot(
                slot.copy(
                    status = SlotStatus.AVAILABLE,
                    patientName = "",
                    patientPhone = "",
                    familyMemberId = "",
                    bookingSource = BookingSource.NONE,
                    bookedAtMillis = 0L,
                )
            )
        }
    }

    suspend fun generateDoctorSchedule(
        doctorId: String,
        startMinutes: Int,
        endMinutes: Int,
        durationMinutes: Int,
    ): Result<Int> {
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        if (endMinutes <= startMinutes) {
            return Result.failure(IllegalArgumentException("وقت النهاية يجب أن يكون بعد وقت البداية"))
        }
        val updatedDoctor = doctor.copy(
            workStartMinutes = startMinutes,
            workEndMinutes = endMinutes,
            slotDurationMinutes = durationMinutes,
            currentDelayMinutes = 0,
        )
        val newSlots = buildSlotsForDoctor(updatedDoctor)
        dao.replaceDoctorSchedule(
            doctorId = doctorId,
            startMin = startMinutes,
            endMin = endMinutes,
            durationMin = durationMinutes,
            newSlots = newSlots,
        )
        return Result.success(newSlots.size)
    }

    suspend fun delayDoctorAppointments(doctorId: String, additionalMinutes: Int): Result<Unit> {
        val doctor = dao.getDoctorById(doctorId)
            ?: return Result.failure(IllegalStateException("العيادة غير موجودة"))
        dao.addDoctorDelay(doctorId, additionalMinutes)
        dao.shiftUpcomingSlotsDelay(doctorId, additionalMinutes)
        val newTotalDelay = doctor.currentDelayMinutes + additionalMinutes
        dao.insertNotification(
            DelayNotificationEntity(
                doctorId = doctorId,
                doctorName = doctor.doctorName,
                clinicName = doctor.clinicName,
                delayMinutes = additionalMinutes,
                message = "تنبيه عاجل: تم تأخير مواعيد ${doctor.doctorName} (${doctor.clinicName}) لمدة $additionalMinutes دقيقة (إجمالي التأخير: $newTotalDelay دقيقة). تم تحديث موعدك تلقائياً.",
            )
        )
        return Result.success(Unit)
    }

    suspend fun completeVisitAndSavePrescription(
        slotId: String,
        diagnosis: String,
        medications: String,
        notes: String,
        attachmentPath: String,
    ): Result<MedicalRecordEntity> {
        val slot = dao.getSlotById(slotId)
            ?: return Result.failure(IllegalStateException("الحجز غير موجود"))
        val doctor = dao.getDoctorById(slot.doctorId)
            ?: return Result.failure(IllegalStateException("الطبيب غير موجود"))

        dao.updateSlot(slot.copy(status = SlotStatus.COMPLETED))
        val record = MedicalRecordEntity(
            id = "rec_${UUID.randomUUID()}",
            familyMemberId = slot.familyMemberId,
            patientName = slot.patientName,
            patientPhone = slot.patientPhone,
            doctorId = doctor.id,
            doctorName = doctor.doctorName,
            specialty = doctor.specialty,
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = diagnosis.trim(),
            medications = medications.trim(),
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            recordType = MedicalRecordType.PRESCRIPTION,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
        return Result.success(record)
    }

    suspend fun addPatientUploadedAttachment(
        familyMemberId: String,
        patientName: String,
        patientPhone: String,
        title: String,
        notes: String,
        attachmentPath: String,
    ) {
        val record = MedicalRecordEntity(
            id = "upload_${UUID.randomUUID()}",
            familyMemberId = familyMemberId,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = "",
            doctorName = "رفع شخصي من المريض",
            specialty = "تحاليل وأشعة",
            visitDateLabel = TimeUtils.formatTodayLabelAr(),
            diagnosis = title.trim().ifEmpty { "صورة تحليل / أشعة" },
            medications = "",
            notes = notes.trim(),
            attachmentPath = attachmentPath,
            recordType = MedicalRecordType.PATIENT_UPLOAD,
            createdAtMillis = System.currentTimeMillis(),
        )
        dao.insertMedicalRecord(record)
    }

    suspend fun addFamilyMember(
        accountId: String,
        fullName: String,
        relation: String,
        age: Int,
        phone: String,
    ): FamilyMemberEntity {
        val newMember = FamilyMemberEntity(
            id = "mem_${UUID.randomUUID()}",
            accountId = accountId,
            fullName = fullName.trim(),
            relation = relation.trim(),
            age = age,
            phone = phone.trim(),
        )
        dao.insertFamilyMember(newMember)
        setActiveMember(newMember.id)
        return newMember
    }

    suspend fun loginPatient(identifier: String, password: String): Result<PatientAccountEntity> {
        val cleanId = identifier.trim()
        val cleanPass = password.trim()
        val byPhone = dao.findAccountByPhone(cleanId)
        val byNationalId = if (byPhone == null) dao.findAccountByNationalId(cleanId) else null
        val account = byPhone ?: byNationalId
            ?: return Result.failure(IllegalArgumentException("لم يتم العثور على حساب بهذا الرقم"))
        if (account.password != cleanPass && cleanPass.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("كلمة المرور غير صحيحة"))
        }
        val members = dao.observeFamilyMembers(account.id).first()
        appContext.tebDataStore.edit { prefs ->
            prefs[activeAccountKey] = account.id
            prefs[activeMemberKey] = members.firstOrNull()?.id ?: account.id
        }
        return Result.success(account)
    }

    suspend fun registerPatientAccount(
        fullName: String,
        phone: String,
        nationalId: String,
        password: String,
    ): Result<PatientAccountEntity> {
        val existing = dao.findAccountByPhone(phone.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("رقم الهاتف مسجل بالفعل"))
        }
        val accountId = "acc_${UUID.randomUUID()}"
        val account = PatientAccountEntity(
            id = accountId,
            fullName = fullName.trim(),
            phone = phone.trim(),
            nationalId = nationalId.trim(),
            password = password.trim().ifEmpty { "1234" },
        )
        dao.insertAccount(account)
        val selfMemberId = "mem_${UUID.randomUUID()}"
        dao.insertFamilyMember(
            FamilyMemberEntity(
                id = selfMemberId,
                accountId = accountId,
                fullName = account.fullName,
                relation = "نفسي (صاحب الحساب)",
                age = 30,
                phone = account.phone,
            )
        )
        appContext.tebDataStore.edit { prefs ->
            prefs[activeAccountKey] = accountId
            prefs[activeMemberKey] = selfMemberId
        }
        return Result.success(account)
    }

    suspend fun markNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    companion object {
        const val DEFAULT_ACCOUNT_ID = "acc_ahmed"
        const val DEFAULT_MEMBER_SELF_ID = "mem_self_ahmed"
    }
}
