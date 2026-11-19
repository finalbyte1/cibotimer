package com.cibotimer.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLanguageManager {

    const val PREF_KEY_LANGUAGE = "pref_app_language"
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_IT = "it"
    const val LANGUAGE_EN = "en"

    val SUPPORTED_LANGUAGES = listOf(LANGUAGE_IT, LANGUAGE_EN)

    /**
     * Imposta la lingua dell'applicazione per-app tramite AppCompatDelegate.
     * Aggiorna anche Locale.setDefault() e la configurazione delle risorse globali
     * per garantire che tutti i getter di modelli, singletons e UI recepiscano istantaneamente il cambio.
     */
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("cibotimer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_LANGUAGE, languageCode).apply()

        val effectiveLocale = getEffectiveLocale(context)
        Locale.setDefault(effectiveLocale)

        val appLocales = when (languageCode) {
            LANGUAGE_IT -> LocaleListCompat.forLanguageTags(LANGUAGE_IT)
            LANGUAGE_EN -> LocaleListCompat.forLanguageTags(LANGUAGE_EN)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(appLocales)

        try {
            val res = context.resources
            val config = res.configuration
            config.setLocale(effectiveLocale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)

            val appRes = context.applicationContext.resources
            val appConfig = appRes.configuration
            appConfig.setLocale(effectiveLocale)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * Applica la lingua salvata a runtime all'avvio dell'applicazione.
     */
    fun applyStoredLanguage(context: Context) {
        val effectiveLocale = getEffectiveLocale(context)
        Locale.setDefault(effectiveLocale)
        try {
            val res = context.resources
            val config = res.configuration
            config.setLocale(effectiveLocale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)

            val appRes = context.applicationContext.resources
            val appConfig = appRes.configuration
            appConfig.setLocale(effectiveLocale)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * Ritorna il codice lingua attualmente salvato ("system", "it", "en").
     */
    fun getStoredLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("cibotimer_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_LANGUAGE, null)
        if (saved != null) return saved

        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]?.language ?: LANGUAGE_SYSTEM
        } else {
            LANGUAGE_SYSTEM
        }
    }

    /**
     * Risolve il Locale effettivo da utilizzare:
     * - Se impostato su "it" -> Locale.ITALIAN
     * - Se impostato su "en" -> Locale.ENGLISH
     * - Se impostato su "system": usa la lingua di sistema se supportata ("it"), altrimenti fallback a inglese ("en").
     */
    fun getEffectiveLocale(context: Context? = null, overrideSystemLocale: Locale? = null): Locale {
        val stored = if (context != null) getStoredLanguage(context) else {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            if (!appLocales.isEmpty) appLocales[0]?.language ?: LANGUAGE_SYSTEM else LANGUAGE_SYSTEM
        }
        return when (stored) {
            LANGUAGE_IT -> Locale.ITALIAN
            LANGUAGE_EN -> Locale.ENGLISH
            else -> {
                val sys = overrideSystemLocale ?: context?.let {
                    try {
                        it.resources.configuration.locales[0]
                    } catch (_: Throwable) {
                        Locale.getDefault()
                    }
                } ?: Locale.getDefault()
                if (sys.language == "it") Locale.ITALIAN else Locale.ENGLISH
            }
        }
    }

    /**
     * Crea un Context configurato con il Locale effettivo selezionato.
     * Utile per Foreground Service, Widget e Notifiche Broadcast.
     */
    fun getLocalizedContext(baseContext: Context): Context {
        val targetLocale = getEffectiveLocale(baseContext)
        val config = baseContext.resources.configuration
        config.setLocale(targetLocale)
        return baseContext.createConfigurationContext(config)
    }
}
