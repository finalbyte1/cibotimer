package com.cibotimer.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver per gestire i clic sul pulsante di azione del widget
 * ed eseguire la transizione di digiuno/alimentazione.
 */
class FastingWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == FastingWidgetUpdater.ACTION_WIDGET_BUTTON_CLICK) {
            FastingWidgetUpdater.handleActionButtonClick(context)
        }
    }
}
