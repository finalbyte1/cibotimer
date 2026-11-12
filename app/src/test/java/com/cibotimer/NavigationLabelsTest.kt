package com.cibotimer

import com.cibotimer.ui.Screen
import com.cibotimer.ui.bottomNavItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NavigationLabelsTest {

    @Test
    fun testBottomNavItemsOrderAndIds() {
        assertEquals(5, bottomNavItems.size)
        assertEquals(Screen.Timer, bottomNavItems[0])
        assertEquals(Screen.Progress, bottomNavItems[1])
        assertEquals(Screen.Coach, bottomNavItems[2])
        assertEquals(Screen.Notifications, bottomNavItems[3])
        assertEquals(Screen.Settings, bottomNavItems[4])
    }

    @Test
    fun testStringsXmlEnglishNavNotificationsIsAlerts() {
        val stringsEnFile = File("src/main/res/values/strings.xml")
        val strings = parseStringsXml(stringsEnFile)

        assertEquals("Alerts", strings["nav_notifications"])
        assertEquals("Notifications", strings["notifications_title"])
        assertEquals("Fasting", strings["nav_timer"])
        assertEquals("Me", strings["nav_progress"])
        assertEquals("Coach", strings["nav_coach"])
        assertEquals("More", strings["nav_settings"])

        // Ensure max character count across English tabs is small (<= 7)
        bottomNavItems.forEach { screen ->
            val resName = getResNameForId(screen.titleRes)
            val text = strings[resName] ?: ""
            assertTrue("English label '$text' for $resName should be <= 8 chars to avoid wrapping on 360dp", text.length <= 8)
        }
    }

    @Test
    fun testStringsXmlItalianNavNotificationsIsNotifiche() {
        val stringsItFile = File("src/main/res/values-it/strings.xml")
        val strings = parseStringsXml(stringsItFile)

        assertEquals("Notifiche", strings["nav_notifications"])
        assertEquals("Notifiche", strings["notifications_title"])
        assertEquals("Digiuno", strings["nav_timer"])
        assertEquals("Io", strings["nav_progress"])
        assertEquals("Coach", strings["nav_coach"])
        assertEquals("Altro", strings["nav_settings"])
    }

    private fun parseStringsXml(file: File): Map<String, String> {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) {
                map[name] = node.textContent
            }
        }
        return map
    }

    private fun getResNameForId(resId: Int): String {
        return when (resId) {
            R.string.nav_timer -> "nav_timer"
            R.string.nav_progress -> "nav_progress"
            R.string.nav_coach -> "nav_coach"
            R.string.nav_notifications -> "nav_notifications"
            R.string.nav_settings -> "nav_settings"
            else -> "unknown"
        }
    }
}
