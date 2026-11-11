package com.cibotimer

import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanCategory
import com.cibotimer.domain.FastingPlanTemplates
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
        assertEquals(9, luigiPlan.periods?.size)

        val periods = luigiPlan.getEffectivePeriods()
        assertEquals(9, periods.size)
        assertEquals("p1", periods[0].id)
        assertEquals(1, periods[0].periodNumber)
        assertEquals(0, periods[0].startHour)
        assertEquals(13, periods[0].endHour)
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
}
