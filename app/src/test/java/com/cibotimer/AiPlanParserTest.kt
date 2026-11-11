package com.cibotimer

import com.cibotimer.data.remote.OpenAiApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiPlanParserTest {

    private val client = OpenAiApiClient()

    @Test
    fun testExtractPlanFromJsonBlock() {
        val aiResponse = """
            Certamente! In base alle tue esigenze ho preparato un piano personalizzato per te:
            
            ```fasting_plan
            {
              "id": "ai-custom-plan-1",
              "name": "Piano Settimanale Turnista",
              "description": "Ottimizzato per chi lavora su turni pomeridiani.",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 21, "startMinute": 0, "endHour": 13, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 21, "startMinute": 0, "endHour": 13, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 21, "startMinute": 0, "endHour": 13, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 21, "startMinute": 0, "endHour": 13, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 21, "startMinute": 0, "endHour": 13, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 22, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 }
              ]
            }
            ```
            
            Puoi applicarlo toccando il pulsante sottostante!
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull("Plan should be successfully extracted from markdown code block", plan)
        assertEquals("ai-custom-plan-1", plan!!.id)
        assertEquals("Piano Settimanale Turnista", plan.name)
        assertEquals(7, plan.days.size)
        assertEquals(21, plan.days[0].startHour)
        assertEquals(13, plan.days[0].endHour)
        assertEquals(960, plan.days[0].fastingDurationMinutes)
    }

    @Test
    fun testExtractPlanWhenNoJsonBlockPresent() {
        val simpleText = "Bevi tanta acqua ed elettroliti durante la mattinata."
        val plan = client.extractPlanFromJsonBlock(simpleText)
        assertEquals(null, plan)
    }

    @Test
    fun testExtractPlanAutoHealsZeroFastingDurationFor18HoursPeak() {
        // Scenario segnalato dall'utente: "modifica il piano attuale, voglio al massimo 18 ore di picco per tutti i giorni"
        // L'AI genera gli orari (18:00 - 12:00) ma per errore scrive fastingDurationMinutes: 0
        val aiResponse = """
            Ecco il piano aggiornato con 18 ore di picco per tutti i giorni della settimana:
            
            ```fasting_plan
            {
              "id": "custom-18h-peak",
              "name": "Piano 18 Ore Picco",
              "description": "18 ore di digiuno al giorno dalle 18:00 alle 12:00.",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 0 }
              ]
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull(plan)
        assertEquals(7, plan!!.days.size)

        // Tutti i giorni devono essere stati automaticamente corretti a 1080 minuti (18 ore esatte)
        plan.days.forEachIndexed { index, day ->
            assertEquals("Giorno ${index + 1} deve avere dayOfWeek ${index + 1}", index + 1, day.dayOfWeek)
            assertEquals("Giorno ${index + 1} deve essere fastingDay", true, day.isFastingDay)
            assertEquals("Giorno ${index + 1} startHour", 18, day.startHour)
            assertEquals("Giorno ${index + 1} endHour", 12, day.endHour)
            assertEquals("Giorno ${index + 1} deve essere sanificato a 1080 minuti (18 ore)", 1080, day.fastingDurationMinutes)
            assertEquals("18h", day.durationHoursString)
        }
    }

    @Test
    fun testExtractPlanAutoHealsDurationInHoursInsteadOfMinutes() {
        // L'AI inserisce 18 al posto di 1080 minuti
        val aiResponse = """
            ```fasting_plan
            {
              "id": "custom-18h",
              "name": "Piano 18h",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 14, "endMinute": 0, "fastingDurationMinutes": 18 }
              ]
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull(plan)
        assertEquals(7, plan!!.days.size)
        plan.days.forEach { day ->
            assertEquals(1080, day.fastingDurationMinutes)
            assertEquals(14, day.endHour)
        }
    }

    @Test
    fun testExtractPlanGuaranteesAllSevenDaysMondayToSunday() {
        // L'AI omette i giorni del weekend nel JSON
        val aiResponse = """
            ```fasting_plan
            {
              "id": "custom-workweek",
              "name": "Piano Giorni Lavorativi",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 }
              ]
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull(plan)
        assertEquals("Deve sempre contenere esattamente 7 giorni (Lunedì a Domenica)", 7, plan!!.days.size)
        assertEquals(6, plan.days[5].dayOfWeek) // Sabato
        assertEquals(7, plan.days[6].dayOfWeek) // Domenica
    }

    @Test
    fun testExtractPlanAutoCorrectsEatingWindowInversion() {
        // L'utente chiede di mangiare dalle 12:00 alle 20:00 (digiuno 16:8).
        // L'AI inserisce erroneamente la finestra pasti (12 -> 20) in startHour/endHour, ma indica 960m (16h) di digiuno.
        val aiResponse = """
            Certamente! Ho impostato la tua finestra di pasto dalle 12:00 alle 20:00 e il digiuno per 16 ore:
            ```fasting_plan
            {
              "id": "custom-inversion-test",
              "name": "Piano 16:8 Pranzo-Cena",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 12, "startMinute": 0, "endHour": 20, "endMinute": 0, "fastingDurationMinutes": 960 }
              ]
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull(plan)
        // Deve rilevare che 12:00 -> 20:00 è la finestra pasti da 8h e invertire a digiuno 20:00 -> 12:00 (16h = 960m)
        plan!!.days.forEach { day ->
            assertEquals("Start hour deve essere l'inizio del digiuno alle 20:00", 20, day.startHour)
            assertEquals("End hour deve essere la fine del digiuno alle 12:00", 12, day.endHour)
            assertEquals("Durata digiuno deve essere 960 minuti (16h)", 960, day.fastingDurationMinutes)
            assertEquals("16h", day.durationHoursString)
        }
    }

    @Test
    fun testExtractPlanWithCommentsAndTrailingCommas() {
        // L'AI genera JSON con commenti e virgole finali
        val aiResponse = """
            ```json
            {
              "id": "comments-test",
              "name": "Piano con Commenti",
              // Commento di prova
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960, },
              ],
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull("Deve gestire correttamente commenti e virgole finali senza fallire", plan)
        assertEquals("Piano con Commenti", plan!!.name)
        assertEquals(7, plan.days.size)
        assertEquals(960, plan.days[0].fastingDurationMinutes)
    }

    @Test
    fun testExtractPlanWithoutMarkdownCodeFences() {
        // L'AI omette i backticks e fornisce il JSON grezzo nel testo
        val aiResponse = """
            Ecco la configurazione del piano nutrizionale:
            {
              "id": "raw-inline-json",
              "name": "Piano Inline",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 }
              ]
            }
            Fammi sapere se vuoi attivarlo!
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull("Deve estrarre il JSON anche senza i backticks di markdown", plan)
        assertEquals("Piano Inline", plan!!.name)
        assertEquals(7, plan.days.size)
    }

    @Test
    fun testExtractPlanWithRestDays() {
        // L'utente vuole digiunare dal lunedì al venerdì e avere riposo nel weekend
        val aiResponse = """
            ```fasting_plan
            {
              "id": "workdays-only",
              "name": "5 Giorni Digiuno + 2 Weekend Libero",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 20, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 960 },
                { "dayOfWeek": 6, "isFastingDay": false, "startHour": 0, "startMinute": 0, "endHour": 0, "endMinute": 0, "fastingDurationMinutes": 0 },
                { "dayOfWeek": 7, "isFastingDay": false, "startHour": 0, "startMinute": 0, "endHour": 0, "endMinute": 0, "fastingDurationMinutes": 0 }
              ]
            }
            ```
        """.trimIndent()

        val plan = client.extractPlanFromJsonBlock(aiResponse)
        assertNotNull(plan)
        assertEquals(7, plan!!.days.size)
        // Primi 5 giorni sono di digiuno
        for (i in 0..4) {
            assertEquals(true, plan.days[i].isFastingDay)
            assertEquals(960, plan.days[i].fastingDurationMinutes)
        }
        // Sabato e Domenica sono di riposo
        assertEquals(false, plan.days[5].isFastingDay)
        assertEquals(0, plan.days[5].fastingDurationMinutes)
        assertEquals(false, plan.days[6].isFastingDay)
        assertEquals(0, plan.days[6].fastingDurationMinutes)
    }

    @Test
    fun testAiPromptMandatesAskingFastingStartTime() {
        val config = com.cibotimer.data.models.AiConfig()
        org.junit.Assert.assertTrue(
            "Il prompt del Coach AI deve obbligare a chiedere a che orario iniziare il digiuno prima di creare un piano",
            config.systemPrompt.contains("CHIEDI SEMPRE L'ORARIO DI INIZIO") ||
                    config.systemPrompt.contains("a che orario preferisce iniziare il digiuno")
        )
    }
}
