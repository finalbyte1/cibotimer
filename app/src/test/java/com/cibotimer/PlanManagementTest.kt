package com.cibotimer

import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanCategory
import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.domain.PlanShiftEngine
import com.cibotimer.domain.WeekSpanModel
import com.cibotimer.domain.toSpan
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PlanManagementTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testPlanSerializationWithCustomName() {
        val basePlan = FastingPlanTemplates.PLAN_16_8
        val customName = "Mio Digiuno Personalizzato"
        val customPlan = basePlan.copy(
            id = "custom-${UUID.randomUUID().toString().take(8)}",
            name = customName,
            category = PlanCategory.CUSTOM,
            isCustom = true
        )

        val encoded = json.encodeToString(FastingPlan.serializer(), customPlan)
        val decoded = json.decodeFromString<FastingPlan>(encoded)

        assertEquals(customName, decoded.name)
        assertEquals(PlanCategory.CUSTOM, decoded.category)
        assertTrue(decoded.isCustom)
        assertEquals(7, decoded.days.size)
    }

    @Test
    fun testPlanDeletionFromList() {
        val planA = FastingPlanTemplates.PLAN_16_8.copy(id = "custom-1", name = "Piano 1", isCustom = true)
        val planB = FastingPlanTemplates.PLAN_18_6.copy(id = "custom-2", name = "Piano 2", isCustom = true)
        val planC = FastingPlanTemplates.PLAN_20_4.copy(id = "custom-3", name = "Piano 3", isCustom = true)

        var customPlans = listOf(planA, planB, planC)
        var activePlan: FastingPlan = planB

        // Elimina planB (che è quello attivo)
        val toDeleteId = "custom-2"
        customPlans = customPlans.filter { it.id != toDeleteId }

        if (activePlan.id == toDeleteId) {
            activePlan = FastingPlanTemplates.PLAN_16_8
        }

        assertEquals(2, customPlans.size)
        assertFalse(customPlans.any { it.id == "custom-2" })
        assertEquals(FastingPlanTemplates.PLAN_16_8.id, activePlan.id)
    }

    @Test
    fun testLuigi1PlanPeriodsIntegrity() {
        val luigiPlan = FastingPlanTemplates.PLAN_LUIGI_1
        assertEquals("Luigi 1", luigiPlan.name)
        assertNotNull(luigiPlan.periods)
        assertEquals(7, luigiPlan.periods?.size)

        val periods = luigiPlan.getEffectivePeriods()
        assertEquals(7, periods.size)
        assertEquals("p1", periods[0].id)
        assertEquals(1, periods[0].periodNumber)
        assertEquals(14, periods[0].startHour)
        assertEquals(7, periods[0].endHour)
        assertEquals(45, periods[0].endMinute)
        assertEquals(1065, periods[0].fastingDurationMinutes)

        // P7: Dom 16:00 -> Lun 13:00 (21h = 1260m)
        val p7 = periods[6]
        assertEquals("p7", p7.id)
        assertEquals(7, p7.periodNumber)
        assertEquals(7, p7.startDayOfWeek)
        assertEquals(16, p7.startHour)
        assertEquals(0, p7.startMinute)
        assertEquals(1, p7.endDayOfWeek)
        assertEquals(13, p7.endHour)
        assertEquals(0, p7.endMinute)
        assertEquals(1260, p7.fastingDurationMinutes)
    }

    @Test
    fun testSavePlanWithoutActivating() {
        val currentActive = FastingPlanTemplates.PLAN_16_8
        val proposed = FastingPlanTemplates.PLAN_18_6.copy(id = "custom-future", name = "Piano per il futuro", isCustom = true)

        var activePlan = currentActive
        var customPlans = emptyList<FastingPlan>()

        val activate = false
        customPlans = customPlans.filter { it.id != proposed.id } + proposed
        if (activate) {
            activePlan = proposed
        }

        // Il piano proposto deve essere nei customPlans
        assertEquals(1, customPlans.size)
        assertEquals("Piano per il futuro", customPlans[0].name)
        // Ma il piano attivo NON deve essere cambiato!
        assertEquals("16-8", activePlan.id)
    }

    @Test
    fun testSaveAsCreatesNewCustomPlanWithUniqueIdAndName() {
        val originalPlan = FastingPlanTemplates.PLAN_16_8
        val modifiedWorkingPlan = com.cibotimer.domain.PlanShiftEngine.shiftDay(originalPlan, dayOfWeek = 1, deltaHours = 2)

        val newCustomName = "16-8 Lavoro Turni"
        val newUniqueId = "custom-${UUID.randomUUID().toString().take(8)}"

        val saveAsPlan = modifiedWorkingPlan.copy(
            id = newUniqueId,
            name = newCustomName,
            description = "Piano modificato a mano con orari per turnisti",
            category = PlanCategory.CUSTOM,
            isCustom = true
        )

        assertNotEquals(originalPlan.id, saveAsPlan.id)
        assertEquals("16-8 Lavoro Turni", saveAsPlan.name)
        assertTrue(saveAsPlan.isCustom)
        assertEquals(PlanCategory.CUSTOM, saveAsPlan.category)

        // Verifica orari modificati rispetto all'originale
        val originalMon = originalPlan.days.first { it.dayOfWeek == 1 }
        val saveAsMon = saveAsPlan.days.first { it.dayOfWeek == 1 }
        assertEquals(20, originalMon.startHour)
        assertEquals(22, saveAsMon.startHour)

        // Martedì invariato (non-cascading)
        val originalTue = originalPlan.days.first { it.dayOfWeek == 2 }
        val saveAsTue = saveAsPlan.days.first { it.dayOfWeek == 2 }
        assertEquals(originalTue.startHour, saveAsTue.startHour)
    }

    @Test
    fun testSaveActivePlanAlwaysActivatesWhenPlanIsActive() {
        val currentActive = FastingPlanTemplates.PLAN_16_8
        var activePlan = currentActive
        var customPlans = emptyList<FastingPlan>()

        // Modifica orari di un piano attivo (es. 16-8)
        val modifiedWorkingPlan = com.cibotimer.domain.PlanShiftEngine.shiftDay(currentActive, dayOfWeek = 1, deltaHours = 1)
        val newCustomName = "16-8 Personalizzato"
        val newUniqueId = "custom-test1234"

        val newCustomPlan = modifiedWorkingPlan.copy(
            id = newUniqueId,
            name = newCustomName,
            category = PlanCategory.CUSTOM,
            isCustom = true
        )

        val isActive = true
        // Anche se un callback ricevesse activate = false, essendo il piano di partenza attivo, DEVE attivarsi!
        val activateArg = false
        val shouldActivate = if (isActive || activateArg) true else activateArg

        customPlans = customPlans.filter { it.id != newCustomPlan.id } + newCustomPlan
        if (shouldActivate) {
            activePlan = newCustomPlan
        }

        assertEquals(1, customPlans.size)
        assertEquals(newUniqueId, activePlan.id)
        assertEquals("16-8 Personalizzato", activePlan.name)
        assertTrue(activePlan.isCustom)
        assertEquals(21, activePlan.days.first { it.dayOfWeek == 1 }.startHour)
    }

    @Test
    fun testUpdateCustomActivePlanInPlace() {
        val originalCustom = FastingPlanTemplates.PLAN_16_8.copy(
            id = "custom-myplan",
            name = "Il Mio Digiuno",
            category = PlanCategory.CUSTOM,
            isCustom = true
        )
        var activePlan = originalCustom
        var customPlans = listOf(originalCustom)

        // Aggiorna sul posto (Salva modifiche)
        val modifiedPlan = com.cibotimer.domain.PlanShiftEngine.shiftDay(originalCustom, dayOfWeek = 2, deltaHours = 2)
        val updatedPlan = modifiedPlan.copy(
            id = originalCustom.id,
            name = "Il Mio Digiuno Rinnovato",
            category = PlanCategory.CUSTOM,
            isCustom = true
        )

        val exists = customPlans.any { it.id == updatedPlan.id }
        customPlans = if (exists) {
            customPlans.map { if (it.id == updatedPlan.id) updatedPlan else it }
        } else {
            customPlans + updatedPlan
        }
        activePlan = updatedPlan

        assertEquals(1, customPlans.size)
        assertEquals("custom-myplan", activePlan.id)
        assertEquals("Il Mio Digiuno Rinnovato", activePlan.name)
        assertEquals(22, activePlan.days.first { it.dayOfWeek == 2 }.startHour)
    }

    @Test
    fun testPlanLuigi1DaySelectionMapping() {
        val luigiPlan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = luigiPlan.getEffectivePeriods()
        assertEquals(7, periods.size)

        // Test tap su Martedì (dayIndex = 2) -> Periodo 2
        val dayIndexMar = 2
        val dayPeriodsMar = periods.filter { it.startDayOfWeek == dayIndexMar }
        assertEquals(1, dayPeriodsMar.size)
        val selectedPeriod = dayPeriodsMar.first()
        assertEquals(2, selectedPeriod.periodNumber) // Periodo 2
        assertEquals(2, selectedPeriod.startDayOfWeek)

        // Test tap su Lunedì (dayIndex = 1): ha il periodo che inizia Lunedì (p1)
        val dayIndexLun = 1
        val dayPeriodsLun = periods.filter { it.startDayOfWeek == dayIndexLun }
        assertEquals(1, dayPeriodsLun.size)
        assertEquals(1, dayPeriodsLun[0].periodNumber)

        // Test periodo Domenica (p7: Dom 16:00 -> Lun 13:00, 21h, endDayOfWeek = 1)
        val p7 = periods.first { it.periodNumber == 7 }
        assertEquals(7, p7.startDayOfWeek) // Dom
        assertEquals(1, p7.endDayOfWeek)   // Lunedì (1)
        assertEquals(1260, p7.fastingDurationMinutes)
    }

    @Test
    fun testUnifiedWeekSpanModelCases() {
        val userPlan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = userPlan.getEffectivePeriods()
        assertEquals(7, periods.size)

        // Caso (1): Spans di Lunedì (d = 0) -> solo 1 periodo inizia Lunedì (p1: 14:00 -> Mar 07:45)
        val spans = periods.map { it.toSpan() }
        val startingMon = spans.filter { ((it.start / 1440) % 7 + 7) % 7 == 0 }
        assertEquals(1, startingMon.size)
        assertEquals(840, startingMon[0].start)
        assertEquals(1905, startingMon[0].end)
        assertEquals(1065, startingMon[0].durationMinutes) // 17h 45m

        // Caso (2): Fasts in Lunedì (d = 0) considera la continuazione da Domenica (00:00 - 13:00) e il nuovo digiuno (14:00 - 24:00)
        val fastsMon = WeekSpanModel.fastsInDay(spans, 0)
        assertEquals(2, fastsMon.size)
        assertEquals(0, fastsMon[0].first)   // 00:00 da Dom p7
        assertEquals(780, fastsMon[0].second) // 13:00 (fine digiuno domenicale)
        assertEquals(840, fastsMon[1].first)  // 14:00 (inizio digiuno Lun p1)
        assertEquals(1440, fastsMon[1].second) // 24:00 (continua in Mar)

        // Caso (3): Dom (d = 6) ha il pasto 07:45->16:00 e il digiuno da 16:00 a 24:00 verso Lunedì
        val fastsSun = WeekSpanModel.fastsInDay(spans, 6)
        assertEquals(2, fastsSun.size)
        assertEquals(0, fastsSun[0].first)
        assertEquals(465, fastsSun[0].second) // 07:45 da Sab p6
        assertEquals(960, fastsSun[1].first)  // 16:00 da Dom p7
        assertEquals(1440, fastsSun[1].second) // 24:00

        // Caso (4): Limiti del Periodo 7 (Dom 16:00 -> Lun 13:00)
        // Verso prev (Sab 21:00 -> Dom 07:45): fine pasto Dom 07:45
        val b7 = WeekSpanModel.bounds(spans, 6)
        assertEquals(7, b7.prevEndDayOfWeek) // Domenica!
        assertEquals(7, b7.prevEndHour)       // 07:45!
        assertEquals(45, b7.prevEndMinute)
        // Verso next (Lun 14:00 -> Mar 07:45): inizio pasto Lun 14:00
        assertEquals(1, b7.nextStartDayOfWeek) // Lunedì!
        assertEquals(14, b7.nextStartHour)      // 14:00!
        assertEquals(0, b7.nextStartMinute)
    }

    @Test
    fun testCustomPlanCircularSundayMondayIntegrity() {
        // Criteri di verifica da Modifica.md
        val plan = FastingPlanTemplates.PLAN_LUIGI_1
        val periods = plan.getEffectivePeriods()

        // 1. Durate esatte dei 7 digiuni risultanti
        assertEquals("17h 45m", periods[0].durationHoursString) // Lun 14:00 -> Mar 07:45
        assertEquals("18h", periods[1].durationHoursString)      // Mar 13:00 -> Mer 07:00
        assertEquals("19h", periods[2].durationHoursString)      // Mer 12:00 -> Gio 07:00
        assertEquals("18h 45m", periods[3].durationHoursString) // Gio 13:00 -> Ven 07:45
        assertEquals("19h", periods[4].durationHoursString)      // Ven 13:00 -> Sab 08:00
        assertEquals("10h 45m", periods[5].durationHoursString) // Sab 21:00 -> Dom 07:45
        assertEquals("21h", periods[6].durationHoursString)      // Dom 16:00 -> Lun 13:00

        // 2. Modifica periodo 7 (Dom 16:00 -> Lun 13:00): resta unico periodo e la durata si aggiorna attraverso la mezzanotte
        // Spostamento di -1h: Dom 15:00 -> Lun 12:00 (durata 21h = 1260m)
        val shiftedMinus1h = PlanShiftEngine.shiftPeriod(plan, periodNumber = 7, deltaHours = -1)
        val p7Shifted = shiftedMinus1h.getEffectivePeriods().first { it.periodNumber == 7 }
        assertEquals(7, p7Shifted.startDayOfWeek)
        assertEquals(15, p7Shifted.startHour)
        assertEquals(1, p7Shifted.endDayOfWeek)
        assertEquals(12, p7Shifted.endHour)
        assertEquals(1260, p7Shifted.fastingDurationMinutes)
        assertEquals("21h", p7Shifted.durationHoursString)

        // Riduzione durata di 60 min: Dom 16:00 -> Lun 12:00 (durata 20h = 1200m)
        val adjustedMinus1h = PlanShiftEngine.adjustPeriodDuration(plan, periodNumber = 7, deltaMinutes = -60)
        val p7Adjusted = adjustedMinus1h.getEffectivePeriods().first { it.periodNumber == 7 }
        assertEquals(7, p7Adjusted.startDayOfWeek)
        assertEquals(16, p7Adjusted.startHour)
        assertEquals(1, p7Adjusted.endDayOfWeek)
        assertEquals(12, p7Adjusted.endHour)
        assertEquals(1200, p7Adjusted.fastingDurationMinutes)
        assertEquals("20h", p7Adjusted.durationHoursString)

        // 3. Serializzazione e salvataggio JSON: nessuna divisione del periodo a mezzanotte
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(FastingPlan.serializer(), plan)
        val deserialized = kotlinx.serialization.json.Json.decodeFromString(FastingPlan.serializer(), jsonStr)
        val desPeriods = deserialized.getEffectivePeriods()
        assertEquals(7, desPeriods.size)
        val desP7 = desPeriods[6]
        assertEquals(7, desP7.startDayOfWeek)
        assertEquals(16, desP7.startHour)
        assertEquals(1, desP7.endDayOfWeek)
        assertEquals(13, desP7.endHour)
        assertEquals(1260, desP7.fastingDurationMinutes)
        assertEquals("21h", desP7.durationHoursString)
    }

    @Test
    fun testLegacy8PeriodsAutomaticallyMergedTo7PeriodsCircular() {
        // Piano legacy salvato su telefono con 8 periodi (spezzato a mezzanotte tra Dom 16:00->24:00 e Lun 00:00->13:00)
        val legacy8Periods = listOf(
            FastingPeriod("p1", 1, 1, 0, 0, 1, 13, 0, 780),    // Lun 00:00 -> Lun 13:00 (13h)
            FastingPeriod("p2", 2, 1, 14, 0, 2, 7, 45, 1065),  // Lun 14:00 -> Mar 07:45 (17h 45m)
            FastingPeriod("p3", 3, 2, 13, 0, 3, 7, 0, 1080),   // Mar 13:00 -> Mer 07:00 (18h)
            FastingPeriod("p4", 4, 3, 12, 0, 4, 7, 0, 1140),   // Mer 12:00 -> Gio 07:00 (19h)
            FastingPeriod("p5", 5, 4, 13, 0, 5, 7, 45, 1125),  // Gio 13:00 -> Ven 07:45 (18h 45m)
            FastingPeriod("p6", 6, 5, 13, 0, 6, 8, 0, 1140),   // Ven 13:00 -> Sab 08:00 (19h)
            FastingPeriod("p7", 7, 6, 21, 0, 7, 7, 45, 645),   // Sab 21:00 -> Dom 07:45 (10h 45m)
            FastingPeriod("p8", 8, 7, 16, 0, 1, 0, 0, 480)     // Dom 16:00 -> Lun 00:00 (8h)
        )
        val legacyPlan = FastingPlan(
            id = "custom_screenshot_plan",
            name = "Piano Personalizzato Utente",
            isCustom = true,
            days = FastingPlanTemplates.PLAN_LUIGI_1.days,
            periods = legacy8Periods
        )

        // 1. getEffectivePeriods() unisce istantaneamente p8 e p1 in un unico periodo continuo da 21h
        val effective = legacyPlan.getEffectivePeriods()
        assertEquals(7, effective.size)

        // Periodo 1: Lun 14:00 -> Mar 07:45 (17h 45m = 1065m)
        assertEquals(1, effective[0].startDayOfWeek)
        assertEquals(14, effective[0].startHour)
        assertEquals(0, effective[0].startMinute)
        assertEquals(2, effective[0].endDayOfWeek)
        assertEquals(7, effective[0].endHour)
        assertEquals(45, effective[0].endMinute)
        assertEquals(1065, effective[0].fastingDurationMinutes)
        assertEquals("17h 45m", effective[0].durationHoursString)

        // Periodi intermedi 2..6 corrispondenti all'elenco DIGIUNI RISULTANTI
        assertEquals("18h", effective[1].durationHoursString)
        assertEquals("19h", effective[2].durationHoursString)
        assertEquals("18h 45m", effective[3].durationHoursString)
        assertEquals("19h", effective[4].durationHoursString)
        assertEquals("10h 45m", effective[5].durationHoursString)

        // Periodo 7: Dom 16:00 -> Lun 13:00 (21h = 1260m)
        assertEquals(7, effective[6].startDayOfWeek)
        assertEquals(16, effective[6].startHour)
        assertEquals(0, effective[6].startMinute)
        assertEquals(1, effective[6].endDayOfWeek)
        assertEquals(13, effective[6].endHour)
        assertEquals(0, effective[6].endMinute)
        assertEquals(1260, effective[6].fastingDurationMinutes)
        assertEquals("21h", effective[6].durationHoursString)

        // 2. repairPlan aggiorna permanentemente anche i giorni
        val repaired = PlanShiftEngine.repairPlan(legacyPlan)
        assertEquals(7, repaired.getEffectivePeriods().size)
        assertEquals(7, repaired.periods?.size)
    }
}
