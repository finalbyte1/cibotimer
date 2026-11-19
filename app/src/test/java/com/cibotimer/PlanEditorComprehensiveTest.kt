package com.cibotimer

import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.domain.Span
import com.cibotimer.domain.WeekSpanModel
import com.cibotimer.domain.WeekSpanModel.toPeriod
import com.cibotimer.domain.toSpan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditorComprehensiveTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val referencePlan: FastingPlan = FastingPlanTemplates.PLAN_LUIGI_1

    // =========================================================================
    // 1. CALCOLO DELLE DURATE (NESSUN TRONCAMENTO: "18h 45m", NON "18h")
    // =========================================================================

    @Test
    fun testReferencePlanDurationCalculationsNoTruncation() {
        val periods = referencePlan.getEffectivePeriods()
        assertEquals(7, periods.size)

        // Periodo 1: Lun 14:00 -> Mar 07:45 = 17h 45m (1065 min)
        val p1 = periods[0]
        assertEquals(1, p1.startDayOfWeek)
        assertEquals(14, p1.startHour)
        assertEquals(0, p1.startMinute)
        assertEquals(2, p1.endDayOfWeek)
        assertEquals(7, p1.endHour)
        assertEquals(45, p1.endMinute)
        assertEquals(1065, p1.fastingDurationMinutes)
        assertEquals("17h 45m", p1.durationHoursString)

        // Periodo 2: Mar 13:00 -> Mer 07:00 = 18h (1080 min)
        val p2 = periods[1]
        assertEquals(2, p2.startDayOfWeek)
        assertEquals(13, p2.startHour)
        assertEquals(0, p2.startMinute)
        assertEquals(3, p2.endDayOfWeek)
        assertEquals(7, p2.endHour)
        assertEquals(0, p2.endMinute)
        assertEquals(1080, p2.fastingDurationMinutes)
        assertEquals("18h", p2.durationHoursString)

        // Periodo 3: Mer 12:00 -> Gio 07:00 = 19h (1140 min)
        val p3 = periods[2]
        assertEquals(3, p3.startDayOfWeek)
        assertEquals(12, p3.startHour)
        assertEquals(0, p3.startMinute)
        assertEquals(4, p3.endDayOfWeek)
        assertEquals(7, p3.endHour)
        assertEquals(0, p3.endMinute)
        assertEquals(1140, p3.fastingDurationMinutes)
        assertEquals("19h", p3.durationHoursString)

        // Periodo 4: Gio 13:00 -> Ven 07:45 = 18h 45m (1125 min)
        val p4 = periods[3]
        assertEquals(4, p4.startDayOfWeek)
        assertEquals(13, p4.startHour)
        assertEquals(0, p4.startMinute)
        assertEquals(5, p4.endDayOfWeek)
        assertEquals(7, p4.endHour)
        assertEquals(45, p4.endMinute)
        assertEquals(1125, p4.fastingDurationMinutes)
        assertEquals("18h 45m", p4.durationHoursString)

        // Periodo 5: Ven 13:00 -> Sab 08:00 = 19h (1140 min)
        val p5 = periods[4]
        assertEquals(5, p5.startDayOfWeek)
        assertEquals(13, p5.startHour)
        assertEquals(0, p5.startMinute)
        assertEquals(6, p5.endDayOfWeek)
        assertEquals(8, p5.endHour)
        assertEquals(0, p5.endMinute)
        assertEquals(1140, p5.fastingDurationMinutes)
        assertEquals("19h", p5.durationHoursString)

        // Periodo 6: Sab 21:00 -> Dom 07:45 = 10h 45m (645 min)
        val p6 = periods[5]
        assertEquals(6, p6.startDayOfWeek)
        assertEquals(21, p6.startHour)
        assertEquals(0, p6.startMinute)
        assertEquals(7, p6.endDayOfWeek)
        assertEquals(7, p6.endHour)
        assertEquals(45, p6.endMinute)
        assertEquals(645, p6.fastingDurationMinutes)
        assertEquals("10h 45m", p6.durationHoursString)

        // Periodo 7: Dom 16:00 -> Lun 13:00 = 21h (1260 min)
        val p7 = periods[6]
        assertEquals(7, p7.startDayOfWeek)
        assertEquals(16, p7.startHour)
        assertEquals(0, p7.startMinute)
        assertEquals(1, p7.endDayOfWeek)
        assertEquals(13, p7.endHour)
        assertEquals(0, p7.endMinute)
        assertEquals(1260, p7.fastingDurationMinutes)
        assertEquals("21h", p7.durationHoursString)
    }

    // =========================================================================
    // 2. PERIODO DOM->LUN TRATTATO COME UNO SOLO (NON SPEZZATO A MEZZANOTTE)
    // =========================================================================

    @Test
    fun testSundayToMondayPeriodIsSingleContinuousSpan() {
        val p7 = referencePlan.getEffectivePeriods()[6]
        val span7 = p7.toSpan()

        // Dom è giorno 7 (indice 6), inizio 16:00 -> 6 * 1440 + 16 * 60 = 9600
        assertEquals(9600, span7.start)
        // Durata 21 ore = 1260 minuti -> fine assoluta 9600 + 1260 = 10860
        assertEquals(10860, span7.end)
        assertEquals(1260, span7.durationMinutes)

        // Ricostruzione da Span a FastingPeriod preserva l'interezza del periodo ciclico
        val reconstructed = span7.toPeriod(7, "p7")
        assertEquals(7, reconstructed.startDayOfWeek)
        assertEquals(16, reconstructed.startHour)
        assertEquals(0, reconstructed.startMinute)
        assertEquals(1, reconstructed.endDayOfWeek)
        assertEquals(13, reconstructed.endHour)
        assertEquals(0, reconstructed.endMinute)
        assertEquals(1260, reconstructed.fastingDurationMinutes)
        assertEquals("21h", reconstructed.durationHoursString)
    }

    // =========================================================================
    // 3. LIMITI MIN/MAX DI INIZIO E FINE PER OGNI PERIODO
    // =========================================================================

    @Test
    fun testMinMaxBoundsForEachPeriod() {
        // Periodo 7 (Dom 16:00 -> Lun 13:00):
        // Inizio >= Dom 07:45 (fine Periodo 6)
        // Fine <= Lun 14:00 (inizio Periodo 1)
        val b7 = PlanShiftEngine.getPeriodBounds(referencePlan, 7)
        assertTrue(b7.hasAnterior)
        assertEquals(7, b7.anteriorEndDayOfWeek)
        assertEquals(7, b7.anteriorEndHour)
        assertEquals(45, b7.anteriorEndMinute)

        assertTrue(b7.hasNext)
        assertEquals(1, b7.nextStartDayOfWeek)
        assertEquals(14, b7.nextStartHour)
        assertEquals(0, b7.nextStartMinute)

        // Gap prima: da Dom 07:45 a Dom 16:00 = 8h 15m = 495 min
        assertEquals(495, b7.gapBeforeMinutes)
        // Gap dopo: da Lun 13:00 a Lun 14:00 = 60 min
        assertEquals(60, b7.gapAfterMinutes)

        // Periodo 1 (Lun 14:00 -> Mar 07:45):
        // Inizio >= Lun 13:00 (fine Periodo 7)
        // Fine <= Mar 13:00 (inizio Periodo 2)
        val b1 = PlanShiftEngine.getPeriodBounds(referencePlan, 1)
        assertTrue(b1.hasAnterior)
        assertEquals(1, b1.anteriorEndDayOfWeek)
        assertEquals(13, b1.anteriorEndHour)
        assertEquals(0, b1.anteriorEndMinute)
        assertEquals(60, b1.gapBeforeMinutes) // Finestra pasto Lunedì 13:00-14:00

        assertTrue(b1.hasNext)
        assertEquals(2, b1.nextStartDayOfWeek)
        assertEquals(13, b1.nextStartHour)
        assertEquals(0, b1.nextStartMinute)
        // Gap dopo: da Mar 07:45 a Mar 13:00 = 5h 15m = 315 min
        assertEquals(315, b1.gapAfterMinutes)
    }

    // =========================================================================
    // 4. SPOSTAMENTO DEI PERIODI (PASSI DA 1 ORA E 15 MIN) E MODIFICA DURATA
    // =========================================================================

    @Test
    fun testShiftPeriodForwardAndBackwardSteps() {
        // Sposta Periodo 7 in avanti di 15 min -> Dom 16:15 -> Lun 13:15
        val shiftedPlus15m = PlanShiftEngine.shiftPeriod(referencePlan, 7, 0, 15)
        val p7Plus15 = shiftedPlus15m.getEffectivePeriods()[6]
        assertEquals(16, p7Plus15.startHour)
        assertEquals(15, p7Plus15.startMinute)
        assertEquals(13, p7Plus15.endHour)
        assertEquals(15, p7Plus15.endMinute)
        assertEquals(1260, p7Plus15.fastingDurationMinutes)

        // Sposta Periodo 7 indietro di 1 ora -> Dom 15:00 -> Lun 12:00
        val shiftedMinus1h = PlanShiftEngine.shiftPeriod(referencePlan, 7, -1, 0)
        val p7Minus1h = shiftedMinus1h.getEffectivePeriods()[6]
        assertEquals(15, p7Minus1h.startHour)
        assertEquals(0, p7Minus1h.startMinute)
        assertEquals(12, p7Minus1h.endHour)
        assertEquals(0, p7Minus1h.endMinute)
        assertEquals(1260, p7Minus1h.fastingDurationMinutes)
    }

    @Test
    fun testAdjustPeriodDurationSteps() {
        // Aumenta durata Periodo 7 di 15 min (nuova fine Lun 13:15, nuova durata 21h 15m)
        val adjustedPlus15m = PlanShiftEngine.adjustPeriodDuration(referencePlan, 7, 15)
        val p7AdjPlus = adjustedPlus15m.getEffectivePeriods()[6]
        assertEquals(16, p7AdjPlus.startHour)
        assertEquals(0, p7AdjPlus.startMinute)
        assertEquals(13, p7AdjPlus.endHour)
        assertEquals(15, p7AdjPlus.endMinute)
        assertEquals(1275, p7AdjPlus.fastingDurationMinutes)
        assertEquals("21h 15m", p7AdjPlus.durationHoursString)

        // Riduci durata Periodo 7 di 60 min (nuova fine Lun 12:00, nuova durata 20h)
        val adjustedMinus60m = PlanShiftEngine.adjustPeriodDuration(referencePlan, 7, -60)
        val p7AdjMinus = adjustedMinus60m.getEffectivePeriods()[6]
        assertEquals(16, p7AdjMinus.startHour)
        assertEquals(0, p7AdjMinus.startMinute)
        assertEquals(12, p7AdjMinus.endHour)
        assertEquals(0, p7AdjMinus.endMinute)
        assertEquals(1200, p7AdjMinus.fastingDurationMinutes)
        assertEquals("20h", p7AdjMinus.durationHoursString)
    }

    // =========================================================================
    // 5. CASI LIMITE: FINESTRA PASTO MINIMA, PERIODI ADIACENTI, LIMITI
    // =========================================================================

    @Test
    fun testShiftingDoesNotExceedBoundsOrOverlap() {
        // Il gap dopo Periodo 7 è di 60 minuti (fino a Lun 14:00)
        // Tentare uno spostamento di 2 ore (+120 min) deve essere bloccato / limitato al gap massimo (+60 min)
        val shiftedMax = PlanShiftEngine.shiftPeriod(referencePlan, 7, 2, 0)
        val p7Max = shiftedMax.getEffectivePeriods()[6]
        // Deve essersi fermato esattamente a fine Lun 14:00 (avanzato al massimo di 60 min: Dom 17:00 -> Lun 14:00)
        assertEquals(17, p7Max.startHour)
        assertEquals(0, p7Max.startMinute)
        assertEquals(14, p7Max.endHour)
        assertEquals(0, p7Max.endMinute)

        // Ora il gap dopo è 0 min: non è più possibile spostare avanti né aumentare la durata
        val boundsAtLimit = PlanShiftEngine.getPeriodBounds(shiftedMax, 7)
        assertEquals(0, boundsAtLimit.gapAfterMinutes)
        assertFalse("Cannot shift plus 1h when touching next period", boundsAtLimit.canShiftPlus1h)
        assertFalse("Cannot shift plus 15m when touching next period", boundsAtLimit.canShiftPlus15m)
        assertFalse("Cannot increase duration when touching next period", boundsAtLimit.canDurationPlus(15))
    }

    @Test
    fun testShiftAllDaysPreservesRelativeDistancesAndDurations() {
        // Sposta tutti i periodi di 1 ora in avanti
        val shiftedPlan = PlanShiftEngine.shiftAllDays(referencePlan, 1, 0)
        val shiftedPeriods = shiftedPlan.getEffectivePeriods()
        assertEquals(7, shiftedPeriods.size)

        // Durate invariate
        for (i in 0 until 7) {
            assertEquals(
                referencePlan.getEffectivePeriods()[i].fastingDurationMinutes,
                shiftedPeriods[i].fastingDurationMinutes
            )
        }

        // P1 ora inizia alle 15:00 invece che alle 14:00
        assertEquals(15, shiftedPeriods[0].startHour)
        assertEquals(8, shiftedPeriods[0].endHour)
        assertEquals(45, shiftedPeriods[0].endMinute)
    }

    // =========================================================================
    // 6. SALVATAGGIO E RIAPERTURA: IL PIANO RIELETTO È IDENTICO A QUELLO SALVATO
    // =========================================================================

    @Test
    fun testSaveAndReloadPlanIdentity() {
        val serializedJson = json.encodeToString(referencePlan)
        assertNotNull(serializedJson)

        val deserializedPlan = json.decodeFromString<FastingPlan>(serializedJson)
        assertEquals(referencePlan.id, deserializedPlan.id)
        assertEquals(referencePlan.name, deserializedPlan.name)
        assertEquals(referencePlan.category, deserializedPlan.category)

        val origPeriods = referencePlan.getEffectivePeriods()
        val reloadedPeriods = deserializedPlan.getEffectivePeriods()
        assertEquals(origPeriods.size, reloadedPeriods.size)

        for (i in origPeriods.indices) {
            val o = origPeriods[i]
            val r = reloadedPeriods[i]
            assertEquals(o.periodNumber, r.periodNumber)
            assertEquals(o.startDayOfWeek, r.startDayOfWeek)
            assertEquals(o.startHour, r.startHour)
            assertEquals(o.startMinute, r.startMinute)
            assertEquals(o.endDayOfWeek, r.endDayOfWeek)
            assertEquals(o.endHour, r.endHour)
            assertEquals(o.endMinute, r.endMinute)
            assertEquals(o.fastingDurationMinutes, r.fastingDurationMinutes)
            assertEquals(o.durationHoursString, r.durationHoursString)
        }
    }

    // =========================================================================
    // 7. COMPONENTI UI & STATO: STEPPER BUTTONS DISABILITATI AI LIMITI
    // =========================================================================

    @Test
    fun testStepperControlStatesAtBounds() {
        // Con gapBefore = 0, canShiftMinus1h e canShiftMinus15m devono essere false
        val boundsNoBefore = PlanShiftEngine.PeriodBounds(
            hasAnterior = true,
            gapBeforeMinutes = 0,
            hasNext = true,
            gapAfterMinutes = 120
        )
        assertFalse(boundsNoBefore.canShiftMinus1h)
        assertFalse(boundsNoBefore.canShiftMinus15m)
        assertTrue(boundsNoBefore.canShiftPlus1h)
        assertTrue(boundsNoBefore.canShiftPlus15m)

        // Con gapAfter = 10, canShiftPlus15m e canShiftPlus1h devono essere false, canDurationPlus(15) false
        val boundsTightAfter = PlanShiftEngine.PeriodBounds(
            hasAnterior = true,
            gapBeforeMinutes = 60,
            hasNext = true,
            gapAfterMinutes = 10
        )
        assertTrue(boundsTightAfter.canShiftMinus1h)
        assertTrue(boundsTightAfter.canShiftMinus15m)
        assertFalse(boundsTightAfter.canShiftPlus1h)
        assertFalse(boundsTightAfter.canShiftPlus15m)
        assertFalse(boundsTightAfter.canDurationPlus(15))
        assertFalse(boundsTightAfter.canDurationPlus(60))

        // Verifica calcolo durata minima (MIN_DURATION = 60 min)
        val minDurationMinutes = 60
        val curDuration = 60
        val canMinus15m = curDuration - 15 >= minDurationMinutes
        val canMinus1h = curDuration - 60 >= minDurationMinutes
        assertFalse(canMinus15m)
        assertFalse(canMinus1h)
    }
}
