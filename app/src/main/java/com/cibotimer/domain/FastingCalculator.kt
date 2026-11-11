package com.cibotimer.domain

import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingStage
import com.cibotimer.data.models.FastingState
import java.util.Calendar
import kotlin.math.max

data class TimelineSegment(
    val startMin: Float,
    val endMin: Float,
    val isFasting: Boolean
)

data class ScheduledPeriodInfo(
    val period: FastingPeriod,
    val isNowInsideFasting: Boolean,
    val periodStartEpochMillis: Long,
    val periodEndEpochMillis: Long,
    val durationSeconds: Long,
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val progressFraction: Float,
    val eatingWindowStartMillis: Long = 0L,
    val eatingWindowEndMillis: Long = 0L,
    val eatingDurationSeconds: Long = 0L,
    val eatingElapsedSeconds: Long = 0L,
    val eatingRemainingSeconds: Long = 0L,
    val eatingProgressFraction: Float = 0f
)

object FastingCalculator {

    val STAGES = listOf(
        FastingStage(
            stageIndex = 1,
            startMinutes = 0,
            endMinutes = 240,
            title = "Digestione & Glicemia",
            subtitle = "Il corpo assorbe i nutrienti dell'ultimo pasto",
            description = "I livelli di glicemia e insulina sono elevati. Il corpo sta elaborando il cibo e rifornendo i depositi di glicogeno nel fegato e nei muscoli.",
            colorHex = 0xFF38BDF8,
            titleEn = "Digestion & Blood Sugar",
            subtitleEn = "Body absorbs nutrients from the last meal",
            descriptionEn = "Blood sugar and insulin levels are elevated. The body is processing food and replenishing glycogen stores in the liver and muscles."
        ),
        FastingStage(
            stageIndex = 2,
            startMinutes = 240,
            endMinutes = 480,
            title = "Calo dell'Insulina",
            subtitle = "Fine digestione e prime riserve",
            description = "L'insulina nel sangue si abbassa progressivamente. Lo stomaco si svuota e il corpo inizia ad attingere al glicogeno immagazzinato nel fegato per ricavare energia.",
            colorHex = 0xFF22D3EE,
            titleEn = "Insulin Drop",
            subtitleEn = "Digestion completes and early reserves activate",
            descriptionEn = "Blood insulin levels gradually decline. The stomach empties and the body begins tapping into liver glycogen stores for energy."
        ),
        FastingStage(
            stageIndex = 3,
            startMinutes = 480,
            endMinutes = 720,
            title = "Combustione Grassi",
            subtitle = "La lipolisi entra in funzione",
            description = "Il glicogeno epatico si sta esaurendo. Il fegato accelera la conversione degli acidi grassi in carburante: il metabolismo dei grassi è ora attivo al 100%.",
            colorHex = 0xFFFB923C,
            titleEn = "Fat Burning",
            subtitleEn = "Lipolysis kicks into full gear",
            descriptionEn = "Liver glycogen is depleting. The liver accelerates converting fatty acids into fuel: fat metabolism is fully engaged."
        ),
        FastingStage(
            stageIndex = 4,
            startMinutes = 720,
            endMinutes = 960,
            title = "Chetosi",
            subtitle = "Corpi chetonici e massima lucidità",
            description = "I livelli di corpi chetonici nel sangue aumentano sensibilmente. Il cervello e il cuore usano i chetoni come fonte energetica ad altissima efficienza.",
            colorHex = 0xFFF472B6,
            titleEn = "Ketosis",
            subtitleEn = "Ketone bodies and heightened mental clarity",
            descriptionEn = "Blood ketone levels rise significantly. The brain and heart use ketones as a clean, highly efficient energy source."
        ),
        FastingStage(
            stageIndex = 5,
            startMinutes = 960,
            endMinutes = 1440,
            title = "Autofagia",
            subtitle = "Pulizia e ringiovanimento cellulare",
            description = "Le cellule iniziano l'autofagia: degradano e riciclano proteine danneggiate, organelli vecchi e tossine intracellulari, promuovendo longevità e rigenerazione.",
            colorHex = 0xFFC084FC,
            titleEn = "Autophagy",
            subtitleEn = "Cellular renewal and deep cleansing",
            descriptionEn = "Cells initiate autophagy: breaking down and recycling damaged proteins, aged organelles, and cellular debris for longevity and repair."
        ),
        FastingStage(
            stageIndex = 6,
            startMinutes = 1440,
            endMinutes = 2880,
            title = "Ormone della Crescita",
            subtitle = "Riparazione tessuti e muscoli",
            description = "I livelli dell'ormone della crescita (HGH) salgono fino a 5 volte rispetto al basale. Questo protegge la massa magra e accelera la riparazione dei tessuti.",
            colorHex = 0xFFA78BFA,
            titleEn = "Growth Hormone",
            subtitleEn = "Tissue repair and muscle preservation",
            descriptionEn = "Human Growth Hormone (HGH) levels increase up to 5x baseline, protecting lean muscle mass and accelerating tissue regeneration."
        ),
        FastingStage(
            stageIndex = 7,
            startMinutes = 2880,
            endMinutes = Int.MAX_VALUE,
            title = "Rigenerazione Immunitaria",
            subtitle = "Rinnovamento delle cellule staminali",
            description = "Le cellule immunitarie vecchie vengono riciclate e le cellule staminali ematopoietiche generano nuovi globuli bianchi, resettando il sistema immunitario.",
            colorHex = 0xFF4ADE80,
            titleEn = "Immune Renewal",
            subtitleEn = "Stem cell rejuvenation",
            descriptionEn = "Aged immune cells are recycled and hematopoietic stem cells trigger the formation of fresh white blood cells, rejuvenating the immune system."
        )
    )

    fun calculateElapsedMinutes(session: FastingSession, nowEpochMillis: Long = System.currentTimeMillis()): Long {
        if (session.state == FastingState.NOT_STARTED) return 0L
        val endTime = session.actualEndTimeEpochMillis ?: nowEpochMillis
        val diff = endTime - session.startTimeEpochMillis
        return max(0L, diff / (60 * 1000))
    }

    fun calculateElapsedSeconds(session: FastingSession, nowEpochMillis: Long = System.currentTimeMillis()): Long {
        if (session.state == FastingState.NOT_STARTED) return 0L
        val endTime = session.actualEndTimeEpochMillis ?: nowEpochMillis
        val diff = endTime - session.startTimeEpochMillis
        return max(0L, diff / 1000)
    }

    fun calculateTotalPlannedSeconds(session: FastingSession): Long {
        return session.targetDurationMinutes * 60L
    }

    fun calculateRemainingMinutes(session: FastingSession, nowEpochMillis: Long = System.currentTimeMillis()): Long {
        if (session.state == FastingState.NOT_STARTED) return session.targetDurationMinutes.toLong()
        val plannedEnd = session.plannedEndTimeEpochMillis
        val diff = plannedEnd - nowEpochMillis
        return max(0L, diff / (60 * 1000))
    }

    /**
     * Calcola la partizione completa a 24 ore (0..1440 minuti) per un giorno della settimana,
     * determinando con precisione matematica i segmenti di DIGIUNO (Azzurro) e di PASTO (Ambra),
     * tenendo conto anche dell'eventuale prolungamento notturno del giorno precedente.
     */
    fun compute24hSegments(day: com.cibotimer.data.models.DayPlan, prevDay: com.cibotimer.data.models.DayPlan): List<TimelineSegment> {
        val totalMinutesInDay = 1440f
        val segments = mutableListOf<TimelineSegment>()

        if (day.fastingDurationMinutes >= 1440 && day.isFastingDay) {
            return listOf(TimelineSegment(0f, totalMinutesInDay, true))
        }
        if (!day.isFastingDay && !prevDay.isFastingDay) {
            return listOf(TimelineSegment(0f, totalMinutesInDay, false))
        }

        // 1. Prolungamento mattutino dal giorno precedente
        val morningFastEnd = if (prevDay.isFastingDay) {
            val prevStart = prevDay.startHour * 60 + prevDay.startMinute
            val prevEnd = prevStart + prevDay.fastingDurationMinutes
            if (prevEnd > 1440) {
                (prevEnd - 1440).coerceAtMost(1440).toFloat()
            } else 0f
        } else 0f

        // 2. Digiuno iniziato oggi
        val todayFastStart = if (day.isFastingDay) {
            (day.startHour * 60 + day.startMinute).toFloat()
        } else totalMinutesInDay

        val todayFastEnd = if (day.isFastingDay) {
            (todayFastStart + day.fastingDurationMinutes).coerceAtMost(totalMinutesInDay)
        } else totalMinutesInDay

        // Caso unione digiuno mattutino e serale
        if (morningFastEnd >= todayFastStart) {
            val end = maxOf(morningFastEnd, todayFastEnd)
            segments.add(TimelineSegment(0f, end, true))
            if (end < totalMinutesInDay) {
                segments.add(TimelineSegment(end, totalMinutesInDay, false))
            }
            return segments
        }

        var cur = 0f

        // Mattina: digiuno dalla notte precedente
        if (morningFastEnd > 0f) {
            segments.add(TimelineSegment(0f, morningFastEnd, true))
            cur = morningFastEnd
        }

        // Finestra pasto tra digiuno mattutino e inizio digiuno serale
        if (todayFastStart > cur) {
            segments.add(TimelineSegment(cur, todayFastStart, false))
            cur = todayFastStart
        }

        // Digiuno iniziato oggi
        if (todayFastEnd > cur) {
            segments.add(TimelineSegment(cur, todayFastEnd, true))
            cur = todayFastEnd
        }

        // Finestra pasto serale residua
        if (totalMinutesInDay > cur) {
            segments.add(TimelineSegment(cur, totalMinutesInDay, false))
        }

        return segments
    }

    /**
     * Calcola i segmenti a 24 ore (Digiuno / Pasto) per un singolo giorno (1..7, Lunedì..Domenica)
     * basandosi su una lista arbitraria di periodi di digiuno (FastingPeriod).
     * Gestisce perfettamente il wrapping ciclico della settimana (da Domenica sera a Lunedì mattina).
     */
    fun computePeriods24hSegments(
        periods: List<com.cibotimer.data.models.FastingPeriod>,
        dayIndex: Int
    ): List<TimelineSegment> {
        val totalMinutesInDay = 1440
        val dayStartMin = ((dayIndex - 1).coerceIn(0, 6)) * totalMinutesInDay
        val dayEndMin = dayStartMin + totalMinutesInDay
        val weekTotalMinutes = 7 * totalMinutesInDay

        val fastingIntervals = mutableListOf<Pair<Int, Int>>()
        for (p in periods) {
            if (!p.isFasting) continue
            val startDay = ((p.startDayOfWeek - 1).coerceIn(0, 6))
            val pStart = startDay * totalMinutesInDay + p.startHour * 60 + p.startMinute
            val duration = p.fastingDurationMinutes.coerceAtLeast(0)
            if (duration <= 0) continue
            val pEnd = pStart + duration

            // Proietta il periodo nei cicli settimanali precedente (-1), corrente (0) e successivo (+1)
            for (weekOffset in listOf(-1, 0, 1)) {
                val startWithOffset = pStart + weekOffset * weekTotalMinutes
                val endWithOffset = pEnd + weekOffset * weekTotalMinutes

                val overlapStart = maxOf(dayStartMin, startWithOffset)
                val overlapEnd = minOf(dayEndMin, endWithOffset)
                if (overlapStart < overlapEnd) {
                    fastingIntervals.add(Pair(overlapStart - dayStartMin, overlapEnd - dayStartMin))
                }
            }
        }

        fastingIntervals.sortBy { it.first }

        // Fonde eventuali intervalli di digiuno contigui o sovrapposti
        val mergedIntervals = mutableListOf<Pair<Int, Int>>()
        for (interval in fastingIntervals) {
            if (mergedIntervals.isEmpty()) {
                mergedIntervals.add(interval)
            } else {
                val last = mergedIntervals.last()
                if (interval.first <= last.second) {
                    mergedIntervals[mergedIntervals.lastIndex] = Pair(last.first, maxOf(last.second, interval.second))
                } else {
                    mergedIntervals.add(interval)
                }
            }
        }

        val segments = mutableListOf<TimelineSegment>()
        var cur = 0
        for ((fStart, fEnd) in mergedIntervals) {
            if (fStart > cur) {
                segments.add(TimelineSegment(cur.toFloat(), fStart.toFloat(), isFasting = false))
            }
            segments.add(TimelineSegment(fStart.toFloat(), fEnd.toFloat(), isFasting = true))
            cur = fEnd
        }
        if (cur < totalMinutesInDay) {
            segments.add(TimelineSegment(cur.toFloat(), totalMinutesInDay.toFloat(), isFasting = false))
        }

        return segments
    }

    fun calculateRemainingSeconds(session: FastingSession, nowEpochMillis: Long = System.currentTimeMillis()): Long {
        if (session.state == FastingState.NOT_STARTED) return calculateTotalPlannedSeconds(session)
        val plannedEnd = session.plannedEndTimeEpochMillis
        val diff = plannedEnd - nowEpochMillis
        return max(0L, diff / 1000)
    }

    fun calculateProgressFraction(session: FastingSession, nowEpochMillis: Long = System.currentTimeMillis()): Float {
        val totalPlannedSeconds = calculateTotalPlannedSeconds(session).toFloat()
        if (totalPlannedSeconds <= 0f) return 0f
        val elapsedSeconds = calculateElapsedSeconds(session, nowEpochMillis).toFloat()
        return (elapsedSeconds / totalPlannedSeconds).coerceIn(0f, 1f)
    }

    fun getCurrentStage(elapsedMinutes: Long): FastingStage {
        return STAGES.firstOrNull { stage ->
            elapsedMinutes >= stage.startMinutes && elapsedMinutes < stage.endMinutes
        } ?: STAGES.last()
    }

    fun getStageForElapsedSeconds(elapsedSeconds: Long): FastingStage {
        val elapsedMinutes = elapsedSeconds / 60
        return getCurrentStage(elapsedMinutes)
    }

    /**
     * Risolve il periodo di digiuno programmato in base all'orario reale corrente (wall-clock),
     * determinando con esattezza assoluta se l'utente si trova attualmente all'interno di una
     * finestra di digiuno programmata, oppure quale sia il prossimo digiuno in programma.
     */
    fun resolveCurrentScheduledPeriod(
        plan: FastingPlan,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduledPeriodInfo {
        val effectivePeriods = plan.getEffectivePeriods()
        if (effectivePeriods.isEmpty()) {
            val fallback = FastingPeriod("fallback", 1, 1, 20, 0, 2, 12, 0, 960)
            return ScheduledPeriodInfo(
                period = fallback,
                isNowInsideFasting = false,
                periodStartEpochMillis = nowMillis,
                periodEndEpochMillis = nowMillis + 16 * 3600_000L,
                durationSeconds = 16 * 3600L,
                elapsedSeconds = 0L,
                remainingSeconds = 16 * 3600L,
                progressFraction = 0f
            )
        }

        // Calcola l'inizio della settimana corrente (Lunedì 00:00:00.000)
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
        cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1))
        val thisMondayStartMillis = cal.timeInMillis

        data class PeriodInstance(
            val period: FastingPeriod,
            val startMillis: Long,
            val endMillis: Long
        )

        val oneWeekMillis = 7 * 24 * 3600_000L
        val instances = mutableListOf<PeriodInstance>()
        val hasEightDayPeriods = effectivePeriods.any { it.startDayOfWeek >= 8 || it.endDayOfWeek >= 8 }
        val weekOffsets = if (hasEightDayPeriods) listOf(0, 1) else listOf(-1, 0, 1)

        for (weekOffset in weekOffsets) {
            val mondayStart = thisMondayStartMillis + weekOffset * oneWeekMillis
            for (p in effectivePeriods) {
                val startWeekMin = (p.startDayOfWeek - 1) * 1440 + p.startHour * 60 + p.startMinute
                var endWeekMin = (p.endDayOfWeek - 1) * 1440 + p.endHour * 60 + p.endMinute
                if (endWeekMin <= startWeekMin) {
                    endWeekMin += 7 * 1440
                }
                val startMillis = mondayStart + startWeekMin * 60_000L
                val endMillis = mondayStart + endWeekMin * 60_000L
                instances.add(PeriodInstance(p, startMillis, endMillis))
            }
        }
        instances.sortBy { it.startMillis }

        // 1. Verifica se adesso siamo all'interno di una finestra di digiuno attiva
        val activeInstance = instances.find { it.startMillis <= nowMillis && nowMillis < it.endMillis }
        if (activeInstance != null) {
            val totalSec = maxOf(1L, (activeInstance.endMillis - activeInstance.startMillis) / 1000L)
            val elapsedSec = maxOf(0L, (nowMillis - activeInstance.startMillis) / 1000L)
            val remainingSec = maxOf(0L, (activeInstance.endMillis - nowMillis) / 1000L)
            val fraction = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

            return ScheduledPeriodInfo(
                period = activeInstance.period,
                isNowInsideFasting = true,
                periodStartEpochMillis = activeInstance.startMillis,
                periodEndEpochMillis = activeInstance.endMillis,
                durationSeconds = totalSec,
                elapsedSeconds = elapsedSec,
                remainingSeconds = remainingSec,
                progressFraction = fraction
            )
        }

        // 2. Finestra di pasto o pausa tra digiuni: individua la finestra alimentare tra l'ultimo digiuno e il prossimo
        val nextInstance = instances.filter { it.startMillis > nowMillis }.minByOrNull { it.startMillis }
            ?: instances.first()
        val totalSec = maxOf(1L, (nextInstance.endMillis - nextInstance.startMillis) / 1000L)

        val prevInstance = instances.filter { it.endMillis <= nowMillis }.maxByOrNull { it.endMillis }
        val eatingStartMillis = prevInstance?.endMillis ?: (nextInstance.startMillis - 8 * 3600_000L)
        val eatingEndMillis = nextInstance.startMillis
        val eatingDurationSec = maxOf(1L, (eatingEndMillis - eatingStartMillis) / 1000L)
        val eatingElapsedSec = maxOf(0L, (nowMillis - eatingStartMillis) / 1000L)
        val eatingRemainingSec = maxOf(0L, (eatingEndMillis - nowMillis) / 1000L)
        val eatingFraction = (eatingElapsedSec.toFloat() / eatingDurationSec.toFloat()).coerceIn(0f, 1f)

        return ScheduledPeriodInfo(
            period = nextInstance.period,
            isNowInsideFasting = false,
            periodStartEpochMillis = nextInstance.startMillis,
            periodEndEpochMillis = nextInstance.endMillis,
            durationSeconds = totalSec,
            elapsedSeconds = 0L,
            remainingSeconds = totalSec,
            progressFraction = 0f,
            eatingWindowStartMillis = eatingStartMillis,
            eatingWindowEndMillis = eatingEndMillis,
            eatingDurationSeconds = eatingDurationSec,
            eatingElapsedSeconds = eatingElapsedSec,
            eatingRemainingSeconds = eatingRemainingSec,
            eatingProgressFraction = eatingFraction
        )
    }
}
