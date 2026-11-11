package com.cibotimer

import android.app.Application
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.notifications.NotificationHelper
import com.cibotimer.update.UpdateManager

class CiboTimerApp : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var updateManager: UpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        NotificationHelper.createNotificationChannels(this)
        repository = AppRepository(this)
        updateManager = UpdateManager(this)
        if (updateManager.autoCheckEnabled) {
            updateManager.checkForUpdates(manual = false)
        }
    }

    companion object {
        lateinit var instance: CiboTimerApp
            private set
    }
}

@Deprecated("Usa CiboTimerApp", ReplaceWith("CiboTimerApp"))
typealias BodyFastApp = CiboTimerApp
