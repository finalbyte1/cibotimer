package com.cibotimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import com.cibotimer.CiboTimerApp
import com.cibotimer.MainActivity
import com.cibotimer.R
import com.cibotimer.data.models.FastingDayFormatter
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.FastingState
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.domain.DateTimeFormatters
import com.cibotimer.domain.FastingCalculator
import java.util.Calendar
import java.util.Locale

/**
 * Stato normalizzato e condiviso per i formati widget 1x1 e 2x1.
 */
data class FastingWidgetState(
    // Formato 2x1
    val status2x1: String,             // es. "In digiuno", "Pasto", "In pausa"
    val timeRemaining2x1: String,      // es. "5h 50m", "--"
    val nextEvent2x1: String,          // es. "Mangiare alle 7:00", "Mangiare alle 7:00 Gio", "Digiuno alle 12:00 Mer"

    // Formato 1x1
    val status1x1: String,             // es. "In digiuno", "Pasto", "In pausa"
    val timeRemaining1x1: String,      // es. "5h 50m", "--"
    val nextEvent1x1: String,          // es. "Mangi 7:00", "Stop 12:00", "Avvia"

    // Colori e risorse per stato
    val backgroundResId: Int,          // R.drawable.widget_bg_fasting / widget_bg_eating
    val buttonBackgroundResId: Int,    // R.drawable.widget_button_circle_fasting / widget_button_circle_eating
    val secondaryTextColor: Int,       // #8FA3BF (digiuno) o #A7D9C4 (pasto)
    val primaryTextColor: Int = 0xFFFFFFFF.toInt(), // Bianco #FFFFFF
    val nextEventColor2x1: Int = 0xFFFFFFFF.toInt(), // Bianco #FFFFFF (colore chiaro)

    // Azione rapida pulsante circolare
    val actionIconResId: Int,          // R.drawable.ic_widget_flame, ic_widget_cutlery, ic_widget_play
    val actionContentDescription: String, // "Mangiare", "Digiunare", "Avvia"

    // Campi per retrocompatibilità
    val title2x1: String = nextEvent2x1,
    val subtitle2x1: String = "$status2x1 • $timeRemaining2x1",
    val bottom1x1: String = nextEvent1x1,
    val nextPeriodText: String = nextEvent2x1,
    val statusText: String = "$status2x1 • $timeRemaining2x1"
)

/**
 * Motore unico e centralizzato per estrazione dati, calcolo tempi,
 * formattazione testi, rendering RemoteViews e aggiornamento di tutti i widget CiboTimer.
 */
object FastingWidgetUpdater {

    private const val TAG = "FastingWidget"

    const val ACTION_UPDATE_ALL = "com.cibotimer.widget.ACTION_UPDATE_ALL"
    const val ACTION_WIDGET_BUTTON_CLICK = "com.cibotimer.widget.ACTION_WIDGET_BUTTON_CLICK"

    /**
     * Aggiorna contemporaneamente tutte le istanze attive di FastingWidget1x1Receiver,
     * FastingWidget2x1Receiver e del provider legacy CiboTimerWidgetProvider.
     */
    fun updateAll(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val state = computeState(context)

            // 1. Aggiorna istanze FastingWidget1x1Receiver (1x1)
            val cn1x1 = ComponentName(context, FastingWidget1x1Receiver::class.java)
            val ids1x1 = appWidgetManager.getAppWidgetIds(cn1x1)
            for (id in ids1x1) {
                logWidgetDimensions(appWidgetManager, id, "1x1")
                val views = build1x1RemoteViews(context, state)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 2. Aggiorna istanze FastingWidget2x1Receiver (2x1)
            val cn2x1 = ComponentName(context, FastingWidget2x1Receiver::class.java)
            val ids2x1 = appWidgetManager.getAppWidgetIds(cn2x1)
            for (id in ids2x1) {
                logWidgetDimensions(appWidgetManager, id, "2x1")
                val views = build2x1RemoteViews(context, state)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 3. Aggiorna istanze CiboTimerWidgetProvider per retrocompatibilità
            val cnLegacy = ComponentName(context, CiboTimerWidgetProvider::class.java)
            val idsLegacy = appWidgetManager.getAppWidgetIds(cnLegacy)
            for (id in idsLegacy) {
                logWidgetDimensions(appWidgetManager, id, "legacy")
                val views = build2x1RemoteViews(context, state)
                appWidgetManager.updateAppWidget(id, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
        }
    }

    /**
     * Logga le dimensioni effettive del widget (OPTION_APPWIDGET_MIN_WIDTH, MIN_HEIGHT, ecc.)
     * per verificare che il layout rispetti gli spazi disponibili sul launcher (es. One UI).
     */
    fun logWidgetDimensions(appWidgetManager: AppWidgetManager, appWidgetId: Int, widgetType: String) {
        try {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId) ?: return
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            Log.i(TAG, "Widget [$widgetType] id=$appWidgetId dimensions: min=${minWidth}x${minHeight}dp, max=${maxWidth}x${maxHeight}dp")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log dimensions for widget id=$appWidgetId", e)
        }
    }

    /**
     * Costruisce la RemoteViews per il formato compatto 1x1.
     * Misure reali stimate (One UI): ~66x100dp. Testi impilati in colonna verticale centrata.
     */
    fun build1x1RemoteViews(context: Context, state: FastingWidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fasting_1x1)

        val openAppPendingIntent = createOpenAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_1x1_root, openAppPendingIntent)

        val actionPendingIntent = createActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_1x1_action_container, actionPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_1x1_action_button, actionPendingIntent)

        // Colori dinamici in base allo stato
        views.setInt(R.id.widget_1x1_root, "setBackgroundResource", state.backgroundResId)
        views.setInt(R.id.widget_1x1_action_button, "setBackgroundResource", state.buttonBackgroundResId)

        // Testi (impilati in verticale: 1. Bottone, 2. Tempo, 3. Stato, 4. Prossimo evento)
        views.setTextViewText(R.id.widget_1x1_time_remaining, state.timeRemaining1x1)
        views.setTextViewText(R.id.widget_1x1_status, state.status1x1)
        views.setTextViewText(R.id.widget_1x1_bottom_text, state.nextEvent1x1)

        // Colori testo dipendenti dallo stato
        views.setTextColor(R.id.widget_1x1_time_remaining, state.primaryTextColor)
        views.setTextColor(R.id.widget_1x1_status, state.secondaryTextColor)
        views.setTextColor(R.id.widget_1x1_bottom_text, state.secondaryTextColor)

        // Blocco scala caratteri di sistema: imposta sempre unità DIP
        views.setTextViewTextSize(R.id.widget_1x1_time_remaining, TypedValue.COMPLEX_UNIT_DIP, 16f)
        views.setTextViewTextSize(R.id.widget_1x1_status, TypedValue.COMPLEX_UNIT_DIP, 10f)
        views.setTextViewTextSize(R.id.widget_1x1_bottom_text, TypedValue.COMPLEX_UNIT_DIP, 10f)

        // Icona e contentDescription azione rapida
        views.setImageViewResource(R.id.widget_1x1_action_button, state.actionIconResId)
        views.setContentDescription(R.id.widget_1x1_action_button, state.actionContentDescription)

        return views
    }

    /**
     * Costruisce la RemoteViews per il formato orizzontale 2x1.
     * Misure reali stimate (One UI): ~152x100dp. Colonna testi a sinistra (3 righe), pulsante a destra.
     */
    fun build2x1RemoteViews(context: Context, state: FastingWidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fasting_2x1)

        val openAppPendingIntent = createOpenAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_2x1_root, openAppPendingIntent)

        val actionPendingIntent = createActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_2x1_action_container, actionPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_2x1_action_button, actionPendingIntent)

        // Colori dinamici in base allo stato
        views.setInt(R.id.widget_2x1_root, "setBackgroundResource", state.backgroundResId)
        views.setInt(R.id.widget_2x1_action_button, "setBackgroundResource", state.buttonBackgroundResId)

        // Testi (tre righe a sinistra: 1. Stato, 2. Tempo, 3. Prossimo evento)
        views.setTextViewText(R.id.widget_2x1_status, state.status2x1)
        views.setTextViewText(R.id.widget_2x1_time_remaining, state.timeRemaining2x1)
        views.setTextViewText(R.id.widget_2x1_next_period, state.nextEvent2x1)

        // Colori testo dipendenti dallo stato
        views.setTextColor(R.id.widget_2x1_status, state.secondaryTextColor)
        views.setTextColor(R.id.widget_2x1_time_remaining, state.primaryTextColor)
        views.setTextColor(R.id.widget_2x1_next_period, state.nextEventColor2x1)

        // Blocco scala caratteri di sistema: imposta sempre unità DIP
        views.setTextViewTextSize(R.id.widget_2x1_status, TypedValue.COMPLEX_UNIT_DIP, 11f)
        views.setTextViewTextSize(R.id.widget_2x1_time_remaining, TypedValue.COMPLEX_UNIT_DIP, 22f)
        views.setTextViewTextSize(R.id.widget_2x1_next_period, TypedValue.COMPLEX_UNIT_DIP, 12f)

        // Icona e contentDescription azione rapida
        views.setImageViewResource(R.id.widget_2x1_action_button, state.actionIconResId)
        views.setContentDescription(R.id.widget_2x1_action_button, state.actionContentDescription)

        return views
    }

    /**
     * Formatta l'orario target garantendo il formato coerente (es. "7:00", "12:00", "12:30" o "7:00 AM", "12:00 PM").
     */
    fun formatTargetTime(context: Context?, hour: Int, minute: Int): String {
        return if (context != null) {
            val locale = DateTimeFormatters.getActiveLocale(context)
            DateTimeFormatters.formatTime(hour, minute, context, locale)
        } else {
            "%d:%02d".format(hour, minute)
        }
    }

    fun formatTargetTime(hour: Int, minute: Int): String = "%d:%02d".format(hour, minute)

    fun formatEatingTargetTime(hour: Int, minute: Int): String = formatTargetTime(hour, minute)

    fun formatFastingTargetTime(hour: Int, minute: Int): String = formatTargetTime(hour, minute)

    /**
     * Formatta la durata breve (es. "5h 50m").
     */
    fun formatDurationShort(durationMillis: Long): String {
        val diffMinutes = (durationMillis / 60000L).coerceAtLeast(0)
        val hours = diffMinutes / 60
        val mins = diffMinutes % 60
        return "${hours}h ${mins}m"
    }

    /**
     * Calcola in modo centralizzato lo stato, i testi per entrambi i formati (1x1 e 2x1),
     * i tempi rimanenti, le icone/azioni e i colori/sfondi dello stato.
     */
    fun computeState(context: Context): FastingWidgetState {
        val repository = try {
            (context.applicationContext as? CiboTimerApp)?.repository
                ?: AppRepository(context.applicationContext)
        } catch (e: Exception) {
            null
        }

        val now = System.currentTimeMillis()
        try {
            repository?.syncOrAdvanceSession(now)
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing session in computeState", e)
        }

        val session = repository?.currentSession?.value
        val activePlan = repository?.activePlan?.value

        val fastingBg = R.drawable.widget_bg_fasting
        val eatingBg = R.drawable.widget_bg_eating
        val fastingBtnBg = R.drawable.widget_button_circle_fasting
        val eatingBtnBg = R.drawable.widget_button_circle_eating

        val fastingSecondaryColor = Color.parseColor("#8FA3BF")
        val eatingSecondaryColor = Color.parseColor("#A7D9C4")
        val primaryColor = Color.parseColor("#FFFFFF")

        if (session != null && !session.isCompleted) {
            val targetMillis = session.plannedEndTimeEpochMillis
            val isCurrentlyEating = (session.state == FastingState.EATING)

            val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayStr = getDayShortName(dayOfWeek, context)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            val isTargetToday = isSameDay(now, targetMillis)
            val timeRemainingStr = formatDurationShort(targetMillis - now)
            val timeStr = formatTargetTime(context, hour, minute)

            if (isCurrentlyEating) {
                // Pasto in corso (si può mangiare)
                // Sfondo verde scuro #0E3B2C, bordo 0.5dp #1F8F6B, testo secondario #A7D9C4,
                // testo principale bianco, pulsante con bordo/icona verde #34D399 e icona posate
                val status = context.getString(R.string.widget_label_eating) // "Pasto"

                // Prossimo evento:
                // 2x1: "Digiuno alle 12:00" (se oggi) / "Digiuno alle 12:00 Mer" (se non oggi)
                val nextEvent2x1 = if (isTargetToday) {
                    context.getString(R.string.widget_fast_at, timeStr)
                } else {
                    context.getString(R.string.widget_fast_at_with_day, timeStr, dayStr)
                }

                // 1x1: "Stop 12:00" (giorno mai mostrato in 1x1)
                val nextEvent1x1 = context.getString(R.string.widget_fast_short_at, timeStr)

                return FastingWidgetState(
                    status2x1 = status,
                    timeRemaining2x1 = timeRemainingStr,
                    nextEvent2x1 = nextEvent2x1,
                    status1x1 = status,
                    timeRemaining1x1 = timeRemainingStr,
                    nextEvent1x1 = nextEvent1x1,
                    backgroundResId = eatingBg,
                    buttonBackgroundResId = eatingBtnBg,
                    secondaryTextColor = eatingSecondaryColor,
                    primaryTextColor = primaryColor,
                    nextEventColor2x1 = primaryColor,
                    actionIconResId = R.drawable.ic_widget_cutlery,
                    actionContentDescription = context.getString(R.string.widget_action_fast)
                )
            } else {
                // Digiuno in corso (non si può mangiare)
                // Sfondo #0B1B33, bordo 0.5dp #1C3A63, testo secondario #8FA3BF,
                // pulsante con bordo/icona ciano #22D3EE e icona fiamma
                val status = context.getString(R.string.widget_label_fasting) // "In digiuno"

                // Prossimo evento:
                // 2x1: "Mangiare alle 7:00" (se oggi) / "Mangiare alle 7:00 Gio" (se non oggi)
                val nextEvent2x1 = if (isTargetToday) {
                    context.getString(R.string.widget_eat_at, timeStr)
                } else {
                    context.getString(R.string.widget_eat_at_with_day, timeStr, dayStr)
                }

                // 1x1: "Mangi 7:00" (giorno mai mostrato in 1x1)
                val nextEvent1x1 = context.getString(R.string.widget_eat_short_at, timeStr)

                return FastingWidgetState(
                    status2x1 = status,
                    timeRemaining2x1 = timeRemainingStr,
                    nextEvent2x1 = nextEvent2x1,
                    status1x1 = status,
                    timeRemaining1x1 = timeRemainingStr,
                    nextEvent1x1 = nextEvent1x1,
                    backgroundResId = fastingBg,
                    buttonBackgroundResId = fastingBtnBg,
                    secondaryTextColor = fastingSecondaryColor,
                    primaryTextColor = primaryColor,
                    nextEventColor2x1 = primaryColor,
                    actionIconResId = R.drawable.ic_widget_flame,
                    actionContentDescription = context.getString(R.string.widget_action_eat)
                )
            }
        } else {
            // Nessuna sessione attiva (In pausa)
            val status = context.getString(R.string.widget_label_paused) // "In pausa"
            val scheduled = computeNextScheduledFast(context, activePlan)

            val nextEvent2x1 = scheduled?.title ?: context.getString(R.string.widget_no_active_fast)
            val nextEvent1x1 = scheduled?.shortText ?: context.getString(R.string.widget_action_start)
            val timeRemaining = scheduled?.remainingTimeStr ?: "--"

            return FastingWidgetState(
                status2x1 = status,
                timeRemaining2x1 = timeRemaining,
                nextEvent2x1 = nextEvent2x1,
                status1x1 = status,
                timeRemaining1x1 = timeRemaining,
                nextEvent1x1 = nextEvent1x1,
                backgroundResId = fastingBg,
                buttonBackgroundResId = fastingBtnBg,
                secondaryTextColor = fastingSecondaryColor,
                primaryTextColor = primaryColor,
                nextEventColor2x1 = primaryColor,
                actionIconResId = R.drawable.ic_widget_play,
                actionContentDescription = context.getString(R.string.widget_action_start)
            )
        }
    }

    /**
     * Gestisce il tocco sul pulsante circolare d'azione rapida:
     * - Esegue la transizione di stato nel repository
     * - Aggiorna istantaneamente tutti i widget
     * - Riporta l'applicazione in primo piano
     */
    fun handleActionButtonClick(context: Context) {
        try {
            val repository = (context.applicationContext as? CiboTimerApp)?.repository
                ?: AppRepository(context.applicationContext)
            val session = repository.currentSession.value

            if (session != null && !session.isCompleted) {
                if (session.state == FastingState.FASTING) {
                    repository.transitionToEating()
                } else if (session.state == FastingState.EATING) {
                    repository.startFastingNow()
                }
            } else {
                repository.startFasting()
            }

            updateAll(context)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(openAppIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createOpenAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, FastingWidgetActionReceiver::class.java).apply {
            action = ACTION_WIDGET_BUTTON_CLICK
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    data class ScheduledFastInfo(
        val title: String,
        val shortText: String,
        val remainingTimeStr: String?
    )

    private fun computeNextScheduledFast(context: Context, plan: FastingPlan?): ScheduledFastInfo? {
        if (plan == null || (plan.days.isEmpty() && plan.periods.isNullOrEmpty())) return null
        val now = System.currentTimeMillis()
        val scheduled = FastingCalculator.resolveCurrentScheduledPeriod(plan, now)

        val targetMillis = if (scheduled.isNowInsideFasting) {
            scheduled.periodEndEpochMillis
        } else {
            scheduled.periodStartEpochMillis
        }

        val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayStr = getDayShortName(dayOfWeek, context)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeStr = formatTargetTime(context, hour, minute)
        val isToday = isSameDay(now, targetMillis)

        val title = if (scheduled.isNowInsideFasting) {
            if (isToday) {
                context.getString(R.string.widget_eat_at, timeStr)
            } else {
                context.getString(R.string.widget_eat_at_with_day, timeStr, dayStr)
            }
        } else {
            if (isToday) {
                context.getString(R.string.widget_fast_at, timeStr)
            } else {
                context.getString(R.string.widget_fast_at_with_day, timeStr, dayStr)
            }
        }

        val shortText = if (scheduled.isNowInsideFasting) {
            context.getString(R.string.widget_eat_short_at, timeStr)
        } else {
            context.getString(R.string.widget_fast_short_at, timeStr)
        }

        val remainingMillis = (targetMillis - now).coerceAtLeast(0L)
        val remainingTimeStr = formatDurationShort(remainingMillis)

        return ScheduledFastInfo(
            title = title,
            shortText = shortText,
            remainingTimeStr = remainingTimeStr
        )
    }

    fun isSameDay(time1Millis: Long, time2Millis: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1Millis }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2Millis }
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
               cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getDayShortName(calendarDayOfWeek: Int, context: Context? = null): String {
        val isoDay = when (calendarDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        val locale = context?.let { DateTimeFormatters.getActiveLocale(it) } ?: Locale.getDefault()
        return FastingDayFormatter.formatShort(isoDay, locale)
    }

    fun getDayShortNameFrom1To7(dayOfWeek: Int, context: Context? = null): String {
        val locale = context?.let { DateTimeFormatters.getActiveLocale(it) } ?: Locale.getDefault()
        return FastingDayFormatter.formatShort(dayOfWeek, locale)
    }
}
