package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.config.AppConfig
import com.example.data.local.BookingSource
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
}
