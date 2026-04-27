package com.hermes.mdnotes.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: NotesWidget = NotesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    val widget = NotesWidget()
                    glanceIds.forEach { widget.update(context, it) }
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.hermes.mdnotes.ACTION_REFRESH_WIDGET"

        fun triggerUpdate(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    val widget = NotesWidget()
                    glanceIds.forEach { widget.update(context, it) }
                } catch (_: Exception) {}
            }
        }
    }
}
