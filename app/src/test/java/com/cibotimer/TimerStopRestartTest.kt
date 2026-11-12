package com.cibotimer

import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState
import com.cibotimer.domain.FastingCalculator
import com.cibotimer.domain.FastingPlanTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TimerStopRestartTest {

    @Test
    fun testRestartFastingSessionAfterStop() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val now = System.currentTimeMillis()

        // 1. Risolvi il periodo previsto dal piano
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)
        val durationSeconds = scheduled.durationSeconds
        assertTrue("La durata del digiuno deve essere maggiore di 0", durationSeconds > 0)
        assertEquals(16 * 3600L, durationSeconds)

        // 2. Simula avvio prima sessione
        val session1 = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = now,
            plannedEndTimeEpochMillis = now + (durationSeconds * 1000L),
            targetDurationMinutes = (durationSeconds / 60L).toInt(),
            state = FastingState.FASTING
        )

        assertEquals(FastingState.FASTING, session1.state)
        assertEquals(now, session1.startTimeEpochMillis)
        assertEquals(16 * 60, session1.targetDurationMinutes)

        // 3. Simula arresto timer dopo 2 ore
        val stopTime = now + 2 * 3600 * 1000L
        val stoppedSession = session1.copy(
            actualEndTimeEpochMillis = stopTime,
            state = FastingState.FASTING
        )
        assertNotNull(stoppedSession.actualEndTimeEpochMillis)
        assertEquals(stopTime, stoppedSession.actualEndTimeEpochMillis)

        // 4. Simula riavvio immediato (dopo lo stop)
        val restartTime = stopTime + 1000L
        val scheduledRestart = FastingCalculator.resolveCurrentScheduledPeriod(plan, restartTime)
        val newDurationSeconds = scheduledRestart.durationSeconds

        val session2 = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = restartTime,
            plannedEndTimeEpochMillis = restartTime + (newDurationSeconds * 1000L),
            targetDurationMinutes = (newDurationSeconds / 60L).toInt(),
            state = FastingState.FASTING
        )

        // Verifica che la nuova sessione sia attiva e riparta esattamente dal momento del tap
        assertEquals(FastingState.FASTING, session2.state)
        assertEquals(restartTime, session2.startTimeEpochMillis)
        assertEquals(restartTime + 16 * 3600 * 1000L, session2.plannedEndTimeEpochMillis)
        assertEquals(0L, FastingCalculator.calculateElapsedSeconds(session2, restartTime))
        assertEquals(16 * 3600L, FastingCalculator.calculateRemainingSeconds(session2, restartTime))
        assertEquals(0f, FastingCalculator.calculateProgressFraction(session2, restartTime), 0.001f)
    }

    @Test
    fun testStartFastingDuringEatingWindowAlwaysStartsFasting() {
        val plan = FastingPlanTemplates.PLAN_20_4
        val now = 1700000000000L

        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)
        val durationMinutes = (scheduled.durationSeconds / 60L).toInt()
        assertEquals(20 * 60, durationMinutes)

        val newSession = FastingSession(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            planName = plan.name,
            startTimeEpochMillis = now,
            plannedEndTimeEpochMillis = now + (scheduled.durationSeconds * 1000L),
            targetDurationMinutes = durationMinutes,
            state = FastingState.FASTING
        )

        assertEquals(FastingState.FASTING, newSession.state)
        assertEquals(now, newSession.startTimeEpochMillis)
        assertEquals(now + 20 * 3600 * 1000L, newSession.plannedEndTimeEpochMillis)
    }
}
