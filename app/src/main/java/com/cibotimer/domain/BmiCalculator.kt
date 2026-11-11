package com.cibotimer.domain

import com.cibotimer.R
import kotlin.math.roundToInt

data class BmiResult(
    val bmiValue: Double,
    val category: String,
    val colorHex: Long,
    val categoryResId: Int = 0
)

object BmiCalculator {

    fun calculate(weightKg: Double, heightCm: Double): BmiResult {
        if (heightCm <= 0.0 || weightKg <= 0.0) {
            return BmiResult(0.0, "Non disponibile", 0xFF9E9E9E, R.string.bmi_category_unavailable)
        }
        val heightM = heightCm / 100.0
        val rawBmi = weightKg / (heightM * heightM)
        val roundedBmi = (rawBmi * 10.0).roundToInt() / 10.0

        val (category, color, resId) = when {
            roundedBmi < 18.5 -> Triple("Sottopeso", 0xFF2196F3, R.string.bmi_category_underweight)
            roundedBmi < 25.0 -> Triple("Normopeso", 0xFF4CAF50, R.string.bmi_category_normal)
            roundedBmi < 30.0 -> Triple("Sovrappeso", 0xFFFF9800, R.string.bmi_category_overweight)
            roundedBmi < 35.0 -> Triple("Obesità Grado I", 0xFFF44336, R.string.bmi_category_obese_1)
            else -> Triple("Obesità Severa", 0xFFD32F2F, R.string.bmi_category_obese_severe)
        }

        return BmiResult(roundedBmi, category, color, resId)
    }
}
