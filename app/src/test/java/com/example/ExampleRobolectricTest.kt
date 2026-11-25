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
import com.example.ui.AppLanguage
import com.example.util.BilingualMedicalTranslator
import com.example.util.MedicalSummaryAnalyzer
import com.example.util.SpecialtyCatalog
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
        runBlocking { repository.clearPrefsForTesting() }
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

        val seededDoctors = repository.doctorsFlow.first()
        assertEquals(2, seededDoctors.size)
        assertTrue(seededDoctors.any { it.id == "doc_fekry_hajayzah" && it.doctorName == "د. محمد فكري" && it.specialty == "النساء والتوليد وتأخر الإنجاب" })
        assertTrue(seededDoctors.any { it.id == "doc_shady_hajayzah" && it.doctorName == "د. شادي" && it.specialty == "طب الأطفال وحديثي الولادة" })
        assertTrue(repository.labsFlow.first().isEmpty())
        assertTrue(repository.allAccountsFlow.first().isEmpty())
        assertEquals(24, repository.allSlotsFlow.first().size)
        assertTrue(repository.allMedicalRecordsFlow.first().isEmpty())
        assertEquals(5, repository.activationCodesFlow.first().size)

        assertEquals(AppLanguage.AR, repository.appLanguageFlow.first())
        repository.setAppLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, repository.appLanguageFlow.first())
        repository.setAppLanguage(AppLanguage.AR)
        assertEquals(AppLanguage.AR, repository.appLanguageFlow.first())
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
        val rxRecord = records.first { it.patientName == "ياسر محمود السيد" && it.diagnosis.contains("نزلة معوية") }
        assertEquals("Mild Acute Gastroenteritis", rxRecord.diagnosisEn)
        assertTrue(rxRecord.medicationsEn.contains("Antinal"))
        assertTrue(rxRecord.medicationsEn.contains("every 6 hours for 5 days"))
        assertTrue(rxRecord.aiReportAr.contains("التقرير الطبي المبسط"))
        assertTrue(rxRecord.aiReportEn.contains("Clinical Medical Report"))
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
        assertEquals("6:00 PM", TimeUtils.formatMinutesAr(18 * 60, isEn = true))
        assertEquals("9:00 AM", TimeUtils.formatMinutesAr(9 * 60, isEn = true))
    }

    @Test
    fun externalDoctorVisitAndMedicalLabPortalAndAiSummaryWorkEndToEnd() = runBlocking {
        repository.ensureSeeded()

        val regRes = repository.registerPatientAccount(
            fullName = "محمود حسن علي",
            phone = "01099887766",
            nationalId = "29501011200123",
            password = "1234",
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

        val summaryEn = MedicalSummaryAnalyzer.summarize(sonMember.fullName, sonRecords, isEn = true)
        assertTrue(summaryEn.headlineSummary.contains("Summary for"))
        assertTrue(summaryEn.disclaimer.contains("Medical Notice:"))

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
            password = "1234",
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
            password = "5678",
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
        assertTrue(repository.verifyAdminPin("132027", t0 + 5 * 60_000L).isFailure)
        assertTrue(repository.verifyAdminPin("202600", t0 + 16 * 60_000L).isFailure)
        assertTrue(repository.verifyAdminPin("132027", t0 + 16 * 60_000L).isSuccess)
        assertTrue(repository.isAdminLoggedInFlow.first())
        repository.setAdminLoggedIn(false)
        assertFalse(repository.isAdminLoggedInFlow.first())

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

    @Test
    fun seededRealDoctorsLoginsAdminEditDeleteAndMultiSpecialtyFilterWork() = runBlocking {
        repository.ensureSeeded()

        // Default credentials work for Dr. Mohamed Fekry & Dr. Shady
        assertTrue(repository.loginDoctor("dr_fekry", "1234").isSuccess)
        assertTrue(repository.loginDoctor("dr_shady", "1234").isSuccess)

        // All 18 unified specialties are present in order
        assertEquals(18, SpecialtyCatalog.all.size)
        assertEquals("طب الأطفال وحديثي الولادة", SpecialtyCatalog.all.first().nameAr)
        assertEquals("تخصصات أخرى", SpecialtyCatalog.all.last().nameAr)

        // Admin updates Dr. Fekry with an additional specialty
        val updateRes = repository.updateDoctorByAdmin(
            doctorId = "doc_fekry_hajayzah",
            doctorName = "د. محمد فكري",
            specialty = "النساء والتوليد وتأخر الإنجاب",
            clinicName = "عيادة د. محمد فكري للنساء والتوليد",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "قرية الحجايزة",
            bio = "أخصائي النساء والتوليد",
            mapsUrl = "",
            feeEgp = 200,
            workStartMinutes = 17 * 60,
            workEndMinutes = 21 * 60,
            slotDurationMinutes = 20,
            username = "dr_fekry_updated",
            password = "pass_new",
            additionalSpecialties = "طب وجراحة الأسنان|الأشعة التشخيصية",
        )
        assertTrue(updateRes.isSuccess)
        val updatedFekry = updateRes.getOrThrow()
        assertEquals(200, updatedFekry.feeEgp)
        assertTrue(SpecialtyCatalog.doctorMatchesSpecialty(updatedFekry, "النساء والتوليد وتأخر الإنجاب"))
        assertTrue(SpecialtyCatalog.doctorMatchesSpecialty(updatedFekry, "الأشعة التشخيصية"))
        assertFalse(SpecialtyCatalog.doctorMatchesSpecialty(updatedFekry, "طب الأطفال وحديثي الولادة"))
        assertTrue(repository.loginDoctor("dr_fekry_updated", "pass_new").isSuccess)

        // Deleting a seeded doctor persists even if ensureSeeded() runs again
        repository.deleteDoctorByAdmin("doc_shady_hajayzah")
        repository.ensureSeeded()
        val remaining = repository.doctorsFlow.first()
        assertEquals(1, remaining.size)
        assertFalse(remaining.any { it.id == "doc_shady_hajayzah" })
    }

    @Test
    fun bilingualTranslatorGeneratesArAndEnRxAndClinicalReport() {
        val bundle = BilingualMedicalTranslator.buildBilingualBundle(
            patientName = "عمر محمود",
            doctorName = "د. شادي",
            specialty = "طب الأطفال وحديثي الولادة",
            diagnosisInput = "التهاب حاد باللوزتين",
            medicationsInput = "• أوجمنتين شراب (Augmentin) 457 مجم/5 مل — 5 سم كل 12 ساعة لمدة 7 أيام",
            notesInput = "الراحة وشرب سوائل دافئة",
        )
        assertEquals("التهاب حاد باللوزتين", bundle.diagnosisAr)
        assertEquals("Acute Tonsillitis", bundle.diagnosisEn)
        assertTrue(bundle.medicationsEn.contains("Augmentin"))
        assertTrue(bundle.medicationsEn.contains("5 ml every 12 hours for 7 days"))
        assertTrue(bundle.aiReportAr.contains("د. شادي"))
        assertTrue(bundle.aiReportEn.contains("Acute Tonsillitis"))
        assertTrue(bundle.aiReportEn.contains("Pediatrics & Neonatology"))
    }

    @Test
    fun masterOtp999000WithArabicDigitsClinicActivationAndAdminUsersCrudWork() = runBlocking {
        repository.ensureSeeded()
        assertEquals("https://teb-alhajayzah.github.io/", AppConfig.defaultBrand.liveWebUrl)

        // 1) Register user A and activate using Master OTP with Arabic numerals
        val accA = repository.registerPatientAccount(
            fullName = "أحمد مصطفى سالم",
            phone = "01012345678",
            nationalId = "",
            password = "1122",
        ).getOrThrow()
        assertFalse(accA.isPhoneVerified)

        val masterOtpRes = repository.verifyAndBurnActivationCode(
            accountId = accA.id,
            accountName = accA.fullName,
            accountPhone = accA.phone,
            enteredCode = "٩٩٩٠٠٠",
        )
        assertTrue(masterOtpRes.isSuccess)
        val verifiedA = repository.allAccountsFlow.first().first { it.id == accA.id }
        assertTrue(verifiedA.isPhoneVerified)
        assertEquals("كود OTP", verifiedA.verifiedBy)
        // Single-use pool remains untouched (still 5 unused)
        assertEquals(5, repository.activationCodesFlow.first().count { !it.isUsed })

        // 2) Register user B with family member, search by phone in clinic & activate directly
        val accB = repository.registerPatientAccount(
            fullName = "منى إبراهيم علي",
            phone = "01199887766",
            nationalId = "",
            password = "3344",
        ).getOrThrow()
        repository.addFamilyMember(
            accountId = accB.id,
            fullName = "سلمى منى",
            relation = "الابنة",
            age = 5,
            phone = accB.phone,
        )

        val searchResults = repository.searchAccountsByPhoneForClinic("0119988")
        assertEquals(1, searchResults.size)
        assertEquals(2, searchResults.first().familyMembers.size)

        val clinicActRes = repository.activatePatientByClinic(accB.id, "د. محمد فكري")
        assertTrue(clinicActRes.isSuccess)
        val verifiedB = repository.allAccountsFlow.first().first { it.id == accB.id }
        assertTrue(verifiedB.isPhoneVerified)
        assertEquals("عيادة د. محمد فكري", verifiedB.verifiedBy)

        // 3) Admin Users management: unverify, re-verify manually, edit details, and cascade delete
        repository.setAccountVerificationByAdmin(accB.id, false)
        assertFalse(repository.allAccountsFlow.first().first { it.id == accB.id }.isPhoneVerified)

        repository.setAccountVerificationByAdmin(accB.id, true)
        val adminVerifiedB = repository.allAccountsFlow.first().first { it.id == accB.id }
        assertTrue(adminVerifiedB.isPhoneVerified)
        assertEquals("المدير العام", adminVerifiedB.verifiedBy)

        val editRes = repository.updateAccountByAdmin(
            accountId = accB.id,
            fullName = "منى إبراهيم المعدلة",
            phone = "01199887700",
            newPassword = "newpass99",
        )
        assertTrue(editRes.isSuccess)
        assertEquals("منى إبراهيم المعدلة", editRes.getOrThrow().fullName)
        assertEquals("01199887700", editRes.getOrThrow().phone)
        assertEquals("newpass99", editRes.getOrThrow().password)

        repository.deleteAccountByAdmin(accB.id)
        assertFalse(repository.allAccountsFlow.first().any { it.id == accB.id })
        assertTrue(repository.allFamilyMembersFlow.first().none { it.accountId == accB.id })
    }

    @Test
    fun perUser24HourOtpAutoGenerationExpiryAndRegenerationWork() = runBlocking {
        repository.ensureSeeded()
        val t0 = 10_000_000_000L

        // 1) Registration auto-generates a unique 6-digit OTP valid for 24h
        val user1 = repository.registerPatientAccount(
            fullName = "خالد عبد الرحمن",
            phone = "01022334455",
            nationalId = "",
            password = "1020",
            nowMillis = t0,
        ).getOrThrow()

        assertEquals(6, user1.otpCode.length)
        assertTrue(user1.otpCode.all { it.isDigit() })
        assertFalse(user1.otpCode == TebRepository.MASTER_ADMIN_OTP)
        assertEquals(t0 + TebRepository.OTP_TTL_MILLIS, user1.otpExpiresAtMillis)
        assertFalse(user1.otpUsed)
        assertFalse(user1.isPhoneVerified)

        // 2) Attempting verification after 25 hours (> 24h TTL) must fail
        val expiredRes = repository.verifyAndBurnActivationCode(
            accountId = user1.id,
            accountName = user1.fullName,
            accountPhone = user1.phone,
            enteredCode = user1.otpCode,
            nowMillis = t0 + TebRepository.OTP_TTL_MILLIS + 3_600_000L,
        )
        assertTrue(expiredRes.isFailure)
        assertFalse(repository.allAccountsFlow.first().first { it.id == user1.id }.isPhoneVerified)

        // 3) Regenerate fresh 24h OTP and verify within window -> succeeds and burns code
        val tRegen = t0 + TebRepository.OTP_TTL_MILLIS + 3_700_000L
        val regenerated = repository.regenerateAccountOtp(user1.id, nowMillis = tRegen).getOrThrow()
        assertEquals(6, regenerated.otpCode.length)
        assertEquals(tRegen + TebRepository.OTP_TTL_MILLIS, regenerated.otpExpiresAtMillis)
        assertFalse(regenerated.otpUsed)

        val verifyOk = repository.verifyAndBurnActivationCode(
            accountId = user1.id,
            accountName = user1.fullName,
            accountPhone = user1.phone,
            enteredCode = regenerated.otpCode,
            nowMillis = tRegen + 60_000L,
        )
        assertTrue(verifyOk.isSuccess)
        val updatedUser1 = repository.allAccountsFlow.first().first { it.id == user1.id }
        assertTrue(updatedUser1.isPhoneVerified)
        assertTrue(updatedUser1.otpUsed)
        assertEquals("كود OTP", updatedUser1.verifiedBy)

        // 4) Unverified user2 cannot use user1's bound OTP code, must use their own or 999000
        val user2 = repository.registerPatientAccount(
            fullName = "هبة سعيد محمد",
            phone = "01066778899",
            nationalId = "",
            password = "3040",
            nowMillis = tRegen,
        ).getOrThrow()
        assertFalse(user2.otpCode == regenerated.otpCode)
        assertTrue(
            repository.verifyAndBurnActivationCode(
                accountId = user2.id,
                accountName = user2.fullName,
                accountPhone = user2.phone,
                enteredCode = regenerated.otpCode,
                nowMillis = tRegen + 120_000L,
            ).isFailure
        )
        assertTrue(
            repository.verifyAndBurnActivationCode(
                accountId = user2.id,
                accountName = user2.fullName,
                accountPhone = user2.phone,
                enteredCode = user2.otpCode,
                nowMillis = tRegen + 120_000L,
            ).isSuccess
        )
        assertTrue(repository.allAccountsFlow.first().first { it.id == user2.id }.isPhoneVerified)
    }

    @Test
    fun patientBioChronicsPersistenceSummaryEnrichmentFileDeletionAndVersionOneLockWork() = runBlocking {
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository.ensureSeeded()

        val acc = repository.registerPatientAccount(
            fullName = "سعيد إبراهيم محمد",
            phone = "01077665544",
            nationalId = "",
            password = "7788",
        ).getOrThrow()
        val member = repository.observeFamilyMembers(acc.id).first().first()

        // 1) Save Patient Bio & Chronic Conditions
        val bioSaveRes = repository.updateMemberBioAndChronic(
            memberId = member.id,
            age = 42,
            weightKg = 78.5,
            heightCm = 174.0,
            bloodType = "O+",
            chronicConditions = listOf("diabetes", "hypertension", "التهاب المفاصل المزمن"),
            drugAllergies = "البنسلين والأسبرين",
            otherChronicNotes = "استئصال زائدة سابقاً",
        )
        assertTrue(bioSaveRes.isSuccess)
        val updatedMem = bioSaveRes.getOrThrow()
        assertEquals(42, updatedMem.age)
        assertEquals(78.5, updatedMem.weightKg, 0.01)
        assertEquals(174.0, updatedMem.heightCm, 0.01)
        assertEquals("O+", updatedMem.bloodType)
        assertTrue(
            com.example.util.ChronicConditionsCatalog.parseKeys(updatedMem.chronicConditions)
                .containsAll(listOf("diabetes", "hypertension", "التهاب المفاصل المزمن"))
        )
        assertEquals("البنسلين والأسبرين", updatedMem.drugAllergies)

        // 2) Create temporary internal files for testing deletion
        val attachmentsDir = java.io.File(context.filesDir, "medical_images").apply { mkdirs() }
        val uploadPdfFile = java.io.File(attachmentsDir, "urine_test.pdf").apply { writeText("%PDF-1.4 dummy") }
        val rxPhotoFile = java.io.File(attachmentsDir, "rx_paper.jpg").apply { writeText("jpg-dummy") }

        val uploadRec = repository.addPatientUploadedAttachment(
            familyMemberId = updatedMem.id,
            patientName = updatedMem.fullName,
            patientPhone = acc.phone,
            title = "تحليل بول كامل Urine Analysis",
            notes = "Pus cells 12-15 / HPF",
            attachmentPath = uploadPdfFile.absolutePath,
            attachmentMimeType = "application/pdf",
            attachmentName = "urine_test.pdf",
        )

        val rxRec = repository.addExternalDoctorVisitRecord(
            familyMemberId = updatedMem.id,
            patientName = updatedMem.fullName,
            patientPhone = acc.phone,
            doctorName = "أشرف حسن",
            specialty = "الباطنة العامة والجهاز الهضمي والكبد",
            visitDateLabel = "18/11/2026",
            diagnosis = "ارتفاع ضغط الدم",
            medicationsText = "• كونكور 5 مجم — قرص صباحاً يومياً",
            notes = "قياس ضغط الدم يومياً",
            attachmentPath = rxPhotoFile.absolutePath,
            attachmentMimeType = "image/jpeg",
            attachmentName = "rx_paper.jpg",
        ).getOrThrow()

        val memberRecords = repository.allMedicalRecordsFlow.first().filter { it.familyMemberId == updatedMem.id }
        assertEquals(2, memberRecords.size)

        // 3) Verify MedicalSummaryAnalyzer includes Bio, Chronic Conditions, and Allergies
        val summaryAr = MedicalSummaryAnalyzer.summarize(
            member = updatedMem,
            patientName = updatedMem.fullName,
            records = memberRecords,
            isEn = false,
        )
        assertTrue(summaryAr.vitalsSummary.contains("O+"))
        assertTrue(summaryAr.vitalsSummary.contains("78.5"))
        assertTrue(summaryAr.chronicConditionsSummary.contains("السكر"))
        assertTrue(summaryAr.chronicConditionsSummary.contains("ضغط الدم"))
        assertEquals("البنسلين والأسبرين", summaryAr.drugAllergies)
        assertEquals(1, summaryAr.labAndImagingCount)
        assertEquals(1, summaryAr.externalVisitsCount)

        // 3b) Smart Offline Fallback: when Gemini/Firebase is unreachable or quota exceeded,
        // refreshMemberAiSummaryWithGemini succeeds seamlessly with an organized 2-part structured summary and ZERO error.
        val fallbackRefreshRes = repository.refreshMemberAiSummaryWithGemini(updatedMem.id)
        assertTrue(fallbackRefreshRes.isSuccess)
        val afterFallback = fallbackRefreshRes.getOrThrow()
        assertEquals("", afterFallback.aiSummaryError)
        assertTrue(afterFallback.aiSummaryAr.contains("الحالة العامة والقيم الطبيعية"))
        assertTrue(afterFallback.aiSummaryAr.contains("نقاط تحتاج لمتابعة أو انتباه الطبيب"))
        assertTrue(afterFallback.aiSummaryAr.contains("1."))
        assertTrue(afterFallback.aiSummaryAr.contains("البيانات الحيوية"))
        assertTrue(afterFallback.aiSummaryAr.contains("O+"))
        assertTrue(afterFallback.aiSummaryAr.contains("السكر"))
        assertTrue(afterFallback.aiSummaryAr.contains("Urine Analysis") || afterFallback.aiSummaryAr.contains("urine_test.pdf"))
        assertTrue(afterFallback.aiSummaryAr.contains("كونكور"))
        assertTrue(afterFallback.aiSummaryEn.contains("Overall Status & Normal Values"))
        assertTrue(afterFallback.aiSummaryEn.contains("Points Requiring Physician Follow-Up"))
        assertTrue(afterFallback.aiSummaryEn.contains("Vitals & Bio"))
        assertTrue(afterFallback.aiSummaryEn.contains("Diabetes"))
        assertTrue(afterFallback.aiSummaryEn.contains("urine_test.pdf"))

        // 3c) Smart Local Clinical Parser ignores dates/barcodes/reporting/request/age/gender metadata
        // and formats Arabic patient explanations in simple reassuring Arabic WITHOUT English jargon.
        val sampleLabText = """
            Lab Name: Al-Shifa Medical Lab
            Patient Name: سعيد إبراهيم محمد
            Reporting Date: 19/11/2026
            Request Date: 18-11-2026
            Request ID: 2026 10-20
            Age: 35
            Gender: Male
            Barcode: 88772026
            Hemoglobin: 9.8 g/dL (12.0 - 16.0)
            Glucose: 215 mg/dL (70 - 140)
            Pus cells: 15-20 /HPF (0 - 5)
            RBCs: 8-10 /HPF (0 - 3)
            Calcium Oxalate Crystals: ++ (Nil)
            Bacteria: Absent
            Albumin: Nil
        """.trimIndent()
        val parsedLocalAr = MedicalSummaryAnalyzer.parseClinicalLabText(listOf(sampleLabText), updatedMem, isEn = false)
        assertTrue(parsedLocalAr.normalCount >= 2)
        assertTrue(parsedLocalAr.normalHighlights.any { it.contains("سلبية / طبيعية ولا يوجد بها أي مشكلة") })
        assertTrue(parsedLocalAr.followUpPoints.any { it.contains("وجود نسبة بسيطة من خلايا الصديد في البول، وتحتاج للمتابعة مع الطبيب لوصف العلاج المناسب") })
        assertTrue(parsedLocalAr.followUpPoints.any { it.contains("وجود نسبة أملاح (أوكسالات) في البول، ويُنصح بشرب كميات كافية من الماء ومراجعة الطبيب") })
        assertTrue(parsedLocalAr.followUpPoints.any { it.contains("نسبة الهيموجلوبين (الأنيميا) أقل من المعدل الطبيعي وتحتاج لمتابعة الطبيب") })
        assertTrue(parsedLocalAr.followUpPoints.any { it.contains("215") })
        val allArabicOutputs = (parsedLocalAr.normalHighlights + parsedLocalAr.followUpPoints).joinToString(" | ")
        val forbiddenEnglishOrMetaTokens = listOf(
            "Al-Shifa", "Reporting", "Request", "19/11/2026", "2026", "Gender", "Age", "Barcode",
            "Absent", "Pus cells", "RBCs", "Oxalate", "Hemoglobin", "Nil", "Ref",
        )
        forbiddenEnglishOrMetaTokens.forEach { forbidden ->
            assertFalse("Arabic output must not contain '$forbidden': $allArabicOutputs", allArabicOutputs.contains(forbidden, ignoreCase = true))
        }

        // 4) Delete patient PDF upload -> deletes record and physical file
        val delPdfRes = repository.deleteMedicalRecordOrAttachment(uploadRec.id)
        assertTrue(delPdfRes.isSuccess)
        assertFalse(uploadPdfFile.exists())
        val afterPdfDel = repository.allMedicalRecordsFlow.first().filter { it.familyMemberId == updatedMem.id }
        assertEquals(1, afterPdfDel.size)
        assertEquals(rxRec.id, afterPdfDel.first().id)

        // 5) Delete attachment from visit record -> keeps clinical visit, clears attachment & deletes file
        val delRxFileRes = repository.deleteMedicalRecordOrAttachment(rxRec.id)
        assertTrue(delRxFileRes.isSuccess)
        assertFalse(rxPhotoFile.exists())
        val remainingVisit = repository.allMedicalRecordsFlow.first().first { it.familyMemberId == updatedMem.id }
        assertEquals("", remainingVisit.attachmentPath)
        assertEquals("ارتفاع ضغط الدم", remainingVisit.diagnosis)
    }

    @Test
    fun otaVersionLockAndFooterContractMatchProductionRelease() {
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
        assertEquals("https://teb-alhajayzah.github.io/", AppConfig.defaultBrand.liveWebUrl)

        val versionFile = listOf(
            java.io.File("version.json"),
            java.io.File("../version.json"),
        ).firstOrNull { it.exists() }
        assertTrue("Root version.json must exist", versionFile != null && versionFile.exists())

        val json = org.json.JSONObject(versionFile!!.readText())
        assertEquals("طب الحجايزة", json.getString("appName"))
        assertEquals("1.0.0", json.getString("version"))
        assertEquals("1.0.0", json.getString("versionName"))
        assertEquals(1, json.getInt("versionCode"))
        assertEquals("طب الحجايزة.apk", json.getString("apkFileName"))
        assertEquals("https://teb-alhajayzah.github.io/", json.getString("liveUrl"))
        assertTrue(json.getBoolean("forceCleanCache"))
        assertTrue(json.getBoolean("otaSync"))

        assertEquals(0, repository.compareAppVersions("1.0.0", "1.0.0"))
        assertEquals(0, repository.compareAppVersions("v1.0.0", "1.0.0"))
        assertTrue(repository.compareAppVersions("1.0.1", "1.0.0") > 0)
        assertTrue(repository.compareAppVersions("2.0.0", "1.9.9") > 0)
        assertTrue(repository.compareAppVersions("1.0.0", "1.0.1") < 0)
    }

    @Test
    fun messagingSystemAdminChatSwitchesBroadcastsAndRoleNotificationsWork() = runBlocking {
        repository.ensureSeeded()
        val doctors = repository.doctorsFlow.first()
        val doc1 = doctors.first()
        val acc = repository.registerPatientAccount(
            fullName = "أحمد محمود علي",
            phone = "01055443322",
            nationalId = "",
            password = "4321",
        ).getOrThrow()

        // 1) Patient -> Clinic works by default
        assertTrue(repository.brandConfigFlow.first().globalChatEnabled)
        assertTrue(doc1.chatEnabled)
        val p2c = repository.sendPatientToClinicMessage(acc, doc1.id, "هل العيادة فاتحة اليوم؟")
        assertTrue(p2c.isSuccess)
        assertFalse(p2c.getOrThrow().isReadByClinic)

        // Clinic replies to patient + sends direct patient alert
        val c2p = repository.sendClinicReplyToPatient(doc1, acc.id, acc.fullName, acc.phone, "نعم، مفتوحة من 5 مساءً")
        assertTrue(c2p.isSuccess)
        assertFalse(c2p.getOrThrow().isReadByPatient)
        val cAlert = repository.sendClinicToPatientAlert(doc1, acc.id, acc.fullName, "يرجى إحضار أشعة الصدر معك")
        assertTrue(cAlert.isSuccess)
        assertEquals(com.example.data.local.NotificationAudience.PATIENT, cAlert.getOrThrow().targetAudience)

        // 2) Master Global Chat Switch blocks patient -> clinic chat
        repository.setGlobalChatEnabled(false)
        assertFalse(repository.brandConfigFlow.first().globalChatEnabled)
        val blockedByGlobal = repository.sendPatientToClinicMessage(acc, doc1.id, "رسالة أثناء الإيقاف العام")
        assertTrue(blockedByGlobal.isFailure)

        // Re-enable global, disable per-clinic chat switch -> blocks only that clinic
        repository.setGlobalChatEnabled(true)
        repository.setClinicChatEnabled(doc1.id, false)
        val blockedByClinic = repository.sendPatientToClinicMessage(acc, doc1.id, "رسالة أثناء إيقاف العيادة")
        assertTrue(blockedByClinic.isFailure)
        repository.setClinicChatEnabled(doc1.id, true)

        // 3) Patient <-> Admin & Clinic <-> Admin Support Inbox + Broadcasts
        val p2a = repository.sendPatientToAdminMessage(acc, "عندي استفسار للدعم الفني").getOrThrow()
        assertFalse(p2a.isReadByAdmin)
        val c2a = repository.sendClinicToAdminMessage(doc1, "طلب تحديث بيانات العيادة").getOrThrow()
        assertFalse(c2a.isReadByAdmin)

        repository.sendAdminReplyToPatient(acc.id, acc.fullName, acc.phone, "تم استلام طلبك").getOrThrow()
        repository.sendAdminReplyToClinic(doc1, "تم تنفيذ تحديث العيادة").getOrThrow()
        val clinicAlert = repository.sendAdminClinicAlert(doc1, "تحديث نظام التذكير بالعيادة").getOrThrow()
        assertEquals(com.example.data.local.NotificationAudience.CLINIC, clinicAlert.targetAudience)
        val broadcast = repository.sendAdminGlobalBroadcast("تحديث هام: ساعات العمل في العيد").getOrThrow()
        assertEquals(com.example.data.local.NotificationAudience.ALL, broadcast.targetAudience)

        val allNotifs = repository.notificationsFlow.first()
        val patientVisible = allNotifs.filter {
            repository.isNotificationVisibleForRole(it, "PATIENT", acc.id, acc.phone, emptySet(), null)
        }
        val clinicVisible = allNotifs.filter {
            repository.isNotificationVisibleForRole(it, "DOCTOR_ADMIN", "", "", emptySet(), doc1.id)
        }
        assertTrue(patientVisible.any { it.id == cAlert.getOrThrow().id })
        assertTrue(patientVisible.any { it.id == broadcast.id })
        assertFalse(patientVisible.any { it.id == clinicAlert.id })
        assertTrue(clinicVisible.any { it.id == clinicAlert.id })
        assertTrue(clinicVisible.any { it.id == broadcast.id })
        assertFalse(clinicVisible.any { it.id == cAlert.getOrThrow().id })

        // Mark patient thread read clears unread flag only for patient
        repository.markDirectThreadRead(
            threadType = com.example.data.local.ChatThreadType.PATIENT_CLINIC,
            viewerRole = com.example.data.local.MessageSenderRole.PATIENT,
            patientAccountId = acc.id,
            doctorId = doc1.id,
        )
        val msgs = repository.directMessagesFlow.first()
        assertTrue(msgs.filter { it.threadType == com.example.data.local.ChatThreadType.PATIENT_CLINIC && it.patientAccountId == acc.id }.all { it.isReadByPatient })
    }

    @Test
    fun cloudNamespaceSettingsAndAppointmentTwoHourCancelRescheduleRulesWork() = runBlocking {
        repository.ensureSeeded()

        // 1. Verify namespace isolation paths, embedded cloud defaults, and locked v1.0.0 metadata
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
        assertEquals(
            "ai-studio-masariktourism-bc99256d-aa39-4656-8a59-94fe2aefe864",
            com.example.config.CloudNamespacePaths.FIRESTORE_DATABASE_ID,
        )
        assertEquals("teb_alhajayzah", com.example.config.CloudNamespacePaths.ROOT)
        assertEquals("teb_alhajayzah_settings", com.example.config.CloudNamespacePaths.SETTINGS)
        assertEquals("settings", com.example.config.CloudNamespacePaths.SETTINGS_DOC)
        assertEquals("teb_alhajayzah_accounts", com.example.config.CloudNamespacePaths.USERS)
        assertEquals("teb_alhajayzah_clinics", com.example.config.CloudNamespacePaths.CLINICS)
        assertEquals("teb_alhajayzah_appointments", com.example.config.CloudNamespacePaths.APPOINTMENTS)
        assertEquals("teb_alhajayzah_messages", com.example.config.CloudNamespacePaths.MESSAGES)
        assertEquals("teb_alhajayzah_notifications", com.example.config.CloudNamespacePaths.NOTIFICATIONS)
        assertEquals("teb_alhajayzah_records", com.example.config.CloudNamespacePaths.RECORDS)

        val initialBrand = repository.brandConfigFlow.first()
        assertTrue(initialBrand.isCloudConfigured)
        assertEquals("https://masarik-otp.masarik-tourism.workers.dev", initialBrand.workerUrl)
        assertEquals("AIzaSyB0qr3K8jf1LgKog8b5knh1W4AClpw5iIs", initialBrand.firebaseApiKey)
        assertEquals("gen-lang-client-0015800098.firebaseapp.com", initialBrand.firebaseAuthDomain)
        assertEquals("gen-lang-client-0015800098", initialBrand.firebaseProjectId)
        assertEquals("gen-lang-client-0015800098.firebasestorage.app", initialBrand.firebaseStorageBucket)
        assertEquals("588431549120", initialBrand.firebaseMessagingSenderId)
        assertEquals("1:588431549120:web:c10278b3df39d11a668325", initialBrand.firebaseAppId)
        assertEquals(
            "BNjwnHCGNHMZt2rSbDbkIsa2NCI96ivhJSGYNA9MgbMehoQNnyP2MyUmqrDav2MUfg4YPMeeiOyIqQTE7MfA6kiw",
            initialBrand.firebaseVapidKey,
        )

        // Verify Firestore REST serialization round-trip preserves settings & array fields cleanly
        val settingsDoc = org.json.JSONObject().put(
            "fields",
            com.example.data.remote.FirestoreCloudSyncService.toFirestoreFields(
                com.example.data.remote.FirestoreCloudSyncService.settingsToJson(initialBrand)
            ),
        )
        val decodedSettings = com.example.data.remote.FirestoreCloudSyncService.fromFirestoreDocument(settingsDoc)
        assertEquals("gen-lang-client-0015800098", decodedSettings.getString("firebaseProjectId"))
        assertEquals("https://masarik-otp.masarik-tourism.workers.dev", decodedSettings.getString("workerUrl"))

        // 2. Verify Firebase Web Config & Cloudflare Worker URL override & reset back to embedded defaults
        repository.updateFullPlatformConfig(
            platformNameAr = "طب الحجايزة",
            villageNameAr = "قرية الحجايزة والقرى المجاورة",
            hospitalNameAr = "مستشفى الحجايزة التخصصي",
            supportPhone = "01125002428",
            homeAnnouncementAr = "",
            apkUrl = AppConfig.defaultBrand.apkDownloadUrl,
            medsApiUrl = "",
            adminPin = "202600",
            firebaseApiKey = "AIzaTestKey123",
            firebaseAuthDomain = "teb-test.firebaseapp.com",
            firebaseProjectId = "teb-test-id",
            firebaseStorageBucket = "teb-test.appspot.com",
            firebaseMessagingSenderId = "1234567890",
            firebaseAppId = "1:1234567890:web:abcdef",
            firebaseVapidKey = "BTestVapidKey",
            workerUrl = "https://teb-worker.example.workers.dev",
        )
        val savedBrand = repository.brandConfigFlow.first()
        assertTrue(savedBrand.isCloudConfigured)
        assertEquals("AIzaTestKey123", savedBrand.firebaseApiKey)
        assertEquals("teb-test-id", savedBrand.firebaseProjectId)
        assertEquals("BTestVapidKey", savedBrand.firebaseVapidKey)
        assertEquals("https://teb-worker.example.workers.dev", savedBrand.workerUrl)

        repository.resetFullPlatformConfig()
        val resetBrand = repository.brandConfigFlow.first()
        assertTrue(resetBrand.isCloudConfigured)
        assertEquals(AppConfig.defaultBrand.workerUrl, resetBrand.workerUrl)
        assertEquals(AppConfig.defaultBrand.firebaseApiKey, resetBrand.firebaseApiKey)
        assertEquals(AppConfig.defaultBrand.firebaseProjectId, resetBrand.firebaseProjectId)
        assertEquals(AppConfig.defaultBrand.firebaseVapidKey, resetBrand.firebaseVapidKey)

        // 3. Patient 2-hour rule: deny within 2 hours, allow when >= 120 mins remain + notify clinic
        val doc = repository.doctorsFlow.first().first()
        val docSlots = repository.observeSlotsForDoctor(doc.id).first()
            .sortedBy { it.startMinutes }
        val slotA = docSlots[0]
        val slotB = docSlots[1]
        val slotC = docSlots[2]

        assertTrue(repository.bookSlot(slotA.id, "أحمد علي", "01012345678", "mem_1", BookingSource.APP).isSuccess)

        // With 90 mins left (< 120), patient cannot cancel or reschedule
        val tooLateCancel = repository.cancelSlotByPatient(
            slotId = slotA.id,
            nowMinutesOfDay = slotA.effectiveMinutes - 90,
            enforceTwoHourRule = true,
        )
        assertTrue(tooLateCancel.isFailure)

        val tooLateReschedule = repository.rescheduleSlotByPatient(
            fromSlotId = slotA.id,
            toSlotId = slotB.id,
            nowMinutesOfDay = slotA.effectiveMinutes - 60,
            enforceTwoHourRule = true,
        )
        assertTrue(tooLateReschedule.isFailure)

        // With 150 mins left (>= 120), patient reschedules slotA -> slotB & notifies CLINIC
        val okReschedule = repository.rescheduleSlotByPatient(
            fromSlotId = slotA.id,
            toSlotId = slotB.id,
            nowMinutesOfDay = slotA.effectiveMinutes - 150,
            enforceTwoHourRule = true,
        )
        assertTrue(okReschedule.isSuccess)
        val updatedSlots1 = repository.observeSlotsForDoctor(doc.id).first().associateBy { it.id }
        assertEquals(SlotStatus.AVAILABLE, updatedSlots1[slotA.id]?.status)
        assertEquals(SlotStatus.BOOKED, updatedSlots1[slotB.id]?.status)
        assertEquals("أحمد علي", updatedSlots1[slotB.id]?.patientName)
        assertTrue(
            repository.notificationsFlow.first().any {
                it.targetAudience == com.example.data.local.NotificationAudience.CLINIC &&
                    it.targetId == doc.id &&
                    it.message.contains("أحمد علي")
            }
        )

        // 4. Clinic Secretary can reschedule (slotB -> slotC) & cancel without 2-hour rule, notifying PATIENT
        assertTrue(repository.rescheduleSlotByClinic(slotB.id, slotC.id).isSuccess)
        val updatedSlots2 = repository.observeSlotsForDoctor(doc.id).first().associateBy { it.id }
        assertEquals(SlotStatus.AVAILABLE, updatedSlots2[slotB.id]?.status)
        assertEquals(SlotStatus.BOOKED, updatedSlots2[slotC.id]?.status)

        assertTrue(repository.cancelSlotByClinic(slotC.id).isSuccess)
        val updatedSlots3 = repository.observeSlotsForDoctor(doc.id).first().associateBy { it.id }
        assertEquals(SlotStatus.AVAILABLE, updatedSlots3[slotC.id]?.status)
        assertTrue(
            repository.notificationsFlow.first().any {
                it.targetAudience == com.example.data.local.NotificationAudience.PATIENT &&
                    it.targetId == "mem_1"
            }
        )

        // Patient can also cancel >= 2 hours early cleanly
        assertTrue(repository.bookSlot(slotA.id, "سعيد حسن", "01022334455", "mem_2", BookingSource.APP).isSuccess)
        val okPatientCancel = repository.cancelSlotByPatient(
            slotId = slotA.id,
            nowMinutesOfDay = slotA.effectiveMinutes - 130,
            enforceTwoHourRule = true,
        )
        assertTrue(okPatientCancel.isSuccess)
        assertEquals(SlotStatus.AVAILABLE, repository.observeSlotsForDoctor(doc.id).first().first { it.id == slotA.id }.status)
    }

    @Test
    fun appointmentReminderTriggersNumericPinAndAdminMessageDeletionWork() = runBlocking {
        repository.ensureSeeded()

        // 1. Appointment reminders: -2h + -30m when >= 2h away; -1h + -30m when ~90m away; empty when past
        val baseCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 10)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val t10am = baseCal.timeInMillis
        val twoHourCase = com.example.util.TebNotificationHelper.computeReminderTriggersMillis(
            slotId = "slot_2h",
            doctorName = "د. علي",
            clinicName = "عيادة الباطنة",
            effectiveMinutes = 14 * 60,
            nowMillis = t10am,
        )
        assertEquals(2, twoHourCase.size)
        assertEquals("REMIND_2H", twoHourCase[0].kind)
        assertEquals("REMIND_30M", twoHourCase[1].kind)

        val oneHourCase = com.example.util.TebNotificationHelper.computeReminderTriggersMillis(
            slotId = "slot_1h",
            doctorName = "د. علي",
            clinicName = "عيادة الباطنة",
            effectiveMinutes = 11 * 60 + 30,
            nowMillis = t10am,
        )
        assertEquals(2, oneHourCase.size)
        assertEquals("REMIND_1H", oneHourCase[0].kind)
        assertEquals("REMIND_30M", oneHourCase[1].kind)

        val pastCase = com.example.util.TebNotificationHelper.computeReminderTriggersMillis(
            slotId = "slot_past",
            doctorName = "د. علي",
            clinicName = "عيادة الباطنة",
            effectiveMinutes = 9 * 60,
            nowMillis = t10am,
        )
        assertTrue(pastCase.isEmpty())

        // 2. Numeric PIN validation & Arabic-Indic digit normalization across registration & login
        assertTrue(repository.isValidNumericPin("1234"))
        assertTrue(repository.isValidNumericPin("١٢٣٤٥٦"))
        assertFalse(repository.isValidNumericPin("123"))
        assertFalse(repository.isValidNumericPin("abc123"))
        assertFalse(repository.isValidNumericPin("12@45"))

        val regRes = repository.registerPatientAccount(
            fullName = "محمود حسن",
            phone = "٠١٠٩٩٨٨٧٧٦٦",
            nationalId = "",
            password = "٢٥٨٠",
        )
        assertTrue(regRes.isSuccess)
        val loginRes = repository.loginPatient("01099887766", "2580")
        assertTrue(loginRes.isSuccess)
        assertEquals("محمود حسن", loginRes.getOrThrow().fullName)

        // 3. Admin single message delete & full conversation thread deletion
        val doc = repository.doctorsFlow.first().first()
        val msg1 = com.example.data.local.DirectMessageEntity(
            id = "msg_adm_del_1",
            threadType = com.example.data.local.ChatThreadType.CLINIC_ADMIN,
            doctorId = doc.id,
            doctorName = doc.doctorName,
            clinicName = doc.clinicName,
            senderRole = com.example.data.local.MessageSenderRole.ADMIN,
            senderName = "الإدارة",
            body = "رسالة أولى",
            createdAtMillis = 1000L,
        )
        val msg2 = msg1.copy(id = "msg_adm_del_2", body = "رسالة ثانية", createdAtMillis = 2000L)
        val msg3 = com.example.data.local.DirectMessageEntity(
            id = "msg_adm_keep_3",
            threadType = com.example.data.local.ChatThreadType.PATIENT_ADMIN,
            patientAccountId = "acc_other",
            patientName = "مريض آخر",
            senderRole = com.example.data.local.MessageSenderRole.PATIENT,
            senderName = "مريض آخر",
            body = "رسالة أخرى",
            createdAtMillis = 3000L,
        )
        db.tebDao().insertDirectMessage(msg1)
        db.tebDao().insertDirectMessage(msg2)
        db.tebDao().insertDirectMessage(msg3)

        assertTrue(repository.deleteDirectMessageByAdmin("msg_adm_del_1").isSuccess)
        val afterSingle = repository.directMessagesFlow.first()
        assertFalse(afterSingle.any { it.id == "msg_adm_del_1" })
        assertTrue(afterSingle.any { it.id == "msg_adm_del_2" })

        val clearedCount = repository.deleteChatThreadByAdmin(
            threadType = com.example.data.local.ChatThreadType.CLINIC_ADMIN,
            doctorId = doc.id,
        ).getOrThrow()
        assertEquals(1, clearedCount)
        val afterThread = repository.directMessagesFlow.first()
        assertFalse(afterThread.any { it.threadType == com.example.data.local.ChatThreadType.CLINIC_ADMIN && it.doctorId == doc.id })
        assertTrue(afterThread.any { it.id == "msg_adm_keep_3" })
    }

    @Test
    fun permanentMedicalLogGoogleDrivePerClinicAndRetentionPruningWork() = runBlocking {
        repository.ensureSeeded()
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
        assertEquals("teb_alhajayzah_permanent_logs", com.example.config.CloudNamespacePaths.PERMANENT_LOGS)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 1. Google Drive Web App URL priority: clinic URL overrides platform fallback; otherwise falls back to platform URL
        assertEquals(
            "https://script.google.com/macros/s/clinic123/exec",
            com.example.data.remote.GoogleDriveStorageHelper.resolveWebAppUrl(
                clinicDriveUrl = "https://script.google.com/macros/s/clinic123/exec",
                platformFallbackDriveUrl = "https://script.google.com/macros/s/fallback999/exec",
            ),
        )
        assertEquals(
            "https://script.google.com/macros/s/fallback999/exec",
            com.example.data.remote.GoogleDriveStorageHelper.resolveWebAppUrl(
                clinicDriveUrl = "",
                platformFallbackDriveUrl = "https://script.google.com/macros/s/fallback999/exec",
            ),
        )
        assertEquals(360, com.example.data.remote.FirestoreCloudSyncService.normalizeRetentionDays(45))
        assertEquals(90, com.example.data.remote.FirestoreCloudSyncService.normalizeRetentionDays(90))
        assertEquals(180, com.example.data.remote.FirestoreCloudSyncService.normalizeRetentionDays(180))
        assertEquals(360, com.example.data.remote.FirestoreCloudSyncService.normalizeRetentionDays(360))

        // 2. Super Admin saves clinic Drive URL & platform fallback + retention policy (90 days)
        val clinicDriveUrl = "https://script.google.com/macros/s/clinic_obgyn/exec"
        val doc = repository.addDoctorByAdmin(
            doctorName = "د. هدى سالم",
            specialty = "النساء والتوليد وتأخر الإنجاب",
            clinicName = "عيادة د. هدى",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "الحجايزة",
            bio = "",
            mapsUrl = "",
            feeEgp = 180,
            workStartMinutes = 17 * 60,
            workEndMinutes = 19 * 60,
            slotDurationMinutes = 20,
            username = "dr_hoda",
            password = "123",
            driveWebAppUrl = clinicDriveUrl,
        ).getOrThrow()
        assertEquals(clinicDriveUrl, doc.driveWebAppUrl)

        repository.updateFullPlatformConfig(
            platformNameAr = "طب الحجايزة",
            villageNameAr = "قرية الحجايزة والقرى المجاورة",
            hospitalNameAr = "مستشفى الحجايزة التخصصي",
            supportPhone = "01125002428",
            homeAnnouncementAr = "",
            apkUrl = AppConfig.defaultBrand.apkDownloadUrl,
            medsApiUrl = "",
            adminPin = "132027",
            defaultDriveWebAppUrl = "https://script.google.com/macros/s/platform_default/exec",
            fileRetentionDays = 90,
        )
        val savedCfg = repository.brandConfigFlow.first()
        assertEquals("https://script.google.com/macros/s/platform_default/exec", savedCfg.defaultDriveWebAppUrl)
        assertEquals(90, savedCfg.fileRetentionDays)

        // 3. Register patient & create: (a) patient lab upload, (b) clinic visit Rx, (c) external visit
        val acc = repository.registerPatientAccount(
            fullName = "فاطمة عبد الله حسن",
            phone = "01033445566",
            nationalId = "",
            password = "2468",
        ).getOrThrow()
        val member = repository.observeFamilyMembers(acc.id).first().first()

        val attachmentsDir = java.io.File(context.filesDir, "medical_images").apply { mkdirs() }
        val oldLabFile = java.io.File(attachmentsDir, "old_cbc.pdf").apply { writeText("%PDF-1.4 old lab") }
        val recentLabFile = java.io.File(attachmentsDir, "recent_xray.jpg").apply { writeText("recent image bytes") }

        val uploadRec = repository.addPatientUploadedAttachment(
            familyMemberId = member.id,
            patientName = member.fullName,
            patientPhone = acc.phone,
            title = "تحليل بول شامل Urine Analysis",
            notes = "Pus cells: 12-15 /HPF",
            attachmentPath = oldLabFile.absolutePath,
            attachmentMimeType = "application/pdf",
            attachmentName = "old_cbc.pdf",
        )

        val recentRec = repository.addPatientUploadedAttachment(
            familyMemberId = member.id,
            patientName = member.fullName,
            patientPhone = acc.phone,
            title = "أشعة صدر",
            notes = "سليمة وضمن الطبيعي",
            attachmentPath = recentLabFile.absolutePath,
            attachmentMimeType = "image/jpeg",
            attachmentName = "recent_xray.jpg",
        )

        val slot = repository.observeSlotsForDoctor(doc.id).first().first { it.status == SlotStatus.AVAILABLE }
        assertTrue(repository.bookSlot(slot.id, member.fullName, acc.phone, member.id, BookingSource.APP).isSuccess)
        val rxRec = repository.completeVisitAndSavePrescription(
            slotId = slot.id,
            diagnosis = "أنيميا الحمل البسيطة",
            medications = "• حمض الفوليك 5 مجم — قرص يومياً\n• أوستيوكير أقراص — قرص بعد الغداء",
            notes = "المتابعة بعد أسبوعين",
            attachmentPath = "",
        ).getOrThrow()

        val extRec = repository.addExternalDoctorVisitRecord(
            familyMemberId = member.id,
            patientName = member.fullName,
            patientPhone = acc.phone,
            doctorName = "أحمد شريف",
            specialty = "الباطنة العامة والجهاز الهضمي والكبد",
            visitDateLabel = "20/11/2026",
            diagnosis = "التهاب بالمعدة",
            medicationsText = "• كونترولوك 40 مجم — قرص قبل الإفطار لمدة 14 يوم",
            notes = "تجنب الأطعمة الحارة",
            attachmentPath = "",
        ).getOrThrow()

        // Verify all 4 events auto-created entries in permanent_medical_logs immediately
        val allLogsBefore = repository.allPermanentLogsFlow.first().filter { it.familyMemberId == member.id }
        assertEquals(4, allLogsBefore.size)

        val uploadLog = allLogsBefore.first { it.sourceRecordId == uploadRec.id }
        assertEquals(com.example.data.local.PermanentLogEventType.PATIENT_UPLOAD, uploadLog.eventType)
        assertTrue(uploadLog.narrativeAr.contains("خلايا الصديد"))
        assertTrue(uploadLog.narrativeAr.contains("استرشادي") || uploadLog.narrativeAr.contains("لا يغني"))
        assertFalse(uploadLog.narrativeAr.contains("Pus cells", ignoreCase = true))

        val rxLog = allLogsBefore.first { it.sourceRecordId == rxRec.id }
        assertEquals(com.example.data.local.PermanentLogEventType.CLINIC_VISIT, rxLog.eventType)
        assertTrue(rxLog.narrativeAr.contains("د. هدى سالم"))
        assertTrue(rxLog.narrativeAr.contains("أنيميا الحمل البسيطة"))
        assertTrue(rxLog.narrativeAr.contains("حمض الفوليك"))
        assertTrue(rxLog.narrativeAr.contains("سبب الوصف ودواعي الاستخدام"))

        val extLog = allLogsBefore.first { it.sourceRecordId == extRec.id }
        assertEquals(com.example.data.local.PermanentLogEventType.EXTERNAL_VISIT, extLog.eventType)
        assertTrue(extLog.narrativeAr.contains("كونترولوك"))
        assertTrue(extLog.narrativeAr.contains("حماية المعدة"))

        // 4. Simulate Drive backup URL + old timestamp (120 days ago > 90 days retention) on uploadRec
        val nowMs = System.currentTimeMillis()
        val expiredTimestamp = nowMs - (120L * TebRepository.ONE_DAY_MILLIS)
        db.tebDao().insertMedicalRecord(
            uploadRec.copy(
                createdAtMillis = expiredTimestamp,
                driveFileUrl = "https://drive.google.com/file/d/fake_drive_id/view",
            )
        )
        val prunedCount = repository.pruneExpiredHeavyAttachments(nowMillis = nowMs)
        assertEquals(1, prunedCount)
        assertFalse(oldLabFile.exists())
        assertTrue(recentLabFile.exists())

        val prunedRec = db.tebDao().getMedicalRecordById(uploadRec.id)!!
        assertEquals("", prunedRec.attachmentPath)
        assertEquals("https://drive.google.com/file/d/fake_drive_id/view", prunedRec.driveFileUrl)

        // Manual deletion of recent attachment file removes the physical file & record, NEVER the permanent log!
        assertTrue(repository.deleteMedicalRecordOrAttachment(recentRec.id).isSuccess)
        assertFalse(recentLabFile.exists())

        val allLogsAfterCleanupAndDelete = repository.allPermanentLogsFlow.first().filter { it.familyMemberId == member.id }
        assertEquals(4, allLogsAfterCleanupAndDelete.size)
        assertTrue(allLogsAfterCleanupAndDelete.any { it.sourceRecordId == uploadRec.id && it.narrativeAr.contains("خلايا الصديد") })
        assertTrue(allLogsAfterCleanupAndDelete.any { it.sourceRecordId == recentRec.id })
    }

    @Test
    fun adminPin132027SessionPersistenceDualDriveFamilyLinkingAndRealVisitorStatsWork() = runBlocking {
        repository.ensureSeeded()

        // 1. Default PIN 132027 + Persistent Session + Manual Logout
        assertFalse(repository.isAdminLoggedInFlow.first())
        assertTrue(repository.verifyAdminPin("202600").isFailure)
        assertTrue(repository.verifyAdminPin("132027").isSuccess)
        assertTrue(repository.isAdminLoggedInFlow.first())
        repository.setAdminLoggedIn(false)
        assertFalse(repository.isAdminLoggedInFlow.first())

        // 2. Dual Google Drive URL resolution order (Primary -> Secondary -> Platform Fallback)
        val candidates = com.example.data.remote.GoogleDriveStorageHelper.resolveCandidateUrls(
            primaryClinicUrl = "https://script.google.com/macros/s/clinic_1/exec",
            secondaryClinicUrl = "https://script.google.com/macros/s/clinic_2/exec",
            platformFallbackUrl = "https://script.google.com/macros/s/platform_fb/exec",
        )
        assertEquals(
            listOf(
                "https://script.google.com/macros/s/clinic_1/exec",
                "https://script.google.com/macros/s/clinic_2/exec",
                "https://script.google.com/macros/s/platform_fb/exec",
            ),
            candidates
        )
        val firstDoc = repository.doctorsFlow.first().first()
        assertTrue(
            repository.updateDoctorDriveAccounts(
                doctorId = firstDoc.id,
                primaryUrl = "https://script.google.com/macros/s/clinic_p/exec",
                secondaryUrl = "https://script.google.com/macros/s/clinic_s/exec",
            ).isSuccess
        )
        val updatedDoc = repository.doctorsFlow.first().first { it.id == firstDoc.id }
        assertEquals("https://script.google.com/macros/s/clinic_p/exec", updatedDoc.driveWebAppUrl)
        assertEquals("https://script.google.com/macros/s/clinic_s/exec", updatedDoc.secondaryDriveWebAppUrl)

        // 3. Family Spouse Linking, Proof Approval & Strict Personal Record Privacy Switch
        val husband = repository.registerPatientAccount(
            fullName = "أحمد محمود علي",
            phone = "01011223344",
            nationalId = "29001011234567",
            password = "1234",
        ).getOrThrow()
        val wife = repository.registerPatientAccount(
            fullName = "منى حسن إبراهيم",
            phone = "01099887766",
            nationalId = "29202021234567",
            password = "5678",
        ).getOrThrow()
        val child = repository.addFamilyMember(
            accountId = husband.id,
            fullName = "يوسف أحمد محمود",
            relation = "الابن",
            age = 7,
            phone = husband.phone,
            nationalId = "31905051234567",
        )
        assertEquals("31905051234567", child.nationalId)

        val linkReq = repository.submitFamilyLinkRequest(
            requesterAccountId = wife.id,
            targetPhone = husband.phone,
            relation = "SPOUSE",
            proofMarriageContractUrl = "data:image/jpeg;base64,marriage",
            proofNationalIdFrontUrl = "data:image/jpeg;base64,front",
            proofNationalIdBackUrl = "data:image/jpeg;base64,back",
        ).getOrThrow()
        assertEquals(com.example.data.local.FamilyLinkStatus.PENDING, linkReq.status)

        assertTrue(repository.approveFamilyLinkRequest(linkReq.id).isSuccess)

        val wifeVisibleBeforeOptIn = repository.observeFamilyMembers(wife.id).first()
        assertTrue(wifeVisibleBeforeOptIn.any { it.id == child.id })
        assertFalse(wifeVisibleBeforeOptIn.any { it.accountId == husband.id && it.relation.contains("نفسي") })

        assertTrue(repository.setSpouseViewPermission(husband.id, true).isSuccess)
        val wifeVisibleAfterOptIn = repository.observeFamilyMembers(wife.id).first()
        assertTrue(wifeVisibleAfterOptIn.any { it.accountId == husband.id && it.relation.contains("نفسي") })

        // 4. Real Visitor Stats (APK vs Web, 0 when empty)
        val emptyStats = repository.getVisitorStats()
        assertEquals(0, emptyStats.todayTotal)
        assertEquals(0, emptyStats.todayApk)
        assertEquals(0, emptyStats.todayWeb)

        repository.recordVisitorHit("APK")
        repository.recordVisitorHit("APK")
        repository.recordVisitorHit("WEB")
        val populatedStats = repository.getVisitorStats()
        assertEquals(3, populatedStats.todayTotal)
        assertEquals(2, populatedStats.todayApk)
        assertEquals(1, populatedStats.todayWeb)
        assertEquals(2, populatedStats.weekApk)
        assertEquals(1, populatedStats.monthWeb)
    }

    @Test
    fun specialtyClinicalTemplatesCustomFavoritesAndGeneralManagerConfigWork() {
        assertEquals("طب الحجايزة", AppConfig.defaultBrand.platformNameAr)
        assertEquals("v1.0.0", AppConfig.defaultBrand.appVersionLabel)
        assertEquals("132027", AppConfig.defaultBrand.adminPin)
        assertEquals("المدير العام", AppConfig.defaultBrand.adminOwnerName)
        assertEquals("General Manager", AppConfig.defaultBrand.adminOwnerNameEn)

        val obgynDoc = com.example.data.local.ClinicDoctorEntity(
            id = "doc_obgyn_test",
            doctorName = "د. محمد فكري",
            specialty = "النساء والتوليد وتأخر الإنجاب",
            clinicName = "عيادة النساء والتوليد",
            clinicType = ClinicType.PRIVATE,
            locationLabel = "قرية الحجايزة",
            feeEgp = 150,
            workStartMinutes = 17 * 60,
            workEndMinutes = 21 * 60,
            slotDurationMinutes = 20,
            username = "dr_test1",
            password = "123",
        )
        val obgynDiags = com.example.util.SpecialtyClinicalTemplates.diagnosesForDoctor(obgynDoc)
        val obgynMeds = com.example.util.SpecialtyClinicalTemplates.medicationsForDoctor(obgynDoc)
        assertTrue(obgynDiags.any { it.ar.contains("متابعة حمل طبيعي") })
        assertTrue(obgynDiags.any { it.ar.contains("أنيميا الحمل") })
        assertTrue(obgynMeds.any { it.tradeName.contains("حمض الفوليك") })
        assertTrue(obgynMeds.any { it.tradeName.contains("دوفاستون") })

        val pedDoc = obgynDoc.copy(id = "doc_ped_test", specialty = "طب الأطفال وحديثي الولادة")
        val pedDiags = com.example.util.SpecialtyClinicalTemplates.diagnosesForDoctor(pedDoc)
        val pedMeds = com.example.util.SpecialtyClinicalTemplates.medicationsForDoctor(pedDoc)
        assertTrue(pedDiags.any { it.ar.contains("صفراء حديثي الولادة") })
        assertTrue(pedDiags.any { it.ar.contains("نزلة معوية") })
        assertTrue(pedMeds.any { it.tradeName.contains("Cetal") })
        assertTrue(pedMeds.any { it.tradeName.contains("ORS") })

        val customMedKey = com.example.util.SpecialtyClinicalTemplates.encodeCustomMedFav("بانادول إكسترا | Extra", "قرص كل 8 ساعات")
        val decodedMed = com.example.util.SpecialtyClinicalTemplates.decodeCustomMedFav(customMedKey)
        assertEquals("بانادول إكسترا   Extra" to "قرص كل 8 ساعات", decodedMed)

        val customDiagKey = com.example.util.SpecialtyClinicalTemplates.encodeCustomDiagFav("التهاب حاد بالمعدة")
        assertEquals("التهاب حاد بالمعدة", com.example.util.SpecialtyClinicalTemplates.decodeCustomDiagFav(customDiagKey))
    }

    @Test
    fun newUserAdminAlertClinicPhoneActivationAdminWarningAndDriveCleanupWork() = runBlocking {
        repository.ensureSeeded()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 1. Registering a new patient immediately creates an ADMIN notification
        val newAcc = repository.registerPatientAccount(
            fullName = "علاء الدين مصطفى",
            phone = "01044556677",
            nationalId = "",
            password = "1357",
        ).getOrThrow()

        val adminAlerts = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.ADMIN &&
                it.targetId == newAcc.id
        }
        assertEquals(1, adminAlerts.size)
        assertTrue(adminAlerts.first().message.contains("علاء الدين مصطفى"))
        assertTrue(adminAlerts.first().message.contains("01044556677"))

        // 2. Doctor activates patient account directly by phone number -> sets verifiedBy = "عيادة د. ..."
        val activatedByClinic = repository.activatePatientByClinicPhone(
            rawPhone = "٠١٠٤٤٥٥٦٦٧٧",
            doctorName = "محمد فكري",
        ).getOrThrow()
        assertTrue(activatedByClinic.isPhoneVerified)
        assertEquals("عيادة د. محمد فكري", activatedByClinic.verifiedBy)

        // 3. Super Admin sends a warning message to patient -> creates warning chat bubble & targeted PATIENT notification
        val warnMsg = repository.sendAdminReplyToPatient(
            patientAccountId = newAcc.id,
            patientName = newAcc.fullName,
            patientPhone = newAcc.phone,
            body = "يرجى عدم تكرار الحجز الوهمي وإلا سيتم إيقاف الحساب",
            isWarning = true,
        ).getOrThrow()
        assertTrue(warnMsg.body.contains("[تحذير إداري]"))

        val patientWarnNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.PATIENT &&
                it.targetId == newAcc.id
        }
        assertTrue(patientWarnNotifs.any { it.message.contains("تحذير إداري") })

        // 4. Storage 90% alert & manual heavy attachment cleanup preserve permanent_medical_logs 100%
        repository.emitStorage90PercentAlert("عيادة د. محمد فكري — Drive #1", 92)
        val storageAlerts = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.ADMIN &&
                it.message.contains("92%")
        }
        assertEquals(1, storageAlerts.size)

        val member = repository.observeFamilyMembers(newAcc.id).first().first()
        val attachmentsDir = java.io.File(context.filesDir, "medical_images").apply { mkdirs() }
        val heavyFile = java.io.File(attachmentsDir, "heavy_mri.pdf").apply { writeText("%PDF-1.4 heavy") }
        val rec = repository.addPatientUploadedAttachment(
            familyMemberId = member.id,
            patientName = member.fullName,
            patientPhone = newAcc.phone,
            title = "أشعة رنين مغناطيسي",
            notes = "سليمة ولا توجد مشاكل",
            attachmentPath = heavyFile.absolutePath,
            attachmentMimeType = "application/pdf",
            attachmentName = "heavy_mri.pdf",
        )
        val logsBefore = repository.allPermanentLogsFlow.first().filter { it.familyMemberId == member.id }
        assertEquals(1, logsBefore.size)

        val cleanedCount = repository.cleanHeavyAttachmentsForStorageTarget("__ALL__")
        assertEquals(1, cleanedCount)
        assertFalse(heavyFile.exists())

        val recAfterClean = db.tebDao().getMedicalRecordById(rec.id)!!
        assertEquals("", recAfterClean.attachmentPath)
        assertEquals("", recAfterClean.driveFileUrl)

        val logsAfterClean = repository.allPermanentLogsFlow.first().filter { it.familyMemberId == member.id }
        assertEquals(1, logsAfterClean.size)
        assertEquals(logsBefore.first().id, logsAfterClean.first().id)
    }

    @Test
    fun accountPhoneCloudKeySupportMessageAdminAlertAndMessageReadCompatibilityWork() = runBlocking {
        repository.ensureSeeded()

        // 1. Account cloud document key resolves to normalized phone digits under named DB + teb_alhajayzah_accounts/{phone}
        assertEquals(
            "01012345678",
            com.example.data.remote.FirestoreCloudSyncService.accountCloudDocId("٠١٠١٢٣٤٥٦٧٨", "acc_fallback"),
        )
        assertEquals(
            "01141271027",
            com.example.data.remote.FirestoreCloudSyncService.accountCloudDocId("+201141271027", "acc_fallback"),
        )
        val url = com.example.data.remote.FirestoreCloudSyncService.documentUrl(
            projectId = "gen-lang-client-0015800098",
            apiKey = "test-key",
            subCollection = "accounts",
            docId = com.example.data.remote.FirestoreCloudSyncService.accountCloudDocId("01099887766", "acc_x"),
        )
        assertTrue(
            url.contains(
                "/databases/${com.example.config.CloudNamespacePaths.FIRESTORE_DATABASE_ID}/documents/teb_alhajayzah_accounts/01099887766?"
            )
        )
        val sampleAccUnverified = com.example.data.local.PatientAccountEntity(
            id = "acc_rt_1",
            fullName = "تجربة موثق",
            phone = "01099887766",
            nationalId = "",
            password = "1234",
            isPhoneVerified = false,
            otpCode = "654321",
        )
        val unverifiedJson = com.example.data.remote.FirestoreCloudSyncService.accountToJson(sampleAccUnverified)
        assertFalse(unverifiedJson.getBoolean("isVerified"))
        assertFalse(unverifiedJson.getBoolean("isPhoneVerified"))
        assertFalse(com.example.data.remote.FirestoreCloudSyncService.accountFromJson(unverifiedJson)!!.isPhoneVerified)
        val verifiedJson = com.example.data.remote.FirestoreCloudSyncService.accountToJson(
            sampleAccUnverified.copy(isPhoneVerified = true)
        )
        assertTrue(verifiedJson.getBoolean("isVerified"))
        assertTrue(com.example.data.remote.FirestoreCloudSyncService.accountFromJson(verifiedJson)!!.isPhoneVerified)

        // 2. Web message JSON with only legacy isRead=false maps accurately to unread for target role
        val webSupportMsgJson = org.json.JSONObject().apply {
            put("id", "msg_web_1")
            put("threadType", com.example.data.local.ChatThreadType.PATIENT_ADMIN)
            put("patientAccountId", "acc_p1")
            put("patientName", "سالم محمد")
            put("patientPhone", "01055443322")
            put("senderRole", com.example.data.local.MessageSenderRole.PATIENT)
            put("senderName", "سالم محمد")
            put("body", "أحتاج تفعيل الحساب فوراً")
            put("isRead", false)
            put("createdAtMillis", 1_700_000_000_000L)
        }
        val parsedMsg = com.example.data.remote.FirestoreCloudSyncService.messageFromJson(webSupportMsgJson)!!
        assertTrue(parsedMsg.isReadByPatient)
        assertFalse(parsedMsg.isReadByAdmin)
        val roundTripJson = com.example.data.remote.FirestoreCloudSyncService.messageToJson(parsedMsg)
        assertFalse(roundTripJson.getBoolean("isRead"))

        // 3. Sending a support message from patient creates an ADMIN notification immediately
        val acc = repository.registerPatientAccount(
            fullName = "خالد محمود حسن",
            phone = "01077889900",
            nationalId = "",
            password = "2468",
        ).getOrThrow()
        assertFalse(acc.isPhoneVerified)

        val supportMsg = repository.sendPatientToAdminMessage(
            account = acc,
            body = "لو سمحت الكود لم يصلني بعد",
        ).getOrThrow()
        assertEquals(com.example.data.local.ChatThreadType.PATIENT_ADMIN, supportMsg.threadType)

        val adminSupportNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.ADMIN &&
                it.targetId == acc.id &&
                it.message.contains("لو سمحت الكود لم يصلني بعد")
        }
        assertEquals(1, adminSupportNotifs.size)

        // 4. Regenerating OTP & verifying via inline OTP flow unlocks account verification cleanly
        val renewed = repository.regenerateAccountOtp(acc.id).getOrThrow()
        assertEquals(6, renewed.otpCode.length)
        val verifyRes = repository.verifyAndBurnActivationCode(
            accountId = acc.id,
            accountName = acc.fullName,
            accountPhone = acc.phone,
            enteredCode = renewed.otpCode,
        )
        assertTrue(verifyRes.isSuccess)
        val verifiedAcc = db.tebDao().getAccountById(acc.id)!!
        assertTrue(verifiedAcc.isPhoneVerified)
        assertEquals("كود OTP", verifiedAcc.verifiedBy)
    }

    @Test
    fun auditGapChecksNumericPinFcmPathOtpFamilyLinkRxAndLabNotificationsWork() = runBlocking {
        repository.ensureSeeded()

        // 1. Numeric-only PIN enforcement on registerPatientAccount
        val nonNumericPinRes = repository.registerPatientAccount(
            fullName = "عميل تجربة",
            phone = "01011223344",
            nationalId = "",
            password = "abc123",
        )
        assertTrue(nonNumericPinRes.isFailure)

        val shortPinRes = repository.registerPatientAccount(
            fullName = "عميل تجربة",
            phone = "01011223344",
            nationalId = "",
            password = "123",
        )
        assertTrue(shortPinRes.isFailure)

        // 2. FCM tokens namespace & URL builder
        assertEquals("teb_alhajayzah_fcm_tokens", com.example.config.CloudNamespacePaths.FCM_TOKENS)
        assertEquals("teb_alhajayzah_fcm_tokens", com.example.config.CloudNamespacePaths.DATA_FCM_TOKENS)
        val fcmDocUrl = com.example.data.remote.FirestoreCloudSyncService.fcmTokensDocUrl(
            projectId = "gen-lang-client-0015800098",
            apiKey = "test-key",
            docId = "user_010",
        )
        assertTrue(
            fcmDocUrl.contains(
                "/databases/${com.example.config.CloudNamespacePaths.FIRESTORE_DATABASE_ID}/documents/teb_alhajayzah_fcm_tokens/user_010?"
            )
        )

        // 3. Valid registration + regenerateAccountOtp(notifyAdmin = true) emits ADMIN notification
        val p1 = repository.registerPatientAccount(
            fullName = "طارق محمود سالم",
            phone = "01011223344",
            nationalId = "29201011200999",
            password = "5678",
        ).getOrThrow()
        val p2 = repository.registerPatientAccount(
            fullName = "سارة أحمد علي",
            phone = "01022334455",
            nationalId = "",
            password = "9012",
        ).getOrThrow()

        val renewedWithNotify = repository.regenerateAccountOtp(p1.id, notifyAdmin = true).getOrThrow()
        val otpAdminNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.ADMIN &&
                it.targetId == p1.id &&
                it.message.contains(renewedWithNotify.otpCode)
        }
        assertEquals(1, otpAdminNotifs.size)

        // 4. Family link request creates ADMIN notification
        val flr = repository.submitFamilyLinkRequest(
            requesterAccountId = p1.id,
            targetPhone = p2.phone,
            relation = "SPOUSE",
        ).getOrThrow()
        val flrNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.ADMIN &&
                it.targetId == flr.id
        }
        assertEquals(1, flrNotifs.size)

        // 5. Completing visit with prescription emits PATIENT notification
        val doc = repository.doctorsFlow.first().first()
        val slot = repository.observeSlotsForDoctor(doc.id).first().first()
        val member1 = repository.observeFamilyMembers(p1.id).first().first()
        assertTrue(repository.bookSlot(slot.id, p1.fullName, p1.phone, member1.id, BookingSource.APP).isSuccess)
        val rxRecord = repository.completeVisitAndSavePrescription(
            slotId = slot.id,
            diagnosis = "التهاب الحلق الحاد",
            medications = "أوجمنتين 1 جم كل 12 ساعة",
            notes = "شرب سوائل دافئة",
            attachmentPath = "",
        ).getOrThrow()
        assertEquals(com.example.data.local.MedicalRecordType.PRESCRIPTION, rxRecord.recordType)
        val rxPatientNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.PATIENT &&
                it.targetId == p1.id &&
                it.message.contains("التهاب الحلق الحاد")
        }
        assertEquals(1, rxPatientNotifs.size)

        // 6. Uploading lab result emits PATIENT notification
        val lab = repository.labsFlow.first().firstOrNull() ?: repository.addLabByAdmin(
            labName = "معمل الشفاء للتحاليل",
            phone = "01155443322",
            locationLabel = "الحجايزة",
            username = "shifa_lab",
            password = "1234",
        ).getOrThrow()
        val labRecord = repository.addLabResultRecord(
            lab = lab,
            member = member1,
            accountPhone = p1.phone,
            testTitle = "تحليل صورة دم كاملة CBC",
            notes = "الهيموجلوبين طبيعي",
            attachmentPath = "/tmp/cbc_report.pdf",
            attachmentMimeType = "application/pdf",
            attachmentName = "cbc_report.pdf",
        ).getOrThrow()
        assertEquals(com.example.data.local.MedicalRecordType.LAB_RESULT, labRecord.recordType)
        val labPatientNotifs = repository.notificationsFlow.first().filter {
            it.targetAudience == com.example.data.local.NotificationAudience.PATIENT &&
                it.targetId == p1.id &&
                it.message.contains("CBC")
        }
        assertEquals(1, labPatientNotifs.size)
    }
}
