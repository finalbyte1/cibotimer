package com.cibotimer

import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.domain.FastingPlanTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimerStopRestartTest {

    @Test
    fun testCurrentTimeAwareFastingWindow() {
        val plan = FastingPlanTemplates.PLAN_16_8 // Digiuno 20:00 -> 12:00, Pasto 12:00 -> 20:00

        // Simula le ore 22:00 di oggi (pieno orario di digiuno, iniziato alle 20:00)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)
        assertTrue("Alle 22:00 il piano 16-8 deve trovarsi all'interno della finestra di digiuno", scheduled.isNowInsideFasting)

        // Creazione sessione coerente con l'ora attuale (come fa startFasting)
        val session = FastingSession(
            id = "test-fasting-current-time",
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = scheduled.periodStartEpochMillis,
            plannedEndTimeEpochMillis = scheduled.periodEndEpochMillis,
            targetDurationMinutes = (scheduled.durationSeconds / 60L).toInt(),
            state = FastingState.FASTING
        )

        assertEquals(FastingState.FASTING, session.state)
        // Alle 22:00 sono trascorse esattamente 2 ore dall'inizio alle 20:00
        val elapsedSec = FastingCalculator.calculateElapsedSeconds(session, now)
        assertEquals(2 * 3600L, elapsedSec)

        // Rimangono 14 ore fino alle 12:00 del giorno successivo
        val remainingSec = FastingCalculator.calculateRemainingSeconds(session, now)
        assertEquals(14 * 3600L, remainingSec)
    }

    @Test
    fun testCurrentTimeAwareEatingWindow() {
        val plan = FastingPlanTemplates.PLAN_16_8 // Digiuno 20:00 -> 12:00, Pasto 12:00 -> 20:00

        // Simula le ore 15:00 di oggi (pieno orario di alimentazione/pasto, iniziato alle 12:00 e fino alle 20:00)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)
        assertFalse("Alle 15:00 il piano 16-8 deve trovarsi nella finestra di pasto/alimentazione", scheduled.isNowInsideFasting)

        // Creazione sessione coerente con l'ora attuale (come fa startFasting per la finestra pasto)
        val session = FastingSession(
            id = "test-eating-current-time",
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = scheduled.eatingWindowStartMillis,
            plannedEndTimeEpochMillis = scheduled.eatingWindowEndMillis,
            targetDurationMinutes = (scheduled.eatingDurationSeconds / 60L).toInt(),
            state = FastingState.EATING
        )

        assertEquals(FastingState.EATING, session.state)
        // Alle 15:00 sono trascorse 3 ore dall'apertura della finestra pasto alle 12:00
        val elapsedSec = (now - session.startTimeEpochMillis) / 1000L
        assertEquals(3 * 3600L, elapsedSec)

        // Rimangono 5 ore fino alle 20:00 (inizio del digiuno)
        val remainingSec = (session.plannedEndTimeEpochMillis - now) / 1000L
        assertEquals(5 * 3600L, remainingSec)
    }

    @Test
    fun testStopAndRestartAlwaysAlignsWithCurrentTime() {
        val plan = FastingPlanTemplates.PLAN_20_4 // Digiuno 20:00 -> 16:00, Pasto 16:00 -> 20:00

        // Ore 21:00 (1 ora di digiuno)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        // 1. Sessione attiva
        val scheduled1 = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)
        val session1 = FastingSession(
            id = "sess-1",
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = scheduled1.periodStartEpochMillis,
            plannedEndTimeEpochMillis = scheduled1.periodEndEpochMillis,
            targetDurationMinutes = (scheduled1.durationSeconds / 60L).toInt(),
            state = FastingState.FASTING
        )
        assertEquals(1 * 3600L, FastingCalculator.calculateElapsedSeconds(session1, now))

        // 2. Stop manuale dell'utente
        val stoppedSession = session1.copy(actualEndTimeEpochMillis = now)
        assertEquals(now, stoppedSession.actualEndTimeEpochMillis)

        // 3. Riavvio alle 21:30 (dopo 30 minuti)
        val restartTime = now + 30 * 60_000L
        val scheduledRestart = FastingCalculator.resolveCurrentScheduledPeriod(plan, restartTime)
        assertTrue(scheduledRestart.isNowInsideFasting)

        val session2 = FastingSession(
            id = "sess-2",
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = scheduledRestart.periodStartEpochMillis,
            plannedEndTimeEpochMillis = scheduledRestart.periodEndEpochMillis,
            targetDurationMinutes = (scheduledRestart.durationSeconds / 60L).toInt(),
            state = FastingState.FASTING
        )

        // La sessione riavviata tiene conto dell'orario attuale (21:30 -> 1h 30m trascorsi, 18h 30m rimanenti)
        val elapsedSec = FastingCalculator.calculateElapsedSeconds(session2, restartTime)
        val remainingSec = FastingCalculator.calculateRemainingSeconds(session2, restartTime)
        assertEquals(5400L, elapsedSec) // 1h 30m = 90 min * 60 = 5400s
        assertEquals(20 * 3600L - 5400L, remainingSec)
    }
}
