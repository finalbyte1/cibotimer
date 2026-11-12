package com.cibotimer

import android.graphics.Color
import com.cibotimer.widget.FastingWidgetState
import com.cibotimer.widget.FastingWidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FastingWidgetStateTest {

    @Test
    fun testTimeFormattingRules() {
        // Regola formato orario target coerente con i minuti (es. "7:00", "12:00", "7:30", "12:15")
        assertEquals("7:00", FastingWidgetUpdater.formatEatingTargetTime(7, 0))
        assertEquals("7:30", FastingWidgetUpdater.formatEatingTargetTime(7, 30))
        assertEquals("14:00", FastingWidgetUpdater.formatEatingTargetTime(14, 0))
        assertEquals("14:05", FastingWidgetUpdater.formatEatingTargetTime(14, 5))

        assertEquals("12:00", FastingWidgetUpdater.formatFastingTargetTime(12, 0))
        assertEquals("12:15", FastingWidgetUpdater.formatFastingTargetTime(12, 15))
        assertEquals("20:00", FastingWidgetUpdater.formatFastingTargetTime(20, 0))
        assertEquals("20:45", FastingWidgetUpdater.formatFastingTargetTime(20, 45))
    }

    @Test
    fun testDurationFormatting() {
        val eightHoursFiftyMins = (8 * 60 + 50) * 60 * 1000L
        assertEquals("8h 50m", FastingWidgetUpdater.formatDurationShort(eightHoursFiftyMins))

        val threeHours = 3 * 60 * 60 * 1000L
        assertEquals("3h 0m", FastingWidgetUpdater.formatDurationShort(threeHours))

        val zeroMillis = 0L
        assertEquals("0h 0m", FastingWidgetUpdater.formatDurationShort(zeroMillis))
    }

    @Test
    fun testIsSameDayCalculation() {
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 10, 0, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 23, 59, 0)
        }
        val calTomorrow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 0, 1, 0)
        }

        assertTrue(FastingWidgetUpdater.isSameDay(cal1.timeInMillis, cal2.timeInMillis))
        assertFalse(FastingWidgetUpdater.isSameDay(cal1.timeInMillis, calTomorrow.timeInMillis))
    }

    @Test
    fun testFastingWidgetStateColorsAndCompatibility() {
        val fastingState = FastingWidgetState(
            status2x1 = "In digiuno",
            timeRemaining2x1 = "5h 50m",
            nextEvent2x1 = "Mangiare alle 7:00",
            status1x1 = "In digiuno",
            timeRemaining1x1 = "5h 50m",
            nextEvent1x1 = "Mangi 7:00",
            backgroundResId = R.drawable.widget_bg_fasting,
            buttonBackgroundResId = R.drawable.widget_button_circle_fasting,
            secondaryTextColor = 0xFF8FA3BF.toInt(),
            primaryTextColor = 0xFFFFFFFF.toInt(),
            actionIconResId = R.drawable.ic_widget_flame,
            actionContentDescription = "Mangiare"
        )

        assertEquals("In digiuno", fastingState.status2x1)
        assertEquals("5h 50m", fastingState.timeRemaining2x1)
        assertEquals("Mangiare alle 7:00", fastingState.nextEvent2x1)
        assertEquals("In digiuno", fastingState.status1x1)
        assertEquals("5h 50m", fastingState.timeRemaining1x1)
        assertEquals("Mangi 7:00", fastingState.nextEvent1x1)

        // Verifiche retrocompatibilità
        assertEquals("Mangiare alle 7:00", fastingState.title2x1)
        assertEquals("In digiuno • 5h 50m", fastingState.subtitle2x1)
        assertEquals("Mangi 7:00", fastingState.bottom1x1)
        assertEquals("Mangiare alle 7:00", fastingState.nextPeriodText)
        assertEquals("In digiuno • 5h 50m", fastingState.statusText)

        // Stato Pasto (Eating)
        val eatingState = FastingWidgetState(
            status2x1 = "Pasto",
            timeRemaining2x1 = "3h 0m",
            nextEvent2x1 = "Digiuno alle 12:00",
            status1x1 = "Pasto",
            timeRemaining1x1 = "3h 0m",
            nextEvent1x1 = "Stop 12:00",
            backgroundResId = R.drawable.widget_bg_eating,
            buttonBackgroundResId = R.drawable.widget_button_circle_eating,
            secondaryTextColor = 0xFFA7D9C4.toInt(),
            primaryTextColor = 0xFFFFFFFF.toInt(),
            actionIconResId = R.drawable.ic_widget_cutlery,
            actionContentDescription = "Digiunare"
        )

        assertEquals("Pasto", eatingState.status2x1)
        assertEquals("3h 0m", eatingState.timeRemaining2x1)
        assertEquals("Digiuno alle 12:00", eatingState.nextEvent2x1)
        assertEquals("Pasto", eatingState.status1x1)
        assertEquals("3h 0m", eatingState.timeRemaining1x1)
        assertEquals("Stop 12:00", eatingState.nextEvent1x1)
    }

    @Test
    fun testTextConstraintsNoEllipsis() {
        // Nessun testo deve contenere ellissi pre-tagliate ("…")
        val sampleTexts1x1 = listOf("In digiuno", "Pasto", "In pausa", "5h 50m", "Mangi 7:00", "Stop 12:00", "Avvia")
        for (text in sampleTexts1x1) {
            assertFalse("Il testo 1x1 '$text' non deve contenere ellissi", text.contains("…"))
            assertFalse("Il testo 1x1 '$text' non deve contenere tre punti", text.contains("..."))
        }

        val sampleTexts2x1 = listOf(
            "In digiuno", "Pasto", "In pausa",
            "5h 50m", "16h 0m", "--",
            "Mangiare alle 7:00", "Mangiare alle 7:00 Gio",
            "Digiuno alle 12:00", "Digiuno alle 12:00 Mer",
            "Nessun digiuno"
        )
        for (text in sampleTexts2x1) {
            assertFalse("Il testo 2x1 '$text' non deve contenere ellissi", text.contains("…"))
            assertFalse("Il testo 2x1 '$text' non deve contenere tre punti", text.contains("..."))
        }
    }

    @Test
    fun testTimeFractionCalculation() {
        // Verifica calcolo frazione oraria 0-24h (86400 secondi)
        // 00:00:00 -> 0.0
        val cal0 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val frac0 = (cal0.get(Calendar.HOUR_OF_DAY) * 3600f + cal0.get(Calendar.MINUTE) * 60f + cal0.get(Calendar.SECOND).toFloat()) / 86400f
        assertEquals(0f, frac0, 0.0001f)

        // 12:00:00 -> 0.5
        val cal12 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val frac12 = (cal12.get(Calendar.HOUR_OF_DAY) * 3600f + cal12.get(Calendar.MINUTE) * 60f + cal12.get(Calendar.SECOND).toFloat()) / 86400f
        assertEquals(0.5f, frac12, 0.0001f)

        // 18:00:00 -> 0.75
        val cal18 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val frac18 = (cal18.get(Calendar.HOUR_OF_DAY) * 3600f + cal18.get(Calendar.MINUTE) * 60f + cal18.get(Calendar.SECOND).toFloat()) / 86400f
        assertEquals(0.75f, frac18, 0.0001f)
    }
}
