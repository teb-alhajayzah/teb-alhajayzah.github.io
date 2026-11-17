package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.config.AppConfig
import com.example.data.local.BookingSource
import com.example.data.local.ClinicType
import com.example.data.local.MedicalRecordType
import com.example.data.local.SlotStatus
import com.example.data.local.TebDatabase
import com.example.data.medications.MedicationCatalog
import com.example.data.repository.TebRepository
import com.example.util.MedicalSummaryAnalyzer
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
    fun appBrandStartsCleanAtVersionOneWithoutMockData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("طب الحجايزة", context.getString(R.string.app_name))
        assertEquals("v1.0.0", AppConfig.defaultBrand.appVersionLabel)
        assertTrue(AppConfig.defaultBrand.apkDownloadUrl.endsWith("teb-alhajayzah.apk"))
        assertEquals("01125002428", AppConfig.defaultBrand.supportPhone)
        assertEquals("01141271027", AppConfig.defaultBrand.otpWhatsappPhone)

        repository.ensureSeeded()

        assertTrue(repository.doctorsFlow.first().isEmpty())
        assertTrue(repository.labsFlow.first().isEmpty())
        assertTrue(repository.allAccountsFlow.first().isEmpty())
        assertTrue(repository.allSlotsFlow.first().isEmpty())
        assertTrue(repository.allMedicalRecordsFlow.first().isEmpty())
        assertEquals(5, repository.activationCodesFlow.first().size)
    }

    @Test
    fun realTimeBookingPreventsDoubleBookingDelayShiftsAndRxPersists() = runBlocking {
        repository.ensureSeeded()

        val docRes = repository.addDoctorByAdmin(
            doctorName = "د. كريم عادل سليم",
            specialty = "الباطنة",
            clinicName = "عيادة الحجايزة للباطنة",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة - الشارع الرئيسي",
            bio = "أخصائي الباطنة",
            mapsUrl = "https://maps.google.com/?q=30.892,31.592",
            feeEgp = 150,
            workStartMinutes = 17 * 60,
            workEndMinutes = 19 * 60,
            slotDurationMinutes = 20,
            username = "dr_kareem",
            password = "pass_doc",
        )
        assertTrue(docRes.isSuccess)
        val doctor = docRes.getOrThrow()

        val slots = repository.observeSlotsForDoctor(doctor.id).first()
        assertEquals(6, slots.size)
        val firstAvailable = slots.first { it.status == SlotStatus.AVAILABLE }

        val firstBook = repository.bookSlot(
            slotId = firstAvailable.id,
            patientName = "ياسر محمود السيد",
            patientPhone = "01098765432",
            familyMemberId = "mem_self",
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
        repository.delayDoctorAppointments(doctor.id, 15)

        val shiftedSlot = repository.observeSlotsForDoctor(doctor.id).first()
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
        assertTrue(records.any { it.patientName == "ياسر محمود السيد" && it.diagnosis.contains("نزلة معوية") })
    }

    @Test
    fun egyptianMedicationsCatalogAndFavoritesPersistPerDoctor() = runBlocking {
        repository.ensureSeeded()

        val meds = MedicationCatalog.builtInMedications
        assertTrue(meds.any { it.specialty == "أسنان" && it.tradeName.contains("كتافاست") })
        assertTrue(meds.any { it.specialty == "نساء وتوليد" && it.activeIngredient.contains("حمض الفوليك") })
        assertTrue(meds.any { it.specialty == "أطفال وحديثي الولادة" })
        assertTrue(meds.any { it.specialty == "عظام ومفاصل" })

        val doctor = repository.addDoctorByAdmin(
            doctorName = "د. طارق عيسى",
            specialty = "أسنان",
            clinicName = "عيادة الأسنان",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة",
            bio = "",
            mapsUrl = "",
            feeEgp = 120,
            workStartMinutes = 18 * 60,
            workEndMinutes = 20 * 60,
            slotDurationMinutes = 30,
            username = "dr_tariq",
            password = "123",
        ).getOrThrow()

        val targetMedId = "med_dalacin_c"
        repository.toggleFavoriteMedication(doctor.id, targetMedId)
        val afterAdd = repository.allDoctorFavoritesFlow.first()
            .filter { it.doctorId == doctor.id }
            .map { it.medicationId }
            .toSet()
        assertTrue(targetMedId in afterAdd)

        repository.toggleFavoriteMedication(doctor.id, targetMedId)
        val afterRemove = repository.allDoctorFavoritesFlow.first()
            .filter { it.doctorId == doctor.id }
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
    fun externalDoctorVisitAndMedicalLabPortalAndAiSummaryWorkEndToEnd() = runBlocking {
        repository.ensureSeeded()

        val regRes = repository.registerPatientAccount(
            fullName = "محمود حسن علي",
            phone = "01099887766",
            nationalId = "29501011200123",
            password = "pass123",
        )
        assertTrue(regRes.isSuccess)
        val acc = regRes.getOrThrow()
        val selfMember = repository.observeFamilyMembers(acc.id).first().first()
        val sonMember = repository.addFamilyMember(
            accountId = acc.id,
            fullName = "عمر محمود حسن",
            relation = "الابن",
            age = 8,
            phone = acc.phone,
        )

        // 1) Patient logs an external/private doctor visit with dynamic medications
        val extVisit = repository.addExternalDoctorVisitRecord(
            familyMemberId = sonMember.id,
            patientName = sonMember.fullName,
            patientPhone = acc.phone,
            doctorName = "سامح عبد العزيز",
            specialty = "أطفال",
            visitDateLabel = "15/11/2026",
            diagnosis = "التهاب حاد باللوزتين",
            medicationsText = "• أوجمنتين 457 شراب (5 مل كل 12 ساعة - 7 أيام)\n• سيتال شراب (عند اللزوم - 3 أيام)",
            notes = "الراحة وشرب سوائل دافئة",
            attachmentPath = "",
        )
        assertTrue(extVisit.isSuccess)
        assertEquals(MedicalRecordType.EXTERNAL_VISIT, extVisit.getOrThrow().recordType)

        // 2) Super Admin creates a medical lab, edits it, and Lab logs in
        val labRes = repository.addLabByAdmin(
            labName = "معمل الأمل للتحاليل الطبية",
            phone = "01155443322",
            locationLabel = "الحجايزة - أمام المركز الطبي",
            username = "amal_lab",
            password = "lab_password",
        )
        assertTrue(labRes.isSuccess)
        val createdLab = labRes.getOrThrow()

        assertTrue(repository.loginLab("amal_lab", "wrong").isFailure)
        assertTrue(repository.loginLab("amal_lab", "lab_password").isSuccess)
        assertEquals(createdLab.id, repository.loggedInLabIdFlow.first())

        // 3) Lab searches patient by phone and retrieves family members
        val found = repository.searchPatientsByPhoneForLab("01099887766")
        assertEquals(1, found.size)
        assertEquals(2, found.first().familyMembers.size)

        // 4) Lab uploads PDF test report to son's medical file
        val labRecRes = repository.addLabResultRecord(
            lab = createdLab,
            member = sonMember,
            accountPhone = acc.phone,
            testTitle = "تحليل صورة دم كاملة CBC + CRP",
            notes = "نسبة الهيموجلوبين 12.1 والالتهاب بسيط",
            attachmentPath = "/data/user/0/com.example/files/cbc_report.pdf",
            attachmentMimeType = "application/pdf",
            attachmentName = "cbc_report.pdf",
        )
        assertTrue(labRecRes.isSuccess)
        assertEquals(MedicalRecordType.LAB_RESULT, labRecRes.getOrThrow().recordType)

        // Verify son's records and AI summary while selfMember's file remains isolated
        val sonRecords = repository.allMedicalRecordsFlow.first().filter { it.familyMemberId == sonMember.id }
        val selfRecords = repository.allMedicalRecordsFlow.first().filter { it.familyMemberId == selfMember.id }
        assertEquals(2, sonRecords.size)
        assertTrue(selfRecords.isEmpty())

        val summary = MedicalSummaryAnalyzer.summarize(sonMember.fullName, sonRecords)
        assertEquals(1, summary.externalVisitsCount)
        assertEquals(1, summary.labAndImagingCount)
        assertTrue(summary.keyDiagnoses.any { it.contains("التهاب حاد باللوزتين") })
        assertTrue(summary.currentMedications.any { it.contains("أوجمنتين") })
        assertTrue(summary.recentLabsAndImaging.any { it.contains("CBC") })
        assertTrue(summary.disclaimer.contains("تنبيه طبي قياسي"))

        // Admin updates and deletes lab
        val updatedLab = repository.updateLabByAdmin(
            labId = createdLab.id,
            labName = "معمل الأمل المطور",
            phone = "01155443322",
            locationLabel = "الحجايزة",
            username = "amal_lab2",
            password = "new_lab_password",
        )
        assertTrue(updatedLab.isSuccess)
        assertTrue(repository.loginLab("amal_lab2", "new_lab_password").isSuccess)

        repository.deleteLabByAdmin(createdLab.id)
        assertTrue(repository.labsFlow.first().isEmpty())
        assertEquals(null, repository.loggedInLabIdFlow.first())
    }

    @Test
    fun singleUseActivationOtpCodesAndAdminPinLockoutAndPhoneBlockingWork() = runBlocking {
        repository.ensureSeeded()

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

        val allCodes = repository.activationCodesFlow.first()
        assertEquals(15, allCodes.size) // 5 initial + 10
        assertEquals(15, allCodes.map { it.code }.toSet().size)

        val pickedCode = allCodes.first { !it.isUsed }.code
        assertEquals(6, pickedCode.length)

        assertTrue(
            repository.verifyAndBurnActivationCode(
                accountId = createdAcc.id,
                accountName = createdAcc.fullName,
                accountPhone = createdAcc.phone,
                enteredCode = "000000",
            ).isFailure
        )

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

        // Second account attempting to reuse burned code must fail
        val secondAcc = repository.registerPatientAccount(
            fullName = "حساب ثاني",
            phone = "01088776655",
            nationalId = "",
            password = "pass",
        ).getOrThrow()
        assertTrue(
            repository.verifyAndBurnActivationCode(
                accountId = secondAcc.id,
                accountName = secondAcc.fullName,
                accountPhone = secondAcc.phone,
                enteredCode = pickedCode,
            ).isFailure
        )

        // Admin PIN lockout after 3 wrong attempts -> 15 min lockout
        val t0 = 1_000_000L
        assertTrue(repository.verifyAdminPin("111111", t0).isFailure)
        assertTrue(repository.verifyAdminPin("222222", t0 + 1000).isFailure)
        assertTrue(repository.verifyAdminPin("333333", t0 + 2000).isFailure)
        assertTrue(repository.verifyAdminPin("202600", t0 + 5 * 60_000L).isFailure)
        assertTrue(repository.verifyAdminPin("202600", t0 + 16 * 60_000L).isSuccess)

        // Phone blocking cancels existing booking and blocks new bookings
        val doc = repository.addDoctorByAdmin(
            doctorName = "د. علي نور",
            specialty = "عظام",
            clinicName = "عيادة العظام",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة",
            bio = "",
            mapsUrl = "",
            feeEgp = 100,
            workStartMinutes = 17 * 60,
            workEndMinutes = 18 * 60,
            slotDurationMinutes = 20,
            username = "dr_ali",
            password = "123",
        ).getOrThrow()
        val availSlot = repository.observeSlotsForDoctor(doc.id).first().first { it.status == SlotStatus.AVAILABLE }
        assertTrue(repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP).isSuccess)

        assertTrue(repository.blockPhone("01055443322", "حجز وهمي").isSuccess)
        val releasedSlot = repository.observeSlotsForDoctor(doc.id).first().first { it.id == availSlot.id }
        assertEquals(SlotStatus.AVAILABLE, releasedSlot.status)

        assertTrue(repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP).isFailure)

        repository.unblockPhone("01055443322")
        assertTrue(repository.bookSlot(availSlot.id, "مريض مخالف", "01055443322", "", BookingSource.APP).isSuccess)
    }
}
