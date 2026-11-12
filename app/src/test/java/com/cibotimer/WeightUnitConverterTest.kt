package com.cibotimer

import com.cibotimer.data.models.WeightUnit
import com.cibotimer.domain.WeightUnitConverter
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WeightUnitConverterTest {

    @Test
    fun testKgToDisplayAndDisplayToKg() {
        // KG unit
        assertEquals(75.5, WeightUnitConverter.kgToDisplay(75.5, WeightUnit.KG), 0.0001)
        assertEquals(75.5, WeightUnitConverter.displayToKg(75.5, WeightUnit.KG), 0.0001)

        // LB unit
        val displayLb = WeightUnitConverter.kgToDisplay(100.0, WeightUnit.LB)
        assertEquals(220.462262, displayLb, 0.0001)

        val kgFromLb = WeightUnitConverter.displayToKg(220.462262, WeightUnit.LB)
        assertEquals(100.0, kgFromLb, 0.0001)
    }

    @Test
    fun testRoundTripStabilityWithoutDrifting() {
        val testLbValues = listOf(100.0, 135.5, 175.2, 200.0, 215.8, 300.4)
        for (lb in testLbValues) {
            val savedKg = WeightUnitConverter.displayToKg(lb, WeightUnit.LB)
            val redisplayedLb = WeightUnitConverter.kgToDisplayRounded(savedKg, WeightUnit.LB)
            assertEquals("Round-trip stability failed for $lb lb", lb, redisplayedLb, 0.0001)
        }
    }

    @Test
    fun testFormatWeight() {
        val weightKg = 75.5
        val formattedKg = WeightUnitConverter.formatWeight(weightKg, WeightUnit.KG, locale = Locale.US)
        assertEquals("75.5 kg", formattedKg)

        val formattedKgNoUnit = WeightUnitConverter.formatWeight(weightKg, WeightUnit.KG, showUnit = false, locale = Locale.US)
        assertEquals("75.5", formattedKgNoUnit)

        // 75.5 * 2.20462262 = 166.4489... -> 166.4 lb
        val formattedLb = WeightUnitConverter.formatWeight(weightKg, WeightUnit.LB, locale = Locale.US)
        assertEquals("166.4 lb", formattedLb)
    }

    @Test
    fun testFormatDelta() {
        assertEquals("-1.5 kg", WeightUnitConverter.formatDelta(-1.5, WeightUnit.KG, locale = Locale.US))
        assertEquals("+2.0 kg", WeightUnitConverter.formatDelta(2.0, WeightUnit.KG, locale = Locale.US))
        assertEquals("+0.0 kg", WeightUnitConverter.formatDelta(0.0, WeightUnit.KG, locale = Locale.US))

        // 1.0 kg -> +2.2 lb
        assertEquals("+2.2 lb", WeightUnitConverter.formatDelta(1.0, WeightUnit.LB, locale = Locale.US))
        assertEquals("-2.2 lb", WeightUnitConverter.formatDelta(-1.0, WeightUnit.LB, locale = Locale.US))
    }

    @Test
    fun testIsValidWeight() {
        // Valid KG bounds [25.0, 350.0]
        assertTrue(WeightUnitConverter.isValidWeight(25.0, WeightUnit.KG))
        assertTrue(WeightUnitConverter.isValidWeight(350.0, WeightUnit.KG))
        assertTrue(WeightUnitConverter.isValidWeight(75.0, WeightUnit.KG))
        assertFalse(WeightUnitConverter.isValidWeight(24.9, WeightUnit.KG))
        assertFalse(WeightUnitConverter.isValidWeight(350.1, WeightUnit.KG))

        // Valid LB bounds: 25 kg ≈ 55.11 lb, 350 kg ≈ 771.61 lb
        assertTrue(WeightUnitConverter.isValidWeight(55.2, WeightUnit.LB))
        assertTrue(WeightUnitConverter.isValidWeight(175.0, WeightUnit.LB))
        assertTrue(WeightUnitConverter.isValidWeight(770.0, WeightUnit.LB))
        assertFalse(WeightUnitConverter.isValidWeight(50.0, WeightUnit.LB))
        assertFalse(WeightUnitConverter.isValidWeight(800.0, WeightUnit.LB))
    }

    @Test
    fun testDefaultWeightUnitForLocale() {
        assertEquals(WeightUnit.LB, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale.US))
        assertEquals(WeightUnit.LB, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale("en", "US")))
        assertEquals(WeightUnit.LB, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale("en", "LR")))
        assertEquals(WeightUnit.LB, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale("my", "MM")))

        assertEquals(WeightUnit.KG, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale.ITALY))
        assertEquals(WeightUnit.KG, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale.UK))
        assertEquals(WeightUnit.KG, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale.GERMANY))
        assertEquals(WeightUnit.KG, WeightUnitConverter.getDefaultWeightUnitForLocale(Locale.FRANCE))
    }

    @Test
    fun testGetFormattedBounds() {
        val (minKg, maxKg) = WeightUnitConverter.getFormattedBounds(WeightUnit.KG, locale = Locale.US)
        assertEquals("25.0 kg", minKg)
        assertEquals("350.0 kg", maxKg)

        val (minLb, maxLb) = WeightUnitConverter.getFormattedBounds(WeightUnit.LB, locale = Locale.US)
        assertTrue(minLb.endsWith("lb"))
        assertTrue(maxLb.endsWith("lb"))
    }
}
