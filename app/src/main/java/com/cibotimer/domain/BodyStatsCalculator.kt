package com.cibotimer.domain

import com.cibotimer.data.models.WeightEntry
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeightProgressInfo(
    val startingWeightKg: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val lostKg: Double,
    val remainingKg: Double,
    val totalGoalDeltaKg: Double,
    val progressFraction: Float,
    val progressPercent: Int,
    val isGoalReached: Boolean,
    val isWeightLoss: Boolean
)

data class WhrResult(
    val ratio: Double,
    val category: String,
    val colorHex: Long,
    val description: String
)

data class CircumferenceMetric(
    val id: String,
    val label: String,
    val currentCm: Double?,
    val initialCm: Double?,
    val diffCm: Double?,
    val lastUpdatedMillis: Long?
)

data class TotalCircumferenceInfo(
    val currentTotalCm: Double,
    val initialTotalCm: Double?,
    val totalDiffCm: Double?
)

object BodyStatsCalculator {

    /**
     * Calcola l'avanzamento verso l'obiettivo di peso, i kg persi/guadagnati, i kg rimanenti
     * e la frazione percentuale di completamento.
     */
    fun calculateWeightProgress(
        startingWeightKg: Double,
        currentWeightKg: Double,
        targetWeightKg: Double
    ): WeightProgressInfo {
        val isWeightLoss = startingWeightKg >= targetWeightKg
        val totalDelta = abs(startingWeightKg - targetWeightKg)
        val lostKg = if (isWeightLoss) {
            (startingWeightKg - currentWeightKg * 10.0).roundToInt() / 10.0 // prevent IEEE 754 precision issues
            startingWeightKg - currentWeightKg
        } else {
            currentWeightKg - startingWeightKg
        }

        val roundedLostKg = (lostKg * 10.0).roundToInt() / 10.0
        val remainingKg = if (isWeightLoss) {
            (currentWeightKg - targetWeightKg).coerceAtLeast(0.0)
        } else {
            (targetWeightKg - currentWeightKg).coerceAtLeast(0.0)
        }
        val roundedRemainingKg = (remainingKg * 10.0).roundToInt() / 10.0

        val isGoalReached = if (isWeightLoss) {
            currentWeightKg <= targetWeightKg
        } else {
            currentWeightKg >= targetWeightKg
        }

        val progressFraction = if (totalDelta > 0.0) {
            if (isGoalReached) 1f
            else (lostKg / totalDelta).toFloat().coerceIn(0f, 1f)
        } else {
            if (isGoalReached) 1f else 0f
        }

        val progressPercent = (progressFraction * 100f).roundToInt()

        return WeightProgressInfo(
            startingWeightKg = (startingWeightKg * 10.0).roundToInt() / 10.0,
            currentWeightKg = (currentWeightKg * 10.0).roundToInt() / 10.0,
            targetWeightKg = (targetWeightKg * 10.0).roundToInt() / 10.0,
            lostKg = roundedLostKg,
            remainingKg = roundedRemainingKg,
            totalGoalDeltaKg = (totalDelta * 10.0).roundToInt() / 10.0,
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            isGoalReached = isGoalReached,
            isWeightLoss = isWeightLoss
        )
    }

    /**
     * Rapporto Vita / Fianchi (WHR - Waist to Hip Ratio).
     * Indicatore cardiovascolare e metabolico standard OMS.
     */
    fun calculateWhr(waistCm: Double?, hipsCm: Double?): WhrResult? {
        if (waistCm == null || hipsCm == null || waistCm <= 0.0 || hipsCm <= 0.0) {
            return null
        }
        val ratio = (waistCm / hipsCm * 100.0).roundToInt() / 100.0
        val (category, colorHex, description) = when {
            ratio < 0.85 -> Triple(
                "Basso Rischio (Ottimale)",
                0xFF00E5FF,
                "Distribuzione equilibrata del grasso corporeo secondo i parametri OMS."
            )
            ratio < 0.90 -> Triple(
                "Rischio Moderato",
                0xFFFF9F1C,
                "Leggera concentrazione addominale. Il digiuno intermittente stimola la lipolisi viscerale."
            )
            else -> Triple(
                "Rischio Elevato",
                0xFFEF4444,
                "Elevata concentrazione di grasso viscerale. Continua con regolarità per ridurre il rischio metabolico."
            )
        }
        return WhrResult(
            ratio = ratio,
            category = category,
            colorHex = colorHex,
            description = description
        )
    }

    /**
     * Estrae le metriche delle 5 circonferenze principali confrontando la misurazione
     * più recente con la prima misurazione disponibile.
     */
    fun calculateCircumferences(entries: List<WeightEntry>): List<CircumferenceMetric> {
        val sortedAsc = entries.sortedBy { it.epochMillis }
        val sortedDesc = entries.sortedByDescending { it.epochMillis }

        fun resolveMetric(id: String, label: String, extractor: (WeightEntry) -> Double?): CircumferenceMetric {
            val latestEntry = sortedDesc.firstOrNull { extractor(it) != null }
            val firstEntry = sortedAsc.firstOrNull { extractor(it) != null }

            val currentVal = latestEntry?.let(extractor)
            val initialVal = firstEntry?.let(extractor)
            val diff = if (currentVal != null && initialVal != null) {
                ((currentVal - initialVal) * 10.0).roundToInt() / 10.0
            } else null

            return CircumferenceMetric(
                id = id,
                label = label,
                currentCm = currentVal,
                initialCm = initialVal,
                diffCm = diff,
                lastUpdatedMillis = latestEntry?.epochMillis
            )
        }

        return listOf(
            resolveMetric("waist", "Vita") { it.waistCm },
            resolveMetric("hips", "Fianchi") { it.hipsCm },
            resolveMetric("chest", "Torace") { it.chestCm },
            resolveMetric("thighs", "Cosce") { it.thighsCm },
            resolveMetric("arms", "Braccia") { it.armsCm }
        )
    }

    /**
     * Calcola la somma delle circonferenze per l'ultima misurazione e la prima misurazione.
     */
    fun calculateTotalCircumference(entries: List<WeightEntry>): TotalCircumferenceInfo? {
        val sortedDesc = entries.sortedByDescending { it.epochMillis }
        val sortedAsc = entries.sortedBy { it.epochMillis }

        val latestWithAny = sortedDesc.firstOrNull {
            it.waistCm != null || it.hipsCm != null || it.chestCm != null || it.thighsCm != null || it.armsCm != null
        } ?: return null

        val firstWithAny = sortedAsc.firstOrNull {
            it.waistCm != null || it.hipsCm != null || it.chestCm != null || it.thighsCm != null || it.armsCm != null
        }

        fun sumEntry(entry: WeightEntry): Double {
            var sum = 0.0
            entry.waistCm?.let { sum += it }
            entry.hipsCm?.let { sum += it }
            entry.chestCm?.let { sum += it }
            entry.thighsCm?.let { sum += it }
            entry.armsCm?.let { sum += it }
            return (sum * 10.0).roundToInt() / 10.0
        }

        val currentTotal = sumEntry(latestWithAny)
        val initialTotal = firstWithAny?.let { sumEntry(it) }
        val diff = if (initialTotal != null) {
            ((currentTotal - initialTotal) * 10.0).roundToInt() / 10.0
        } else null

        return TotalCircumferenceInfo(
            currentTotalCm = currentTotal,
            initialTotalCm = initialTotal,
            totalDiffCm = diff
        )
    }
}
