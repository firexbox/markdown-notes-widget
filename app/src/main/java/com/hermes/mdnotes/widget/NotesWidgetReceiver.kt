package com.hermes.mdnotes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
         * 主程序触发 Widget 刷新 — Glance.update + AppWidgetManager 双重保障
         */
        fun triggerUpdate(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    glanceIds.forEach { NotesWidget().update(context, it) }
                    // 额外通知系统强制重绘，对标 updateAppWidget()
                    val awm = AppWidgetManager.getInstance(context)
                    val cn = ComponentName(context, NotesWidgetReceiver::class.java)
                    awm.getAppWidgetIds(cn).forEach { awm.notifyAppWidgetViewDataChanged(it, android.R.id.background) }
                } catch (_: Exception) {}
            }
        }
    }
}
