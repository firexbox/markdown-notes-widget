package com.hermes.mdnotes.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: NotesWidget = NotesWidget()

    companion object {
        /**
         * 从主 App 触发 Widget 刷新
         */
        fun triggerUpdate(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    glanceIds.forEach { NotesWidget().update(context, it) }
                } catch (_: Exception) {}
            }
        }
    }
}
