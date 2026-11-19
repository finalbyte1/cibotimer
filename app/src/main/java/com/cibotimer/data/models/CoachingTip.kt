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
    fun getLocalizedTitle(locale: Locale = Locale.getDefault()): String =
        if (locale.language == "it") title else (titleEn ?: title)

    fun getLocalizedSubtitle(locale: Locale = Locale.getDefault()): String =
        if (locale.language == "it") subtitle else (subtitleEn ?: subtitle)

    fun getLocalizedCategory(locale: Locale = Locale.getDefault()): String =
        if (locale.language == "it") category else (categoryEn ?: category)

    fun getLocalizedContentMarkdown(locale: Locale = Locale.getDefault()): String =
        if (locale.language == "it") contentMarkdown else (contentMarkdownEn ?: contentMarkdown)

    val localizedTitle: String
        get() = getLocalizedTitle()

    val localizedSubtitle: String
        get() = getLocalizedSubtitle()

    val localizedCategory: String
        get() = getLocalizedCategory()

    val localizedContentMarkdown: String
        get() = getLocalizedContentMarkdown()
}
