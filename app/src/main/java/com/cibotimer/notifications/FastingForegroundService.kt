package com.cibotimer.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.cibotimer.CiboTimerApp
import com.cibotimer.MainActivity
import com.cibotimer.R
import com.cibotimer.data.models.FastingState
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.widget.FastingWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FastingForegroundService : Service() {

    private var startTimeMillis: Long = 0L
    private var plannedEndTimeMillis: Long = 0L
    private var planName: String = "Digiuno"
    private var isEating: Boolean = false

    private val widgetTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    FastingWidgetUpdater.updateAll(context)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(widgetTickReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_FASTING) {
            stopForegroundTimer()
            return START_NOT_STICKY
        }

        if (action == ACTION_NOTIFICATION_DISMISSED) {
            if (FastingPersistence.isExpectedRunning(this)) {
                val notif = buildTimerNotification(this, startTimeMillis, plannedEndTimeMillis, planName, isEating)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notif)
            }
            return START_STICKY
        }

        // Se riavviato dal sistema (START_STICKY) senza parametri, ricostruisce lo stato
        if (intent == null || !intent.hasExtra(EXTRA_START_TIME)) {
            val savedState = FastingPersistence.getState(this)
            if (savedState.isActive && savedState.plannedEndTime > 0L) {
                startTimeMillis = savedState.startTime
                plannedEndTimeMillis = savedState.plannedEndTime
                planName = savedState.planName
                isEating = savedState.isEating
            } else {
                val app = applicationContext as? CiboTimerApp
                val currentSession = app?.repository?.currentSession?.value
                if (currentSession != null) {
                    startTimeMillis = currentSession.startTimeEpochMillis
                    plannedEndTimeMillis = currentSession.plannedEndTimeEpochMillis
                    planName = currentSession.planName
                    isEating = (currentSession.state == FastingState.EATING)
                } else {
                    stopForegroundTimer()
                    return START_NOT_STICKY
                }
            }
        } else {
            startTimeMillis = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
            plannedEndTimeMillis = intent.getLongExtra(EXTRA_PLANNED_END_TIME, startTimeMillis + 16 * 3600 * 1000L)
            planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "Digiuno"
            isEating = intent.getBooleanExtra(EXTRA_IS_EATING, false)
        }

        // Salva lo stato in modo duraturo
        FastingPersistence.saveState(
            context = this,
            startTime = startTimeMillis,
            plannedEndTime = plannedEndTimeMillis,
            planName = planName,
            isEating = isEating,
            isActive = true
        )

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
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
            isRunning = true
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        FastingWidgetUpdater.updateAll(this)

        return START_STICKY
    }

    private fun stopForegroundTimer() {
        isRunning = false
        FastingPersistence.clearState(this)

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

        try {
            val app = applicationContext as? CiboTimerApp
            if (app?.repository?.currentSession?.value != null) {
                app.repository.stopTimer()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        FastingWidgetUpdater.updateAll(this)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        try {
            unregisterReceiver(widgetTickReceiver)
        } catch (e: Exception) {
            // ignore
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.cibotimer.action.START_TIMER"
        const val ACTION_UPDATE = "com.cibotimer.action.UPDATE_TIMER"
        const val ACTION_STOP_FASTING = "com.cibotimer.action.STOP_FASTING"
        const val ACTION_NOTIFICATION_DISMISSED = "com.cibotimer.action.NOTIFICATION_DISMISSED"

        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_PLANNED_END_TIME = "EXTRA_PLANNED_END_TIME"
        const val EXTRA_PLAN_NAME = "EXTRA_PLAN_NAME"
        const val EXTRA_IS_EATING = "EXTRA_IS_EATING"

        private const val REQUEST_CODE_OPEN_APP = 100
        private const val REQUEST_CODE_STOP_FASTING = 101
        private const val REQUEST_CODE_DISMISSED = 102

        @Volatile
        var isRunning: Boolean = false

        /**
         * Formatta il testo del prossimo evento con supporto per la localizzazione:
         * In IT: "Mangiare alle 20:00" / "Digiuno alle 12:00" (o domani / giorno della settimana).
         * In EN: "Eat at 8:00 PM" / "Fast at 12:00 PM" (o tomorrow / day of the week).
         */
        fun formatNextEvent(
            context: Context?,
            targetMillis: Long,
            isEating: Boolean,
            locale: Locale = context?.let { DateTimeFormatters.getActiveLocale(it) } ?: Locale.getDefault()
        ): String {
            val targetCal = Calendar.getInstance().apply { timeInMillis = targetMillis }
            val nowCal = Calendar.getInstance()
            val isToday = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

            val hour = targetCal.get(Calendar.HOUR_OF_DAY)
            val minute = targetCal.get(Calendar.MINUTE)
            val timeStr = if (context != null) {
                DateTimeFormatters.formatTime(hour, minute, context, locale)
            } else {
                if (locale.language == "it") "%d:%02d".format(hour, minute) else DateTimeFormatters.formatTime(hour, minute, null, locale)
            }

            return if (isToday) {
                if (context != null) {
                    if (isEating) context.getString(R.string.notif_fgs_fast_today, timeStr) else context.getString(R.string.notif_fgs_eat_today, timeStr)
                } else {
                    val verb = if (isEating) (if (locale.language == "it") "Digiuno" else "Fast") else (if (locale.language == "it") "Mangiare" else "Eat")
                    if (locale.language == "it") "$verb alle $timeStr" else "$verb at $timeStr"
                }
            } else {
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val isTomorrow = targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                        targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
                if (isTomorrow) {
                    if (context != null) {
                        if (isEating) context.getString(R.string.notif_fgs_fast_tomorrow, timeStr) else context.getString(R.string.notif_fgs_eat_tomorrow, timeStr)
                    } else {
                        val verb = if (isEating) (if (locale.language == "it") "Digiuno" else "Fast") else (if (locale.language == "it") "Mangiare" else "Eat")
                        if (locale.language == "it") "$verb domani alle $timeStr" else "$verb tomorrow at $timeStr"
                    }
                } else {
                    val isoDay = if (targetCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else targetCal.get(Calendar.DAY_OF_WEEK) - 1
                    val dayStr = DateTimeFormatters.formatShortDay(isoDay, locale, capitalize = (locale.language != "it"))
                    if (context != null) {
                        if (isEating) context.getString(R.string.notif_fgs_fast_date, dayStr, timeStr) else context.getString(R.string.notif_fgs_eat_date, dayStr, timeStr)
                    } else {
                        val verb = if (isEating) (if (locale.language == "it") "Digiuno" else "Fast") else (if (locale.language == "it") "Mangiare" else "Eat")
                        if (locale.language == "it") "$verb $dayStr alle $timeStr" else "$verb $dayStr at $timeStr"
                    }
                }
            }
        }

        fun formatNextEvent(targetMillis: Long, isEating: Boolean): String =
            formatNextEvent(null, targetMillis, isEating, Locale.ITALIAN)

        fun buildTimerNotification(
            context: Context,
            startTimeMillis: Long,
            plannedEndTimeMillis: Long,
            planName: String,
            isEating: Boolean = false
        ): Notification {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_APP,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopFastingIntent = Intent(context, FastingForegroundService::class.java).apply {
                action = ACTION_STOP_FASTING
            }
            val pendingStopFasting = PendingIntent.getService(
                context,
                REQUEST_CODE_STOP_FASTING,
                stopFastingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deleteIntent = Intent(context, FastingForegroundService::class.java).apply {
                action = ACTION_NOTIFICATION_DISMISSED
            }
            val pendingDelete = PendingIntent.getService(
                context,
                REQUEST_CODE_DISMISSED,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notifTitle = if (isEating) context.getString(R.string.notif_title_eating) else context.getString(R.string.notif_title_fasting)
            val notifText = formatNextEvent(context, plannedEndTimeMillis, isEating)

            val smallIcon = NotificationHelper.getNotificationIcon(isEating)
            val notifColor = NotificationHelper.getNotificationColor(isEating)

            val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_PERSISTENT_TIMER_ID)
                .setSmallIcon(smallIcon)
                .setColor(notifColor)
                .setContentTitle(notifTitle)
                .setContentText(notifText)
                .setContentIntent(pendingOpenApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(
                    smallIcon,
                    "Stop digiuno",
                    pendingStopFasting
                )
                .setDeleteIntent(pendingDelete)

            // Live ticking countdown chronometer
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(plannedEndTimeMillis)

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
            NotificationHelper.createNotificationChannels(context)

            // Salva lo stato prima di avviare il servizio
            FastingPersistence.saveState(context, startTime, plannedEndTime, planName, isEating, true)

            // Notifica immediata
            try {
                val notif = buildTimerNotification(context, startTime, plannedEndTime, planName, isEating)
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            try {
                val intent = Intent(context, FastingForegroundService::class.java).apply {
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

        fun update(context: Context) {
            val app = context.applicationContext as? CiboTimerApp
            val session = app?.repository?.currentSession?.value
            if (session != null) {
                start(
                    context = context,
                    startTime = session.startTimeEpochMillis,
                    plannedEndTime = session.plannedEndTimeEpochMillis,
                    planName = session.planName,
                    isEating = (session.state == FastingState.EATING)
                )
            }
        }

        fun stop(context: Context) {
            isRunning = false
            FastingPersistence.clearState(context)
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            try {
                context.stopService(Intent(context, FastingForegroundService::class.java))
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}

typealias FastingTimerService = FastingForegroundService
