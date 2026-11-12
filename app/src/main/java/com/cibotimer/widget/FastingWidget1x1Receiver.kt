package com.cibotimer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * AppWidgetProvider per il formato compatto "Digiuno 1x1".
 */
class FastingWidget1x1Receiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val state = FastingWidgetUpdater.computeState(context)
        for (id in appWidgetIds) {
            FastingWidgetUpdater.logWidgetDimensions(appWidgetManager, id, "1x1")
            val views = FastingWidgetUpdater.build1x1RemoteViews(context, state)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        FastingWidgetUpdater.logWidgetDimensions(appWidgetManager, appWidgetId, "1x1")
        val state = FastingWidgetUpdater.computeState(context)
        val views = FastingWidgetUpdater.build1x1RemoteViews(context, state)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == FastingWidgetUpdater.ACTION_UPDATE_ALL ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            FastingWidgetUpdater.updateAll(context)
        }
    }
}
