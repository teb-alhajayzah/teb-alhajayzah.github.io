package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.config.AppConfig
import com.example.data.local.BookingSource
import com.example.data.local.ClinicType
import com.example.auth.FirebaseReadyTestOtpProvider
import com.example.data.local.SlotStatus
import com.example.data.local.TebDatabase
import com.example.data.medications.MedicationCatalog
import com.example.data.repository.TebRepository
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    private lateinit var db: TebDatabase
    private lateinit var repository: TebRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TebDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TebRepository(db.tebDao(), context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appNameMatchesArabicBrand() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("طب الحجايزة", context.getString(R.string.app_name))
        assertEquals("v1.0.0", AppConfig.defaultBrand.appVersionLabel)
        assertTrue(AppConfig.defaultBrand.apkDownloadUrl.endsWith("teb-alhajayzah.apk"))
        assertEquals("01125002428", AppConfig.defaultBrand.supportPhone)
        assertEquals("01141271027", AppConfig.defaultBrand.otpWhatsappPhone)
    }

    @Test
    fun realTimeBookingPreventsDoubleBookingAndDelayShiftsSlots() = runBlocking {
        repository.ensureSeeded()

        val slots = repository.observeSlotsForDoctor("doc_batna_1").first()
        val firstAvailable = slots.first { it.status == SlotStatus.AVAILABLE }

        val firstBook = repository.bookSlot(
            slotId = firstAvailable.id,
            patientName = "فاطمة علي محمود",
            patientPhone = "01098765432",
            familyMemberId = "mem_wife",
            source = BookingSource.APP,
        )
        assertTrue(firstBook.isSuccess)

        val secondBook = repository.bookSlot(
            slotId = firstAvailable.id,
            patientName = "مريض آخر",
            patientPhone = "01000000000",
            familyMemberId = "",
            source = BookingSource.PHONE_WALKIN,
        )
        assertTrue(secondBook.isFailure)

        val originalStart = firstAvailable.startMinutes
        repository.delayDoctorAppointments("doc_batna_1", 15)

        val shiftedSlot = repository.observeSlotsForDoctor("doc_batna_1").first()
            .first { it.id == firstAvailable.id }
        assertEquals(15, shiftedSlot.delayMinutes)
        assertEquals(originalStart + 15, shiftedSlot.effectiveMinutes)

        val rxResult = repository.completeVisitAndSavePrescription(
            slotId = firstAvailable.id,
            diagnosis = "نزلة معوية بسيطة",
            medications = "• أنتينال (Antinal) 200 مجم — كبسولة كل 6 ساعات لمدة 5 أيام",
            notes = "شرب سوائل بكثرة",
            attachmentPath = "",
        )
        assertTrue(rxResult.isSuccess)

        val records = repository.allMedicalRecordsFlow.first()
        assertTrue(records.any { it.patientName == "فاطمة علي محمود" && it.diagnosis.contains("نزلة معوية") })
    }

    @Test
    fun egyptianMedicationsCatalogAndFavoritesPersistPerDoctor() = runBlocking {
        repository.ensureSeeded()

        val meds = MedicationCatalog.builtInMedications
        assertTrue(meds.any { it.specialty == "أسنان" && it.tradeName.contains("كتافاست") })
        assertTrue(meds.any { it.specialty == "نساء وتوليد" && it.activeIngredient.contains("حمض الفوليك") })
        assertTrue(meds.any { it.specialty == "أطفال وحديثي الولادة" })
        assertTrue(meds.any { it.specialty == "عظام ومفاصل" })

        val doctorId = "doc_dental_1"
        val targetMedId = "med_dalacin_c"
        val initialFavs = repository.allDoctorFavoritesFlow.first()
            .filter { it.doctorId == doctorId }
            .map { it.medicationId }
            .toSet()
        assertFalse(targetMedId in initialFavs)

        repository.toggleFavoriteMedication(doctorId, targetMedId)
        val afterAdd = repository.allDoctorFavoritesFlow.first()
            .filter { it.doctorId == doctorId }
            .map { it.medicationId }
            .toSet()
        assertTrue(targetMedId in afterAdd)

        repository.toggleFavoriteMedication(doctorId, targetMedId)
        val afterRemove = repository.allDoctorFavoritesFlow.first()
            .filter { it.doctorId == doctorId }
            .map { it.medicationId }
            .toSet()
        assertFalse(targetMedId in afterRemove)
    }

    @Test
    fun arabicTimeFormatterFormatsEveningSlotsCorrectly() {
        assertEquals("6:00 م", TimeUtils.formatMinutesAr(18 * 60))
        assertEquals("6:20 م", TimeUtils.formatMinutesAr(18 * 60 + 20))
        assertEquals("9:00 ص", TimeUtils.formatMinutesAr(9 * 60))
    }

    @Test
    fun superAdminDoctorCrudAndIndependentDoctorLoginWorkEndToEnd() = runBlocking {
        repository.ensureSeeded()

        val badLogin = repository.loginDoctor("01001001001", "wrongpass")
        assertTrue(badLogin.isFailure)

        val validSeededLogin = repository.loginDoctor("01001001001", "1234")
        assertTrue(validSeededLogin.isSuccess)
        assertEquals("doc_batna_1", repository.loggedInDoctorIdFlow.first())

        repository.logoutDoctor()
        assertEquals(null, repository.loggedInDoctorIdFlow.first())

        val addedRes = repository.addDoctorByAdmin(
            doctorName = "د. كريم عادل سليم",
            specialty = "الأنف والأذن والحنجرة",
            clinicName = "عيادة الشفاء التخصصية",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة - بجوار الوحدة الصحية",
            bio = "استشاري جراحة الأنف والأذن والحنجرة",
            mapsUrl = "https://maps.google.com/?q=30.892,31.592",
            feeEgp = 180,
            workStartMinutes = 17 * 60,
            workEndMinutes = 19 * 60,
            slotDurationMinutes = 20,
            username = "01055566677",
            password = "docpass99",
        )
        assertTrue(addedRes.isSuccess)
        val createdDoc = addedRes.getOrThrow()

        val allDocs = repository.doctorsFlow.first()
        assertTrue(allDocs.any { it.id == createdDoc.id && it.mapsUrl.contains("maps.google.com") })
        val generatedSlots = repository.observeSlotsForDoctor(createdDoc.id).first()
        assertEquals(6, generatedSlots.size)

        val newDocLogin = repository.loginDoctor("01055566677", "docpass99")
        assertTrue(newDocLogin.isSuccess)
        assertEquals(createdDoc.id, repository.loggedInDoctorIdFlow.first())

        val updateRes = repository.updateDoctorByAdmin(
            doctorId = createdDoc.id,
            doctorName = "د. كريم عادل سليم",
            specialty = "الأنف والأذن والحنجرة",
            clinicName = "عيادة الشفاء المطورة",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة - الطريق الرئيسي",
            bio = "استشاري أول الأنف والأذن",
            mapsUrl = "https://maps.google.com/?q=30.893,31.593",
            feeEgp = 200,
            workStartMinutes = 18 * 60,
            workEndMinutes = 20 * 60,
            slotDurationMinutes = 30,
            username = "dr_karim",
            password = "newpass88",
        )
        assertTrue(updateRes.isSuccess)
        assertEquals(4, repository.observeSlotsForDoctor(createdDoc.id).first().size)

        val updatedLogin = repository.loginDoctor("dr_karim", "newpass88")
        assertTrue(updatedLogin.isSuccess)

        repository.deleteDoctorByAdmin(createdDoc.id)
        assertFalse(repository.doctorsFlow.first().any { it.id == createdDoc.id })
        assertTrue(repository.observeSlotsForDoctor(createdDoc.id).first().isEmpty())
        assertEquals(null, repository.loggedInDoctorIdFlow.first())

        repository.updateAdminCredentials("eng_mohamed", "admin2026")
        assertEquals("eng_mohamed", repository.adminUsernameFlow.first())
        assertEquals("admin2026", repository.adminPasswordFlow.first())
    }

    @Test
    fun singleUseActivationOtpCodesAndAdminPinLockoutAndPhoneBlockingWork() = runBlocking {
        repository.ensureSeeded()

        val otpProvider = FirebaseReadyTestOtpProvider()
        assertEquals("+201012345678", otpProvider.formatEgyptianE164("01012345678"))

        val regRes = repository.registerPatientAccount(
            fullName = "محمود حسن علي",
            phone = "01099887766",
            nationalId = "29501011200123",
            password = "pass123",
        )
        assertTrue(regRes.isSuccess)
        val createdAcc = regRes.getOrThrow()
        assertFalse(createdAcc.isPhoneVerified)

        val gen10Res = repository.generateActivationCodeBatch(10, "ADMIN")
        assertTrue(gen10Res.isSuccess)
        assertEquals(10, gen10Res.getOrThrow())

        val gen20Res = repository.generateActivationCodeBatch(20, "د. محمد عبد الرحمن")
        assertTrue(gen20Res.isSuccess)
        assertEquals(20, gen20Res.getOrThrow())

        val allCodes = repository.activationCodesFlow.first()
        assertEquals(35, allCodes.size) // 5 seeded + 10 + 20
        assertEquals(35, allCodes.map { it.code }.toSet().size)

        val pickedCode = allCodes.first { !it.isUsed }.code
        assertEquals(6, pickedCode.length)

        // Wrong code must fail
        assertTrue(
            repository.verifyAndBurnActivationCode(
                accountId = createdAcc.id,
                accountName = createdAcc.fullName,
                accountPhone = createdAcc.phone,
                enteredCode = "000000",
            ).isFailure
        )

        // Valid unused code activates account and burns the code permanently
        val verifyRes = repository.verifyAndBurnActivationCode(
            accountId = createdAcc.id,
            accountName = createdAcc.fullName,
            accountPhone = createdAcc.phone,
            enteredCode = pickedCode,
        )
        assertTrue(verifyRes.isSuccess)

        val updatedAccount = repository.allAccountsFlow.first().first { it.id == createdAcc.id }
        assertTrue(updatedAccount.isPhoneVerified)

        val burnedEntity = repository.activationCodesFlow.first().first { it.code == pickedCode }
        assertTrue(burnedEntity.isUsed)
        assertEquals("محمود حسن علي", burnedEntity.usedByName)
        assertEquals("01099887766", burnedEntity.usedByPhone)

        // Second account attempting to reuse the burned code must be rejected
        val secondAccRes = repository.registerPatientAccount(
            fullName = "حساب ثاني",
            phone = "01088776655",
            nationalId = "",
            password = "pass",
        )
        assertTrue(secondAccRes.isSuccess)
        val secondAcc = secondAccRes.getOrThrow()
        val reuseRes = repository.verifyAndBurnActivationCode(
            accountId = secondAcc.id,
            accountName = secondAcc.fullName,
            accountPhone = secondAcc.phone,
            enteredCode = pickedCode,
        )
        assertTrue(reuseRes.isFailure)
        assertFalse(repository.allAccountsFlow.first().first { it.id == secondAcc.id }.isPhoneVerified)

        // Admin PIN lockout after 3 wrong attempts -> 15 min lockout
        val t0 = 1_000_000L
        assertTrue(repository.verifyAdminPin("111111", t0).isFailure)
        assertTrue(repository.verifyAdminPin("222222", t0 + 1000).isFailure)
        assertTrue(repository.verifyAdminPin("333333", t0 + 2000).isFailure)

        // Even correct PIN is rejected while locked out during the 15-minute window
        val duringLockout = repository.verifyAdminPin("202600", t0 + 5 * 60_000L)
        assertTrue(duringLockout.isFailure)

        // After 15 minutes pass, valid PIN unlocks and resets attempts
        val afterLockout = repository.verifyAdminPin("202600", t0 + 16 * 60_000L)
        assertTrue(afterLockout.isSuccess)

        // Full platform config + PIN update + announcement banner
        repository.updateFullPlatformConfig(
            platformNameAr = "طب الحجايزة المطور",
            villageNameAr = "الحجايزة والقرى",
            hospitalNameAr = "مستشفى الحجايزة التخصصي",
            supportPhone = "01125002428",
            homeAnnouncementAr = "إعلان تجريبي هام",
            apkUrl = AppConfig.defaultBrand.apkDownloadUrl,
            medsApiUrl = "",
            adminPin = "998877",
        )
        val updatedConfig = repository.brandConfigFlow.first()
        assertEquals("إعلان تجريبي هام", updatedConfig.homeAnnouncementAr)
        assertEquals("998877", updatedConfig.adminPin)
        assertTrue(repository.verifyAdminPin("998877", t0 + 17 * 60_000L).isSuccess)

        // Phone blocking cancels existing booking and blocks new bookings
        val availSlot = repository.observeSlotsForDoctor("doc_batna_1").first().first { it.status == SlotStatus.AVAILABLE }
        assertTrue(repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP).isSuccess)

        assertTrue(repository.blockPhone("01055443322", "حجز وهمي").isSuccess)
        val releasedSlot = repository.observeSlotsForDoctor("doc_batna_1").first().first { it.id == availSlot.id }
        assertEquals(SlotStatus.AVAILABLE, releasedSlot.status)

        val blockedBook = repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP)
        assertTrue(blockedBook.isFailure)

        repository.unblockPhone("01055443322")
        assertTrue(repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP).isSuccess)
    }
}
