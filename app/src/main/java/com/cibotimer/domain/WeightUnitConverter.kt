package com.cibotimer.domain

import com.cibotimer.data.models.WeightUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

object WeightUnitConverter {
    const val KG_TO_LB = 2.20462262

    // Limiti di validazione fisiologici accettabili (25.0 kg - 350.0 kg o equivalente in lb)
    const val MIN_WEIGHT_KG = 25.0
    const val MAX_WEIGHT_KG = 350.0

    /**
     * Ritorna l'unità di peso predefinita in base al Paese del Locale fornito.
     * LB per US (Stati Uniti), LR (Liberia), MM (Myanmar); KG per tutti gli altri.
     */
    fun getDefaultWeightUnitForLocale(locale: Locale = Locale.getDefault()): WeightUnit {
        val country = locale.country.uppercase()
        return if (country == "US" || country == "LR" || country == "MM") {
            WeightUnit.LB
        } else {
            WeightUnit.KG
        }
    }

    /**
     * Converte un valore memorizzato in kg nel valore da mostrare nell'unità specificata.
     * NON esegue arrotondamenti per preservare la massima precisione numerica.
     */
    fun kgToDisplay(kg: Double, unit: WeightUnit): Double {
        return if (unit == WeightUnit.LB) kg * KG_TO_LB else kg
    }

    /**
     * Converte un valore inserito dall'utente nell'unità specificata nel valore equivalente in kg.
     * NON esegue arrotondamenti per non degradare la precisione dei dati salvati.
     */
    fun displayToKg(value: Double, unit: WeightUnit): Double {
        return if (unit == WeightUnit.LB) value / KG_TO_LB else value
    }

    /**
     * Converte in display e arrotonda a 1 cifra decimale.
     */
    fun kgToDisplayRounded(kg: Double, unit: WeightUnit): Double {
        val displayVal = kgToDisplay(kg, unit)
        return (displayVal * 10.0).roundToInt() / 10.0
    }

    /**
     * Formatta il valore di peso partendo dal valore memorizzato in kg.
     * Arrotonda a 1 decimale e usa il separatore decimale del Locale corrente.
     *
     * @param kg Valore in kg da convertire e formattare.
     * @param unit Unità desiderata (KG o LB).
     * @param showUnit Se true appende il simbolo dell'unità (es. "75,2 kg" o "165,8 lb").
     * @param locale Locale per la separazione decimale.
     */
    fun formatWeight(
        kg: Double,
        unit: WeightUnit,
        showUnit: Boolean = true,
        locale: Locale = Locale.getDefault()
    ): String {
        val displayVal = kgToDisplayRounded(kg, unit)
        val symbols = DecimalFormatSymbols(locale)
        val df = DecimalFormat("0.0", symbols)
        val formattedNumber = df.format(displayVal)
        return if (showUnit) "$formattedNumber ${unit.symbol}" else formattedNumber
    }

    /**
     * Formatta una variazione delta di peso con segno esplicito (+ / -).
     * Esempi: "-1,5 kg", "+2,2 lb", "+0,0 kg".
     */
    fun formatDelta(
        deltaKg: Double,
        unit: WeightUnit,
        showUnit: Boolean = true,
        locale: Locale = Locale.getDefault()
    ): String {
        val displayVal = kgToDisplayRounded(deltaKg, unit)
        val symbols = DecimalFormatSymbols(locale)
        val df = DecimalFormat("+0.0;-0.0", symbols)
        val formattedNumber = df.format(displayVal)
        return if (showUnit) "$formattedNumber ${unit.symbol}" else formattedNumber
    }

    /**
     * Valida se un valore inserito dall'utente (espresso nell'unità di visualizzazione)
     * ricade nei limiti accettabili (25 - 350 kg o equivalente).
     */
    fun isValidWeight(valueInDisplayUnit: Double, unit: WeightUnit): Boolean {
        val kg = displayToKg(valueInDisplayUnit, unit)
        return kg in MIN_WEIGHT_KG..MAX_WEIGHT_KG
    }

    /**
     * Ritorna i limiti formattati per la validazione nell'unità specificata.
     */
    fun getFormattedBounds(unit: WeightUnit, locale: Locale = Locale.getDefault()): Pair<String, String> {
        val minStr = formatWeight(MIN_WEIGHT_KG, unit, showUnit = true, locale = locale)
        val maxStr = formatWeight(MAX_WEIGHT_KG, unit, showUnit = true, locale = locale)
        return Pair(minStr, maxStr)
    }
}
