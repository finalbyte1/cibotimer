package com.cibotimer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WaterEntry(
    val id: String,
    val epochMillis: Long,
    val amountMl: Int
)
