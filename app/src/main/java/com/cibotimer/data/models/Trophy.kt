package com.cibotimer.data.models

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
    val unlockedDateEpochMillis: Long? = null
) {
    val progressFraction: Float
        get() = if (maxProgress > 0) (currentProgress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f
}
