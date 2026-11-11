package com.cibotimer

import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanShiftEngineTest {

    @Test
    fun testShiftAllDaysPlusOneHourPreservesDuration() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        val initialDurations = initialPlan.days.map { it.fastingDurationMinutes }

        // Shift all days by +1 hour
        val shiftedPlan = PlanShiftEngine.shiftAllDays(initialPlan, deltaHours = 1)

        assertEquals(7, shiftedPlan.days.size)

        // Verify each day shifted by +1 hour and duration is IDENTICAL
        shiftedPlan.days.forEachIndexed { index, day ->
            val originalDay = initialPlan.days[index]
            val expectedStart = (originalDay.startHour + 1) % 24
            val expectedEnd = (originalDay.endHour + 1) % 24

            assertEquals("Start hour must shift +1", expectedStart, day.startHour)
            assertEquals("End hour must shift +1", expectedEnd, day.endHour)
            assertEquals("Duration must remain strictly identical", initialDurations[index], day.fastingDurationMinutes)
            assertEquals(960, day.fastingDurationMinutes)
        }
    }

    @Test
    fun testShiftAllDaysMinusOneHourPreservesDuration() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        val initialDurations = initialPlan.days.map { it.fastingDurationMinutes }

        // Shift all days by -1 hour
        val shiftedPlan = PlanShiftEngine.shiftAllDays(initialPlan, deltaHours = -1)

        assertEquals(7, shiftedPlan.days.size)

        shiftedPlan.days.forEachIndexed { index, day ->
            val originalDay = initialPlan.days[index]
            val expectedStart = if (originalDay.startHour - 1 < 0) originalDay.startHour - 1 + 24 else originalDay.startHour - 1
            val expectedEnd = if (originalDay.endHour - 1 < 0) originalDay.endHour - 1 + 24 else originalDay.endHour - 1

            assertEquals("Start hour must shift -1", expectedStart, day.startHour)
            assertEquals("End hour must shift -1", expectedEnd, day.endHour)
            assertEquals("Duration must remain strictly identical", initialDurations[index], day.fastingDurationMinutes)
            assertEquals(960, day.fastingDurationMinutes)
        }
    }

    @Test
    fun testShiftSingleDayPreservesDuration() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        // Shift Wednesday (dayOfWeek = 3) by +2 hours
        val shiftedPlan = PlanShiftEngine.shiftDay(initialPlan, dayOfWeek = 3, deltaHours = 2, deltaMinutes = 0)

        val wednesday = shiftedPlan.days.first { it.dayOfWeek == 3 }
        assertEquals(22, wednesday.startHour) // 20 + 2
        assertEquals(14, wednesday.endHour)   // 12 + 2
        assertEquals(960, wednesday.fastingDurationMinutes)

        // Monday (dayOfWeek = 1) remains unaffected
        val monday = shiftedPlan.days.first { it.dayOfWeek == 1 }
        assertEquals(20, monday.startHour)
        assertEquals(12, monday.endHour)
        assertEquals(960, monday.fastingDurationMinutes)
    }

    @Test
    fun testAdjustDurationModifiesEndHour() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        // Extend Monday by +60 minutes (from 16h to 17h)
        val adjustedPlan = PlanShiftEngine.adjustDuration(initialPlan, dayOfWeek = 1, deltaMinutes = 60)

        val monday = adjustedPlan.days.first { it.dayOfWeek == 1 }
        assertEquals(20, monday.startHour)
        assertEquals(13, monday.endHour) // 12:00 + 1 hour -> 13:00
        assertEquals(1020, monday.fastingDurationMinutes) // 960 + 60
    }

    @Test
    fun testToggleDayFasting() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        // Toggle Tuesday (dayOfWeek = 2) to free day
        val modifiedPlan = PlanShiftEngine.toggleDayFasting(initialPlan, dayOfWeek = 2)
        val tuesday = modifiedPlan.days.first { it.dayOfWeek == 2 }
        assertEquals(false, tuesday.isFastingDay)

        // Toggle back to fasting day
        val restoredPlan = PlanShiftEngine.toggleDayFasting(modifiedPlan, dayOfWeek = 2)
        val restoredTuesday = restoredPlan.days.first { it.dayOfWeek == 2 }
        assertEquals(true, restoredTuesday.isFastingDay)
    }

    @Test
    fun testSetAllDaysFasting() {
        val initialPlan = FastingPlanTemplates.PLAN_16_8
        val modifiedPlan = PlanShiftEngine.toggleDayFasting(initialPlan, dayOfWeek = 2)
        assertEquals(false, modifiedPlan.days.first { it.dayOfWeek == 2 }.isFastingDay)

        val reactivatedPlan = PlanShiftEngine.setAllDaysFasting(modifiedPlan, true)
        assertEquals(true, reactivatedPlan.days.all { it.isFastingDay })
    }

    @Test
    fun testPlanLuigi1Specification() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        assertEquals("Luigi 1", luigi1.name)
        assertEquals(7, luigi1.days.size)

        // Lunedì: 14:00 -> Mar 10:00 (20h)
        val mon = luigi1.days.first { it.dayOfWeek == 1 }
        assertEquals(14, mon.startHour)
        assertEquals(10, mon.endHour)
        assertEquals(1200, mon.fastingDurationMinutes)

        // Martedì: 11:00 -> Mer 07:00 (20h)
        val tue = luigi1.days.first { it.dayOfWeek == 2 }
        assertEquals(11, tue.startHour)
        assertEquals(7, tue.endHour)
        assertEquals(1200, tue.fastingDurationMinutes)

        // Mercoledì: 12:00 -> Gio 10:00 (22h)
        val wed = luigi1.days.first { it.dayOfWeek == 3 }
        assertEquals(12, wed.startHour)
        assertEquals(10, wed.endHour)
        assertEquals(1320, wed.fastingDurationMinutes)

        // Giovedì: 12:00 -> Ven 11:00 (23h)
        val thu = luigi1.days.first { it.dayOfWeek == 4 }
        assertEquals(12, thu.startHour)
        assertEquals(11, thu.endHour)
        assertEquals(1380, thu.fastingDurationMinutes)

        // Venerdì: 12:00 -> Sab 08:00 (20h)
        val fri = luigi1.days.first { it.dayOfWeek == 5 }
        assertEquals(12, fri.startHour)
        assertEquals(8, fri.endHour)
        assertEquals(1200, fri.fastingDurationMinutes)

        // Sabato: 21:00 -> Dom 10:00 (13h)
        val sat = luigi1.days.first { it.dayOfWeek == 6 }
        assertEquals(21, sat.startHour)
        assertEquals(10, sat.endHour)
        assertEquals(780, sat.fastingDurationMinutes)

        // Domenica: 16:00 -> Lun 11:00 (19h)
        val sun = luigi1.days.first { it.dayOfWeek == 7 }
        assertEquals(16, sun.startHour)
        assertEquals(11, sun.endHour)
        assertEquals(1140, sun.fastingDurationMinutes)

        // Verifica dei 9 periodi CiboTimer del piano Luigi 1
        val periods = luigi1.getEffectivePeriods()
        assertEquals(9, periods.size)

        // P1: lun 00:00 - lun 13:00 (13h)
        assertEquals(1, periods[0].periodNumber)
        assertEquals(1, periods[0].startDayOfWeek)
        assertEquals(0, periods[0].startHour)
        assertEquals(1, periods[0].endDayOfWeek)
        assertEquals(13, periods[0].endHour)
        assertEquals(780, periods[0].fastingDurationMinutes)

        // P2: lun 14:00 - mar 10:00 (20h)
        assertEquals(2, periods[1].periodNumber)
        assertEquals(1, periods[1].startDayOfWeek)
        assertEquals(14, periods[1].startHour)
        assertEquals(2, periods[1].endDayOfWeek)
        assertEquals(10, periods[1].endHour)
        assertEquals(1200, periods[1].fastingDurationMinutes)

        // P3: mar 11:00 - mer 07:00 (20h)
        assertEquals(3, periods[2].periodNumber)
        assertEquals(2, periods[2].startDayOfWeek)
        assertEquals(11, periods[2].startHour)
        assertEquals(3, periods[2].endDayOfWeek)
        assertEquals(7, periods[2].endHour)
        assertEquals(1200, periods[2].fastingDurationMinutes)

        // P4: mer 12:00 - gio 10:00 (22h)
        assertEquals(4, periods[3].periodNumber)
        assertEquals(3, periods[3].startDayOfWeek)
        assertEquals(12, periods[3].startHour)
        assertEquals(4, periods[3].endDayOfWeek)
        assertEquals(10, periods[3].endHour)
        assertEquals(1320, periods[3].fastingDurationMinutes)

        // P5: gio 12:00 - ven 11:00 (23h)
        assertEquals(5, periods[4].periodNumber)
        assertEquals(4, periods[4].startDayOfWeek)
        assertEquals(12, periods[4].startHour)
        assertEquals(5, periods[4].endDayOfWeek)
        assertEquals(11, periods[4].endHour)
        assertEquals(1380, periods[4].fastingDurationMinutes)

        // P6: ven 12:00 - sab 08:00 (20h)
        assertEquals(6, periods[5].periodNumber)
        assertEquals(5, periods[5].startDayOfWeek)
        assertEquals(12, periods[5].startHour)
        assertEquals(6, periods[5].endDayOfWeek)
        assertEquals(8, periods[5].endHour)
        assertEquals(1200, periods[5].fastingDurationMinutes)

        // P7: sab 21:00 - dom 10:00 (13h)
        assertEquals(7, periods[6].periodNumber)
        assertEquals(6, periods[6].startDayOfWeek)
        assertEquals(21, periods[6].startHour)
        assertEquals(7, periods[6].endDayOfWeek)
        assertEquals(10, periods[6].endHour)
        assertEquals(780, periods[6].fastingDurationMinutes)

        // P8: dom 16:00 - lun 11:00 (19h)
        assertEquals(8, periods[7].periodNumber)
        assertEquals(7, periods[7].startDayOfWeek)
        assertEquals(16, periods[7].startHour)
        assertEquals(8, periods[7].endDayOfWeek)
        assertEquals(11, periods[7].endHour)
        assertEquals(1140, periods[7].fastingDurationMinutes)

        // P9: lun 11:00 - lun 23:00 (12h)
        assertEquals(9, periods[8].periodNumber)
        assertEquals(8, periods[8].startDayOfWeek)
        assertEquals(11, periods[8].startHour)
        assertEquals(8, periods[8].endDayOfWeek)
        assertEquals(23, periods[8].endHour)
        assertEquals(720, periods[8].fastingDurationMinutes)
    }

    @Test
    fun testShiftPeriodDoesNotCascadeByDefault() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        val initialP3 = luigi1.getEffectivePeriods().first { it.periodNumber == 3 }

        // Spostiamo il Periodo 2 di +1 ora senza cascade (comportamento predefinito BodyFast)
        val shifted = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 2, deltaHours = 1, deltaMinutes = 0)
        val p2 = shifted.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(15, p2.startHour) // 14 -> 15
        assertEquals(11, p2.endHour)   // 10 -> 11

        // Il Periodo 3 NON deve essere modificato
        val p3 = shifted.getEffectivePeriods().first { it.periodNumber == 3 }
        assertEquals("Periodo 3 startHour non deve cambiare", initialP3.startHour, p3.startHour)
        assertEquals("Periodo 3 endHour non deve cambiare", initialP3.endHour, p3.endHour)
        assertEquals("Periodo 3 durata non deve cambiare", initialP3.fastingDurationMinutes, p3.fastingDurationMinutes)
    }

    @Test
    fun testShiftDayDoesNotCascadeByDefault() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val initialTue = plan.days.first { it.dayOfWeek == 2 }

        // Modifica Lunedì (dayOfWeek = 1) di +1 ora senza cascade
        val shifted = PlanShiftEngine.shiftDay(plan, dayOfWeek = 1, deltaHours = 1)

        val mon = shifted.days.first { it.dayOfWeek == 1 }
        assertEquals(21, mon.startHour) // 20 -> 21
        assertEquals(13, mon.endHour)   // 12 -> 13

        // Martedì (giorno successivo) deve rimanere RIGOROSAMENTE INVARIATO
        val tue = shifted.days.first { it.dayOfWeek == 2 }
        assertEquals("Martedì startHour non deve cambiare", initialTue.startHour, tue.startHour)
        assertEquals("Martedì endHour non deve cambiare", initialTue.endHour, tue.endHour)
        assertEquals("Martedì durata non deve cambiare", initialTue.fastingDurationMinutes, tue.fastingDurationMinutes)
    }

    @Test
    fun testAdjustDurationDoesNotCascadeByDefault() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val initialTue = plan.days.first { it.dayOfWeek == 2 }

        // Allunghiamo Lunedì di +60 minuti senza cascade
        val adjusted = PlanShiftEngine.adjustDuration(plan, dayOfWeek = 1, deltaMinutes = 60)

        val mon = adjusted.days.first { it.dayOfWeek == 1 }
        assertEquals(20, mon.startHour)
        assertEquals(13, mon.endHour)
        assertEquals(1020, mon.fastingDurationMinutes)

        // Martedì deve rimanere RIGOROSAMENTE INVARIATO
        val tue = adjusted.days.first { it.dayOfWeek == 2 }
        assertEquals("Martedì startHour non deve cambiare", initialTue.startHour, tue.startHour)
        assertEquals("Martedì endHour non deve cambiare", initialTue.endHour, tue.endHour)
        assertEquals("Martedì durata non deve cambiare", initialTue.fastingDurationMinutes, tue.fastingDurationMinutes)
    }

    @Test
    fun testSundayOvernightFastWrapsIntoMondayMorning() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val effectivePeriods = plan.getEffectivePeriods()

        // Domenica (giorno 7): il digiuno inizia alle 20:00 e dura fino alle 24:00 (4 ore = 240 min)
        val sundaySegments = com.cibotimer.domain.FastingCalculator.computePeriods24hSegments(effectivePeriods, 7)
        val sundayFastSegment = sundaySegments.find { it.isFasting && it.startMin == 1200f }
        org.junit.Assert.assertNotNull(sundayFastSegment)
        assertEquals(1200f, sundayFastSegment!!.startMin)
        assertEquals(1440f, sundayFastSegment.endMin)

        // Lunedì (giorno 1): la continuazione del digiuno di Domenica notte copre 00:00 - 12:00 (12 ore = 720 min)
        val mondaySegments = com.cibotimer.domain.FastingCalculator.computePeriods24hSegments(effectivePeriods, 1)
        val mondayMorningFast = mondaySegments.find { it.isFasting && it.startMin == 0f }
        org.junit.Assert.assertNotNull(mondayMorningFast)
        assertEquals(0f, mondayMorningFast!!.startMin)
        assertEquals(720f, mondayMorningFast.endMin)
    }

    @Test
    fun testCascadingShiftPeriodsExplicitFlag() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Spostiamo il Periodo 2 di +1 ora con cascadeNext = true esplicito
        val shifted = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 2, deltaHours = 1, deltaMinutes = 0, cascadeNext = true)
        val p2 = shifted.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(15, p2.startHour)
        assertEquals(11, p2.endHour)

        // Con cascadeNext = true, il Periodo 3 deve iniziare ad 1 ora oltre la fine di P2 (11:00 + 1 = 12:00)
        val p3 = shifted.getEffectivePeriods().first { it.periodNumber == 3 }
        assertEquals(12, p3.startHour)
        assertEquals(8, p3.endHour)
        assertEquals(1200, p3.fastingDurationMinutes)
    }

    @Test
    fun testCascadingShiftIncreasesNextDayStartByOneHourExplicitFlag() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val shifted = PlanShiftEngine.shiftDay(plan, dayOfWeek = 1, deltaHours = 1, cascadeNextDay = true)

        val mon = shifted.days.first { it.dayOfWeek == 1 }
        assertEquals(21, mon.startHour)
        assertEquals(13, mon.endHour)

        val tue = shifted.days.first { it.dayOfWeek == 2 }
        assertEquals(14, tue.startHour)
        assertEquals(6, tue.endHour)
        assertEquals(960, tue.fastingDurationMinutes)
    }

    @Test
    fun testCascadingAdjustDurationIncreasesNextDayStartByOneHourExplicitFlag() {
        val plan = FastingPlanTemplates.PLAN_16_8
        val adjusted = PlanShiftEngine.adjustDuration(plan, dayOfWeek = 1, deltaMinutes = 60, cascadeNextDay = true)

        val mon = adjusted.days.first { it.dayOfWeek == 1 }
        assertEquals(20, mon.startHour)
        assertEquals(13, mon.endHour)
        assertEquals(1020, mon.fastingDurationMinutes)

        val tue = adjusted.days.first { it.dayOfWeek == 2 }
        assertEquals(14, tue.startHour)
        assertEquals(6, tue.endHour)
        assertEquals(960, tue.fastingDurationMinutes)
    }

    @Test
    fun testShiftPeriodCannotSurpassAnteriorPeriod() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Nel piano Luigi 1:
        // Periodo 1: Lun 00:00 - 13:00 (termina alle 13:00 di Lunedì)
        // Periodo 2: Lun 14:00 - Mar 10:00 (inizia alle 14:00 di Lunedì)
        // Il gap disponibile indietro verso il periodo anteriore è di 1 ora (60 min).

        val bounds = PlanShiftEngine.getPeriodBounds(luigi1, periodNumber = 2)
        org.junit.Assert.assertTrue(bounds.hasAnterior)
        assertEquals(1, bounds.anteriorPeriodNumber)
        assertEquals(13, bounds.anteriorEndHour)
        assertEquals(0, bounds.anteriorEndMinute)
        assertEquals(60, bounds.gapBeforeMinutes)
        org.junit.Assert.assertTrue(bounds.canShiftMinus1h)
        org.junit.Assert.assertTrue(bounds.canShiftMinus15m)

        // Spostamento di -2 ore: non deve superare le 13:00 (deve bloccarsi a 13:00)
        val shiftedMinus2h = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 2, deltaHours = -2, deltaMinutes = 0)
        val p2Clamped = shiftedMinus2h.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals("L'orario di inizio non può superare le 13:00 del periodo anteriore", 13, p2Clamped.startHour)
        assertEquals(0, p2Clamped.startMinute)
        assertEquals(9, p2Clamped.endHour) // 13:00 + 20h = 09:00 del giorno dopo
        assertEquals(1200, p2Clamped.fastingDurationMinutes)

        // Quando è a 13:00, il gap prima è 0 e i tasti -1h e -15m devono essere disabilitati
        val boundsAtBorder = PlanShiftEngine.getPeriodBounds(shiftedMinus2h, periodNumber = 2)
        assertEquals(0, boundsAtBorder.gapBeforeMinutes)
        org.junit.Assert.assertFalse("Non deve poter retrocedere di 1 ora", boundsAtBorder.canShiftMinus1h)
        org.junit.Assert.assertFalse("Non deve poter retrocedere di 15 min", boundsAtBorder.canShiftMinus15m)

        // Tentando un ulteriore shift indietro (-15 min), deve rimanere invariato
        val attemptedFurtherShift = PlanShiftEngine.shiftPeriod(shiftedMinus2h, periodNumber = 2, deltaHours = 0, deltaMinutes = -15)
        val p2StillClamped = attemptedFurtherShift.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(13, p2StillClamped.startHour)
        assertEquals(0, p2StillClamped.startMinute)
    }

    @Test
    fun testUpdatePeriodTimeClampsToAnteriorPeriodEnd() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Periodo 1 finisce alle 13:00 di lunedì.
        // Se l'utente imposta l'inizio del Periodo 2 alle 11:00 (prima delle 13:00):
        val updated = PlanShiftEngine.updatePeriodTime(luigi1, periodNumber = 2, newHour = 11, newMinute = 0, isStart = true)
        val p2 = updated.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals("StartHour deve essere bloccato alle 13:00 del periodo anteriore", 13, p2.startHour)
        assertEquals(0, p2.startMinute)
    }

    @Test
    fun testShiftPeriodCannotSurpassNextPeriod() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Periodo 2 termina alle 10:00 di martedì.
        // Periodo 3 inizia alle 11:00 di martedì.
        // Gap in avanti = 60 minuti.
        val bounds = PlanShiftEngine.getPeriodBounds(luigi1, periodNumber = 2)
        org.junit.Assert.assertTrue(bounds.hasNext)
        assertEquals(3, bounds.nextPeriodNumber)
        assertEquals(11, bounds.nextStartHour)
        assertEquals(60, bounds.gapAfterMinutes)

        // Spostamento di +3 ore: deve bloccarsi al massimo a +1 ora (fine a 11:00)
        val shiftedPlus3h = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 2, deltaHours = 3, deltaMinutes = 0)
        val p2 = shiftedPlus3h.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(15, p2.startHour) // 14 -> 15
        assertEquals(11, p2.endHour)   // 10 -> 11 (bloccato all'inizio di P3)
    }
}
