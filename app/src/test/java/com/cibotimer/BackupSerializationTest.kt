package com.cibotimer

import com.cibotimer.data.models.*
import com.cibotimer.domain.FastingPlanTemplates
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun testBackupDataRoundtripSerialization() {
        val backupData = BackupData(
            version = 1,
            exportTimestampEpochMillis = 1700000000000L,
            profile = UserProfile(
                name = "Mario Rossi",
                startingWeightKg = 82.5,
                currentWeightKg = 78.0,
                targetWeightKg = 72.0,
                heightCm = 178.0,
                waterTargetMl = 2500,
                notificationsFastingEnabled = true,
                notificationsStagesEnabled = true,
                notificationsWaterEnabled = false,
                googleDriveAutoBackupEnabled = true,
                googleDriveAccountEmail = "mario.rossi@gmail.com"
            ),
            activePlan = FastingPlanTemplates.PLAN_16_8,
            customPlans = listOf(FastingPlanTemplates.PLAN_18_6),
            sessions = listOf(
                FastingSession(
                    id = "sess-1",
                    planId = "16-8",
                    planName = "16-8",
                    startTimeEpochMillis = 1700000000000L,
                    plannedEndTimeEpochMillis = 1700000000000L + (16 * 3600 * 1000L),
                    actualEndTimeEpochMillis = 1700000000000L + (16 * 3600 * 1000L),
                    targetDurationMinutes = 960,
                    state = FastingState.EATING
                )
            ),
            waterEntries = listOf(
                WaterEntry(id = "w-1", epochMillis = 1700000000000L, amountMl = 250),
                WaterEntry(id = "w-2", epochMillis = 1700003600000L, amountMl = 500)
            ),
            weightEntries = listOf(
                WeightEntry(id = "wg-1", epochMillis = 1699000000000L, weightKg = 82.5, waistCm = 92.0),
                WeightEntry(id = "wg-2", epochMillis = 1700000000000L, weightKg = 78.0, waistCm = 89.0)
            ),
            trophies = listOf(
                Trophy(id = "first_fast", title = "Primo digiuno", description = "Completato", iconName = "trophy", currentProgress = 1, maxProgress = 1, isUnlocked = true)
            ),
            aiConfig = AiConfig(
                isEnabled = true,
                apiKey = "sk-test123456",
                baseUrl = "https://api.openai.com/v1",
                modelName = "gpt-4o-mini"
            ),
            chatHistory = listOf(
                ChatMessage(id = "c-1", sender = MessageSender.USER, text = "Ciao Coach!", timestampEpochMillis = 1700000000000L),
                ChatMessage(id = "c-2", sender = MessageSender.AI, text = "Ciao! Come posso aiutarti oggi?", timestampEpochMillis = 1700000002000L)
            )
        )

        val jsonString = json.encodeToString(backupData)
        assertNotNull(jsonString)

        val restored = json.decodeFromString<BackupData>(jsonString)

        assertEquals(backupData.version, restored.version)
        assertEquals(backupData.profile.name, restored.profile.name)
        assertEquals(backupData.profile.startingWeightKg, restored.profile.startingWeightKg, 0.001)
        assertEquals(backupData.profile.currentWeightKg, restored.profile.currentWeightKg, 0.001)
        assertEquals(backupData.activePlan.id, restored.activePlan.id)
        assertEquals(1, restored.sessions.size)
        assertEquals(2, restored.waterEntries.size)
        assertEquals(2, restored.weightEntries.size)
        assertEquals(1, restored.trophies.size)
        assertEquals(true, restored.aiConfig.isEnabled)
        assertEquals("gpt-4o-mini", restored.aiConfig.modelName)
        assertEquals(2, restored.chatHistory.size)
    }

    @Test
    fun testDefaultResetBackupState() {
        val defaultProfile = UserProfile()
        assertEquals("Utente", defaultProfile.name)
        assertEquals(75.0, defaultProfile.startingWeightKg, 0.001)
        assertEquals(70.0, defaultProfile.targetWeightKg, 0.001)
        assertEquals(false, defaultProfile.googleDriveAutoBackupEnabled)

        val cleanBackup = BackupData(
            profile = defaultProfile,
            activePlan = FastingPlanTemplates.PLAN_LUIGI_1
        )
        val jsonString = json.encodeToString(cleanBackup)
        val decoded = json.decodeFromString<BackupData>(jsonString)
        assertEquals("Utente", decoded.profile.name)
        assertEquals(FastingPlanTemplates.PLAN_LUIGI_1.id, decoded.activePlan.id)
        assertEquals(0, decoded.sessions.size)
        assertEquals(0, decoded.waterEntries.size)
        assertEquals(0, decoded.weightEntries.size)
    }
}
