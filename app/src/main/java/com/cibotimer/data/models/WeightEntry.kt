package com.cibotimer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WeightEntry(
    val id: String,
    val epochMillis: Long,
    val weightKg: Double,
    val notes: String = "",
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val chestCm: Double? = null,
    val thighsCm: Double? = null,
    val armsCm: Double? = null
)
