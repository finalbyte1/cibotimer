package com.cibotimer

import com.cibotimer.notifications.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationIconTest {

    @Test
    fun testNotificationIconAndColorForFasting() {
        val icon = NotificationHelper.getNotificationIcon(isEating = false)
        val color = NotificationHelper.getNotificationColor(isEating = false)

        assertEquals("Fasting status icon must be ic_stat_fasting", R.drawable.ic_stat_fasting, icon)
        assertEquals("Fasting notification color must be #0891B2 (dark cyan)", 0xFF0891B2.toInt(), color)
    }

    @Test
    fun testNotificationIconAndColorForEating() {
        val icon = NotificationHelper.getNotificationIcon(isEating = true)
        val color = NotificationHelper.getNotificationColor(isEating = true)

        assertEquals("Eating status icon must be ic_stat_eating", R.drawable.ic_stat_eating, icon)
        assertEquals("Eating notification color must be #059669 (emerald green)", 0xFF059669.toInt(), color)
    }
}
