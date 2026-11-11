package com.cibotimer.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class FastingState {
    NOT_STARTED,
    FASTING,
    EATING
}

@Serializable
data class FastingSession(
    val id: String,
    val planId: String,
    val planName: String,
    val startTimeEpochMillis: Long,
    val plannedEndTimeEpochMillis: Long,
    val actualEndTimeEpochMillis: Long? = null,
    val state: FastingState = FastingState.FASTING,
    val targetDurationMinutes: Int = 960,
    val notes: String = ""
) {
    val isCompleted: Boolean
        get() = actualEndTimeEpochMillis != null
}
