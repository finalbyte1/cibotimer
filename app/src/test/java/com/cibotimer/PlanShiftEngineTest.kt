package com.cibotimer

import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.domain.Span
import com.cibotimer.domain.toPeriod
import com.cibotimer.domain.toSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

        // Lunedì: 14:00 -> Mar 07:45 (17h45m = 1065m)
        val mon = luigi1.days.first { it.dayOfWeek == 1 }
        assertEquals(14, mon.startHour)
        assertEquals(0, mon.startMinute)
        assertEquals(7, mon.endHour)
        assertEquals(45, mon.endMinute)
        assertEquals(1065, mon.fastingDurationMinutes)

        // Martedì: 13:00 -> Mer 07:00 (18h = 1080m)
        val tue = luigi1.days.first { it.dayOfWeek == 2 }
        assertEquals(13, tue.startHour)
        assertEquals(0, tue.startMinute)
        assertEquals(7, tue.endHour)
        assertEquals(0, tue.endMinute)
        assertEquals(1080, tue.fastingDurationMinutes)

        // Mercoledì: 12:00 -> Gio 07:00 (19h = 1140m)
        val wed = luigi1.days.first { it.dayOfWeek == 3 }
        assertEquals(12, wed.startHour)
        assertEquals(0, wed.startMinute)
        assertEquals(7, wed.endHour)
        assertEquals(0, wed.endMinute)
        assertEquals(1140, wed.fastingDurationMinutes)

        // Giovedì: 13:00 -> Ven 07:45 (18h45m = 1125m)
        val thu = luigi1.days.first { it.dayOfWeek == 4 }
        assertEquals(13, thu.startHour)
        assertEquals(0, thu.startMinute)
        assertEquals(7, thu.endHour)
        assertEquals(45, thu.endMinute)
        assertEquals(1125, thu.fastingDurationMinutes)

        // Venerdì: 13:00 -> Sab 08:00 (19h = 1140m)
        val fri = luigi1.days.first { it.dayOfWeek == 5 }
        assertEquals(13, fri.startHour)
        assertEquals(0, fri.startMinute)
        assertEquals(8, fri.endHour)
        assertEquals(0, fri.endMinute)
        assertEquals(1140, fri.fastingDurationMinutes)

        // Sabato: 21:00 -> Dom 07:45 (10h 45m = 645m)
        val sat = luigi1.days.first { it.dayOfWeek == 6 }
        assertEquals(21, sat.startHour)
        assertEquals(0, sat.startMinute)
        assertEquals(7, sat.endHour)
        assertEquals(45, sat.endMinute)
        assertEquals(645, sat.fastingDurationMinutes)

        // Domenica: 16:00 -> Lun 13:00 (21h = 1260m)
        val sun = luigi1.days.first { it.dayOfWeek == 7 }
        assertEquals(16, sun.startHour)
        assertEquals(0, sun.startMinute)
        assertEquals(13, sun.endHour)
        assertEquals(0, sun.endMinute)
        assertEquals(1260, sun.fastingDurationMinutes)

        // Verifica dei 7 periodi CiboTimer del piano Luigi 1
        val periods = luigi1.getEffectivePeriods()
        assertEquals(7, periods.size)

        // P1: lun 14:00 - mar 07:45 (17h45m = 1065m)
        assertEquals(1, periods[0].periodNumber)
        assertEquals(1, periods[0].startDayOfWeek)
        assertEquals(14, periods[0].startHour)
        assertEquals(0, periods[0].startMinute)
        assertEquals(2, periods[0].endDayOfWeek)
        assertEquals(7, periods[0].endHour)
        assertEquals(45, periods[0].endMinute)
        assertEquals(1065, periods[0].fastingDurationMinutes)

        // P2: mar 13:00 - mer 07:00 (18h = 1080m)
        assertEquals(2, periods[1].periodNumber)
        assertEquals(2, periods[1].startDayOfWeek)
        assertEquals(13, periods[1].startHour)
        assertEquals(0, periods[1].startMinute)
        assertEquals(3, periods[1].endDayOfWeek)
        assertEquals(7, periods[1].endHour)
        assertEquals(0, periods[1].endMinute)
        assertEquals(1080, periods[1].fastingDurationMinutes)

        // P3: mer 12:00 - gio 07:00 (19h = 1140m)
        assertEquals(3, periods[2].periodNumber)
        assertEquals(3, periods[2].startDayOfWeek)
        assertEquals(12, periods[2].startHour)
        assertEquals(0, periods[2].startMinute)
        assertEquals(4, periods[2].endDayOfWeek)
        assertEquals(7, periods[2].endHour)
        assertEquals(0, periods[2].endMinute)
        assertEquals(1140, periods[2].fastingDurationMinutes)

        // P4: gio 13:00 - ven 07:45 (18h45m = 1125m)
        assertEquals(4, periods[3].periodNumber)
        assertEquals(4, periods[3].startDayOfWeek)
        assertEquals(13, periods[3].startHour)
        assertEquals(0, periods[3].startMinute)
        assertEquals(5, periods[3].endDayOfWeek)
        assertEquals(7, periods[3].endHour)
        assertEquals(45, periods[3].endMinute)
        assertEquals(1125, periods[3].fastingDurationMinutes)

        // P5: ven 13:00 - sab 08:00 (19h = 1140m)
        assertEquals(5, periods[4].periodNumber)
        assertEquals(5, periods[4].startDayOfWeek)
        assertEquals(13, periods[4].startHour)
        assertEquals(0, periods[4].startMinute)
        assertEquals(6, periods[4].endDayOfWeek)
        assertEquals(8, periods[4].endHour)
        assertEquals(0, periods[4].endMinute)
        assertEquals(1140, periods[4].fastingDurationMinutes)

        // P6: sab 21:00 - dom 07:45 (10h 45m = 645m)
        assertEquals(6, periods[5].periodNumber)
        assertEquals(6, periods[5].startDayOfWeek)
        assertEquals(21, periods[5].startHour)
        assertEquals(0, periods[5].startMinute)
        assertEquals(7, periods[5].endDayOfWeek)
        assertEquals(7, periods[5].endHour)
        assertEquals(45, periods[5].endMinute)
        assertEquals(645, periods[5].fastingDurationMinutes)

        // P7: dom 16:00 - lun 13:00 (21h = 1260m)
        assertEquals(7, periods[6].periodNumber)
        assertEquals(7, periods[6].startDayOfWeek)
        assertEquals(16, periods[6].startHour)
        assertEquals(0, periods[6].startMinute)
        assertEquals(1, periods[6].endDayOfWeek)
        assertEquals(13, periods[6].endHour)
        assertEquals(0, periods[6].endMinute)
        assertEquals(1260, periods[6].fastingDurationMinutes)
    }

    @Test
    fun testShiftPeriodDoesNotCascadeByDefault() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        val initialP3 = luigi1.getEffectivePeriods().first { it.periodNumber == 3 }

        // Spostiamo il Periodo 2 di +1 ora senza cascade (comportamento predefinito BodyFast)
        val shifted = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 2, deltaHours = 1, deltaMinutes = 0)
        val p2 = shifted.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(14, p2.startHour) // 13 -> 14
        assertEquals(8, p2.endHour)    // 7 -> 8
        assertEquals(0, p2.endMinute)

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
        // Spostiamo il Periodo 1 di +1 ora con cascadeNext = true esplicito
        val shifted = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 1, deltaHours = 1, deltaMinutes = 0, cascadeNext = true)
        val p1 = shifted.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals(15, p1.startHour) // 14 -> 15
        assertEquals(8, p1.endHour)    // 7 -> 8
        assertEquals(45, p1.endMinute)

        // Con cascadeNext = true, il Periodo 2 deve iniziare ad 1 ora oltre la fine di P1 (08:45 + 1h = 09:45)
        val p2 = shifted.getEffectivePeriods().first { it.periodNumber == 2 }
        assertEquals(9, p2.startHour)
        assertEquals(45, p2.startMinute)
        assertEquals(3, p2.endHour)
        assertEquals(45, p2.endMinute)
        assertEquals(1080, p2.fastingDurationMinutes)
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
        // Nel piano Luigi 1 a 7 periodi ciclici:
        // Periodo 7: Dom 16:00 - Lun 13:00 (termina alle 13:00 di Lunedì)
        // Periodo 1: Lun 14:00 - Mar 07:45 (inizia alle 14:00 di Lunedì)
        // Il gap disponibile indietro verso il periodo anteriore (P7) è di 1 ora (60 min).

        val bounds = PlanShiftEngine.getPeriodBounds(luigi1, periodNumber = 1)
        org.junit.Assert.assertTrue(bounds.hasAnterior)
        assertEquals(7, bounds.anteriorPeriodNumber)
        assertEquals(13, bounds.anteriorEndHour)
        assertEquals(0, bounds.anteriorEndMinute)
        assertEquals(60, bounds.gapBeforeMinutes)
        org.junit.Assert.assertTrue(bounds.canShiftMinus1h)
        org.junit.Assert.assertTrue(bounds.canShiftMinus15m)

        // Spostamento di -2 ore: non deve superare le 13:00 (deve bloccarsi a 13:00)
        val shiftedMinus2h = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 1, deltaHours = -2, deltaMinutes = 0)
        val p1Clamped = shiftedMinus2h.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals("L'orario di inizio non può superare le 13:00 del periodo anteriore", 13, p1Clamped.startHour)
        assertEquals(0, p1Clamped.startMinute)
        assertEquals(6, p1Clamped.endHour) // 13:00 + 17h45m = 06:45 del giorno dopo
        assertEquals(45, p1Clamped.endMinute)
        assertEquals(1065, p1Clamped.fastingDurationMinutes)

        // Quando è a 13:00, il gap prima è 0 e i tasti -1h e -15m devono essere disabilitati
        val boundsAtBorder = PlanShiftEngine.getPeriodBounds(shiftedMinus2h, periodNumber = 1)
        assertEquals(0, boundsAtBorder.gapBeforeMinutes)
        org.junit.Assert.assertFalse("Non deve poter retrocedere di 1 ora", boundsAtBorder.canShiftMinus1h)
        org.junit.Assert.assertFalse("Non deve poter retrocedere di 15 min", boundsAtBorder.canShiftMinus15m)

        // Tentando un ulteriore shift indietro (-15 min), deve rimanere invariato
        val attemptedFurtherShift = PlanShiftEngine.shiftPeriod(shiftedMinus2h, periodNumber = 1, deltaHours = 0, deltaMinutes = -15)
        val p1StillClamped = attemptedFurtherShift.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals(13, p1StillClamped.startHour)
        assertEquals(0, p1StillClamped.startMinute)
    }

    @Test
    fun testUpdatePeriodTimeClampsToAnteriorPeriodEnd() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Periodo 7 finisce alle 13:00 di lunedì.
        // Se l'utente imposta l'inizio del Periodo 1 alle 11:00 (prima delle 13:00):
        val updated = PlanShiftEngine.updatePeriodTime(luigi1, periodNumber = 1, newHour = 11, newMinute = 0, isStart = true)
        val p1 = updated.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals("StartHour deve essere bloccato alle 13:00 del periodo anteriore", 13, p1.startHour)
        assertEquals(0, p1.startMinute)
    }

    @Test
    fun testShiftPeriodCannotSurpassNextPeriod() {
        val luigi1 = FastingPlanTemplates.PLAN_LUIGI_1
        // Periodo 1 termina alle 07:45 di martedì.
        // Periodo 2 inizia alle 13:00 di martedì.
        // Gap in avanti = 5h 15m = 315 minuti.
        val bounds = PlanShiftEngine.getPeriodBounds(luigi1, periodNumber = 1)
        org.junit.Assert.assertTrue(bounds.hasNext)
        assertEquals(2, bounds.nextPeriodNumber)
        assertEquals(13, bounds.nextStartHour)
        assertEquals(0, bounds.nextStartMinute)
        assertEquals(315, bounds.gapAfterMinutes)

        // Spostamento di +6 ore (+360 min): deve bloccarsi al gap massimo di 315 min (fine a 13:00)
        val shiftedPlus6h = PlanShiftEngine.shiftPeriod(luigi1, periodNumber = 1, deltaHours = 6, deltaMinutes = 0)
        val p1 = shiftedPlus6h.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals(19, p1.startHour) // 14:00 + 5h15m = 19:15
        assertEquals(15, p1.startMinute)
        assertEquals(13, p1.endHour)   // 07:45 + 5h15m = 13:00 (bloccato all'inizio di P2)
        assertEquals(0, p1.endMinute)
    }

    // --- Helper methods per i test a1..a2 e b1..b8 ---

    private fun sp(day: Int, h: Int, m: Int, dur: Int) = Span(day * 1440 + h * 60 + m, day * 1440 + h * 60 + m + dur)

    private fun createTestPlan(id: String, name: String, spans: List<Span>): FastingPlan {
        val periods = spans.mapIndexed { i, s -> s.toPeriod(i + 1, "p${i + 1}") }
        val dummyDays = (1..7).map { d ->
            DayPlan(
                dayOfWeek = d,
                isFastingDay = true,
                startHour = 20,
                startMinute = 0,
                endHour = 12,
                endMinute = 0,
                fastingDurationMinutes = 960
            )
        }
        return FastingPlan(
            id = id,
            name = name,
            days = dummyDays,
            isCustom = true,
            periods = periods
        )
    }

    private fun createPianoA(): FastingPlan {
        val spans = listOf(
            sp(0, 0, 0, 780),    // P1: Lun 00:00 -> 13:00
            sp(0, 14, 0, 1200),  // P2: Lun 14:00 -> Mar 10:00
            sp(1, 11, 0, 1200),  // P3: Mar 11:00 -> Mer 07:00
            sp(2, 12, 0, 1320),  // P4: Mer 12:00 -> Gio 10:00
            sp(3, 12, 0, 1380),  // P5: Gio 12:00 -> Ven 11:00
            sp(4, 12, 0, 1200),  // P6: Ven 12:00 -> Sab 08:00
            sp(5, 21, 0, 780),   // P7: Sab 21:00 -> Dom 10:00
            sp(6, 16, 0, 1140)   // P8: Dom 16:00 -> Lun 11:00
        )
        return createTestPlan("piano-a", "Piano A", spans)
    }

    private fun createPianoB(): FastingPlan {
        val spans = listOf(
            sp(0, 0, 0, 780),    // P1: Lun 00:00 -> 13:00
            sp(0, 14, 0, 1065),  // P2: Lun 14:00 -> Mar 07:45
            sp(1, 13, 0, 1080),  // P3: Mar 13:00 -> Mer 07:00
            sp(2, 12, 0, 1140),  // P4: Mer 12:00 -> Gio 07:00
            sp(3, 13, 0, 1125),  // P5: Gio 13:00 -> Ven 07:45
            sp(4, 13, 0, 1140),  // P6: Ven 13:00 -> Sab 08:00
            sp(5, 21, 0, 600),   // P7: Sab 21:00 -> Dom 07:00
            sp(6, 16, 0, 960)    // P8: Dom 16:00 -> Lun 08:00
        )
        return createTestPlan("piano-b", "Piano B", spans)
    }

    @Test
    fun testPianoA_repairPlan() {
        // a1) repairPlan: P8 (Dom 16:00 -> Lun 11:00) e P1 (Lun 00:00 -> 13:00) si fondono circolarmente in P7 (Dom 16:00 -> Lun 13:00, 21h).
        val pianoA = createPianoA()
        val repaired = PlanShiftEngine.repairPlan(pianoA)
        val periods = repaired.getEffectivePeriods()

        assertEquals(7, periods.size)
        // P7 è il periodo unificato circolare Dom 16:00 -> Lun 13:00 (1260m = 21h)
        val p7 = periods[6]
        assertEquals(Span(9600, 10860), p7.toSpan())
        assertEquals(7, p7.startDayOfWeek)
        assertEquals(16, p7.startHour)
        assertEquals(0, p7.startMinute)
        assertEquals(1, p7.endDayOfWeek)
        assertEquals(13, p7.endHour)
        assertEquals(0, p7.endMinute)
        assertEquals(1260, p7.fastingDurationMinutes)

        // P1 è Lun 14:00 -> Mar 10:00 (1200m)
        val p1 = periods[0]
        assertEquals(Span(840, 2040), p1.toSpan())
        assertEquals(1, p1.startDayOfWeek)
        assertEquals(14, p1.startHour)
        assertEquals(1200, p1.fastingDurationMinutes)
    }

    @Test
    fun testPianoA_repairPlanIdempotent() {
        // a2) repairPlan applicata due volte dà lo stesso risultato (idempotente).
        val pianoA = createPianoA()
        val repairedOnce = PlanShiftEngine.repairPlan(pianoA)
        val repairedTwice = PlanShiftEngine.repairPlan(repairedOnce)
        assertEquals(repairedOnce.getEffectivePeriods().map { it.toSpan() }, repairedTwice.getEffectivePeriods().map { it.toSpan() })
        assertEquals(repairedOnce, repairedTwice)
    }

    @Test
    fun testPianoB_repairPlan() {
        // b1) repairPlan: P8 (Dom 16:00 -> Lun 08:00) e P1 (Lun 00:00 -> 13:00) si uniscono circolarmente in P7 (Dom 16:00 -> Lun 13:00, 21h = 1260m).
        val pianoB = createPianoB()
        val repaired = PlanShiftEngine.repairPlan(pianoB)
        val periods = repaired.getEffectivePeriods()

        assertEquals(7, periods.size)
        val p7 = periods[6]
        assertEquals(Span(9600, 10860), p7.toSpan())
        assertEquals(7, p7.startDayOfWeek)
        assertEquals(16, p7.startHour)
        assertEquals(0, p7.startMinute)
        assertEquals(1, p7.endDayOfWeek)
        assertEquals(13, p7.endHour)
        assertEquals(0, p7.endMinute)
        assertEquals(1260, p7.fastingDurationMinutes)

        // P1 è Lun 14:00 -> Mar 07:45 (1065m = 17h 45m)
        val p1 = periods[0]
        assertEquals(Span(840, 1905), p1.toSpan())
        assertEquals(1, p1.startDayOfWeek)
        assertEquals(14, p1.startHour)
        assertEquals(0, p1.startMinute)
        assertEquals(2, p1.endDayOfWeek)
        assertEquals(7, p1.endHour)
        assertEquals(45, p1.endMinute)
        assertEquals(1065, p1.fastingDurationMinutes)
    }

    @Test
    fun testPianoB_p7FineChoices() {
        // b2) Dopo repair, P7 (Dom 16:00 -> Lun 13:00) Fine scelta 06:00: nuova fine Lun 06:00, durata 840 min, checkEndTimeClamped = false.
        //     Fine scelta 15:00: limitata a Lun 14:00 (inizio P1) e checkEndTimeClamped = true.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())

        // Scelta 06:00
        val clamped06 = PlanShiftEngine.checkEndTimeClamped(repaired, periodNumber = 7, hour = 6, minute = 0)
        assertFalse("Fine 06:00 non deve essere clamped", clamped06)
        val updated06 = PlanShiftEngine.updatePeriodTime(repaired, periodNumber = 7, newHour = 6, newMinute = 0, isStart = false)
        val p7After06 = updated06.getEffectivePeriods().first { it.periodNumber == 7 }
        assertEquals(1, p7After06.endDayOfWeek) // Lunedì
        assertEquals(6, p7After06.endHour)
        assertEquals(0, p7After06.endMinute)
        assertEquals(840, p7After06.fastingDurationMinutes)

        // Scelta 15:00 (oltre il limite di Lun 14:00 dove inizia P1)
        val clamped15 = PlanShiftEngine.checkEndTimeClamped(repaired, periodNumber = 7, hour = 15, minute = 0)
        assertTrue("Fine 15:00 deve risultare clamped", clamped15)
        val updated15 = PlanShiftEngine.updatePeriodTime(repaired, periodNumber = 7, newHour = 15, newMinute = 0, isStart = false)
        val p7After15 = updated15.getEffectivePeriods().first { it.periodNumber == 7 }
        assertEquals(1, p7After15.endDayOfWeek) // Lunedì
        assertEquals(14, p7After15.endHour)     // Limitata a Lun 14:00
        assertEquals(0, p7After15.endMinute)
        assertEquals(1320, p7After15.fastingDurationMinutes)
    }

    @Test
    fun testPianoB_p7InizioChoice() {
        // b3) Dopo repair, P7 (Dom 16:00 -> Lun 13:00) Inizio scelto 06:00: limitato a Dom 07:00 (fine P6) e checkStartTimeClamped = true.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())

        val clamped06 = PlanShiftEngine.checkStartTimeClamped(repaired, periodNumber = 7, hour = 6, minute = 0)
        assertTrue("Inizio 06:00 deve risultare clamped a Dom 07:00", clamped06)

        val updated06 = PlanShiftEngine.updatePeriodTime(repaired, periodNumber = 7, newHour = 6, newMinute = 0, isStart = true)
        val p7 = updated06.getEffectivePeriods().first { it.periodNumber == 7 }
        assertEquals(7, p7.startDayOfWeek)
        assertEquals(7, p7.startHour)   // Limitato a Dom 07:00
        assertEquals(0, p7.startMinute)
    }

    @Test
    fun testPianoB_p1FineChoice() {
        // b4) P1 Fine scelta 06:00: nuova fine Mar 06:00, durata 960 min.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())

        val clamped06 = PlanShiftEngine.checkEndTimeClamped(repaired, periodNumber = 1, hour = 6, minute = 0)
        assertFalse("Fine 06:00 per P1 non deve essere clamped", clamped06)

        val updated06 = PlanShiftEngine.updatePeriodTime(repaired, periodNumber = 1, newHour = 6, newMinute = 0, isStart = false)
        val p1 = updated06.getEffectivePeriods().first { it.periodNumber == 1 }
        assertEquals(2, p1.endDayOfWeek) // Martedì
        assertEquals(6, p1.endHour)
        assertEquals(0, p1.endMinute)
        assertEquals(960, p1.fastingDurationMinutes) // 14:00 -> 06:00 = 16h = 960 min
    }

    @Test
    fun testPianoB_stepperBoundsFlags() {
        // b5) Dopo repair, P7 ha 60 min gap fino a P1 (13:00 -> 14:00); canShiftPlus15m = true.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())

        val boundsP7 = PlanShiftEngine.getPeriodBounds(repaired, periodNumber = 7)
        assertEquals(60, boundsP7.gapAfterMinutes)
        assertTrue("P7 canShiftPlus15m deve essere true con 60 min gap", boundsP7.canShiftPlus15m)

        val boundsP1 = PlanShiftEngine.getPeriodBounds(repaired, periodNumber = 1)
        assertEquals(60, boundsP1.gapBeforeMinutes)
        assertTrue("P1 canShiftMinus15m deve essere true con 60 min gap", boundsP1.canShiftMinus15m)
    }

    @Test
    fun testPianoB_shiftAllDaysPlus8h() {
        // b6) shiftAllDays(+8h) dopo repair: durate invariate per tutti i 7 periodi;
        // per ogni periodNumber gapBeforeMinutes e gapAfterMinutes di getPeriodBounds restano uguali a prima dello spostamento.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())
        val shifted = PlanShiftEngine.shiftAllDays(repaired, deltaHours = 8)

        val origPeriods = repaired.getEffectivePeriods()
        val shiftedPeriods = shifted.getEffectivePeriods()
        assertEquals(7, shiftedPeriods.size)

        // Durate invariate (il set delle durate si conserva, anche se l'ordine cronologico settimanale ruota)
        assertEquals(
            origPeriods.map { it.fastingDurationMinutes }.sorted(),
            shiftedPeriods.map { it.fastingDurationMinutes }.sorted()
        )

        // Il periodo da 21h (1260 min) ora inizia a Lunedì 00:00 (16:00 Dom + 8h = 24:00 = Lun 00:00)
        val p21h = shiftedPeriods.first { it.fastingDurationMinutes == 1260 }
        assertEquals(1, p21h.startDayOfWeek)
        assertEquals(0, p21h.startHour)
        assertEquals(0, p21h.startMinute)

        // I gap ciclici tra periodi restano invariati
        val gapsBeforeOrig = (1..7).map { PlanShiftEngine.getPeriodBounds(repaired, it).gapBeforeMinutes }.sorted()
        val gapsBeforeShifted = (1..7).map { PlanShiftEngine.getPeriodBounds(shifted, it).gapBeforeMinutes }.sorted()
        assertEquals(gapsBeforeOrig, gapsBeforeShifted)
    }

    @Test
    fun testPianoB_shiftAllDaysPlus11h() {
        // b7) shiftAllDays(+11h): P6 (Sab 21:00) + 11h diventa Dom 08:00 e P7 (Dom 16:00) + 11h diventa Lun 03:00; nessun giorno 0 o 8, nessun valore > 7.
        val repaired = PlanShiftEngine.repairPlan(createPianoB())
        val shifted = PlanShiftEngine.shiftAllDays(repaired, deltaHours = 11)

        val periods = shifted.getEffectivePeriods()
        assertEquals(7, periods.size)

        // Il periodo da 600 min (ex Sab 21:00) + 11h inizia Dom 08:00
        val pDom = periods.first { it.fastingDurationMinutes == 600 }
        assertEquals(7, pDom.startDayOfWeek) // Domenica
        assertEquals(8, pDom.startHour)
        assertEquals(0, pDom.startMinute)

        // Il periodo da 1260 min (ex Dom 16:00) + 11h inizia Lun 03:00
        val pLun = periods.first { it.fastingDurationMinutes == 1260 }
        assertEquals(1, pLun.startDayOfWeek) // Lunedì
        assertEquals(3, pLun.startHour)
        assertEquals(0, pLun.startMinute)

        // Nessun giorno 0 o 8, nessun valore > 7 né in periods né in days
        periods.forEach { p ->
            assertTrue("startDayOfWeek in 1..7 per P${p.periodNumber}", p.startDayOfWeek in 1..7)
            assertTrue("endDayOfWeek in 1..7 per P${p.periodNumber}", p.endDayOfWeek in 1..7)
        }
        shifted.days.forEach { d ->
            assertTrue("dayOfWeek in 1..7 per giorno ${d.dayOfWeek}", d.dayOfWeek in 1..7)
        }
    }

    @Test
    fun testPianoB_tightIntervals60Minutes() {
        // b8) Nessuna eccezione con periodi di 60 minuti e intervalli stretti (updatePeriodTime su inizio e fine).
        val tightSpans = listOf(
            sp(0, 8, 0, 60),   // Lun 08:00 -> 09:00 (60 min)
            sp(0, 9, 0, 60),   // Lun 09:00 -> 10:00 (60 min)
            sp(0, 10, 0, 60)   // Lun 10:00 -> 11:00 (60 min)
        )
        val tightPlan = createTestPlan("tight", "Tight Plan", tightSpans)

        // Update su inizio e fine senza eccezioni (es. IllegalArgumentException su coerceIn invertiti)
        val updatedStart = PlanShiftEngine.updatePeriodTime(tightPlan, periodNumber = 2, newHour = 8, newMinute = 0, isStart = true)
        val p2Start = updatedStart.getEffectivePeriods().first { it.periodNumber == 2 }
        assertTrue("Durata minima rispettata", p2Start.fastingDurationMinutes >= 60)

        val updatedEnd = PlanShiftEngine.updatePeriodTime(tightPlan, periodNumber = 2, newHour = 10, newMinute = 30, isStart = false)
        val p2End = updatedEnd.getEffectivePeriods().first { it.periodNumber == 2 }
        assertTrue("Durata minima rispettata", p2End.fastingDurationMinutes >= 60)
    }
}
