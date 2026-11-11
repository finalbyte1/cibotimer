package com.cibotimer.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cibotimer.CiboTimerApp
import com.cibotimer.MainActivity
import com.cibotimer.R
import com.cibotimer.data.models.FastingSession
import com.cibotimer.data.models.FastingState

object NotificationHelper {

    const val CHANNEL_FASTING_ID = "channel_fasting_reminders"
    const val CHANNEL_STAGES_ID = "channel_fasting_stages"
    const val CHANNEL_WATER_ID = "channel_water_reminders"
    const val CHANNEL_LIVE_TIMER_ID = "channel_cibotimer_timer_v4"

    const val REQUEST_CODE_FASTING_END = 1001
    const val REQUEST_CODE_EATING_END = 1002
    const val REQUEST_CODE_EATING_PRE = 1003
    const val REQUEST_CODE_STAGE_BASE = 2000

    const val NOTIF_ID_TRANSITION_EATING = 3001
    const val NOTIF_ID_TRANSITION_FASTING = 3002
    const val NOTIF_ID_PRE_FASTING = 3003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Rimuove i canali legacy/precedenti affinché Android recepisca il canale ad Alta Priorità
            try {
                notificationManager.deleteNotificationChannel("channel_fasting_live_timer")
                notificationManager.deleteNotificationChannel("channel_cibotimer_live_v2")
                notificationManager.deleteNotificationChannel("channel_cibotimer_live_v3")
            } catch (e: Exception) {
                // Ignore
            }

            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val liveTimerChannel = NotificationChannel(
                CHANNEL_LIVE_TIMER_ID,
                "CiboTimer - Timer in Tempo Reale",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifica persistente e prioritaria nella tendina con timer e fase del digiuno"
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                setSound(defaultSound, audioAttributes)
            }

            val fastingChannel = NotificationChannel(
                CHANNEL_FASTING_ID,
                context.getString(R.string.fasting_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.fasting_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 200, 450)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val stagesChannel = NotificationChannel(
                CHANNEL_STAGES_ID,
                context.getString(R.string.stages_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.stages_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val waterChannel = NotificationChannel(
                CHANNEL_WATER_ID,
                context.getString(R.string.water_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.water_channel_desc)
            }

            notificationManager.createNotificationChannels(listOf(liveTimerChannel, fastingChannel, stagesChannel, waterChannel))
        }
    }

    fun scheduleFastingEndAlarm(context: Context, endTimeEpochMillis: Long, planName: String) {
        if (endTimeEpochMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
            putExtra("EXTRA_TYPE", "FASTING_END")
            putExtra("EXTRA_TITLE", context.getString(R.string.notif_fasting_end_title))
            putExtra("EXTRA_MESSAGE", context.getString(R.string.notif_fasting_end_msg, planName))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FASTING_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, endTimeEpochMillis, pendingIntent)
    }

    fun scheduleEatingWindowEndAlarm(context: Context, nextFastingStartMillis: Long, planName: String) {
        if (nextFastingStartMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
            putExtra("EXTRA_TYPE", "EATING_END")
            putExtra("EXTRA_TITLE", context.getString(R.string.notif_eating_end_title))
            putExtra("EXTRA_MESSAGE", context.getString(R.string.notif_eating_end_msg, planName))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EATING_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, nextFastingStartMillis, pendingIntent)
    }

    fun scheduleEatingWindowPreAlarm(context: Context, preWarningTimeMillis: Long, planName: String) {
        if (preWarningTimeMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
            putExtra("EXTRA_TYPE", "EATING_PRE_REMINDER")
            putExtra("EXTRA_TITLE", context.getString(R.string.notif_eating_pre_title))
            putExtra("EXTRA_MESSAGE", context.getString(R.string.notif_eating_pre_msg, planName))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EATING_PRE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, preWarningTimeMillis, pendingIntent)
    }

    fun cancelFastingAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FASTING_END,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun cancelEatingAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
        }
        val piEnd = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EATING_END,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (piEnd != null) {
            alarmManager.cancel(piEnd)
            piEnd.cancel()
        }
        val piPre = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EATING_PRE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (piPre != null) {
            alarmManager.cancel(piPre)
            piPre.cancel()
        }
    }

    fun scheduleStageAlarm(context: Context, triggerTimeEpochMillis: Long, stageTitle: String, stageDesc: String, requestCode: Int) {
        if (triggerTimeEpochMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FastingAlarmReceiver::class.java).apply {
            action = "com.cibotimer.ACTION_FASTING_ALARM"
            putExtra("EXTRA_TYPE", "STAGE_REACHED")
            putExtra("EXTRA_TITLE", context.getString(R.string.notif_stage_title, stageTitle))
            putExtra("EXTRA_MESSAGE", stageDesc)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, triggerTimeEpochMillis, pendingIntent)
    }

    private fun setAlarmSafely(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        type: String = "GENERAL"
    ) {
        // Salva automaticamente nello storico (Tab Notifiche)
        try {
            CiboTimerApp.instance.repository.addNotification(title, message, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 450, 200, 450))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Riprogramma gli allarmi di fine ciclo con il nuovo nome del piano
     * quando un piano viene rinominato dall'utente.
     */
    fun rescheduleAlarmsWithNewPlanName(context: Context, session: FastingSession, planName: String) {
        if (session.state == FastingState.FASTING) {
            cancelFastingAlarms(context)
            scheduleFastingEndAlarm(context, session.plannedEndTimeEpochMillis, planName)
        } else if (session.state == FastingState.EATING) {
            cancelEatingAlarms(context)
            scheduleEatingWindowEndAlarm(context, session.plannedEndTimeEpochMillis, planName)
            scheduleEatingWindowPreAlarm(context, session.plannedEndTimeEpochMillis - 15 * 60_000L, planName)
        }
    }

    /**
     * Apre direttamente la pagina di configurazione del canale notifica su Android,
     * consentendo all'utente di verificare/impostare l'importanza ("Alta / Importante")
     * e mantenere la notifica fissa e prioritaria nella tendina.
     */
    fun openChannelSettings(context: Context, channelId: String = CHANNEL_LIVE_TIMER_ID) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
