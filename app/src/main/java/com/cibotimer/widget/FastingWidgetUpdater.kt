package com.cibotimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cibotimer.CiboTimerApp
import com.cibotimer.MainActivity
import com.cibotimer.R
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.FastingState
import com.cibotimer.data.repository.AppRepository
import java.util.Calendar
import java.util.Locale

/**
 * Stato normalizzato e formattato del widget del digiuno.
 */
data class FastingWidgetState(
    val nextPeriodText: String,        // "Digiuno alle 12 Mer", "Mangiare alle ore 14 Mer"
    val statusText: String,            // "Pasto in corso • 3h 0m", "Digiuno in corso • 3h 0m"
    val actionIconResId: Int,          // R.drawable.ic_widget_cutlery, R.drawable.ic_widget_flame, R.drawable.ic_widget_play
    val actionContentDescription: String // "Mangiare", "Digiunare", "Avvia"
)

/**
 * Motore per l'estrazione dati, la formattazione e l'aggiornamento
 * del widget di CiboTimer.
 */
object FastingWidgetUpdater {

    const val ACTION_UPDATE_ALL = "com.cibotimer.widget.ACTION_UPDATE_ALL"
    const val ACTION_WIDGET_BUTTON_CLICK = "com.cibotimer.widget.ACTION_WIDGET_BUTTON_CLICK"

    /**
     * Aggiorna simultaneamente tutte le istanze attive del widget e del legacy provider.
     */
    fun updateAll(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val state = computeState(context)

            // 1. Aggiorna istanze FastingWidget2x1Receiver
            val cn2x1 = ComponentName(context, FastingWidget2x1Receiver::class.java)
            val ids2x1 = appWidgetManager.getAppWidgetIds(cn2x1)
            for (id in ids2x1) {
                val views = build2x1RemoteViews(context, state)
                appWidgetManager.updateAppWidget(id, views)
            }

            // 2. Aggiorna istanze CiboTimerWidgetProvider per retrocompatibilità
            val cnLegacy = ComponentName(context, CiboTimerWidgetProvider::class.java)
            val idsLegacy = appWidgetManager.getAppWidgetIds(cnLegacy)
            for (id in idsLegacy) {
                val views = build2x1RemoteViews(context, state)
                appWidgetManager.updateAppWidget(id, views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Costruisce la RemoteViews per il widget della home.
     */
    fun build2x1RemoteViews(context: Context, state: FastingWidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fasting_2x1)

        val openAppPendingIntent = createOpenAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_2x1_root, openAppPendingIntent)

        val actionPendingIntent = createActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_2x1_action_container, actionPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_2x1_action_button, actionPendingIntent)

        views.setTextViewText(R.id.widget_2x1_next_period, state.nextPeriodText)
        views.setTextViewText(R.id.widget_2x1_status, state.statusText)
        views.setImageViewResource(R.id.widget_2x1_action_button, state.actionIconResId)
        views.setContentDescription(R.id.widget_2x1_action_button, state.actionContentDescription)

        return views
    }

    /**
     * Calcola lo stato e i testi del widget.
     */
    fun computeState(context: Context): FastingWidgetState {
        val repository = try {
            (context.applicationContext as? CiboTimerApp)?.repository
                ?: AppRepository(context.applicationContext)
        } catch (e: Exception) {
            null
        }

        val session = repository?.currentSession?.value
        val activePlan = repository?.activePlan?.value

        if (session != null && !session.isCompleted) {
            val now = System.currentTimeMillis()
            val targetMillis = session.plannedEndTimeEpochMillis
            val isCurrentlyEating = (session.state == FastingState.EATING)

            val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayStr = getDayShortName(dayOfWeek)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            val diffMinutes = ((targetMillis - now) / 60000L).coerceAtLeast(0)
            val hours = diffMinutes / 60
            val mins = diffMinutes % 60
            val timeRemainingStr = "${hours}h ${mins}m"

            if (isCurrentlyEating) {
                // Finestra pasto aperta -> Prossimo evento: inizio digiuno
                val nextText = if (minute == 0) "Digiuno alle $hour $dayStr" else String.format(Locale.ITALY, "Digiuno alle %02d:%02d %s", hour, minute, dayStr)
                val status = "Pasto in corso • $timeRemainingStr"

                return FastingWidgetState(
                    nextPeriodText = nextText,
                    statusText = status,
                    actionIconResId = R.drawable.ic_widget_cutlery,
                    actionContentDescription = "Mangiare"
                )
            } else {
                // Digiuno in corso -> Prossimo evento: inizio finestra pasto
                val nextText = if (minute == 0) "Mangiare alle ore $hour $dayStr" else String.format(Locale.ITALY, "Mangiare alle %02d:%02d %s", hour, minute, dayStr)
                val status = "Digiuno in corso • $timeRemainingStr"

                return FastingWidgetState(
                    nextPeriodText = nextText,
                    statusText = status,
                    actionIconResId = R.drawable.ic_widget_flame,
                    actionContentDescription = "Digiunare"
                )
            }
        } else {
            // Nessuna sessione attiva (In pausa)
            val scheduled = computeNextScheduledFast(activePlan)
            val nextText = scheduled ?: "Nessun digiuno attivo"

            return FastingWidgetState(
                nextPeriodText = nextText,
                statusText = "In pausa • Tocca per avviare",
                actionIconResId = R.drawable.ic_widget_play,
                actionContentDescription = "Avvia"
            )
        }
    }

    /**
     * Gestisce il tap sul pulsante circolare di azione:
     * - Esegue la transizione di stato
     * - Aggiorna immediatamente il widget
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

    private fun computeNextScheduledFast(plan: FastingPlan?): String? {
        if (plan == null || plan.days.isEmpty()) return null
        val nowCal = Calendar.getInstance()
        val currentDay = when (val d = nowCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> d - 1
        }
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = nowCal.get(Calendar.MINUTE)

        for (offset in 0..6) {
            val checkDay = ((currentDay - 1 + offset) % 7) + 1
            val dayPlan = plan.days.find { it.dayOfWeek == checkDay } ?: continue
            if (!dayPlan.isFastingDay) continue

            if (offset == 0) {
                if (dayPlan.startHour > currentHour || (dayPlan.startHour == currentHour && dayPlan.startMinute > currentMinute)) {
                    val dayStr = getDayShortNameFrom1To7(checkDay)
                    return if (dayPlan.startMinute == 0) "Digiuno alle ${dayPlan.startHour} $dayStr" else String.format(Locale.ITALY, "Digiuno alle %02d:%02d %s", dayPlan.startHour, dayPlan.startMinute, dayStr)
                }
            } else {
                val dayStr = getDayShortNameFrom1To7(checkDay)
                return if (dayPlan.startMinute == 0) "Digiuno alle ${dayPlan.startHour} $dayStr" else String.format(Locale.ITALY, "Digiuno alle %02d:%02d %s", dayPlan.startHour, dayPlan.startMinute, dayStr)
            }
        }
        return null
    }

    private fun getDayShortName(calendarDayOfWeek: Int): String {
        return when (calendarDayOfWeek) {
            Calendar.MONDAY -> "Lun"
            Calendar.TUESDAY -> "Mar"
            Calendar.WEDNESDAY -> "Mer"
            Calendar.THURSDAY -> "Gio"
            Calendar.FRIDAY -> "Ven"
            Calendar.SATURDAY -> "Sab"
            Calendar.SUNDAY -> "Dom"
            else -> ""
        }
    }

    private fun getDayShortNameFrom1To7(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Lun"
            2 -> "Mar"
            3 -> "Mer"
            4 -> "Gio"
            5 -> "Ven"
            6 -> "Sab"
            7 -> "Dom"
            else -> ""
        }
    }
}
