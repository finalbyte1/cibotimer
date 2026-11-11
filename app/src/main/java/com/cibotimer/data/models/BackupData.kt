package com.cibotimer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String = "Utente",
    val startingWeightKg: Double = 75.0,
    val currentWeightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val waterTargetMl: Int = 2000,
    val notificationsFastingEnabled: Boolean = true,
    val notificationsStagesEnabled: Boolean = true,
    val notificationsWaterEnabled: Boolean = true,
    val notificationsWeighInEnabled: Boolean = true,
    val googleDriveAutoBackupEnabled: Boolean = false,
    val googleDriveAccountEmail: String = "",
    val googleDriveFolderId: String = "",
    val hasSelectedInitialPlan: Boolean = false
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportTimestampEpochMillis: Long = System.currentTimeMillis(),
    val profile: UserProfile,
    val activePlan: FastingPlan,
    val customPlans: List<FastingPlan> = emptyList(),
    val sessions: List<FastingSession> = emptyList(),
    val waterEntries: List<WaterEntry> = emptyList(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val trophies: List<Trophy> = emptyList(),
    val aiConfig: AiConfig = AiConfig(),
    val chatHistory: List<ChatMessage> = emptyList(),
    val appNotifications: List<AppNotificationItem> = emptyList()
)
