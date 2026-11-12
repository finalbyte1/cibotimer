package com.cibotimer.domain

import android.content.Context
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Utility centralizzata per la formattazione di date, giorni e orari.
 *
 * Supporta:
 * - Formato 24 ore fisso per la lingua italiana (es. "mer 20:00", "20:00")
 * - Formato 12 ore (con AM/PM, es. "Wed 8:00 PM", "8:00 PM") o 24 ore per la lingua inglese
 *   e altre lingue in base all'impostazione di sistema (android.text.format.DateFormat.is24HourFormat).
 * - Nomi dei giorni della settimana localizzati e coerenti tramite DayOfWeek.getDisplayName.
 */
object DateTimeFormatters {

    /**
     * Ritorna il Locale attivo associato al contesto fornito o quello predefinito della JVM.
     */
    fun getActiveLocale(context: Context? = null): Locale {
        return context?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
    }

    /**
     * Determina se utilizzare il formato a 24 ore.
     * In italiano è sempre a 24 ore. In inglese/altre lingue segue l'impostazione di sistema.
     */
    fun is24Hour(context: Context? = null, locale: Locale = Locale.getDefault()): Boolean {
        if (locale.language == "it") return true
        if (context != null) {
            return android.text.format.DateFormat.is24HourFormat(context)
        }
        return false // Default per l'inglese in assenza di contesto Android
    }

    /**
     * Formatta il giorno della settimana abbreviato.
     * In italiano: "lun", "mar", "mer", "gio", "ven", "sab", "dom" (o maiuscolo se capitalize=true).
     * In inglese: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun".
     *
     * @param dayOfWeek Giorno della settimana (1 = Lunedì / Monday, ..., 7 = Domenica / Sunday).
     */
    fun formatShortDay(
        dayOfWeek: Int,
        locale: Locale = Locale.getDefault(),
        capitalize: Boolean = false
    ): String {
        val dow = DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
        val raw = dow.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
        return if (capitalize) {
            raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } else {
            raw
        }
    }

    /**
     * Formatta il giorno della settimana completo (es. "Lunedì" / "Monday").
     */
    fun formatFullDay(
        dayOfWeek: Int,
        locale: Locale = Locale.getDefault(),
        capitalize: Boolean = true
    ): String {
        val dow = DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
        val raw = dow.getDisplayName(TextStyle.FULL, locale)
        return if (capitalize) {
            raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } else {
            raw
        }
    }

    /**
     * Formatta solo l'orario (es. "20:00" in IT, "8:00 PM" in EN 12h, "20:00" in EN 24h).
     */
    fun formatTime(
        hour: Int,
        minute: Int,
        context: Context? = null,
        locale: Locale = Locale.getDefault(),
        is24HourOverride: Boolean? = null
    ): String {
        val use24Hour = is24HourOverride ?: is24Hour(context, locale)
        val formatter = if (use24Hour || locale.language == "it") {
            DateTimeFormatter.ofPattern("HH:mm", locale)
        } else {
            DateTimeFormatter.ofPattern("h:mm a", locale)
        }
        return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(formatter)
    }

    /**
     * Formatta solo l'orario da un timestamp epoch millis.
     */
    fun formatTime(
        epochMillis: Long,
        context: Context? = null,
        locale: Locale = Locale.getDefault(),
        is24HourOverride: Boolean? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return formatTime(zdt.hour, zdt.minute, context, locale, is24HourOverride)
    }

    /**
     * Formatta giorno abbreviato + orario (es. "mer 20:00" in IT, "Wed 8:00 PM" in EN 12h, "Wed 20:00" in EN 24h).
     */
    fun formatDayAndTime(
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val dayStr = formatShortDay(dayOfWeek, locale, capitalize = (locale.language != "it"))
        val timeStr = formatTime(hour, minute, null, locale, is24Hour)
        return "$dayStr $timeStr"
    }

    /**
     * Formatta giorno abbreviato + orario partendo da un timestamp epoch millis.
     */
    fun formatDayAndTime(
        epochMillis: Long,
        context: Context? = null,
        locale: Locale = Locale.getDefault(),
        is24HourOverride: Boolean? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val use24Hour = is24HourOverride ?: is24Hour(context, locale)
        return formatDayAndTime(zdt.dayOfWeek.value, zdt.hour, zdt.minute, use24Hour, locale)
    }

    /**
     * Formatta data estesa + orario (es. per notifiche, storico pesate o tooltips grafici).
     * In italiano: "12 nov 2026, 20:00" o con giorno "mer 12 nov 2026, 20:00".
     * In inglese: "Nov 12, 2026, 8:00 PM" o con giorno "Wed Nov 12, 2026, 8:00 PM".
     */
    fun formatFullDateTime(
        epochMillis: Long,
        context: Context? = null,
        locale: Locale = Locale.getDefault(),
        includeDayOfWeek: Boolean = false,
        is24HourOverride: Boolean? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val use24Hour = is24HourOverride ?: is24Hour(context, locale)

        val pattern = if (locale.language == "it") {
            if (includeDayOfWeek) "EEE d MMM yyyy, HH:mm" else "dd MMM yyyy, HH:mm"
        } else {
            val timePart = if (use24Hour) "HH:mm" else "h:mm a"
            if (includeDayOfWeek) "EEE, MMM d, yyyy, $timePart" else "MMM d, yyyy, $timePart"
        }
        return zdt.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * Formatta data breve (es. "12 nov" in IT, "Nov 12" in EN) per assi grafici.
     */
    fun formatShortDate(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val pattern = if (locale.language == "it") "d MMM" else "MMM d"
        return zdt.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}
