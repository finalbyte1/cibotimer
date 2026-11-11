package com.cibotimer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CoachingTip(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val contentMarkdown: String,
    val readTimeMinutes: Int = 3
)
