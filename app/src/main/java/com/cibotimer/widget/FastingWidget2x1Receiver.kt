package com.cibotimer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * AppWidgetProvider per il formato orizzontale "Digiuno 2x1".
 */
open class FastingWidget2x1Receiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val state = FastingWidgetUpdater.computeState(context)
        for (id in appWidgetIds) {
            val views = FastingWidgetUpdater.build2x1RemoteViews(context, state)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == FastingWidgetUpdater.ACTION_UPDATE_ALL ||
            action == "android.appwidget.action.APPWIDGET_UPDATE"
        ) {
            FastingWidgetUpdater.updateAll(context)
        }
    }
}
