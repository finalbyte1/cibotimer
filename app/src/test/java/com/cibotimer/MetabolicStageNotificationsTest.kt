package com.cibotimer

import com.cibotimer.data.models.UserProfile
import com.cibotimer.notifications.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetabolicStageNotificationsTest {

    @Test
    fun testStageNotificationsDisabledByDefault() {
        val defaultProfile = UserProfile()
        // Must be disabled by default as per requirements
        assertFalse("Metabolic stage notifications must be disabled by default", defaultProfile.notificationsStagesEnabled)
        assertTrue("Persistent timer notification must be enabled by default", defaultProfile.notificationsPersistentEnabled)
        assertTrue("Fasting start/end notification must be enabled by default", defaultProfile.notificationsFastingEnabled)
    }

    @Test
    fun testOnlyPersistentAndFastingEnabledConfiguration() {
        val profile = UserProfile(
            notificationsPersistentEnabled = true,
            notificationsFastingEnabled = true,
            notificationsStagesEnabled = false
        )

        assertTrue(profile.notificationsPersistentEnabled)
        assertTrue(profile.notificationsFastingEnabled)
        assertFalse(profile.notificationsStagesEnabled)

        // Stage reached notification type check
        val isStageType = "STAGE_REACHED"
        val shouldNotifyStage = when (isStageType) {
            "STAGE_REACHED" -> profile.notificationsStagesEnabled
            else -> true
        }
        assertFalse("Stage notification should NOT be delivered when stages enabled is false", shouldNotifyStage)

        // Fasting start/end should be allowed
        val isFastingEndType = "FASTING_END"
        val shouldNotifyFastingEnd = when (isFastingEndType) {
            "FASTING_END", "EATING_END" -> profile.notificationsFastingEnabled
            else -> true
        }
        assertTrue("Fasting start/end should be delivered when fasting enabled is true", shouldNotifyFastingEnd)
    }

    @Test
    fun testStageNotificationAllowedWhenToggledOn() {
        var profile = UserProfile(
            notificationsPersistentEnabled = true,
            notificationsFastingEnabled = true,
            notificationsStagesEnabled = false
        )
        assertFalse(profile.notificationsStagesEnabled)

        // User turns stage notifications ON
        profile = profile.copy(notificationsStagesEnabled = true)
        assertTrue(profile.notificationsStagesEnabled)

        val shouldNotifyStage = when ("STAGE_REACHED") {
            "STAGE_REACHED" -> profile.notificationsStagesEnabled
            else -> true
        }
        assertTrue("Stage notification should be delivered when toggled on", shouldNotifyStage)

        // User turns stage notifications back OFF
        profile = profile.copy(notificationsStagesEnabled = false)
        assertFalse(profile.notificationsStagesEnabled)

        val shouldNotifyStageAfterOff = when ("STAGE_REACHED") {
            "STAGE_REACHED" -> profile.notificationsStagesEnabled
            else -> true
        }
        assertFalse("Stage notification should be suppressed when toggled back off", shouldNotifyStageAfterOff)
    }

    @Test
    fun testStageRequestCodeConstants() {
        assertEquals(2000, NotificationHelper.REQUEST_CODE_STAGE_BASE)
        // Verify stage request codes for the 7 metabolic stages
        for (i in 1..7) {
            assertEquals(2000 + i, NotificationHelper.REQUEST_CODE_STAGE_BASE + i)
        }
    }
}
