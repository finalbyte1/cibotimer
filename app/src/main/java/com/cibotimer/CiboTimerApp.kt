package com.cibotimer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.notifications.NotificationHelper
import com.cibotimer.update.UpdateManager
import com.cibotimer.widget.FastingWidgetUpdater

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

        registerWidgetTimeTickReceiver()
    }

    /**
     * Registra un receiver dinamico per aggiornare in tempo reale i widget
     * ad ogni minuto (ACTION_TIME_TICK), all'accensione dello schermo (ACTION_SCREEN_ON),
     * allo sblocco (ACTION_USER_PRESENT) e al cambio di orario/fuso orario.
     */
    private fun registerWidgetTimeTickReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    try {
                        FastingWidgetUpdater.updateAll(context)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: CiboTimerApp
            private set
    }
}

@Deprecated("Usa CiboTimerApp", ReplaceWith("CiboTimerApp"))
typealias BodyFastApp = CiboTimerApp
