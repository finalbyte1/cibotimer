package com.cibotimer.data.models

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class Trophy(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val currentProgress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean = false,
    val unlockedDateEpochMillis: Long? = null,
    val titleEn: String? = null,
    val descriptionEn: String? = null
) {
    val progressFraction: Float
        get() = if (maxProgress > 0) (currentProgress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f

    val localizedTitle: String
        get() {
            if (Locale.getDefault().language == "it") return title
            if (!titleEn.isNullOrBlank()) return titleEn
            return when (id) {
                "first_fast" -> "First Step"
                "seven_fasts" -> "One Week"
                "hours_100" -> "Centurion"
                "hours_500" -> "Steel Consistency"
                "hours_1000" -> "Master of Fasting"
                "autophagy_master" -> "Active Autophagy"
                "warrior" -> "True Warrior"
                "track_weight" -> "Active Tracking"
                "weight_loss_1kg" -> "Weight Milestone"
                "water_goal" -> "Perfect Hydration"
                else -> title
            }
        }

    val localizedDescription: String
        get() = getLocalizedDescription(WeightUnit.KG)

    fun getLocalizedDescription(weightUnit: WeightUnit): String {
        if (id == "weight_loss_1kg") {
            val amountStr = com.cibotimer.domain.WeightUnitConverter.formatWeight(1.0, weightUnit, showUnit = true)
            return if (Locale.getDefault().language == "it") {
                "Riduci il tuo peso di almeno $amountStr dal punto di partenza."
            } else {
                "Reduce your weight by at least $amountStr from your starting point."
            }
        }
        if (Locale.getDefault().language == "it") return description
        if (!descriptionEn.isNullOrBlank()) return descriptionEn
        return when (id) {
            "first_fast" -> "Complete your first fasting period."
            "seven_fasts" -> "Successfully complete 7 fasts."
            "hours_100" -> "Reach 100 total fasting hours."
            "hours_500" -> "Reach 500 total fasting hours."
            "hours_1000" -> "Reach 1000 total fasting hours."
            "autophagy_master" -> "Complete a fast of at least 16 hours."
            "warrior" -> "Complete a fast of 20 hours or more."
            "track_weight" -> "Record your weight at least 3 times."
            "weight_loss_1kg" -> "Reduce your weight by at least 1 kg from your starting point."
            "water_goal" -> "Drink at least 2 liters of water in a day."
            else -> description
        }
    }
}
