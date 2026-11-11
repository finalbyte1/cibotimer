package com.cibotimer.data.models

import kotlinx.serialization.Serializable

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

    fun dayNameShort(day: Int): String = when (day) {
        1 -> "lun"
        2 -> "mar"
        3 -> "mer"
        4 -> "gio"
        5 -> "ven"
        6 -> "sab"
        7 -> "dom"
        else -> "lun"
    }

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
        get() = when (dayOfWeek) {
            1 -> "Lun"
            2 -> "Mar"
            3 -> "Mer"
            4 -> "Gio"
            5 -> "Ven"
            6 -> "Sab"
            7 -> "Dom"
            else -> "Giorno $dayOfWeek"
        }

    val dayName: String
        get() = when (dayOfWeek) {
            1 -> "Lunedì"
            2 -> "Martedì"
            3 -> "Mercoledì"
            4 -> "Giovedì"
            5 -> "Venerdì"
            6 -> "Sabato"
            7 -> "Domenica"
            else -> "Giorno $dayOfWeek"
        }
}

@Serializable
data class FastingPlan(
    val id: String,
    val name: String,
    val category: PlanCategory = PlanCategory.CUSTOM,
    val description: String = "",
    val days: List<DayPlan>,
    val isCustom: Boolean = false,
    val periods: List<FastingPeriod>? = null
) {
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
