package com.cibotimer.domain

import com.cibotimer.data.models.FastingDayFormatter
import com.cibotimer.data.models.FastingPeriod

/**
 * Rappresenta un intervallo di digiuno continuo su un asse temporale settimanale lineare.
 * @param start Minuti dall'inizio della settimana (Lunedì 00:00 = 0).
 * @param end Minuti dall'inizio della settimana (end = start + duration). Può superare 10080 per digiuni a cavallo di fine settimana.
 */
data class Span(val start: Int, val end: Int) {
    val durationMinutes: Int get() = (end - start).coerceAtLeast(0)
}

/**
 * Converte un FastingPeriod in uno Span lineare.
 * giorno = ((startDayOfWeek - 1) % 7 + 7) % 7
 * start = giorno * 1440 + startHour * 60 + startMinute
 * end = start + fastingDurationMinutes
 */
fun FastingPeriod.toSpan(): Span {
    val giorno = ((startDayOfWeek - 1) % 7 + 7) % 7
    val start = giorno * 1440 + startHour * 60 + startMinute
    val duration = fastingDurationMinutes.coerceAtLeast(0)
    return Span(start, start + duration)
}

/**
 * Converte uno Span in un FastingPeriod con indici giorno rigorosamente 1..7 (Lun..Dom).
 */
fun Span.toPeriod(periodNumber: Int = 1, id: String = "p$periodNumber"): FastingPeriod =
    WeekSpanModel.run { toPeriod(periodNumber, id) }

data class SpanBounds(
    val minStart: Int,
    val maxEnd: Int,
    val prevEndDayOfWeek: Int,
    val prevEndHour: Int,
    val prevEndMinute: Int,
    val nextStartDayOfWeek: Int,
    val nextStartHour: Int,
    val nextStartMinute: Int,
    val gapBeforeMinutes: Int,
    val gapAfterMinutes: Int,
    val prevIndex: Int = 0,
    val nextIndex: Int = 0
)

object WeekSpanModel {
    const val MINUTES_PER_DAY = 1440
    const val DAYS_PER_WEEK = 7
    const val WEEK_MINUTES = DAYS_PER_WEEK * MINUTES_PER_DAY // 10080

    /**
     * Converte uno Span in un FastingPeriod con indici giorno rigorosamente 1..7 (Lun..Dom).
     */
    fun Span.toPeriod(periodNumber: Int = 1, id: String = "p$periodNumber"): FastingPeriod {
        val dur = durationMinutes.coerceIn(60, WEEK_MINUTES)
        val normStart = ((start % WEEK_MINUTES) + WEEK_MINUTES) % WEEK_MINUTES
        val startDayOfWeek = (normStart / MINUTES_PER_DAY) + 1
        val startH = (normStart % MINUTES_PER_DAY) / 60
        val startM = (normStart % MINUTES_PER_DAY) % 60

        val endAbsolute = normStart + dur
        val endDayOfWeek = (((endAbsolute / MINUTES_PER_DAY) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK) + 1
        val normEnd = ((endAbsolute % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val endH = normEnd / 60
        val endM = normEnd % 60

        return FastingPeriod(
            id = id,
            periodNumber = periodNumber,
            startDayOfWeek = startDayOfWeek,
            startHour = startH,
            startMinute = startM,
            endDayOfWeek = endDayOfWeek,
            endHour = endH,
            endMinute = endM,
            fastingDurationMinutes = dur,
            isFasting = true
        )
    }

    /**
     * Ripara eventuali conflitti o sovrapposizioni tra periodi consecutivi:
     * se un periodo inizia prima della fine del precedente, sposta start del periodo successivo a prev.end;
     * se la durata scende sotto 60 min, tienila a 60.
     * Restituisce la lista di Span riparati e un flag indicante se è stata apportata una correzione.
     */
    fun repairOverlaps(spans: List<Span>): Pair<List<Span>, Boolean> {
        if (spans.isEmpty()) return Pair(spans, false)

        val normalized = normalize(spans)
        var repaired = normalized.size != spans.size

        val result = normalized.map { s ->
            val modStart = ((s.start % WEEK_MINUTES) + WEEK_MINUTES) % WEEK_MINUTES
            val dur = s.durationMinutes.coerceIn(60, WEEK_MINUTES)
            Span(modStart, modStart + dur)
        }.toMutableList()

        // 1. Ripara sovrapposizioni interne (i da 1 a n-1)
        for (i in 1 until result.size) {
            val prevEnd = result[i - 1].end
            if (result[i].start < prevEnd) {
                val newStart = prevEnd
                val currentEnd = result[i].end
                val newEnd = if (currentEnd - newStart < 60) newStart + 60 else currentEnd
                result[i] = Span(newStart, newEnd)
                repaired = true
            }
        }

        // 2. Ripara wrap ciclico tra ultimo e primo (indice 0 rispetto all'ultimo)
        val n = result.size
        if (n > 1) {
            val lastEffectiveEndAt0 = result[n - 1].end - WEEK_MINUTES
            if (result[0].start < lastEffectiveEndAt0) {
                val newStart = lastEffectiveEndAt0
                val currentEnd = result[0].end
                val newEnd = if (currentEnd - newStart < 60) newStart + 60 else currentEnd
                result[0] = Span(newStart, newEnd)
                repaired = true

                // Propaga se spostando result[0] si genera ora sovrapposizione con i periodi successivi
                for (i in 1 until n) {
                    val prevEnd = result[i - 1].end
                    if (result[i].start < prevEnd) {
                        val s = prevEnd
                        val e = if (result[i].end - s < 60) s + 60 else result[i].end
                        result[i] = Span(s, e)
                    } else {
                        break
                    }
                }
            }
        }

        return Pair(result, repaired)
    }

    /**
     * Normalizza la lista di Span: ordina per inizio, unisce duplicati e sovrapposizioni
     * eliminando i periodi fantasma o ridondanti e trattando la settimana come circolare.
     */
    fun normalize(spans: List<Span>): List<Span> {
        if (spans.isEmpty()) return emptyList()

        // 1. Porta lo start di ogni span nel range principale [0, 10080)
        val valid = spans.mapNotNull { s ->
            val dur = s.end - s.start
            if (dur <= 0) null
            else {
                val modStart = ((s.start % WEEK_MINUTES) + WEEK_MINUTES) % WEEK_MINUTES
                Span(modStart, modStart + dur)
            }
        }
        if (valid.isEmpty()) return emptyList()

        // 2. Ordina per start crescente
        val sorted = valid.sortedWith(compareBy({ it.start }, { it.end }))

        // 3. Elimina duplicati e unisci sovrapposizioni identiche o contenute
        val result = mutableListOf<Span>()
        for (span in sorted) {
            if (result.isEmpty()) {
                result.add(span)
            } else {
                val last = result.last()
                if (span.start == last.start && span.end == last.end) {
                    continue // Duplicato esatto
                } else if (span.start >= last.start && span.end <= last.end) {
                    continue // Completamente contenuto all'interno
                } else if (span.start < last.end) {
                    // Sovrapposizione parziale: unione
                    result[result.size - 1] = Span(last.start, maxOf(last.end, span.end))
                } else {
                    result.add(span)
                }
            }
        }

        // 4. CIRCOLARITÀ SETTIMANALE: Unisci i periodi spezzati a mezzanotte (Domenica 24:00 = Lunedì 00:00).
        // Se l'ultimo periodo (Domenica) raggiunge o supera la fine della settimana (>= 10080)
        // e il primo periodo (Lunedì) inizia esattamente a mezzanotte (start == 0):
        while (result.size > 1) {
            val first = result.first()
            val last = result.last()
            if (first.start == 0 && last.end >= WEEK_MINUTES) {
                // Toccamento o sovrapposizione ciclica attraverso mezzanotte Dom/Lun: unisci in un unico periodo continuo
                val mergedEnd = maxOf(last.end, WEEK_MINUTES + first.end)
                result[result.size - 1] = Span(last.start, mergedEnd)
                result.removeAt(0)
            } else {
                break
            }
        }

        return result
    }

    /**
     * Calcola le parti di digiuno in minuti locali 0..1440 per il giorno d (0=Lun..6=Dom),
     * considerando anche le copie traslate di -10080 e +10080 per il wrap ciclico della settimana.
     */
    fun fastsInDay(spans: List<Span>, d: Int): List<Pair<Int, Int>> {
        val dayIndex0 = ((d % DAYS_PER_WEEK) + DAYS_PER_WEEK) % DAYS_PER_WEEK // 0=Lun..6=Dom
        val dayStart = dayIndex0 * MINUTES_PER_DAY
        val dayEnd = dayStart + MINUTES_PER_DAY

        val raw = mutableListOf<Pair<Int, Int>>()
        for (offset in listOf(-WEEK_MINUTES, 0, WEEK_MINUTES)) {
            for (span in spans) {
                val s = span.start + offset
                val e = span.end + offset
                val overlapStart = maxOf(s, dayStart)
                val overlapEnd = minOf(e, dayEnd)
                if (overlapEnd > overlapStart) {
                    raw.add(Pair(overlapStart - dayStart, overlapEnd - dayStart))
                }
            }
        }
        if (raw.isEmpty()) return emptyList()

        raw.sortBy { it.first }
        val merged = mutableListOf<Pair<Int, Int>>()
        for (interval in raw) {
            if (merged.isEmpty()) {
                merged.add(interval)
            } else {
                val last = merged.last()
                if (interval.first <= last.second) {
                    merged[merged.size - 1] = Pair(last.first, maxOf(last.second, interval.second))
                } else {
                    merged.add(interval)
                }
            }
        }
        return merged
    }

    /**
     * Calcola i limiti temporali rispetto ai vicini (anteriore e successivo)
     * in modo rigorosamente ciclico (primo e ultimo con ±10080).
     */
    fun bounds(spans: List<Span>, i: Int): SpanBounds {
        if (spans.isEmpty() || i !in spans.indices) {
            return SpanBounds(
                minStart = 0,
                maxEnd = WEEK_MINUTES,
                prevEndDayOfWeek = 1,
                prevEndHour = 0,
                prevEndMinute = 0,
                nextStartDayOfWeek = 1,
                nextStartHour = 0,
                nextStartMinute = 0,
                gapBeforeMinutes = Int.MAX_VALUE,
                gapAfterMinutes = Int.MAX_VALUE,
                prevIndex = 0,
                nextIndex = 0
            )
        }
        val n = spans.size
        if (n == 1) {
            val cur = spans[0]
            val prevDay0 = (((cur.end / MINUTES_PER_DAY) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK)
            val nextDay0 = (((cur.start / MINUTES_PER_DAY) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK)
            return SpanBounds(
                minStart = cur.end - WEEK_MINUTES,
                maxEnd = cur.start + WEEK_MINUTES,
                prevEndDayOfWeek = prevDay0 + 1,
                prevEndHour = (cur.end % MINUTES_PER_DAY) / 60,
                prevEndMinute = (cur.end % MINUTES_PER_DAY) % 60,
                nextStartDayOfWeek = nextDay0 + 1,
                nextStartHour = (cur.start % MINUTES_PER_DAY) / 60,
                nextStartMinute = (cur.start % MINUTES_PER_DAY) % 60,
                gapBeforeMinutes = Int.MAX_VALUE,
                gapAfterMinutes = Int.MAX_VALUE,
                prevIndex = 0,
                nextIndex = 0
            )
        }

        val order = spans.indices.sortedBy { spans[it].start }
        val pos = order.indexOf(i)
        val prevIndex = order[(pos - 1 + n) % n]
        val nextIndex = order[(pos + 1) % n]
        val prevOffset = if (pos == 0) -WEEK_MINUTES else 0
        val nextOffset = if (pos == n - 1) WEEK_MINUTES else 0

        val cur = spans[i]
        val prev = spans[prevIndex]
        val next = spans[nextIndex]

        val prevEffectiveEnd = prev.end + prevOffset
        val nextEffectiveStart = next.start + nextOffset

        val gapBefore = maxOf(0, cur.start - prevEffectiveEnd)
        val gapAfter = maxOf(0, nextEffectiveStart - cur.end)

        val normPrevEnd = ((prev.end % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val prevEndDayOfWeek = (((prev.end / MINUTES_PER_DAY) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK) + 1
        val prevEndHour = normPrevEnd / 60
        val prevEndMinute = normPrevEnd % 60

        val normNextStart = ((next.start % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val nextStartDayOfWeek = (((next.start / MINUTES_PER_DAY) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK) + 1
        val nextStartHour = normNextStart / 60
        val nextStartMinute = normNextStart % 60

        return SpanBounds(
            minStart = prevEffectiveEnd,
            maxEnd = nextEffectiveStart,
            prevEndDayOfWeek = prevEndDayOfWeek,
            prevEndHour = prevEndHour,
            prevEndMinute = prevEndMinute,
            nextStartDayOfWeek = nextStartDayOfWeek,
            nextStartHour = nextStartHour,
            nextStartMinute = nextStartMinute,
            gapBeforeMinutes = gapBefore,
            gapAfterMinutes = gapAfter,
            prevIndex = prevIndex,
            nextIndex = nextIndex
        )
    }
}
