package com.cibotimer

import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState
import com.cibotimer.domain.FastingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastingCalculatorTest {

    @Test
    fun testStageProgressionAcrossTime() {
        // 0 to 4 hours: Stage 1 (Digestione)
        assertEquals(1, FastingCalculator.getStageForElapsedSeconds(0).stageNumber)
        assertEquals(1, FastingCalculator.getStageForElapsedSeconds(2 * 3600).stageNumber)
        assertEquals(1, FastingCalculator.getStageForElapsedSeconds(4 * 3600 - 1).stageNumber)

        // 4 to 8 hours: Stage 2 (Calo Insulina)
        assertEquals(2, FastingCalculator.getStageForElapsedSeconds(4 * 3600).stageNumber)
        assertEquals(2, FastingCalculator.getStageForElapsedSeconds(7 * 3600).stageNumber)

        // 8 to 12 hours: Stage 3 (Combustione Grassi)
        assertEquals(3, FastingCalculator.getStageForElapsedSeconds(8 * 3600).stageNumber)
        assertEquals(3, FastingCalculator.getStageForElapsedSeconds(11 * 3600).stageNumber)

        // 12 to 16 hours: Stage 4 (Chetosi)
        assertEquals(4, FastingCalculator.getStageForElapsedSeconds(12 * 3600).stageNumber)
        assertEquals(4, FastingCalculator.getStageForElapsedSeconds(15 * 3600).stageNumber)

        // 16 to 24 hours: Stage 5 (Autofagia)
        assertEquals(5, FastingCalculator.getStageForElapsedSeconds(16 * 3600).stageNumber)
        assertEquals(5, FastingCalculator.getStageForElapsedSeconds(23 * 3600).stageNumber)

        // 24 to 48 hours: Stage 6 (Ormone Crescita)
        assertEquals(6, FastingCalculator.getStageForElapsedSeconds(24 * 3600).stageNumber)
        assertEquals(6, FastingCalculator.getStageForElapsedSeconds(36 * 3600).stageNumber)

        // 48+ hours: Stage 7 (Rigenerazione Immunitaria)
        assertEquals(7, FastingCalculator.getStageForElapsedSeconds(48 * 3600).stageNumber)
        assertEquals(7, FastingCalculator.getStageForElapsedSeconds(72 * 3600).stageNumber)
    }

    @Test
    fun testElapsedAndRemainingCalculations() {
        val start = 1000000000L
        val plannedEnd = start + (16 * 3600 * 1000L) // 16 hours

        val session = FastingSession(
            id = "test-1",
            planId = "16-8",
            planName = "16-8",
            startTimeEpochMillis = start,
            plannedEndTimeEpochMillis = plannedEnd,
            targetDurationMinutes = 16 * 60,
            state = FastingState.FASTING
        )

        // 8 hours into fasting
        val current = start + (8 * 3600 * 1000L)
        val elapsedSec = FastingCalculator.calculateElapsedSeconds(session, current)
        val remainingSec = FastingCalculator.calculateRemainingSeconds(session, current)
        val progress = FastingCalculator.calculateProgressFraction(session, current)

        assertEquals(8 * 3600L, elapsedSec)
        assertEquals(8 * 3600L, remainingSec)
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun testProgressFractionDoesNotExceedOne() {
        val start = 1000000000L
        val plannedEnd = start + (16 * 3600 * 1000L)

        val session = FastingSession(
            id = "test-2",
            planId = "16-8",
            planName = "16-8",
            startTimeEpochMillis = start,
            plannedEndTimeEpochMillis = plannedEnd,
            targetDurationMinutes = 16 * 60,
            state = FastingState.FASTING
        )

        // 20 hours into fasting (4h past target)
        val current = start + (20 * 3600 * 1000L)
        val progress = FastingCalculator.calculateProgressFraction(session, current)
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun testStageInfoFormatting() {
        val stage4 = FastingCalculator.getStageForElapsedSeconds(14 * 3600)
        assertEquals("Chetosi", stage4.titleItalian)
        assertTrue(stage4.benefitsItalian.isNotEmpty())
    }

    @Test
    fun testCompute24hSegmentsOvernight168() {
        val mon = com.cibotimer.data.models.DayPlan(dayOfWeek = 1, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960)
        val tue = com.cibotimer.data.models.DayPlan(dayOfWeek = 2, isFastingDay = true, startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingDurationMinutes = 960)

        val segments = FastingCalculator.compute24hSegments(tue, mon)
        // 0..720 fasting (00:00..12:00), 720..1200 eating (12:00..20:00), 1200..1440 fasting (20:00..24:00)
        assertEquals(3, segments.size)
        assertEquals(0f, segments[0].startMin, 0.01f)
        assertEquals(720f, segments[0].endMin, 0.01f)
        assertTrue(segments[0].isFasting)

        assertEquals(720f, segments[1].startMin, 0.01f)
        assertEquals(1200f, segments[1].endMin, 0.01f)
        assertTrue(!segments[1].isFasting)

        assertEquals(1200f, segments[2].startMin, 0.01f)
        assertEquals(1440f, segments[2].endMin, 0.01f)
        assertTrue(segments[2].isFasting)
    }

    @Test
    fun testResolveCurrentScheduledPeriod_Luigi1_Monday1404() {
        // Lunedì alle ore 14:04:17 (esattamente lo scenario dello screenshot dell'utente)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 4)
            set(java.util.Calendar.SECOND, 17)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(
            plan = com.cibotimer.domain.FastingPlanTemplates.PLAN_LUIGI_1,
            nowMillis = now
        )

        assertTrue("Alle 14:04 di Lunedì deve essere all'interno del digiuno", scheduled.isNowInsideFasting)
        assertEquals(2, scheduled.period.periodNumber) // Periodo 2: lun 14:00 – mar 10:00 (20h)
        assertEquals(20 * 3600L, scheduled.durationSeconds)
        assertEquals(4 * 60 + 17L, scheduled.elapsedSeconds)
        assertEquals(20 * 3600L - (4 * 60 + 17L), scheduled.remainingSeconds) // 19:55:43
        assertTrue(scheduled.progressFraction > 0f && scheduled.progressFraction < 0.01f)
    }

    @Test
    fun testResolveCurrentScheduledPeriod_Luigi1_Fasting_MondayMorningPeriod1() {
        // Lunedì alle 08:00 (dentro il Periodo 1: lun 00:00 – lun 13:00, 13h)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(
            plan = com.cibotimer.domain.FastingPlanTemplates.PLAN_LUIGI_1,
            nowMillis = now
        )

        assertTrue("Alle 08:00 di Lunedì siamo in digiuno (Periodo 1)", scheduled.isNowInsideFasting)
        assertEquals(1, scheduled.period.periodNumber)
        assertEquals(13 * 3600L, scheduled.durationSeconds)
        assertEquals(8 * 3600L, scheduled.elapsedSeconds)
        assertEquals(5 * 3600L, scheduled.remainingSeconds)
    }

    @Test
    fun testResolveCurrentScheduledPeriod_Luigi1_EatingWindow_Monday1330() {
        // Lunedì alle 13:30 (finestra di pasto tra Periodo 1 terminato alle 13:00 e Periodo 2 alle 14:00)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 13)
            set(java.util.Calendar.MINUTE, 30)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(
            plan = com.cibotimer.domain.FastingPlanTemplates.PLAN_LUIGI_1,
            nowMillis = now
        )

        assertTrue("Alle 13:30 di Lunedì non siamo in digiuno (finestra pasto)", !scheduled.isNowInsideFasting)
        assertEquals("Il prossimo digiuno deve essere il Periodo 2", 2, scheduled.period.periodNumber)
        assertEquals(20 * 3600L, scheduled.durationSeconds)
        assertEquals(0f, scheduled.progressFraction, 0.001f)
    }

    @Test
    fun testResolveCurrentScheduledPeriod_Luigi1_SundayOvernight() {
        // Domenica alle 23:00 (dentro il Periodo 8: dom 16:00 – lun 11:00)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(
            plan = com.cibotimer.domain.FastingPlanTemplates.PLAN_LUIGI_1,
            nowMillis = now
        )

        assertTrue("Domenica alle 23:00 siamo in digiuno notturno", scheduled.isNowInsideFasting)
        assertEquals(8, scheduled.period.periodNumber)
        assertEquals(19 * 3600L, scheduled.durationSeconds)
        assertEquals(7 * 3600L, scheduled.elapsedSeconds) // dalle 16:00 alle 23:00 = 7h
        assertEquals(12 * 3600L, scheduled.remainingSeconds) // fino alle 11:00 di lunedì = 12h
    }
}
