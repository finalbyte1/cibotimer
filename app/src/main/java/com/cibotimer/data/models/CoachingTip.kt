package com.cibotimer.data.models

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class CoachingTip(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val contentMarkdown: String,
    val readTimeMinutes: Int = 3,
    val titleEn: String? = null,
    val subtitleEn: String? = null,
    val categoryEn: String? = null,
    val contentMarkdownEn: String? = null
) {
    val localizedTitle: String
        get() = if (Locale.getDefault().language == "it") title else (titleEn ?: title)

    val localizedSubtitle: String
        get() = if (Locale.getDefault().language == "it") subtitle else (subtitleEn ?: subtitle)

    val localizedCategory: String
        get() = if (Locale.getDefault().language == "it") category else (categoryEn ?: category)

    val localizedContentMarkdown: String
        get() = if (Locale.getDefault().language == "it") contentMarkdown else (contentMarkdownEn ?: contentMarkdown)
}
