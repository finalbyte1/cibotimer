package com.cibotimer.notifications

import android.content.Context

data class FastingSavedState(
    val isActive: Boolean,
    val startTime: Long,
    val plannedEndTime: Long,
    val planName: String,
    val isEating: Boolean
)

object FastingPersistence {
    private const val PREFS_NAME = "cibotimer_foreground_service"
    private const val KEY_IS_ACTIVE = "key_is_active"
    private const val KEY_START_TIME = "key_start_time"
    private const val KEY_PLANNED_END_TIME = "key_planned_end_time"
    private const val KEY_PLAN_NAME = "key_plan_name"
    private const val KEY_IS_EATING = "key_is_eating"
    private const val KEY_EXPECTED_RUNNING = "key_expected_running"
    private const val KEY_BATTERY_HINT_SHOWN = "key_battery_hint_shown"

    fun saveState(
        context: Context,
        startTime: Long,
        plannedEndTime: Long,
        planName: String,
        isEating: Boolean,
        isActive: Boolean = true
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ACTIVE, isActive)
            .putLong(KEY_START_TIME, startTime)
            .putLong(KEY_PLANNED_END_TIME, plannedEndTime)
            .putString(KEY_PLAN_NAME, planName)
            .putBoolean(KEY_IS_EATING, isEating)
            .putBoolean(KEY_EXPECTED_RUNNING, isActive)
            .apply()
    }

    fun clearState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ACTIVE, false)
            .putBoolean(KEY_EXPECTED_RUNNING, false)
            .apply()
    }

    fun isExpectedRunning(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXPECTED_RUNNING, false)

    fun setExpectedRunning(context: Context, expected: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPECTED_RUNNING, expected)
            .apply()
    }

    fun isBatteryHintShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_HINT_SHOWN, false)

    fun setBatteryHintShown(context: Context, shown: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BATTERY_HINT_SHOWN, shown)
            .apply()
    }

    fun getState(context: Context): FastingSavedState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FastingSavedState(
            isActive = prefs.getBoolean(KEY_IS_ACTIVE, false),
            startTime = prefs.getLong(KEY_START_TIME, 0L),
            plannedEndTime = prefs.getLong(KEY_PLANNED_END_TIME, 0L),
            planName = prefs.getString(KEY_PLAN_NAME, "Digiuno") ?: "Digiuno",
            isEating = prefs.getBoolean(KEY_IS_EATING, false)
        )
    }
}
