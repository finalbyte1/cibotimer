package com.cibotimer.domain

import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingDayFormatter
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.domain.WeekSpanModel.toPeriod

object PlanShiftEngine {

    /**
     * Rappresenta i limiti temporali e i vincoli rispetto ai periodi adiacenti (anteriore e successivo).
     * Derivato dai minuti assoluti settimanali ciclici [0..10080].
     */
    data class PeriodBounds(
        val hasAnterior: Boolean = false,
        val anteriorPeriodNumber: Int? = null,
        val anteriorEndHour: Int? = null,
        val anteriorEndMinute: Int? = null,
        val anteriorEndDayOfWeek: Int? = null,
        val anteriorEndDayShort: String? = null,
        val gapBeforeMinutes: Int = Int.MAX_VALUE,

        val hasNext: Boolean = false,
        val nextPeriodNumber: Int? = null,
        val nextStartHour: Int? = null,
        val nextStartMinute: Int? = null,
        val nextStartDayOfWeek: Int? = null,
        val nextStartDayShort: String? = null,
        val gapAfterMinutes: Int = Int.MAX_VALUE,

        val minStartAbsolute: Int = 0,
        val maxEndAbsolute: Int = WeekSpanModel.WEEK_MINUTES
    ) {
        val canShiftMinus1h: Boolean
            get() = !hasAnterior || gapBeforeMinutes >= 60

        val canShiftMinus15m: Boolean
            get() = !hasAnterior || gapBeforeMinutes >= 15

        val canShiftPlus1h: Boolean
            get() = !hasNext || gapAfterMinutes >= 60

        val canShiftPlus15m: Boolean
            get() = !hasNext || gapAfterMinutes >= 15

        fun canDurationPlus(deltaMinutes: Int): Boolean =
            !hasNext || gapAfterMinutes >= deltaMinutes
    }

    /**
     * Ripara eventuali sovrapposizioni o conflitti temporali nel piano:
     * - Ordina i periodi cronologicamente per orario di inizio
     * - Sposta l'inizio del periodo in conflitto alla fine del periodo precedente
     * - Garantisce una durata minima di 60 minuti
     * - Gestisce ciclicamente il wrap tra ultimo e primo periodo
     * Se non ci sono conflitti, restituisce il piano invariato.
     */
    fun repairPlan(plan: FastingPlan): FastingPlan {
        val periodsList = plan.getEffectivePeriods()
        if (periodsList.size <= 1) return plan

        val spans = periodsList.map { it.toSpan() }
        val (repairedSpans, wasRepaired) = WeekSpanModel.repairOverlaps(spans)

        val needsUpdate = wasRepaired || plan.periods == null || plan.periods.size != repairedSpans.size
        if (!needsUpdate) return plan

        val repairedPeriods = repairedSpans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, "p${idx + 1}")
        }
        val updatedDays = syncDaysFromPeriods(repairedPeriods, plan.days)
        return plan.copy(periods = repairedPeriods, days = updatedDays)
    }

    /**
     * Calcola i limiti temporali e le distanze minime (gap) rispetto ai periodi adiacenti,
     * in modo rigorosamente ciclico sull'asse lineare settimanale (0..10080).
     */
    fun getPeriodBounds(plan: FastingPlan, periodNumber: Int): PeriodBounds {
        val periodsList = plan.getEffectivePeriods()
        if (periodsList.size <= 1) {
            return PeriodBounds()
        }
        val targetIdx = periodsList.indexOfFirst { it.periodNumber == periodNumber }
        if (targetIdx == -1) {
            return PeriodBounds()
        }

        val spans = periodsList.map { it.toSpan() }
        val b = WeekSpanModel.bounds(spans, targetIdx)

        val prevP = periodsList[b.prevIndex]
        val nextP = periodsList[b.nextIndex]

        return PeriodBounds(
            hasAnterior = true,
            anteriorPeriodNumber = prevP.periodNumber,
            anteriorEndHour = b.prevEndHour,
            anteriorEndMinute = b.prevEndMinute,
            anteriorEndDayOfWeek = b.prevEndDayOfWeek,
            anteriorEndDayShort = FastingDayFormatter.formatShort(b.prevEndDayOfWeek),
            gapBeforeMinutes = b.gapBeforeMinutes,

            hasNext = true,
            nextPeriodNumber = nextP.periodNumber,
            nextStartHour = b.nextStartHour,
            nextStartMinute = b.nextStartMinute,
            nextStartDayOfWeek = b.nextStartDayOfWeek,
            nextStartDayShort = FastingDayFormatter.formatShort(b.nextStartDayOfWeek),
            gapAfterMinutes = b.gapAfterMinutes,

            minStartAbsolute = b.minStart,
            maxEndAbsolute = b.maxEnd
        )
    }

    /**
     * Sposta tutti i periodi (o giorni) del piano di deltaHours e deltaMinutes
     * operando sugli Span interi (modulo 10080), preservando matematicamente durata e distanze.
     */
    fun shiftAllDays(plan: FastingPlan, deltaHours: Int, deltaMinutes: Int = 0): FastingPlan {
        val totalDelta = deltaHours * 60 + deltaMinutes
        if (totalDelta == 0) return plan

        val currentPeriods = plan.getEffectivePeriods()
        val spans = currentPeriods.map { it.toSpan() }

        val shiftedSpans = spans.map { s ->
            val dur = s.durationMinutes
            val rawStart = (s.start + totalDelta) % WeekSpanModel.WEEK_MINUTES
            val normStart = ((rawStart % WeekSpanModel.WEEK_MINUTES) + WeekSpanModel.WEEK_MINUTES) % WeekSpanModel.WEEK_MINUTES
            Span(normStart, normStart + dur)
        }

        val updatedPeriods = shiftedSpans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, currentPeriods[idx].id)
        }
        val updatedDays = syncDaysFromPeriods(updatedPeriods, plan.days)
        return plan.copy(days = updatedDays, periods = updatedPeriods)
    }

    /**
     * Sposta un singolo periodo di deltaHours e deltaMinutes in minuti assoluti.
     * Rispetta i bound ciclici [minStart, maxEnd].
     */
    fun shiftPeriod(
        plan: FastingPlan,
        periodNumber: Int,
        deltaHours: Int,
        deltaMinutes: Int = 0,
        cascadeNext: Boolean = false
    ): FastingPlan {
        val totalDelta = deltaHours * 60 + deltaMinutes
        if (totalDelta == 0) return plan

        val periodsList = plan.getEffectivePeriods().toMutableList()
        val targetIdx = periodsList.indexOfFirst { it.periodNumber == periodNumber }
        if (targetIdx == -1) return plan

        val spans = periodsList.map { it.toSpan() }.toMutableList()
        val b = WeekSpanModel.bounds(spans, targetIdx)
        val curSpan = spans[targetIdx]

        val effectiveDelta = if (totalDelta < 0) {
            val maxEarlier = curSpan.start - b.minStart
            if (maxEarlier <= 0) return plan
            totalDelta.coerceAtLeast(-maxEarlier)
        } else {
            if (!cascadeNext) {
                val maxLater = b.maxEnd - curSpan.end
                if (maxLater <= 0) return plan
                totalDelta.coerceAtMost(maxLater)
            } else {
                totalDelta
            }
        }
        if (effectiveDelta == 0) return plan

        val newStart = curSpan.start + effectiveDelta
        val newEnd = curSpan.end + effectiveDelta
        spans[targetIdx] = Span(newStart, newEnd)

        if (cascadeNext && targetIdx + 1 < spans.size) {
            val nextSpan = spans[targetIdx + 1]
            val nextDur = nextSpan.durationMinutes
            val cascadedStart = newEnd + 60
            spans[targetIdx + 1] = Span(cascadedStart, cascadedStart + nextDur)
        }

        val updatedPeriods = spans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, periodsList[idx].id)
        }
        val updatedDays = syncDaysFromPeriods(updatedPeriods, plan.days)
        return plan.copy(periods = updatedPeriods, days = updatedDays)
    }

    /**
     * Modifica la durata di un singolo periodo, regolando solo l'orario di fine.
     * Garantisce newEnd <= maxEnd e durata compresa tra 60 e 1440 minuti.
     */
    fun adjustPeriodDuration(
        plan: FastingPlan,
        periodNumber: Int,
        deltaMinutes: Int,
        cascadeNext: Boolean = false
    ): FastingPlan {
        if (deltaMinutes == 0) return plan

        val periodsList = plan.getEffectivePeriods().toMutableList()
        val targetIdx = periodsList.indexOfFirst { it.periodNumber == periodNumber }
        if (targetIdx == -1) return plan

        val spans = periodsList.map { it.toSpan() }.toMutableList()
        val b = WeekSpanModel.bounds(spans, targetIdx)
        val curSpan = spans[targetIdx]

        val maxAllowedEnd = if (!cascadeNext) minOf(curSpan.start + 1440, b.maxEnd) else curSpan.start + 1440
        val minAllowedEnd = curSpan.start + 60
        if (minAllowedEnd > maxAllowedEnd) return plan

        val targetEnd = curSpan.end + deltaMinutes
        val newEnd = targetEnd.coerceIn(minAllowedEnd, maxAllowedEnd)
        if (newEnd == curSpan.end) return plan

        spans[targetIdx] = Span(curSpan.start, newEnd)

        if (cascadeNext && targetIdx + 1 < spans.size) {
            val nextSpan = spans[targetIdx + 1]
            val nextDur = nextSpan.durationMinutes
            val cascadedStart = newEnd + 60
            spans[targetIdx + 1] = Span(cascadedStart, cascadedStart + nextDur)
        }

        val updatedPeriods = spans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, periodsList[idx].id)
        }
        val updatedDays = syncDaysFromPeriods(updatedPeriods, plan.days)
        return plan.copy(periods = updatedPeriods, days = updatedDays)
    }

    /**
     * Modifica orario di inizio o fine di un periodo (da TimePickerDialog).
     * Gestisce i minuti assoluti settimanali limitando a [minStart, maxEnd - 60] per l'inizio
     * e a [start + 60, maxEnd] per la fine.
     */
    fun updatePeriodTime(
        plan: FastingPlan,
        periodNumber: Int,
        newHour: Int,
        newMinute: Int,
        isStart: Boolean,
        cascadeNext: Boolean = false
    ): FastingPlan {
        val periodsList = plan.getEffectivePeriods().toMutableList()
        val targetIdx = periodsList.indexOfFirst { it.periodNumber == periodNumber }
        if (targetIdx == -1) return plan

        val spans = periodsList.map { it.toSpan() }.toMutableList()
        val b = WeekSpanModel.bounds(spans, targetIdx)
        val curSpan = spans[targetIdx]
        val pickedMinuteOfDay = newHour * 60 + newMinute

        if (isStart) {
            val oldStart = curSpan.start
            val candidates = (-2..9).map { k -> k * 1440 + pickedMinuteOfDay }
            val closestCandidate = candidates.minByOrNull { kotlin.math.abs(it - oldStart) } ?: oldStart
            val curDur = curSpan.durationMinutes.coerceIn(60, 1440)
            val maxStart = maxOf(b.minStart, b.maxEnd - curDur)
            val finalStart = closestCandidate.coerceIn(b.minStart, maxStart)
            val finalEnd = finalStart + curDur
            spans[targetIdx] = Span(finalStart, finalEnd)
        } else {
            val start = curSpan.start
            val minTargetEnd = start + 60
            val maxTargetEnd = start + 1440

            val candidateEnd = ((-2..9).map { k -> k * 1440 + pickedMinuteOfDay })
                .firstOrNull { it >= minTargetEnd && it <= maxTargetEnd }
                ?: (start + 60)

            val maxAllowedEnd = if (!cascadeNext) b.maxEnd else maxTargetEnd
            val finalEnd = candidateEnd.coerceIn(start + 60, maxOf(start + 60, maxAllowedEnd))
            spans[targetIdx] = Span(start, finalEnd)
        }

        if (cascadeNext && targetIdx + 1 < spans.size) {
            val nextSpan = spans[targetIdx + 1]
            val nextDur = nextSpan.durationMinutes
            val cascadedStart = spans[targetIdx].end + 60
            spans[targetIdx + 1] = Span(cascadedStart, cascadedStart + nextDur)
        }

        val updatedPeriods = spans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, periodsList[idx].id)
        }
        val updatedDays = syncDaysFromPeriods(updatedPeriods, plan.days)
        return plan.copy(periods = updatedPeriods, days = updatedDays)
    }

    /**
     * Verifica se l'orario di inizio scelto è uscito dai limiti ed è stato limitato al bound.
     */
    fun checkStartTimeClamped(plan: FastingPlan, periodNumber: Int, hour: Int, minute: Int): Boolean {
        val periods = plan.getEffectivePeriods()
        val idx = periods.indexOfFirst { it.periodNumber == periodNumber }
        if (idx == -1) return false
        val spans = periods.map { it.toSpan() }
        val b = WeekSpanModel.bounds(spans, idx)
        val curSpan = spans[idx]
        val picked = hour * 60 + minute
        val candidates = (-2..9).map { k -> k * 1440 + picked }
        val closest = candidates.minByOrNull { kotlin.math.abs(it - curSpan.start) } ?: curSpan.start
        val curDur = curSpan.durationMinutes.coerceIn(60, 1440)
        val maxStart = maxOf(b.minStart, b.maxEnd - curDur)
        return closest < b.minStart || closest > maxStart
    }

    /**
     * Verifica se l'orario di fine scelto è uscito dai limiti ed è stato limitato al bound.
     */
    fun checkEndTimeClamped(plan: FastingPlan, periodNumber: Int, hour: Int, minute: Int): Boolean {
        val periods = plan.getEffectivePeriods()
        val idx = periods.indexOfFirst { it.periodNumber == periodNumber }
        if (idx == -1) return false
        val spans = periods.map { it.toSpan() }
        val b = WeekSpanModel.bounds(spans, idx)
        val curSpan = spans[idx]
        val picked = hour * 60 + minute
        val minEnd = curSpan.start + 60
        val maxEnd = curSpan.start + 1440
        val candidate = ((-2..9).map { k -> k * 1440 + picked })
            .firstOrNull { it >= minEnd && it <= maxEnd } ?: (curSpan.start + 60)
        return candidate > b.maxEnd
    }

    /**
     * Elimina un periodo (se ce ne sono più di uno) e ri-numera i rimanenti.
     */
    fun deletePeriod(plan: FastingPlan, periodNumber: Int): FastingPlan {
        val currentPeriods = plan.getEffectivePeriods()
        if (currentPeriods.size <= 1) return plan

        val filtered = currentPeriods.filter { it.periodNumber != periodNumber }
        val renumbered = filtered.mapIndexed { idx, p ->
            p.copy(periodNumber = idx + 1)
        }
        val updatedDays = syncDaysFromPeriods(renumbered, plan.days)
        return plan.copy(periods = renumbered, days = updatedDays)
    }

    /**
     * Aggiunge un nuovo periodo al piano in coda all'ultimo periodo.
     */
    fun addPeriod(plan: FastingPlan): FastingPlan {
        val currentPeriods = plan.getEffectivePeriods().toMutableList()
        val lastP = currentPeriods.lastOrNull()
        val newNumber = currentPeriods.size + 1

        val lastSpan = lastP?.toSpan()
        val newStart = if (lastSpan != null) lastSpan.end + 60 else 20 * 60
        val newEnd = newStart + 16 * 60
        val newSpan = Span(newStart, newEnd)
        val newP = newSpan.toPeriod(newNumber, "p_$newNumber")
        currentPeriods.add(newP)

        val updatedDays = syncDaysFromPeriods(currentPeriods, plan.days)
        return plan.copy(periods = currentPeriods, days = updatedDays)
    }

    /**
     * Sincronizza la lista dei 7 giorni (DayPlan) a partire dalla lista dei periodi,
     * garantendo piena retrocompatibilità con i componenti che leggono plan.days.
     */
    private fun syncDaysFromPeriods(periods: List<FastingPeriod>, existingDays: List<DayPlan>): List<DayPlan> {
        val spans = periods.map { it.toSpan() }
        return (1..7).map { dayOfWeek ->
            val d = dayOfWeek - 1
            val fasts = WeekSpanModel.fastsInDay(spans, d)
            val periodStartingToday = periods.find { it.startDayOfWeek == dayOfWeek }
            val existing = existingDays.find { it.dayOfWeek == dayOfWeek }

            if (periodStartingToday != null) {
                DayPlan(
                    dayOfWeek = dayOfWeek,
                    isFastingDay = periodStartingToday.isFasting,
                    startHour = periodStartingToday.startHour,
                    startMinute = periodStartingToday.startMinute,
                    endHour = periodStartingToday.endHour % 24,
                    endMinute = periodStartingToday.endMinute,
                    fastingDurationMinutes = periodStartingToday.fastingDurationMinutes
                )
            } else if (fasts.isNotEmpty()) {
                val firstFast = fasts.first()
                val totalMins = fasts.sumOf { it.second - it.first }
                DayPlan(
                    dayOfWeek = dayOfWeek,
                    isFastingDay = true,
                    startHour = firstFast.first / 60,
                    startMinute = firstFast.first % 60,
                    endHour = (firstFast.second / 60) % 24,
                    endMinute = firstFast.second % 60,
                    fastingDurationMinutes = totalMins
                )
            } else if (existing != null) {
                existing.copy(isFastingDay = false)
            } else {
                DayPlan(dayOfWeek, false, 20, 0, 12, 0, 0)
            }
        }
    }

    /**
     * Sposta un singolo giorno delegando a shiftPeriod se ci sono periodi attivi.
     */
    fun shiftDay(
        plan: FastingPlan,
        dayOfWeek: Int,
        deltaHours: Int,
        deltaMinutes: Int = 0,
        cascadeNextDay: Boolean = false
    ): FastingPlan {
        val effectivePeriods = plan.getEffectivePeriods()
        val matchingPeriod = effectivePeriods.find { it.startDayOfWeek == dayOfWeek }
        if (matchingPeriod != null) {
            return shiftPeriod(plan, matchingPeriod.periodNumber, deltaHours, deltaMinutes, cascadeNextDay)
        }

        val nextDayOfWeek = if (dayOfWeek == 7) 1 else dayOfWeek + 1
        var modifiedDay: DayPlan? = null

        val updatedDays = plan.days.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                val shifted = shiftSingleDay(day, deltaHours, deltaMinutes)
                modifiedDay = shifted
                shifted
            } else {
                day
            }
        }

        if (!cascadeNextDay || modifiedDay == null) {
            return plan.copy(days = updatedDays)
        }

        val mDay = modifiedDay!!
        val finalDays = updatedDays.map { day ->
            if (day.dayOfWeek == nextDayOfWeek && day.isFastingDay) {
                val targetStartHour = (mDay.endHour + 1) % 24
                val targetStartMinute = mDay.endMinute
                val newEndTotal = (targetStartHour * 60 + targetStartMinute + day.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
                day.copy(
                    startHour = targetStartHour,
                    startMinute = targetStartMinute,
                    endHour = newEndTotal / 60,
                    endMinute = newEndTotal % 60
                )
            } else {
                day
            }
        }

        return plan.copy(days = finalDays)
    }

    /**
     * Modifica la durata per un singolo giorno delegando a adjustPeriodDuration se ci sono periodi attivi.
     */
    fun adjustDuration(
        plan: FastingPlan,
        dayOfWeek: Int,
        deltaMinutes: Int,
        cascadeNextDay: Boolean = false
    ): FastingPlan {
        val effectivePeriods = plan.getEffectivePeriods()
        val matchingPeriod = effectivePeriods.find { it.startDayOfWeek == dayOfWeek }
        if (matchingPeriod != null) {
            return adjustPeriodDuration(plan, matchingPeriod.periodNumber, deltaMinutes, cascadeNextDay)
        }

        val nextDayOfWeek = if (dayOfWeek == 7) 1 else dayOfWeek + 1
        var modifiedDay: DayPlan? = null

        val updatedDays = plan.days.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                val newDuration = (day.fastingDurationMinutes + deltaMinutes).coerceIn(60, 24 * 60)
                val actualDelta = newDuration - day.fastingDurationMinutes
                val totalEndMinutes = (day.endHour * 60 + day.endMinute + actualDelta + 24 * 60 * 10) % (24 * 60)
                val updated = day.copy(
                    endHour = totalEndMinutes / 60,
                    endMinute = totalEndMinutes % 60,
                    fastingDurationMinutes = newDuration
                )
                modifiedDay = updated
                updated
            } else {
                day
            }
        }

        if (!cascadeNextDay || modifiedDay == null) {
            return plan.copy(days = updatedDays)
        }

        val mDay = modifiedDay!!
        val finalDays = updatedDays.map { day ->
            if (day.dayOfWeek == nextDayOfWeek && day.isFastingDay) {
                val targetStartHour = (mDay.endHour + 1) % 24
                val targetStartMinute = mDay.endMinute
                val newEndTotal = (targetStartHour * 60 + targetStartMinute + day.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
                day.copy(
                    startHour = targetStartHour,
                    startMinute = targetStartMinute,
                    endHour = newEndTotal / 60,
                    endMinute = newEndTotal % 60
                )
            } else {
                day
            }
        }

        return plan.copy(days = finalDays)
    }

    /**
     * Modifica la durata del digiuno per tutti i periodi/giorni.
     * Rifiuta la modifica se genera sovrapposizioni tra periodi consecutivi.
     */
    fun adjustDurationAllDays(plan: FastingPlan, deltaMinutes: Int): FastingPlan {
        if (deltaMinutes == 0) return plan

        val currentPeriods = plan.getEffectivePeriods()
        val spans = currentPeriods.map { it.toSpan() }
        val n = spans.size

        val candidateSpans = spans.map { s ->
            val newDur = (s.durationMinutes + deltaMinutes).coerceIn(60, 1440)
            Span(s.start, s.start + newDur)
        }

        if (deltaMinutes > 0 && n > 1) {
            val order = candidateSpans.indices.sortedBy { candidateSpans[it].start }
            for (pos in 0 until n) {
                val idx = order[pos]
                val nextIdx = order[(pos + 1) % n]
                val nextOffset = if (pos == n - 1) WeekSpanModel.WEEK_MINUTES else 0
                val nextEffectiveStart = candidateSpans[nextIdx].start + nextOffset
                if (candidateSpans[idx].end > nextEffectiveStart) {
                    return plan // Rifiuta la modifica se genera sovrapposizioni
                }
            }
        }

        val updatedPeriods = candidateSpans.mapIndexed { idx, span ->
            span.toPeriod(idx + 1, currentPeriods[idx].id)
        }
        val updatedDays = syncDaysFromPeriods(updatedPeriods, plan.days)
        return plan.copy(days = updatedDays, periods = updatedPeriods)
    }

    fun toggleDayFasting(plan: FastingPlan, dayOfWeek: Int): FastingPlan {
        val updatedDays = plan.days.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                day.copy(isFastingDay = !day.isFastingDay)
            } else {
                day
            }
        }
        return plan.copy(days = updatedDays)
    }

    fun setAllDaysFasting(plan: FastingPlan, isFasting: Boolean): FastingPlan {
        val updatedDays = plan.days.map { day ->
            day.copy(isFastingDay = isFasting)
        }
        return plan.copy(days = updatedDays)
    }

    private fun shiftSingleDay(day: DayPlan, deltaHours: Int, deltaMinutes: Int): DayPlan {
        val totalDeltaMinutes = deltaHours * 60 + deltaMinutes

        val newStartTotalMinutes = (day.startHour * 60 + day.startMinute + totalDeltaMinutes + 24 * 60 * 10) % (24 * 60)
        val newEndTotalMinutes = (day.endHour * 60 + day.endMinute + totalDeltaMinutes + 24 * 60 * 10) % (24 * 60)

        return day.copy(
            startHour = newStartTotalMinutes / 60,
            startMinute = newStartTotalMinutes % 60,
            endHour = newEndTotalMinutes / 60,
            endMinute = newEndTotalMinutes % 60
        )
    }
}
