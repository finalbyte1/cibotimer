package com.cibotimer.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.cibotimer.MainActivity
import com.cibotimer.R
import com.cibotimer.domain.FastingCalculator
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class FastingTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    private var startTimeMillis: Long = 0L
    private var plannedEndTimeMillis: Long = 0L
    private var planName: String = "Digiuno"
    private var isEating: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTimer()
            return START_NOT_STICKY
        }

        startTimeMillis = intent?.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()
        plannedEndTimeMillis = intent?.getLongExtra(EXTRA_PLANNED_END_TIME, startTimeMillis + 16 * 3600 * 1000L) ?: (startTimeMillis + 16 * 3600 * 1000L)
        planName = intent?.getStringExtra(EXTRA_PLAN_NAME) ?: "Digiuno"
        isEating = intent?.getBooleanExtra(EXTRA_IS_EATING, false) ?: false

        val notification = buildTimerNotification(this, startTimeMillis, plannedEndTimeMillis, planName, isEating)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            // Aggiorna esplicitamente anche NotificationManager per garantire refresh istantaneo del titolo/testo
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        startPeriodicUpdate()
        com.cibotimer.widget.CiboTimerWidgetProvider.updateAll(this)

        return START_STICKY
    }

    private fun startPeriodicUpdate() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(60_000L) // Refresh stage label every minute
                try {
                    val updatedNotification = buildTimerNotification(this@FastingTimerService, startTimeMillis, plannedEndTimeMillis, planName, isEating)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                    com.cibotimer.widget.CiboTimerWidgetProvider.updateAll(this@FastingTimerService)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    private fun stopTimer() {
        updateJob?.cancel()
        com.cibotimer.widget.CiboTimerWidgetProvider.updateAll(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.cibotimer.action.START_TIMER"
        const val ACTION_STOP = "com.cibotimer.action.STOP_TIMER"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_PLANNED_END_TIME = "EXTRA_PLANNED_END_TIME"
        const val EXTRA_PLAN_NAME = "EXTRA_PLAN_NAME"
        const val EXTRA_IS_EATING = "EXTRA_IS_EATING"

        fun buildTimerNotification(
            context: Context,
            startTimeMillis: Long,
            plannedEndTimeMillis: Long,
            planName: String,
            isEating: Boolean = false
        ): Notification {
            val now = System.currentTimeMillis()
            val elapsedSec = ((now - startTimeMillis) / 1000).coerceAtLeast(0)
            val elapsedH = elapsedSec / 3600
            val elapsedM = (elapsedSec % 3600) / 60
            val elapsedStr = if (elapsedH > 0) "${elapsedH}h ${elapsedM}m passate" else "${elapsedM}m passate"

            val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALIAN)
            val endTimeFormatted = timeFormat.format(Date(plannedEndTimeMillis))

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val (notifTitle, notifText, notifSubText) = if (isEating) {
                Triple(
                    "CiboTimer • $planName • Pasto",
                    "🍽️ Pasto aperto fino alle $endTimeFormatted • Buon appetito!",
                    "Finestra di alimentazione ($elapsedStr)"
                )
            } else {
                val stage = FastingCalculator.getStageForElapsedSeconds(elapsedSec)
                Triple(
                    "CiboTimer • $planName • $elapsedStr",
                    "Fase ${stage.stageIndex}: ${stage.title} • Fine: $endTimeFormatted",
                    "Digiuno in corso"
                )
            }

            val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_LIVE_TIMER_ID)
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContentTitle(notifTitle)
                .setContentText(notifText)
                .setSubText(notifSubText)
                .setContentIntent(pendingOpenApp)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

            // Live ticking countdown chronometer
            if (plannedEndTimeMillis > now) {
                builder.setUsesChronometer(true)
                builder.setChronometerCountDown(true)
                builder.setWhen(plannedEndTimeMillis)
            }

            val notification = builder.build()
            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
            return notification
        }

        fun start(
            context: Context,
            startTime: Long,
            plannedEndTime: Long,
            planName: String,
            isEating: Boolean = false
        ) {
            // Immediate shade posting with NotificationManagerCompat to avoid startup delay
            try {
                val notif = buildTimerNotification(context, startTime, plannedEndTime, planName, isEating)
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            try {
                val intent = Intent(context, FastingTimerService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_START_TIME, startTime)
                    putExtra(EXTRA_PLANNED_END_TIME, plannedEndTime)
                    putExtra(EXTRA_PLAN_NAME, planName)
                    putExtra(EXTRA_IS_EATING, isEating)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            try {
                val intent = Intent(context, FastingTimerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
