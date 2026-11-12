package com.cibotimer

import com.cibotimer.domain.DateTimeFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DateTimeFormattersTest {

    @Test
    fun testFormatDayAndTimeWednesday20() {
        // Mercoledì alle 20:00 (day 3)
        val itResult = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 3,
            hour = 20,
            minute = 0,
            is24Hour = true,
            locale = Locale.ITALIAN
        )
        assertEquals("mer 20:00", itResult)

        val en12Result = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 3,
            hour = 20,
            minute = 0,
            is24Hour = false,
            locale = Locale.ENGLISH
        )
        assertEquals("Wed 8:00 PM", en12Result)

        val en24Result = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 3,
            hour = 20,
            minute = 0,
            is24Hour = true,
            locale = Locale.ENGLISH
        )
        assertEquals("Wed 20:00", en24Result)
    }

    @Test
    fun testFormatDayAndTimeThursday16() {
        // Giovedì alle 16:00 (day 4)
        val itResult = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 4,
            hour = 16,
            minute = 0,
            is24Hour = true,
            locale = Locale.ITALIAN
        )
        assertEquals("gio 16:00", itResult)

        val en12Result = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 4,
            hour = 16,
            minute = 0,
            is24Hour = false,
            locale = Locale.ENGLISH
        )
        assertEquals("Thu 4:00 PM", en12Result)

        val en24Result = DateTimeFormatters.formatDayAndTime(
            dayOfWeek = 4,
            hour = 16,
            minute = 0,
            is24Hour = true,
            locale = Locale.ENGLISH
        )
        assertEquals("Thu 16:00", en24Result)
    }

    @Test
    fun testFormatShortDayItalian() {
        val expectedLower = listOf("lun", "mar", "mer", "gio", "ven", "sab", "dom")
        val expectedCap = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

        for (i in 1..7) {
            val shortLower = DateTimeFormatters.formatShortDay(i, Locale.ITALIAN, capitalize = false)
            val shortCap = DateTimeFormatters.formatShortDay(i, Locale.ITALIAN, capitalize = true)
            assertEquals(expectedLower[i - 1], shortLower)
            assertEquals(expectedCap[i - 1], shortCap)
        }
    }

    @Test
    fun testFormatShortDayEnglish() {
        val expected = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (i in 1..7) {
            val shortDay = DateTimeFormatters.formatShortDay(i, Locale.ENGLISH, capitalize = true)
            assertEquals(expected[i - 1], shortDay)
        }
    }

    @Test
    fun testFormatFullDay() {
        val expectedIt = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")
        val expectedEn = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 1..7) {
            assertEquals(expectedIt[i - 1], DateTimeFormatters.formatFullDay(i, Locale.ITALIAN, capitalize = true))
            assertEquals(expectedEn[i - 1], DateTimeFormatters.formatFullDay(i, Locale.ENGLISH, capitalize = true))
        }
    }

    @Test
    fun testFormatTime12hAnd24h() {
        // 20:00
        assertEquals("20:00", DateTimeFormatters.formatTime(20, 0, null, Locale.ITALIAN, is24HourOverride = true))
        assertEquals("8:00 PM", DateTimeFormatters.formatTime(20, 0, null, Locale.ENGLISH, is24HourOverride = false))
        assertEquals("20:00", DateTimeFormatters.formatTime(20, 0, null, Locale.ENGLISH, is24HourOverride = true))

        // 08:05
        assertEquals("08:05", DateTimeFormatters.formatTime(8, 5, null, Locale.ITALIAN, is24HourOverride = true))
        assertEquals("8:05 AM", DateTimeFormatters.formatTime(8, 5, null, Locale.ENGLISH, is24HourOverride = false))

        // 00:00
        assertEquals("00:00", DateTimeFormatters.formatTime(0, 0, null, Locale.ITALIAN, is24HourOverride = true))
        assertEquals("12:00 AM", DateTimeFormatters.formatTime(0, 0, null, Locale.ENGLISH, is24HourOverride = false))

        // 12:00
        assertEquals("12:00", DateTimeFormatters.formatTime(12, 0, null, Locale.ITALIAN, is24HourOverride = true))
        assertEquals("12:00 PM", DateTimeFormatters.formatTime(12, 0, null, Locale.ENGLISH, is24HourOverride = false))
    }

    @Test
    fun testIs24HourRules() {
        // Italian locale must ALWAYS be 24-hour regardless of Context
        assertTrue(DateTimeFormatters.is24Hour(null, Locale.ITALIAN))
        assertTrue(DateTimeFormatters.is24Hour(null, Locale.forLanguageTag("it-IT")))
    }

    @Test
    fun testFormatShortDate() {
        val zoneId = ZoneId.of("UTC")
        val zdt = ZonedDateTime.of(2026, 11, 12, 10, 0, 0, 0, zoneId)
        val epochMillis = zdt.toInstant().toEpochMilli()

        val itDate = DateTimeFormatters.formatShortDate(epochMillis, Locale.ITALIAN, zoneId)
        val enDate = DateTimeFormatters.formatShortDate(epochMillis, Locale.ENGLISH, zoneId)

        assertEquals("12 nov", itDate)
        assertEquals("Nov 12", enDate)
    }

    @Test
    fun testFormatDayAndTimeFromEpochMillis() {
        val zoneId = ZoneId.of("UTC")
        // 2026-11-11 was a Wednesday
        val zdtWed20 = ZonedDateTime.of(2026, 11, 11, 20, 0, 0, 0, zoneId)
        val epochMillisWed20 = zdtWed20.toInstant().toEpochMilli()

        val itResult = DateTimeFormatters.formatDayAndTime(
            epochMillis = epochMillisWed20,
            context = null,
            locale = Locale.ITALIAN,
            is24HourOverride = true,
            zoneId = zoneId
        )
        assertEquals("mer 20:00", itResult)

        val en12Result = DateTimeFormatters.formatDayAndTime(
            epochMillis = epochMillisWed20,
            context = null,
            locale = Locale.ENGLISH,
            is24HourOverride = false,
            zoneId = zoneId
        )
        assertEquals("Wed 8:00 PM", en12Result)

        // 2026-11-12 is Thursday
        val zdtThu16 = ZonedDateTime.of(2026, 11, 12, 16, 0, 0, 0, zoneId)
        val epochMillisThu16 = zdtThu16.toInstant().toEpochMilli()

        val itResultThu = DateTimeFormatters.formatDayAndTime(
            epochMillis = epochMillisThu16,
            context = null,
            locale = Locale.ITALIAN,
            is24HourOverride = true,
            zoneId = zoneId
        )
        assertEquals("gio 16:00", itResultThu)

        val en12ResultThu = DateTimeFormatters.formatDayAndTime(
            epochMillis = epochMillisThu16,
            context = null,
            locale = Locale.ENGLISH,
            is24HourOverride = false,
            zoneId = zoneId
        )
        assertEquals("Thu 4:00 PM", en12ResultThu)
    }
}
