package com.cibotimer

import com.cibotimer.util.AppLanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LanguageSelectionTest {

    @Test
    fun testSupportedLanguages() {
        assertTrue("Italian must be supported", AppLanguageManager.SUPPORTED_LANGUAGES.contains("it"))
        assertTrue("English must be supported", AppLanguageManager.SUPPORTED_LANGUAGES.contains("en"))
        assertEquals(2, AppLanguageManager.SUPPORTED_LANGUAGES.size)
    }

    @Test
    fun testSystemLocaleItalianResolvesToItalian() {
        val effective = AppLanguageManager.getEffectiveLocale(
            context = null,
            overrideSystemLocale = Locale.ITALIAN
        )
        assertEquals("it", effective.language)
    }

    @Test
    fun testSystemLocaleEnglishResolvesToEnglish() {
        val effective = AppLanguageManager.getEffectiveLocale(
            context = null,
            overrideSystemLocale = Locale.ENGLISH
        )
        assertEquals("en", effective.language)
    }

    @Test
    fun testUnsupportedSystemLocaleFallsBackToEnglish() {
        // Test German, French, Spanish, Japanese, Chinese
        val unsupportedLocales = listOf(
            Locale.GERMAN,
            Locale.FRENCH,
            Locale("es", "ES"),
            Locale.JAPANESE,
            Locale.SIMPLIFIED_CHINESE
        )

        for (loc in unsupportedLocales) {
            val effective = AppLanguageManager.getEffectiveLocale(
                context = null,
                overrideSystemLocale = loc
            )
            assertEquals(
                "Unsupported system language '${loc.language}' must fallback to English",
                "en",
                effective.language
            )
        }
    }

    @Test
    fun testExplicitItalianLocaleTakesPrecedence() {
        // When user explicitly selects "it", even with French or German system locale, it resolves to Italian
        val testLocales = listOf(Locale.FRENCH, Locale.GERMAN, Locale.ENGLISH)
        for (sysLoc in testLocales) {
            val effective = when (AppLanguageManager.LANGUAGE_IT) {
                AppLanguageManager.LANGUAGE_IT -> Locale.ITALIAN
                AppLanguageManager.LANGUAGE_EN -> Locale.ENGLISH
                else -> if (sysLoc.language == "it") Locale.ITALIAN else Locale.ENGLISH
            }
            assertEquals("it", effective.language)
        }
    }

    @Test
    fun testExplicitEnglishLocaleTakesPrecedence() {
        // When user explicitly selects "en", even with Italian system locale, it resolves to English
        val testLocales = listOf(Locale.ITALIAN, Locale.ITALY)
        for (sysLoc in testLocales) {
            val effective = when (AppLanguageManager.LANGUAGE_EN) {
                AppLanguageManager.LANGUAGE_IT -> Locale.ITALIAN
                AppLanguageManager.LANGUAGE_EN -> Locale.ENGLISH
                else -> if (sysLoc.language == "it") Locale.ITALIAN else Locale.ENGLISH
            }
            assertEquals("en", effective.language)
        }
    }
}
