package com.cibotimer.domain

import android.content.Context
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState
import com.cibotimer.data.models.Trophy
import com.cibotimer.data.models.UserProfile
import com.cibotimer.data.models.WeightEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AiCoachPromptBuilder {

    /**
     * Costruisce il system prompt completo, onnisciente e iper-personalizzato per il Coach AI integrando:
     * - Profilo corporeo completo: peso iniziale (alla partenza), peso attuale effettivo, peso obiettivo (target), altezza, unità di misura
     * - Analisi approfondita dei progressi di peso: variazione dal peso iniziale, % completamento obiettivo, kg rimanenti, traguardo raggiunto
     * - Indice di Massa Corporea (BMI): BMI iniziale, BMI attuale e BMI target con relative categorie cliniche OMS
     * - Misure corporee & circonferenze (Vita, Fianchi, Torace, Cosce, Braccia) con delta dalla prima misurazione e WHR (Vita/Fianchi)
     * - Storico cronologico delle ultime pesate registrate (con data, peso e note) per cogliere il trend nel tempo
     * - Idratazione odierna (ml assunti oggi / target giornaliero in ml e percentuale completata)
     * - Stato in tempo reale della sessione: digiuno attivo o finestra pasto, orario inizio, orario fine previsto,
     *   tempo trascorso esatto, tempo rimanente, fase metabolica attiva (1-7) con spiegazione scientifica e previsione della prossima fase
     * - Piano di digiuno attivo nell'app: nome, categoria, descrizione, schema orario completo per tutti i 7 giorni o periodi
     * - Statistiche storiche complessive di digiuno: sessioni completate, ore totali digiunate, record digiuno più lungo, ultime sessioni concluse
     * - Trofei e traguardi sbloccati dall'utente
     * - Istruzioni operative vincolanti per il Coach AI (conoscenza totale del percorso, regole orari, verifica matematica e formato JSON fasting_plan)
     */
    fun buildPrompt(
        basePrompt: String,
        profile: UserProfile,
        activePlan: FastingPlan,
        currentSession: FastingSession?,
        weightEntries: List<WeightEntry>,
        todayWaterMl: Int,
        sessionsHistory: List<FastingSession>,
        trophies: List<Trophy>,
        context: Context? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val effectiveWeight = weightEntries.firstOrNull()?.weightKg ?: profile.currentWeightKg
        val weightUnit = profile.weightUnit

        // Calcoli progressi peso
        val weightProgress = BodyStatsCalculator.calculateWeightProgress(
            startingWeightKg = profile.startingWeightKg,
            currentWeightKg = effectiveWeight,
            targetWeightKg = profile.targetWeightKg
        )

        val startingWeightFormatted = WeightUnitConverter.formatWeight(profile.startingWeightKg, weightUnit, showUnit = true, locale = locale)
        val currentWeightFormatted = WeightUnitConverter.formatWeight(effectiveWeight, weightUnit, showUnit = true, locale = locale)
        val targetWeightFormatted = WeightUnitConverter.formatWeight(profile.targetWeightKg, weightUnit, showUnit = true, locale = locale)

        // Calcolo BMI iniziale, attuale e obiettivo
        val bmiStarting = BmiCalculator.calculate(profile.startingWeightKg, profile.heightCm)
        val bmiCurrent = BmiCalculator.calculate(effectiveWeight, profile.heightCm)
        val bmiTarget = BmiCalculator.calculate(profile.targetWeightKg, profile.heightCm)

        // Circonferenze corporee
        val circumferences = BodyStatsCalculator.calculateCircumferences(weightEntries)
        val recordedCircumferences = circumferences.filter { it.currentCm != null }

        // Rapporto Vita/Fianchi (WHR)
        val latestWithWaistAndHips = weightEntries.firstOrNull { it.waistCm != null && it.hipsCm != null }
        val whr = BodyStatsCalculator.calculateWhr(latestWithWaistAndHips?.waistCm, latestWithWaistAndHips?.hipsCm)

        val userContext = buildString {
            appendLine("=== CONTESTO COMPLETO E AGGIORNATO DELL'UTENTE (COACH AI KNOWLEDGE BASE) ===")
            val nowFormatted = DateTimeFormatters.formatFullDateTime(System.currentTimeMillis(), context, locale, includeDayOfWeek = true)
            appendLine("Data e Ora Corrente: $nowFormatted")
            appendLine()

            // 1. Profilo Corporeo, Peso Iniziale e Progressi
            appendLine("--- 1. PROFILO CORPOREO, PESO E PROGRESSI ---")
            appendLine("- Nome Utente: ${profile.name.ifBlank { "Utente" }}")
            appendLine("- PESO INIZIALE (Partenza): $startingWeightFormatted")
            appendLine("- PESO ATTUALE: $currentWeightFormatted")
            appendLine("- PESO OBIETTIVO (Target): $targetWeightFormatted")
            appendLine("- Altezza: ${profile.heightCm.toInt()} cm | Unità di misura: ${weightUnit.symbol}")

            // Analisi variazione rispetto al peso di partenza
            val lostKgFormatted = String.format(Locale.US, "%.1f", weightProgress.lostKg)
            val diffPercentFormatted = if (profile.startingWeightKg > 0) {
                String.format(Locale.US, "%.1f", kotlin.math.abs(profile.startingWeightKg - effectiveWeight) / profile.startingWeightKg * 100)
            } else "0.0"

            if (weightProgress.isWeightLoss) {
                val sign = if (profile.startingWeightKg >= effectiveWeight) "-" else "+"
                appendLine("- Variazione dal Peso Iniziale: $sign$lostKgFormatted ${weightUnit.symbol} ($diffPercentFormatted% rispetto alla partenza)")
            } else {
                appendLine("- Variazione dal Peso Iniziale: +$lostKgFormatted ${weightUnit.symbol} ($diffPercentFormatted% aumento)")
            }

            if (weightProgress.isGoalReached) {
                appendLine("- Stato Obiettivo Peso: RAGGIUNTO! 🎉 (Peso attuale $currentWeightFormatted vs Obiettivo $targetWeightFormatted)")
            } else {
                val remainingKgFormatted = String.format(Locale.US, "%.1f", weightProgress.remainingKg)
                appendLine("- Stato Obiettivo Peso: Mancano $remainingKgFormatted ${weightUnit.symbol} al traguardo (Avanzamento: ${weightProgress.progressPercent}%)")
            }

            // BMI
            appendLine("- Indice di Massa Corporea (BMI):")
            appendLine("  • BMI Iniziale (alla partenza): ${bmiStarting.bmiValue} (${bmiStarting.category})")
            appendLine("  • BMI Attuale: ${bmiCurrent.bmiValue} (${bmiCurrent.category})")
            appendLine("  • BMI Obiettivo: ${bmiTarget.bmiValue} (${bmiTarget.category})")

            // Circonferenze
            if (recordedCircumferences.isNotEmpty()) {
                appendLine("- Misure Corporee (Circonferenze):")
                recordedCircumferences.forEach { circ ->
                    val diffStr = circ.diffCm?.let { d ->
                        if (d != 0.0) " (${if (d > 0) "+$d" else "$d"} cm dalla prima misurazione)" else ""
                    } ?: ""
                    appendLine("  • ${circ.label}: ${circ.currentCm} cm$diffStr")
                }
            }
            if (whr != null) {
                appendLine("- Rapporto Vita/Fianchi (WHR): ${whr.ratio} (${whr.category})")
            }

            // Ultime pesate registrate
            if (weightEntries.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", locale)
                appendLine("- Storico Ultime Pesate Registrate (dalla più recente):")
                weightEntries.take(5).forEach { entry ->
                    val dateStr = dateFormat.format(Date(entry.epochMillis))
                    val wStr = WeightUnitConverter.formatWeight(entry.weightKg, weightUnit, showUnit = true, locale = locale)
                    val noteStr = if (entry.notes.isNotBlank()) " | Note: \"${entry.notes}\"" else ""
                    appendLine("  • $dateStr: $wStr$noteStr")
                }
            } else {
                appendLine("- Storico Pesate: Nessuna pesata manuale aggiuntiva salvata (il peso attuale coincide con il valore impostato nel profilo).")
            }
            appendLine()

            // 2. Idratazione Giornaliera
            appendLine("--- 2. IDRATAZIONE GIORNALIERA ---")
            val waterPercent = if (profile.waterTargetMl > 0) (todayWaterMl * 100 / profile.waterTargetMl) else 0
            appendLine("- Acqua assunta oggi: $todayWaterMl ml / ${profile.waterTargetMl} ml ($waterPercent% dell'obiettivo giornaliero)")
            if (todayWaterMl >= profile.waterTargetMl) {
                appendLine("- Stato Idratazione: Obiettivo giornaliero raggiunto con successo! 💧")
            } else {
                appendLine("- Stato Idratazione: Mancano ${profile.waterTargetMl - todayWaterMl} ml per completare il fabbisogno giornaliero.")
            }
            appendLine()

            // 3. Stato Sessione e Fasi Metaboliche
            appendLine("--- 3. STATO SESSIONE IN CORSO & METABOLISMO ---")
            if (currentSession != null) {
                val isFasting = currentSession.state == FastingState.FASTING
                val stateStr = if (isFasting) "DIGIUNO ATTIVO" else "FINESTRA PASTO ATTIVA"
                val startFormatted = DateTimeFormatters.formatFullDateTime(currentSession.startTimeEpochMillis, context, locale, includeDayOfWeek = true)
                val endFormatted = DateTimeFormatters.formatFullDateTime(currentSession.plannedEndTimeEpochMillis, context, locale, includeDayOfWeek = true)
                val elapsedMinutes = FastingCalculator.calculateElapsedMinutes(currentSession)
                val remainingMinutes = FastingCalculator.calculateRemainingMinutes(currentSession)
                val progressFraction = ((elapsedMinutes.toFloat() / currentSession.targetDurationMinutes.coerceAtLeast(1).toFloat()) * 100).toInt().coerceIn(0, 100)

                appendLine("- Stato Sessione: $stateStr")
                appendLine("  • Inizio: $startFormatted")
                appendLine("  • Fine prevista: $endFormatted")
                appendLine("  • Tempo trascorso: ${elapsedMinutes / 60}h ${elapsedMinutes % 60}m ($progressFraction% completato)")
                appendLine("  • Tempo rimanente: ${remainingMinutes / 60}h ${remainingMinutes % 60}m")

                if (isFasting) {
                    val stage = FastingCalculator.getCurrentStage(elapsedMinutes)
                    appendLine("  • Fase Metabolica Attuale: Fase ${stage.stageIndex} di 7 - \"${stage.title}\" (${stage.subtitle})")
                    appendLine("    Spiegazione fisiologica: ${stage.description}")

                    val nextStage = FastingCalculator.STAGES.firstOrNull { it.stageIndex == stage.stageIndex + 1 }
                    if (nextStage != null) {
                        val minutesToNext = (nextStage.startMinutes - elapsedMinutes).coerceAtLeast(0)
                        appendLine("    Prossima fase metabolica: \"${nextStage.title}\" prevista tra ${minutesToNext / 60}h ${minutesToNext % 60}m")
                    } else {
                        appendLine("    Sei nella fase metabolica più avanzata (Autofagia Profonda e Rinnovamento Cellulare)!")
                    }
                }
            } else {
                appendLine("- Stato Sessione: Nessuna sessione attiva in questo momento (il timer non è avviato).")
            }
            appendLine()

            // 4. Piano di Digiuno Attivo
            appendLine("--- 4. PIANO DI DIGIUNO ATTIVO NELL'APP ---")
            appendLine("- Nome Piano: \"${activePlan.name}\"")
            appendLine("- Categoria: ${activePlan.category.name}")
            if (activePlan.description.isNotBlank()) {
                appendLine("- Descrizione: ${activePlan.description}")
            }
            val periods = activePlan.getEffectivePeriods()
            if (!activePlan.periods.isNullOrEmpty()) {
                appendLine("- Orari a Periodi (${periods.size} periodi settimanali):")
                periods.forEach { p ->
                    appendLine("  • Periodo ${p.periodNumber}: ${p.startDayShort.uppercase()} ${p.startTimeFormatted} → ${p.endDayShort.uppercase()} ${p.endTimeFormatted} (${p.durationHoursString} digiuno)")
                }
            } else {
                appendLine("- Orari Settimanali:")
                activePlan.days.forEach { d ->
                    if (d.isFastingDay) {
                        val eatingMins = (24 * 60 - d.fastingDurationMinutes).coerceAtLeast(0)
                        appendLine("  • ${d.dayName}: Digiuno ${d.startTimeFormatted} → ${d.endTimeFormatted} (${d.durationHoursString}) | Finestra Pasto: ${eatingMins / 60}h")
                    } else {
                        appendLine("  • ${d.dayName}: Giorno Libero / Riposo")
                    }
                }
            }
            appendLine()

            // 5. Statistiche e Storico Digiuni
            appendLine("--- 5. STATISTICHE GENERALI & STORICO DIGIUNI ---")
            val completedCount = sessionsHistory.size
            val totalFastedHours = sessionsHistory.sumOf { FastingCalculator.calculateElapsedMinutes(it) / 60 }.toInt()
            val longestFastingHours = sessionsHistory.maxOfOrNull { (FastingCalculator.calculateElapsedMinutes(it) / 60).toInt() } ?: 0
            appendLine("- Sessioni di digiuno completate: $completedCount")
            appendLine("- Ore totali di digiuno accumulate: $totalFastedHours ore")
            appendLine("- Digiuno record personale più lungo: $longestFastingHours ore")
            if (sessionsHistory.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)
                appendLine("- Ultime Sessioni Concluse:")
                sessionsHistory.take(3).forEach { s ->
                    val actualMins = FastingCalculator.calculateElapsedMinutes(s)
                    val dateStr = dateFormat.format(Date(s.startTimeEpochMillis))
                    val outcome = if (actualMins >= s.targetDurationMinutes) "Traguardo raggiunto" else "Concluso prima del target"
                    appendLine("  • $dateStr (${s.planName}): ${actualMins / 60}h ${actualMins % 60}m effettuati (target ${s.targetDurationMinutes / 60}h) - $outcome")
                }
            }
            appendLine()

            // 6. Trofei e Traguardi Sbloccati
            appendLine("--- 6. TROFEI E TRAGUARDI SBLOCCATI ---")
            val unlockedTrophies = trophies.filter { it.isUnlocked }
            appendLine("- Traguardi sbloccati: ${unlockedTrophies.size} su ${trophies.size}")
            if (unlockedTrophies.isNotEmpty()) {
                appendLine("- Trofei ottenuti: ${unlockedTrophies.joinToString { it.title }}")
            }
        }

        val operationalRules = """
            === ISTRUZIONI OPERATIVE PER IL COACH AI ===
            1. CONOSCENZA TOTALE DEL PERCORSO DELL'UTENTE:
               Hai piena visibilità e memoria di ogni parametro dell'utente:
               - Conosci il suo PESO INIZIALE di partenza ($startingWeightFormatted), il suo PESO ATTUALE ($currentWeightFormatted) e il suo OBIETTIVO ($targetWeightFormatted).
               - Conosci i kg persi finora (${weightProgress.lostKg} ${weightUnit.symbol}), la percentuale di completamento dell'obiettivo (${weightProgress.progressPercent}%), l'altezza (${profile.heightCm.toInt()} cm) e il BMI iniziale/attuale/target.
               - Conosci lo storico delle pesate, l'idratazione odierna, lo stato del timer e la fase metabolica attuale.
               Quando rispondi a domande dell'utente sui suoi progressi, sul peso, sulla nutrizione, su cosa mangiare o su come gestire il digiuno:
               DIMOSTRA SEMPRE di conoscere questi dati! Cita con precisione il suo peso di partenza, celebrane i traguardi raggiunti, incoraggialo se è vicino all'obiettivo e personalizza ogni consiglio su misura per il suo corpo.
            2. Conosci alla perfezione il piano che l'utente sta seguendo ("${activePlan.name}"). Quando rispondi a domande su cosa fare o a che ora mangiare/digiunare, fai sempre riferimento a questo piano e ai suoi orari esatti.
            3. REGOLA MANDATORIA PRIMA DI CREARE UN PIANO (CHIEDI SEMPRE L'ORARIO DI INIZIO DIGIUNO):
               Quando l'utente ti chiede di CREARE un nuovo piano, impostare un piano personalizzato o trovare una routine di digiuno:
               - SE L'UTENTE NON HA ANCORA INDICATO A CHE ORA VUOLE INIZIARE IL DIGIUNO (o a che ora consuma l'ultimo pasto/cena):
                 NON generare subito il blocco JSON del piano!
                 CHIEDIGLI SEMPRE PRIMA in modo chiaro e cordiale a che ora preferisce far partire il digiuno ogni giorno (ad es. 'A che ora preferisci iniziare il digiuno la sera? Di solito verso le 20:00 o le 21:00 dopo cena, oppure preferisci un altro orario?').
               - PROCEDI A GENERARE il blocco JSON racchiuso tra ```fasting_plan ... ``` SOLO se:
                 a) L'utente ha risposto indicando a che ora vuole iniziare il digiuno; OPPURE
                 b) L'utente ha già indicato chiaramente l'orario di inizio (o di fine cena/pasto) direttamente nella sua richiesta iniziale (es. "crea un piano 16:8 a partire dalle 20:00" o "mangio dalle 12 alle 20").
            4. Se l'utente ti chiede di MODIFICARE il piano attuale (ad es. allungare a 18 ore, anticipare/posticipare orari, cambiare giorni di digiuno) oppure di SALVARLO CON UN ALTRO NOME (es. "salvalo come 'Piano Relax'", "chiamalo 'Digiuno Lavoro'"):
               - Se l'orario di inizio è noto o implicito, applica la modifica e genera il blocco JSON racchiuso tra ```fasting_plan ... ```.
            5. REGOLE CRUCIALI SULLA STRUTTURA DEL BLOCCO JSON (```fasting_plan ... ```):
               - REGOLA DEL NOME: Se l'utente ti ha indicato un nome specifico, assegna ESATTAMENTE quel nome nel campo "name" del JSON. Se l'utente non ha specificato un nome, assegna un nome esplicativo (es. "${activePlan.name} Modificato" o "18:6 Personalizzato").
               - REGOLA CRUCIALE SUGLI ORARI (NON CONFONDERE FINESTRA DIGIUNO E FINESTRA PASTO):
                 * "startHour" e "startMinute": ORARIO IN CUI INIZIA IL DIGIUNO (cioè quando l'utente finisce l'ultimo pasto/cena, es. 20:00 o 21:00).
                 * "endHour" e "endMinute": ORARIO IN CUI TERMINA IL DIGIUNO (cioè quando si apre la finestra del pasto/colazione/pranzo, es. 12:00 del giorno dopo).
                 * ATTENZIONE: Se l'utente dice "voglio mangiare dalle 12 alle 20", la finestra del pasto è 12-20, quindi il DIGIUNO È DALLE 20:00 ALLE 12:00 del giorno successivo! NON inserire 12 in startHour!
               - VERIFICA MATEMATICA OBBLIGATORIA PRIMA DI EMETTERE IL JSON:
                 * Verifica sempre che per ogni giorno di digiuno valga:
                   (endHour * 60 + endMinute - startHour * 60 - startMinute + 1440) % 1440 == fastingDurationMinutes.
                 * Esempio: Inizio digiuno ore 20:00, Fine digiuno ore 12:00 del giorno dopo -> (12*60 - 20*60 + 1440) = 960 minuti = 16 ore esatte.
               - REGOLA SUI GIORNI DI RIPOSO / PASTI LIBERI:
                 * Se l'utente richiede giorni liberi (es. "sabato e domenica liberi", "fine settimana libero", "venerdì riposo"):
                   imposta per quei giorni "isFastingDay": false, "startHour": 0, "startMinute": 0, "endHour": 0, "endMinute": 0, "fastingDurationMinutes": 0.
               - REGOLA OBBLIGATORIA SUI GIORNI DELLA SETTIMANA (ISO 8601):
                 La settimana ha ESATTAMENTE 7 giorni (da 1 a 7) e termina SEMPRE con la Domenica (7):
                 1 = Lunedì, 2 = Martedì, 3 = Mercoledì, 4 = Giovedì, 5 = Venerdì, 6 = Sabato, 7 = Domenica.
                 NON USARE IL CALENDARIO AMERICANO DOVE 1 È DOMENICA!
               - REGOLA OBBLIGATORIA SULLA DURATA ("fastingDurationMinutes"):
                 "fastingDurationMinutes" DEVE ESSERE ESPRESSO IN MINUTI TOTALI (ore * 60). NON scrivere MAI il numero di ore (es. NON scrivere 18, ma 1080)!
                 NON impostare MAI a 0 per i giorni di digiuno!
                 Tabella di conversione:
                 * 12 ore = 720
                 * 14 ore = 840
                 * 16 ore = 960
                 * 18 ore = 1080 (es. inizio ore 18:00 e fine ore 12:00 del giorno dopo, oppure 20:00 -> 14:00)
                 * 20 ore = 1200 (es. inizio ore 20:00 e fine ore 16:00 del giorno dopo)
                 * 23 ore = 1380
                 * 24 ore = 1440
               - SINTASSI JSON RIGOROSA (RFC 8259):
                 * Non inserire MAI virgole prima delle parentesi graffe o quadre chiuse.
                 * Non inserire commenti (niente // o /*) dentro il blocco JSON.
                 * Includi sempre tutti i 7 giorni (da 1 a 7) nell'array 'days'.
               - Esempio di blocco JSON per un piano da 18 ore tutti i giorni:
            ```fasting_plan
            {
              "id": "custom-ai-${UUID.randomUUID().toString().take(6)}",
              "name": "Piano 18:6 Personalizzato",
              "category": "CUSTOM",
              "description": "Piano con 18 ore di digiuno giornaliero dalle 18:00 alle 12:00.",
              "days": [
                { "dayOfWeek": 1, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 2, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 3, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 4, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 5, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 6, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 },
                { "dayOfWeek": 7, "isFastingDay": true, "startHour": 18, "startMinute": 0, "endHour": 12, "endMinute": 0, "fastingDurationMinutes": 1080 }
              ]
            }
            ```
            6. L'applicazione rileverà questo blocco JSON, ne verificherà e sanificherà matematicamente la coerenza oraria e mostrerà all'utente una card con cui potrà applicare il piano o personalizzarne il nome e salvarlo con un tocco.
        """.trimIndent()

        return "$basePrompt\n\n$userContext\n\n$operationalRules"
    }
}
