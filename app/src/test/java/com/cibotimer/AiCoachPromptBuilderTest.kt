package com.cibotimer

import com.cibotimer.domain.FastingPlanTemplates
import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState
import com.cibotimer.data.models.Trophy
import com.cibotimer.data.models.UserProfile
import com.cibotimer.data.models.WeightEntry
import com.cibotimer.data.models.WeightUnit
import com.cibotimer.domain.AiCoachPromptBuilder
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AiCoachPromptBuilderTest {

    @Test
    fun testPromptIncludesStartingWeightAndProgressDetails() {
        val profile = UserProfile(
            name = "Luigi",
            startingWeightKg = 85.0,
            currentWeightKg = 80.0,
            targetWeightKg = 72.0,
            heightCm = 180.0,
            weightUnit = WeightUnit.KG,
            waterTargetMl = 2500
        )
        val plan = FastingPlanTemplates.PLAN_16_8

        val prompt = AiCoachPromptBuilder.buildPrompt(
            basePrompt = "Base AI Prompt",
            profile = profile,
            activePlan = plan,
            currentSession = null,
            weightEntries = emptyList(),
            todayWaterMl = 1500,
            sessionsHistory = emptyList(),
            trophies = emptyList(),
            context = null,
            locale = Locale.ITALY
        )

        // Verifica che il peso iniziale, attuale e target siano chiaramente esposti
        assertTrue("Il prompt deve contenere il PESO INIZIALE di partenza", prompt.contains("PESO INIZIALE (Partenza): 85.0 kg") || prompt.contains("85,0 kg") || prompt.contains("85.0 kg"))
        assertTrue("Il prompt deve contenere il PESO ATTUALE", prompt.contains("PESO ATTUALE: 80.0 kg") || prompt.contains("80,0 kg") || prompt.contains("80.0 kg"))
        assertTrue("Il prompt deve contenere il PESO OBIETTIVO", prompt.contains("PESO OBIETTIVO (Target): 72.0 kg") || prompt.contains("72,0 kg") || prompt.contains("72.0 kg"))

        // Verifica calcolo perdita peso dal peso di partenza
        assertTrue("Il prompt deve indicare i kg persi dal peso iniziale", prompt.contains("-5.0 kg") || prompt.contains("-5,0 kg") || prompt.contains("5.0 kg persi") || prompt.contains("5,0 kg persi"))
        assertTrue("Il prompt deve indicare i kg mancanti al target", prompt.contains("8.0 kg") || prompt.contains("8,0 kg"))

        // Verifica calcoli BMI iniziale, attuale e target
        assertTrue("Il prompt deve calcolare il BMI Iniziale", prompt.contains("BMI Iniziale (alla partenza): 26.2"))
        assertTrue("Il prompt deve calcolare il BMI Attuale", prompt.contains("BMI Attuale: 24.7"))
        assertTrue("Il prompt deve calcolare il BMI Obiettivo", prompt.contains("BMI Obiettivo: 22.2"))

        // Verifica idratazione
        assertTrue("Il prompt deve includere l'idratazione odierna", prompt.contains("1500 ml / 2500 ml") && prompt.contains("60%"))
    }

    @Test
    fun testPromptIncludesWeightHistoryAndMeasurements() {
        val profile = UserProfile(
            name = "Marco",
            startingWeightKg = 90.0,
            currentWeightKg = 87.0,
            targetWeightKg = 80.0,
            heightCm = 175.0
        )
        val now = 1700000000000L
        val weightEntries = listOf(
            WeightEntry(
                id = "w-1",
                epochMillis = now,
                weightKg = 86.5,
                waistCm = 88.0,
                hipsCm = 100.0,
                notes = "Dopo allenamento mattutino"
            ),
            WeightEntry(
                id = "w-2",
                epochMillis = now - 86400000L * 7,
                weightKg = 88.0,
                waistCm = 91.0,
                hipsCm = 102.0
            )
        )

        val prompt = AiCoachPromptBuilder.buildPrompt(
            basePrompt = "Base Prompt",
            profile = profile,
            activePlan = FastingPlanTemplates.PLAN_16_8,
            currentSession = null,
            weightEntries = weightEntries,
            todayWaterMl = 2000,
            sessionsHistory = emptyList(),
            trophies = emptyList(),
            context = null,
            locale = Locale.ITALY
        )

        // Il peso attuale effettivo deve riflettere l'ultima pesata registrata
        assertTrue("Il peso attuale deve riflettere l'ultima pesata registrata", prompt.contains("86.5 kg") || prompt.contains("86,5 kg"))
        // Deve contenere la nota
        assertTrue("Il prompt deve includere le note della pesata", prompt.contains("Dopo allenamento mattutino"))
        // Deve contenere il delta vita (-3 cm)
        assertTrue("Il prompt deve calcolare la variazione della vita", prompt.contains("Vita: 88.0 cm") && prompt.contains("-3.0 cm"))
        // Deve calcolare il WHR
        assertTrue("Il prompt deve calcolare il WHR", prompt.contains("WHR") && prompt.contains("0.88"))
    }

    @Test
    fun testPromptIncludesActiveSessionAndMetabolicStage() {
        val profile = UserProfile()
        val now = System.currentTimeMillis()
        val fourteenHoursAgo = now - (14 * 60 * 60 * 1000L)
        val plannedEnd = now + (2 * 60 * 60 * 1000L)

        val activeSession = FastingSession(
            id = "sess-1",
            planId = FastingPlanTemplates.PLAN_16_8.id,
            planName = FastingPlanTemplates.PLAN_16_8.name,
            startTimeEpochMillis = fourteenHoursAgo,
            plannedEndTimeEpochMillis = plannedEnd,
            state = FastingState.FASTING,
            targetDurationMinutes = 960
        )

        val prompt = AiCoachPromptBuilder.buildPrompt(
            basePrompt = "Base AI",
            profile = profile,
            activePlan = FastingPlanTemplates.PLAN_16_8,
            currentSession = activeSession,
            weightEntries = emptyList(),
            todayWaterMl = 500,
            sessionsHistory = emptyList(),
            trophies = emptyList(),
            context = null,
            locale = Locale.ITALY
        )

        assertTrue("Il prompt deve indicare che il DIGIUNO È ATTIVO", prompt.contains("DIGIUNO ATTIVO"))
        assertTrue("Il prompt deve riportare 14 ore trascorse", prompt.contains("14h") || prompt.contains("14 ore"))
        assertTrue("Il prompt deve identificare la Fase Metabolica (Chetosi o Combustione grassi)", prompt.contains("Fase") && prompt.contains("Chetosi"))
    }

    @Test
    fun testPromptMandatesTotalUserKnowledgeInOperationalRules() {
        val profile = UserProfile(startingWeightKg = 82.0, currentWeightKg = 77.0, targetWeightKg = 70.0)
        val prompt = AiCoachPromptBuilder.buildPrompt(
            basePrompt = "Base AI",
            profile = profile,
            activePlan = FastingPlanTemplates.PLAN_16_8,
            currentSession = null,
            weightEntries = emptyList(),
            todayWaterMl = 1000,
            sessionsHistory = emptyList(),
            trophies = listOf(Trophy("t-1", "Primo Digiuno", "Desc", "icon", 1, 1, isUnlocked = true)),
            context = null,
            locale = Locale.ITALY
        )

        assertTrue("Le istruzioni devono contenere CONOSCENZA TOTALE DEL PERCORSO", prompt.contains("CONOSCENZA TOTALE DEL PERCORSO DELL'UTENTE"))
        assertTrue("Le istruzioni devono imporre di citare il PESO INIZIALE", prompt.contains("PESO INIZIALE di partenza"))
        assertTrue("Le istruzioni devono mantenere la REGOLA MANDATORIA PRIMA DI CREARE UN PIANO", prompt.contains("CHIEDI SEMPRE L'ORARIO DI INIZIO DIGIUNO"))
        assertTrue("Il prompt deve includere i trofei sbloccati", prompt.contains("Primo Digiuno"))
    }
}
