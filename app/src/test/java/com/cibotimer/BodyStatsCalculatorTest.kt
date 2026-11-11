package com.cibotimer

import com.cibotimer.data.models.WeightEntry
import com.cibotimer.domain.BodyStatsCalculator
import org.junit.Assert.*
import org.junit.Test

class BodyStatsCalculatorTest {

    @Test
    fun testWeightLossProgressNormal() {
        val result = BodyStatsCalculator.calculateWeightProgress(
            startingWeightKg = 85.0,
            currentWeightKg = 81.2,
            targetWeightKg = 75.0
        )
        assertEquals(85.0, result.startingWeightKg, 0.01)
        assertEquals(81.2, result.currentWeightKg, 0.01)
        assertEquals(75.0, result.targetWeightKg, 0.01)
        assertEquals(3.8, result.lostKg, 0.01)
        assertEquals(6.2, result.remainingKg, 0.01)
        assertEquals(10.0, result.totalGoalDeltaKg, 0.01)
        assertEquals(0.38f, result.progressFraction, 0.01f)
        assertEquals(38, result.progressPercent)
        assertFalse(result.isGoalReached)
        assertTrue(result.isWeightLoss)
    }

    @Test
    fun testWeightLossProgressGoalReached() {
        val result = BodyStatsCalculator.calculateWeightProgress(
            startingWeightKg = 80.0,
            currentWeightKg = 74.5,
            targetWeightKg = 75.0
        )
        assertEquals(5.5, result.lostKg, 0.01)
        assertEquals(0.0, result.remainingKg, 0.01)
        assertEquals(1.0f, result.progressFraction, 0.001f)
        assertEquals(100, result.progressPercent)
        assertTrue(result.isGoalReached)
    }

    @Test
    fun testWeightGainProgress() {
        val result = BodyStatsCalculator.calculateWeightProgress(
            startingWeightKg = 65.0,
            currentWeightKg = 68.0,
            targetWeightKg = 70.0
        )
        assertFalse(result.isWeightLoss)
        assertEquals(3.0, result.lostKg, 0.01) // gained 3.0 kg
        assertEquals(2.0, result.remainingKg, 0.01)
        assertEquals(0.60f, result.progressFraction, 0.01f)
        assertEquals(60, result.progressPercent)
        assertFalse(result.isGoalReached)
    }

    @Test
    fun testCalculateWhr() {
        // Waist 80 cm, Hips 100 cm -> 0.80 (Optimal)
        val optimal = BodyStatsCalculator.calculateWhr(80.0, 100.0)
        assertNotNull(optimal)
        assertEquals(0.80, optimal!!.ratio, 0.01)
        assertTrue(optimal.category.contains("Ottimale"))

        // Waist 88 cm, Hips 100 cm -> 0.88 (Moderate)
        val moderate = BodyStatsCalculator.calculateWhr(88.0, 100.0)
        assertNotNull(moderate)
        assertEquals(0.88, moderate!!.ratio, 0.01)
        assertTrue(moderate.category.contains("Moderato"))

        // Waist 95 cm, Hips 100 cm -> 0.95 (High)
        val high = BodyStatsCalculator.calculateWhr(95.0, 100.0)
        assertNotNull(high)
        assertEquals(0.95, high!!.ratio, 0.01)
        assertTrue(high.category.contains("Elevato"))

        // Null / Zero checks
        assertNull(BodyStatsCalculator.calculateWhr(null, 100.0))
        assertNull(BodyStatsCalculator.calculateWhr(80.0, null))
        assertNull(BodyStatsCalculator.calculateWhr(0.0, 100.0))
    }

    @Test
    fun testCircumferencesDiff() {
        val entries = listOf(
            WeightEntry(
                id = "1",
                epochMillis = 1000L,
                weightKg = 85.0,
                waistCm = 95.0,
                hipsCm = 105.0,
                chestCm = 102.0,
                thighsCm = 62.0,
                armsCm = 35.0
            ),
            WeightEntry(
                id = "2",
                epochMillis = 2000L,
                weightKg = 81.0,
                waistCm = 90.0,
                hipsCm = 101.0,
                chestCm = 100.0,
                thighsCm = 60.0,
                armsCm = 34.0
            )
        )

        val metrics = BodyStatsCalculator.calculateCircumferences(entries)
        assertEquals(5, metrics.size)

        val waist = metrics.find { it.id == "waist" }!!
        assertEquals(90.0, waist.currentCm!!, 0.01)
        assertEquals(95.0, waist.initialCm!!, 0.01)
        assertEquals(-5.0, waist.diffCm!!, 0.01)

        val hips = metrics.find { it.id == "hips" }!!
        assertEquals(101.0, hips.currentCm!!, 0.01)
        assertEquals(105.0, hips.initialCm!!, 0.01)
        assertEquals(-4.0, hips.diffCm!!, 0.01)

        val total = BodyStatsCalculator.calculateTotalCircumference(entries)
        assertNotNull(total)
        // initial sum = 95 + 105 + 102 + 62 + 35 = 399.0
        // current sum = 90 + 101 + 100 + 60 + 34 = 385.0
        // diff = 385.0 - 399.0 = -14.0 cm
        assertEquals(385.0, total!!.currentTotalCm, 0.01)
        assertEquals(399.0, total.initialTotalCm!!, 0.01)
        assertEquals(-14.0, total.totalDiffCm!!, 0.01)
    }
}
