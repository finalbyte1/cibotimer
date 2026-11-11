package com.cibotimer.widget

import android.content.Context

/**
 * Retrocompatibilità per i widget già posizionati con CiboTimerWidgetProvider (v1.0.44/v1.0.45).
 * Eredita direttamente da FastingWidget2x1Receiver e inoltra tutti gli aggiornamenti a FastingWidgetUpdater.
 */
class CiboTimerWidgetProvider : FastingWidget2x1Receiver() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.cibotimer.widget.ACTION_UPDATE_WIDGET"

        fun updateAll(context: Context) {
            FastingWidgetUpdater.updateAll(context)
        }
    }
}
