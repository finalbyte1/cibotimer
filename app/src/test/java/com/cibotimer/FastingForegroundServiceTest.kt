package com.cibotimer

import com.cibotimer.notifications.FastingForegroundService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FastingForegroundServiceTest {

    @Test
    fun testFormatNextEventToday() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMillis = cal.timeInMillis

        val eatingText = FastingForegroundService.formatNextEvent(targetMillis, isEating = false)
        val fastingText = FastingForegroundService.formatNextEvent(targetMillis, isEating = true)

        assertEquals("Mangiare alle 12:00", eatingText)
        assertEquals("Digiuno alle 12:00", fastingText)
    }

    @Test
    fun testFormatNextEventTomorrow() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMillis = cal.timeInMillis

        val eatingText = FastingForegroundService.formatNextEvent(targetMillis, isEating = false)
        val fastingText = FastingForegroundService.formatNextEvent(targetMillis, isEating = true)

        assertEquals("Mangiare domani alle 7:00", eatingText)
        assertEquals("Digiuno domani alle 7:00", fastingText)
    }
}
