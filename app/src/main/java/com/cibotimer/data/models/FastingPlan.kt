package com.cibotimer.data.models

import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.WeekSpanModel
import com.cibotimer.domain.WeekSpanModel.toPeriod
import com.cibotimer.domain.toSpan
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object FastingDayFormatter {
    fun formatShort(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String {
        return com.cibotimer.domain.DateTimeFormatters.formatShortDay(dayOfWeek, locale, capitalize = true)
    }

    fun formatFull(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String {
        return com.cibotimer.domain.DateTimeFormatters.formatFullDay(dayOfWeek, locale, capitalize = true)
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
data class PlanRatings(
    val weightLoss: Int, // 1..4 (Dimagrimento)
    val health: Int      // 1..4 (Salute)
)

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
    fun getLocalizedName(locale: Locale = Locale.getDefault()): String {
        if (locale.language == "it") return name
        val template = FastingPlanTemplates.ALL_PREDEFINED_PLANS.firstOrNull { it.id == id || it.name == name }
        return nameEn ?: template?.nameEn ?: name
    }

    val localizedName: String
        get() = getLocalizedName(Locale.getDefault())

    fun getLocalizedDescription(locale: Locale = Locale.getDefault()): String {
        if (locale.language == "it") return description
        val template = FastingPlanTemplates.ALL_PREDEFINED_PLANS.firstOrNull { it.id == id || it.name == name }
        return descriptionEn ?: template?.descriptionEn ?: description
    }

    val localizedDescription: String
        get() = getLocalizedDescription(Locale.getDefault())

    val descriptionItalian: String
        get() = description

    fun getEffectivePeriods(): List<FastingPeriod> {
        val rawPeriods = if (!periods.isNullOrEmpty()) {
            periods.filter { it.startDayOfWeek in 1..7 }
        } else {
            days.filter { it.isFastingDay }.mapIndexed { index, day ->
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

        if (rawPeriods.size <= 1) {
            return rawPeriods.mapIndexed { idx, p -> p.copy(periodNumber = idx + 1) }
        }

        val spans = rawPeriods.map { it.toSpan() }
        val normalizedSpans = WeekSpanModel.normalize(spans)
        return normalizedSpans.mapIndexed { index, span ->
            span.toPeriod(index + 1, "p${index + 1}")
        }
    }
}
