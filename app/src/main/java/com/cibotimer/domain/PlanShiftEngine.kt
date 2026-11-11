package com.cibotimer.domain

import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan

object PlanShiftEngine {

    /**
     * Rappresenta i limiti temporali e i vincoli rispetto ai periodi adiacenti (anteriore e successivo).
     * Ispirato direttamente alla funzione BodyFast #17838 getPeriodBounds.
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
        val gapAfterMinutes: Int = Int.MAX_VALUE
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
     * Calcola i limiti temporali e le distanze minime (gap) rispetto ai periodi adiacenti.
     * Garantisce che non sia possibile superare l'orario di fine del periodo anteriore o l'inizio del successivo.
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

        val curP = periodsList[targetIdx]
        val curStartWeekly = ((curP.startDayOfWeek - 1).coerceIn(0, 6)) * 1440 + curP.startHour * 60 + curP.startMinute
        val curEndWeekly = curStartWeekly + curP.fastingDurationMinutes

        // Periodo anteriore
        val (prevP, isCyclicAnterior) = if (targetIdx > 0) {
            Pair(periodsList[targetIdx - 1], false)
        } else {
            Pair(periodsList.last(), true)
        }

        val prevStartWeekly = ((prevP.startDayOfWeek - 1).coerceIn(0, 6)) * 1440 + prevP.startHour * 60 + prevP.startMinute
        val prevEndWeekly = prevStartWeekly + prevP.fastingDurationMinutes

        val gapBefore = if (!isCyclicAnterior) {
            maxOf(0, curStartWeekly - prevEndWeekly)
        } else {
            val prevEndMod = prevEndWeekly % 10080
            val curStartMod = curStartWeekly % 10080
            if (curStartWeekly == 0 && prevEndWeekly <= 10080) {
                0
            } else {
                ((curStartMod - prevEndMod + 10080) % 10080)
            }
        }

        // Periodo successivo
        val (nextP, isCyclicNext) = if (targetIdx < periodsList.size - 1) {
            Pair(periodsList[targetIdx + 1], false)
        } else {
            Pair(periodsList.first(), true)
        }

        val nextStartWeekly = ((nextP.startDayOfWeek - 1).coerceIn(0, 6)) * 1440 + nextP.startHour * 60 + nextP.startMinute

        val gapAfter = if (!isCyclicNext) {
            maxOf(0, nextStartWeekly - curEndWeekly)
        } else {
            val curEndMod = curEndWeekly % 10080
            val nextStartMod = nextStartWeekly % 10080
            ((nextStartMod - curEndMod + 10080) % 10080)
        }

        return PeriodBounds(
            hasAnterior = true,
            anteriorPeriodNumber = prevP.periodNumber,
            anteriorEndHour = prevP.endHour,
            anteriorEndMinute = prevP.endMinute,
            anteriorEndDayOfWeek = prevP.endDayOfWeek,
            anteriorEndDayShort = prevP.endDayShort,
            gapBeforeMinutes = gapBefore,

            hasNext = true,
            nextPeriodNumber = nextP.periodNumber,
            nextStartHour = nextP.startHour,
            nextStartMinute = nextP.startMinute,
            nextStartDayOfWeek = nextP.startDayOfWeek,
            nextStartDayShort = nextP.startDayShort,
            gapAfterMinutes = gapAfter
        )
    }

    /**
     * Sposta tutti i periodi (o giorni) del piano di deltaHours (±1 ora) e deltaMinutes (±15 min),
     * preservando RIGOROSAMENTE e MATEMATICAMENTE la durata del digiuno.
     */
    fun shiftAllDays(plan: FastingPlan, deltaHours: Int, deltaMinutes: Int = 0): FastingPlan {
        val totalDelta = deltaHours * 60 + deltaMinutes
        val currentPeriods = plan.getEffectivePeriods()
        val updatedPeriods = currentPeriods.map { p ->
            val startTotal = (p.startHour * 60 + p.startMinute + totalDelta + 24 * 60 * 10) % (24 * 60)
            val endTotal = (p.endHour * 60 + p.endMinute + totalDelta + 24 * 60 * 10) % (24 * 60)
            p.copy(
                startHour = startTotal / 60,
                startMinute = startTotal % 60,
                endHour = endTotal / 60,
                endMinute = endTotal % 60
            )
        }
        val updatedDays = plan.days.map { day ->
            shiftSingleDay(day, deltaHours, deltaMinutes)
        }
        return plan.copy(days = updatedDays, periods = updatedPeriods)
    }

    /**
     * Sposta un singolo periodo di deltaHours e deltaMinutes.
     * Con cascadeNext = false (predefinito), il periodo successivo rimane rigorosamente INVARIATO.
     * Non permette di superare l'orario di fine del periodo anteriore o l'inizio del periodo successivo.
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

        val bounds = getPeriodBounds(plan, periodNumber)

        val effectiveDelta = if (totalDelta < 0) {
            if (bounds.hasAnterior) {
                val maxEarlier = bounds.gapBeforeMinutes
                if (maxEarlier <= 0) return plan
                totalDelta.coerceAtLeast(-maxEarlier)
            } else {
                totalDelta
            }
        } else {
            if (bounds.hasNext && !cascadeNext) {
                val maxLater = bounds.gapAfterMinutes
                if (maxLater <= 0) return plan
                totalDelta.coerceAtMost(maxLater)
            } else {
                totalDelta
            }
        }

        if (effectiveDelta == 0) return plan

        val p = periodsList[targetIdx]
        val newStartTotal = (p.startHour * 60 + p.startMinute + effectiveDelta + 24 * 60 * 10) % (24 * 60)
        val newEndTotal = (p.endHour * 60 + p.endMinute + effectiveDelta + 24 * 60 * 10) % (24 * 60)
        val updatedTarget = p.copy(
            startHour = newStartTotal / 60,
            startMinute = newStartTotal % 60,
            endHour = newEndTotal / 60,
            endMinute = newEndTotal % 60
        )
        periodsList[targetIdx] = updatedTarget

        if (cascadeNext && targetIdx + 1 < periodsList.size) {
            val nextP = periodsList[targetIdx + 1]
            val nextStartH = (updatedTarget.endHour + 1) % 24
            val nextStartM = updatedTarget.endMinute
            val nextEndTotal = (nextStartH * 60 + nextStartM + nextP.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
            val updatedNext = nextP.copy(
                startHour = nextStartH,
                startMinute = nextStartM,
                endHour = nextEndTotal / 60,
                endMinute = nextEndTotal % 60
            )
            periodsList[targetIdx + 1] = updatedNext
        }

        val updatedDays = syncDaysFromPeriods(periodsList, plan.days)
        return plan.copy(periods = periodsList, days = updatedDays)
    }

    /**
     * Modifica la durata di un singolo periodo, regolando l'orario di fine.
     * Con cascadeNext = false (predefinito), il periodo successivo non viene modificato
     * e non può essere superato l'inizio del periodo successivo.
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

        val p = periodsList[targetIdx]
        val bounds = getPeriodBounds(plan, periodNumber)

        val effectiveDelta = if (deltaMinutes > 0 && !cascadeNext && bounds.hasNext) {
            val maxExtension = bounds.gapAfterMinutes
            if (maxExtension <= 0) return plan
            deltaMinutes.coerceAtMost(maxExtension)
        } else {
            deltaMinutes
        }

        val newDuration = (p.fastingDurationMinutes + effectiveDelta).coerceIn(60, 24 * 60)
        val actualDelta = newDuration - p.fastingDurationMinutes
        if (actualDelta == 0) return plan

        val newEndTotal = (p.endHour * 60 + p.endMinute + actualDelta + 24 * 60 * 10) % (24 * 60)
        val updatedTarget = p.copy(
            endHour = newEndTotal / 60,
            endMinute = newEndTotal % 60,
            fastingDurationMinutes = newDuration
        )
        periodsList[targetIdx] = updatedTarget

        if (cascadeNext && targetIdx + 1 < periodsList.size) {
            val nextP = periodsList[targetIdx + 1]
            val nextStartH = (updatedTarget.endHour + 1) % 24
            val nextStartM = updatedTarget.endMinute
            val nextEndTotal = (nextStartH * 60 + nextStartM + nextP.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
            val updatedNext = nextP.copy(
                startHour = nextStartH,
                startMinute = nextStartM,
                endHour = nextEndTotal / 60,
                endMinute = nextEndTotal % 60
            )
            periodsList[targetIdx + 1] = updatedNext
        }

        val updatedDays = syncDaysFromPeriods(periodsList, plan.days)
        return plan.copy(periods = periodsList, days = updatedDays)
    }

    /**
     * Modifica orario di inizio o fine di un periodo (da TimePicker).
     * Non permette all'orario di inizio di superare o penetrare l'orario di fine del periodo anteriore.
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

        val p = periodsList[targetIdx]
        val bounds = getPeriodBounds(plan, periodNumber)

        var validHour = newHour
        var validMinute = newMinute

        val updatedTarget = if (isStart) {
            if (bounds.hasAnterior && bounds.anteriorEndHour != null && bounds.anteriorEndMinute != null) {
                if (p.startDayOfWeek == bounds.anteriorEndDayOfWeek) {
                    val minTotal = bounds.anteriorEndHour * 60 + bounds.anteriorEndMinute
                    val newTotal = validHour * 60 + validMinute
                    if (newTotal < minTotal) {
                        validHour = bounds.anteriorEndHour
                        validMinute = bounds.anteriorEndMinute
                    }
                }
            }
            val endTotal = (validHour * 60 + validMinute + p.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
            p.copy(
                startHour = validHour,
                startMinute = validMinute,
                endHour = endTotal / 60,
                endMinute = endTotal % 60
            )
        } else {
            if (bounds.hasNext && !cascadeNext && bounds.nextStartHour != null && bounds.nextStartMinute != null) {
                if (p.endDayOfWeek == bounds.nextStartDayOfWeek) {
                    val maxTotal = bounds.nextStartHour * 60 + bounds.nextStartMinute
                    val newTotal = validHour * 60 + validMinute
                    if (newTotal > maxTotal) {
                        validHour = bounds.nextStartHour
                        validMinute = bounds.nextStartMinute
                    }
                }
            }
            var diff = (validHour * 60 + validMinute) - (p.startHour * 60 + p.startMinute)
            if (diff <= 0) diff += 24 * 60
            val newDuration = diff.coerceIn(60, 24 * 60)
            p.copy(
                endHour = validHour,
                endMinute = validMinute,
                fastingDurationMinutes = newDuration
            )
        }
        periodsList[targetIdx] = updatedTarget

        if (cascadeNext && targetIdx + 1 < periodsList.size) {
            val nextP = periodsList[targetIdx + 1]
            val nextStartH = (updatedTarget.endHour + 1) % 24
            val nextStartM = updatedTarget.endMinute
            val nextEndTotal = (nextStartH * 60 + nextStartM + nextP.fastingDurationMinutes + 24 * 60 * 10) % (24 * 60)
            val updatedNext = nextP.copy(
                startHour = nextStartH,
                startMinute = nextStartM,
                endHour = nextEndTotal / 60,
                endMinute = nextEndTotal % 60
            )
            periodsList[targetIdx + 1] = updatedNext
        }

        val updatedDays = syncDaysFromPeriods(periodsList, plan.days)
        return plan.copy(periods = periodsList, days = updatedDays)
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

        val (startDay, startH, startM) = if (lastP != null) {
            val sDay = if (lastP.endHour + 1 >= 24) (if (lastP.endDayOfWeek >= 7) 1 else lastP.endDayOfWeek + 1) else lastP.endDayOfWeek
            Triple(sDay, (lastP.endHour + 1) % 24, lastP.endMinute)
        } else {
            Triple(1, 20, 0)
        }

        val duration = 16 * 60
        val endTotal = (startH * 60 + startM + duration) % (24 * 60)
        val endDay = if ((startH * 60 + startM + duration) >= 24 * 60) (if (startDay >= 7) 1 else startDay + 1) else startDay

        val newP = FastingPeriod(
            id = "p_$newNumber",
            periodNumber = newNumber,
            startDayOfWeek = startDay,
            startHour = startH,
            startMinute = startM,
            endDayOfWeek = endDay,
            endHour = endTotal / 60,
            endMinute = endTotal % 60,
            fastingDurationMinutes = duration,
            isFasting = true
        )
        currentPeriods.add(newP)

        val updatedDays = syncDaysFromPeriods(currentPeriods, plan.days)
        return plan.copy(periods = currentPeriods, days = updatedDays)
    }

    /**
     * Sincronizza la lista dei 7 giorni (DayPlan) a partire dalla lista dei periodi,
     * garantendo piena retrocompatibilità con i componenti che leggono ancora plan.days.
     */
    /**
     * Sincronizza la lista dei 7 giorni (DayPlan) a partire dalla lista dei periodi,
     * garantendo piena retrocompatibilità con i componenti che leggono ancora plan.days.
     */
    private fun syncDaysFromPeriods(periods: List<FastingPeriod>, existingDays: List<DayPlan>): List<DayPlan> {
        return (1..7).map { dayOfWeek ->
            val periodStartingToday = periods.find { it.startDayOfWeek == dayOfWeek }
            val existing = existingDays.find { it.dayOfWeek == dayOfWeek }

            if (periodStartingToday != null) {
                DayPlan(
                    dayOfWeek = dayOfWeek,
                    isFastingDay = periodStartingToday.isFasting,
                    startHour = periodStartingToday.startHour,
                    startMinute = periodStartingToday.startMinute,
                    endHour = periodStartingToday.endHour,
                    endMinute = periodStartingToday.endMinute,
                    fastingDurationMinutes = periodStartingToday.fastingDurationMinutes
                )
            } else if (existing != null) {
                existing.copy(isFastingDay = false)
            } else {
                DayPlan(dayOfWeek, false, 20, 0, 12, 0, 0)
            }
        }
    }

    /**
     * Sposta un singolo giorno di deltaHours e deltaMinutes,
     * mantenendo inalterata la durata del digiuno.
     * Con cascadeNextDay = false (predefinito), il giorno successivo NON subisce modifiche.
     */
    fun shiftDay(
        plan: FastingPlan,
        dayOfWeek: Int,
        deltaHours: Int,
        deltaMinutes: Int = 0,
        cascadeNextDay: Boolean = false
    ): FastingPlan {
        if (!plan.periods.isNullOrEmpty()) {
            val matchingPeriod = plan.periods.find { it.startDayOfWeek == dayOfWeek }
            if (matchingPeriod != null) {
                return shiftPeriod(plan, matchingPeriod.periodNumber, deltaHours, deltaMinutes, cascadeNextDay)
            }
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
     * Modifica la durata del digiuno per un singolo giorno,
     * incrementando o decrementando i minuti di digiuno (e spostando l'orario di fine).
     * Con cascadeNextDay = false (predefinito), il giorno successivo NON subisce modifiche.
     */
    fun adjustDuration(
        plan: FastingPlan,
        dayOfWeek: Int,
        deltaMinutes: Int,
        cascadeNextDay: Boolean = false
    ): FastingPlan {
        if (!plan.periods.isNullOrEmpty()) {
            val matchingPeriod = plan.periods.find { it.startDayOfWeek == dayOfWeek }
            if (matchingPeriod != null) {
                return adjustPeriodDuration(plan, matchingPeriod.periodNumber, deltaMinutes, cascadeNextDay)
            }
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
     * Modifica la durata del digiuno per tutti i giorni,
     * incrementando o decrementando i minuti di digiuno.
     */
    fun adjustDurationAllDays(plan: FastingPlan, deltaMinutes: Int): FastingPlan {
        val currentPeriods = plan.getEffectivePeriods()
        val updatedPeriods = currentPeriods.map { p ->
            val newDuration = (p.fastingDurationMinutes + deltaMinutes).coerceIn(60, 24 * 60)
            val actualDelta = newDuration - p.fastingDurationMinutes
            val totalEndMinutes = (p.endHour * 60 + p.endMinute + actualDelta + 24 * 60 * 10) % (24 * 60)
            p.copy(
                endHour = totalEndMinutes / 60,
                endMinute = totalEndMinutes % 60,
                fastingDurationMinutes = newDuration
            )
        }
        val updatedDays = plan.days.map { day ->
            val newDuration = (day.fastingDurationMinutes + deltaMinutes).coerceIn(60, 24 * 60)
            val actualDelta = newDuration - day.fastingDurationMinutes
            val totalEndMinutes = (day.endHour * 60 + day.endMinute + actualDelta + 24 * 60 * 10) % (24 * 60)
            day.copy(
                endHour = totalEndMinutes / 60,
                endMinute = totalEndMinutes % 60,
                fastingDurationMinutes = newDuration
            )
        }
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
