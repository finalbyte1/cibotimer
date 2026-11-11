package com.cibotimer.data.models

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object FastingDayFormatter {
    fun formatShort(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String {
        return try {
            DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
                .getDisplayName(TextStyle.SHORT, locale)
                .trimEnd('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } catch (_: Throwable) {
            when (dayOfWeek) {
                1 -> if (locale.language == "it") "Lun" else "Mon"
                2 -> if (locale.language == "it") "Mar" else "Tue"
                3 -> if (locale.language == "it") "Mer" else "Wed"
                4 -> if (locale.language == "it") "Gio" else "Thu"
                5 -> if (locale.language == "it") "Ven" else "Fri"
                6 -> if (locale.language == "it") "Sab" else "Sat"
                7 -> if (locale.language == "it") "Dom" else "Sun"
                else -> ""
            }
        }
    }

    fun formatFull(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String {
        return try {
            DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
                .getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } catch (_: Throwable) {
            when (dayOfWeek) {
                1 -> if (locale.language == "it") "Lunedì" else "Monday"
                2 -> if (locale.language == "it") "Martedì" else "Tuesday"
                3 -> if (locale.language == "it") "Mercoledì" else "Wednesday"
                4 -> if (locale.language == "it") "Giovedì" else "Thursday"
                5 -> if (locale.language == "it") "Venerdì" else "Friday"
                6 -> if (locale.language == "it") "Sabato" else "Saturday"
                7 -> if (locale.language == "it") "Domenica" else "Sunday"
                else -> ""
            }
        }
    }
}

@Serializable
enum class PlanCategory {
    STANDARD,
    AUTOPHAGY,
    SPECIAL,
    CUSTOM
}

@Serializable
data class FastingPeriod(
    val id: String = "",
    val periodNumber: Int = 1,
    val startDayOfWeek: Int = 1, // 1 = Lun, 2 = Mar, ..., 7 = Dom (settimana ciclica)
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endDayOfWeek: Int = 1,
    val endHour: Int = 0,
    val endMinute: Int = 0,
    val fastingDurationMinutes: Int = 0,
    val isFasting: Boolean = true
) {
    val durationHoursString: String
        get() {
            val h = fastingDurationMinutes / 60
            val m = fastingDurationMinutes % 60
            return if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    val startTimeFormatted: String
        get() = "%02d:%02d".format(startHour, startMinute)

    val endTimeFormatted: String
        get() = "%02d:%02d".format(endHour, endMinute)

    fun dayNameShort(day: Int): String = FastingDayFormatter.formatShort(day)

    val startDayShort: String get() = dayNameShort(startDayOfWeek)
    val endDayShort: String get() = dayNameShort(endDayOfWeek)
}

@Serializable
data class DayPlan(
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val isFastingDay: Boolean,
    val startHour: Int, // 0..23 (es. 20 per 20:00)
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23 (es. 12 per 12:00 del giorno dopo)
    val endMinute: Int, // 0..59
    val fastingDurationMinutes: Int // es. 960 per 16 ore
) {
    val durationHoursString: String
        get() {
            val h = fastingDurationMinutes / 60
            val m = fastingDurationMinutes % 60
            return if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    val startTimeFormatted: String
        get() = "%02d:%02d".format(startHour, startMinute)

    val endTimeFormatted: String
        get() = "%02d:%02d".format(endHour, endMinute)

    val dayNameShort: String
        get() = FastingDayFormatter.formatShort(dayOfWeek)

    val dayName: String
        get() = FastingDayFormatter.formatFull(dayOfWeek)
}

@Serializable
data class FastingPlan(
    val id: String,
    val name: String,
    val category: PlanCategory = PlanCategory.CUSTOM,
    val description: String = "",
    val days: List<DayPlan>,
    val isCustom: Boolean = false,
    val periods: List<FastingPeriod>? = null,
    val nameEn: String? = null,
    val descriptionEn: String? = null
) {
    val localizedName: String
        get() {
            if (Locale.getDefault().language == "it") return name
            return nameEn ?: name
        }

    val localizedDescription: String
        get() {
            if (Locale.getDefault().language == "it") return description
            return descriptionEn ?: description
        }

    val descriptionItalian: String
        get() = description

    fun getEffectivePeriods(): List<FastingPeriod> {
        if (!periods.isNullOrEmpty()) {
            return periods
        }
        return days.filter { it.isFastingDay }.mapIndexed { index, day ->
            val nextDayOfWeek = if (day.dayOfWeek == 7) 1 else day.dayOfWeek + 1
            val isNextDay = (day.startHour * 60 + day.startMinute + day.fastingDurationMinutes) >= 24 * 60
            FastingPeriod(
                id = "p_${day.dayOfWeek}",
                periodNumber = index + 1,
                startDayOfWeek = day.dayOfWeek,
                startHour = day.startHour,
                startMinute = day.startMinute,
                endDayOfWeek = if (isNextDay) nextDayOfWeek else day.dayOfWeek,
                endHour = day.endHour,
                endMinute = day.endMinute,
                fastingDurationMinutes = day.fastingDurationMinutes,
                isFasting = true
            )
        }
    }
}
