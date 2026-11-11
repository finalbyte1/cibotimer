package com.cibotimer.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val type: String = "GENERAL", // FASTING_END, EATING_END, EATING_PRE_REMINDER, STAGE_REACHED, WATER, WEIGHT
    val isRead: Boolean = false
)
