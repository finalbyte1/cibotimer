package com.cibotimer

import com.cibotimer.domain.BmiCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BmiCalculatorTest {

    @Test
    fun testBmiCalculationNormal() {
        // 70 kg, 175 cm -> 70 / (1.75^2) = 22.86 -> 22.9
        val result = BmiCalculator.calculate(70.0, 175.0)
        assertEquals(22.9, result.bmiValue, 0.01)
        assertEquals("Normopeso", result.category)
    }

    @Test
    fun testBmiCalculationOverweight() {
        // 85 kg, 175 cm -> 85 / (1.75^2) = 27.76 -> 27.8
        val result = BmiCalculator.calculate(85.0, 175.0)
        assertEquals(27.8, result.bmiValue, 0.01)
        assertEquals("Sovrappeso", result.category)
    }

    @Test
    fun testBmiCalculationUnderweight() {
        // 50 kg, 175 cm -> 50 / (1.75^2) = 16.33 -> 16.3
        val result = BmiCalculator.calculate(50.0, 175.0)
        assertEquals(16.3, result.bmiValue, 0.01)
        assertEquals("Sottopeso", result.category)
    }

    @Test
    fun testBmiCalculationObesity() {
        // 100 kg, 175 cm -> 100 / (1.75^2) = 32.65 -> 32.7
        val result = BmiCalculator.calculate(100.0, 175.0)
        assertEquals(32.7, result.bmiValue, 0.01)
        assertEquals("Obesità Grado I", result.category)
    }

    @Test
    fun testBmiCalculationZeroHeightHandledGracefully() {
        val result = BmiCalculator.calculate(70.0, 0.0)
        assertEquals(0.0, result.bmiValue, 0.01)
        assertEquals("Non disponibile", result.category)
    }
}
