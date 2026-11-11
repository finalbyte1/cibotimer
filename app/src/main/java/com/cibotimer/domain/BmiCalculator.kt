package com.cibotimer.domain

import kotlin.math.roundToInt

data class BmiResult(
    val bmiValue: Double,
    val category: String,
    val colorHex: Long
)

object BmiCalculator {

    fun calculate(weightKg: Double, heightCm: Double): BmiResult {
        if (heightCm <= 0.0 || weightKg <= 0.0) {
            return BmiResult(0.0, "Non disponibile", 0xFF9E9E9E)
        }
        val heightM = heightCm / 100.0
        val rawBmi = weightKg / (heightM * heightM)
        val roundedBmi = (rawBmi * 10.0).roundToInt() / 10.0

        val (category, color) = when {
            roundedBmi < 18.5 -> "Sottopeso" to 0xFF2196F3
            roundedBmi < 25.0 -> "Normopeso" to 0xFF4CAF50
            roundedBmi < 30.0 -> "Sovrappeso" to 0xFFFF9800
            roundedBmi < 35.0 -> "Obesità Grado I" to 0xFFF44336
            else -> "Obesità Severa" to 0xFFD32F2F
        }

        return BmiResult(roundedBmi, category, color)
    }
}
