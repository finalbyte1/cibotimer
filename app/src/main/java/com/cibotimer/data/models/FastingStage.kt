package com.cibotimer.data.models

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class FastingStage(
    val stageIndex: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val colorHex: Long,
    val titleEn: String = "",
    val subtitleEn: String = "",
    val descriptionEn: String = ""
) {
    val stageNumber: Int
        get() = stageIndex

    val titleItalian: String
        get() = title

    val shortDescriptionItalian: String
        get() = subtitle

    val benefitsItalian: List<String>
        get() = listOf(subtitle, description)

    fun getLocalizedTitle(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "it" || titleEn.isBlank()) title else titleEn
    }

    fun getLocalizedSubtitle(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "it" || subtitleEn.isBlank()) subtitle else subtitleEn
    }

    fun getLocalizedDescription(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "it" || descriptionEn.isBlank()) description else descriptionEn
    }

    val localizedTitle: String
        get() = getLocalizedTitle()

    val localizedSubtitle: String
        get() = getLocalizedSubtitle()

    val localizedDescription: String
        get() = getLocalizedDescription()

    val durationLabel: String
        get() {
            val startH = startMinutes / 60
            return if (endMinutes == Int.MAX_VALUE) {
                "${startH}h+"
            } else {
                val endH = endMinutes / 60
                "${startH}h - ${endH}h"
            }
        }
}
