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

            val isEating = when (type) {
                "FASTING_END", "EATING_PRE_REMINDER" -> true
                "EATING_END", "STAGE_REACHED" -> false
                else -> {
                    try {
                        (context.applicationContext as? com.cibotimer.CiboTimerApp)?.repository?.currentSession?.value?.state == com.cibotimer.data.models.FastingState.EATING
                    } catch (t: Throwable) {
                        false
                    }
                }
            }
            NotificationHelper.showNotification(context, channelId, notificationId, title, message, type, isEating)

            // Avanzamento automatico continuo del timer (il timer non si ferma mai)
            try {
                if (type == "FASTING_END" || type == "EATING_END") {
                    val repo = com.cibotimer.CiboTimerApp.instance.repository
                    repo.syncOrAdvanceSession()
                    val curr = repo.currentSession.value
                    if (curr != null) {
                        FastingForegroundService.start(
                            context = context,
                            startTime = curr.startTimeEpochMillis,
                            plannedEndTime = curr.plannedEndTimeEpochMillis,
                            planName = curr.planName,
                            isEating = (curr.state == com.cibotimer.data.models.FastingState.EATING)
                        )
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            try {
                com.cibotimer.widget.FastingWidgetUpdater.updateAll(context)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Ripristino allarmi, servizio foreground e widget dopo riavvio o aggiornamento app
            NotificationHelper.createNotificationChannels(context)
            try {
                val repo = com.cibotimer.CiboTimerApp.instance.repository
                repo.syncOrAdvanceSession()
                val session = repo.currentSession.value
                if (session != null) {
                    FastingForegroundService.start(
                        context = context,
                        startTime = session.startTimeEpochMillis,
                        plannedEndTime = session.plannedEndTimeEpochMillis,
                        planName = session.planName,
                        isEating = (session.state == com.cibotimer.data.models.FastingState.EATING)
                    )
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            try {
                com.cibotimer.widget.FastingWidgetUpdater.updateAll(context)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
