package com.example

import com.example.data.medications.MedicationCatalog
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun slotTimeFormattingIsAccurate() {
        assertEquals("5:00 م", TimeUtils.formatMinutesAr(17 * 60))
        assertEquals("6:40 م", TimeUtils.formatMinutesAr(18 * 60 + 40))
    }

    @Test
    fun medicationCatalogCoversCoreSpecialties() {
        val coveredSpecialties = MedicationCatalog.builtInMedications.map { it.specialty }.toSet()
        assertTrue("باطنة وجهاز هضمي" in coveredSpecialties)
        assertTrue("أطفال وحديثي الولادة" in coveredSpecialties)
        assertTrue("عظام ومفاصل" in coveredSpecialties)
        assertTrue("نساء وتوليد" in coveredSpecialties)
        assertTrue("أسنان" in coveredSpecialties)
    }
}
