package com.cibotimer.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FastingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.cibotimer.ACTION_FASTING_ALARM") {
            val title = intent.getStringExtra("EXTRA_TITLE") ?: "CiboTimer"
            val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Promemoria digiuno"
            val type = intent.getStringExtra("EXTRA_TYPE") ?: "FASTING"

            val channelId = if (type == "STAGE_REACHED") {
                NotificationHelper.CHANNEL_STAGES_ID
            } else {
                NotificationHelper.CHANNEL_FASTING_ID
            }

            val notificationId = when (type) {
                "FASTING_END" -> NotificationHelper.NOTIF_ID_TRANSITION_EATING
                "EATING_END" -> NotificationHelper.NOTIF_ID_TRANSITION_FASTING
                "EATING_PRE_REMINDER" -> NotificationHelper.NOTIF_ID_PRE_FASTING
                else -> System.currentTimeMillis().toInt()
            }
            NotificationHelper.showNotification(context, channelId, notificationId, title, message, type)

            // Avanzamento automatico continuo del timer (il timer non si ferma mai)
            try {
                if (type == "FASTING_END" || type == "EATING_END") {
                    com.cibotimer.CiboTimerApp.instance.repository.syncOrAdvanceSession()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Ripristino allarmi dopo riavvio
            NotificationHelper.createNotificationChannels(context)
            try {
                com.cibotimer.CiboTimerApp.instance.repository.syncOrAdvanceSession()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
