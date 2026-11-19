package com.cibotimer

import com.cibotimer.update.UpdateInfo
import com.cibotimer.update.UpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testUpdateInfoCreation() {
        val info = UpdateInfo(
            versionName = "1.0.77",
            versionCode = 78,
            downloadUrl = "https://play.google.com/store/apps/details?id=com.cibotimer",
            apkName = "Google Play",
            size = "15.4 MB",
            releaseNotes = "Aggiornamento Google Play In-App Updates"
        )

        assertEquals("1.0.77", info.versionName)
        assertEquals(78, info.versionCode)
        assertEquals("15.4 MB", info.size)
    }

    @Test
    fun testUpdateStateTransitions() {
        val info = UpdateInfo(
            versionName = "Google Play (Build 78)",
            versionCode = 78,
            downloadUrl = "",
            apkName = "Google Play",
            size = "12 MB"
        )

        val stateAvailable = UpdateState.Available(info)
        assertTrue(stateAvailable is UpdateState.Available)
        assertEquals(78, stateAvailable.info.versionCode)

        val stateDownloading = UpdateState.Downloading(45, info)
        assertEquals(45, stateDownloading.progressPercent)

        // Verifica che ReadyToInstall possa essere istanziato con apkUri e apkFile nulli
        // come previsto per gli aggiornamenti Google Play In-App Updates (Flexible flow)
        val stateReadyInApp = UpdateState.ReadyToInstall(apkFile = null, apkUri = null, info = info)
        assertNull(stateReadyInApp.apkFile)
        assertNull(stateReadyInApp.apkUri)
        assertEquals(info, stateReadyInApp.info)

        val stateUpToDate = UpdateState.UpToDate
        assertTrue(stateUpToDate is UpdateState.UpToDate)

        val stateError = UpdateState.Error("Play Store non disponibile")
        assertEquals("Play Store non disponibile", stateError.message)
    }
}
