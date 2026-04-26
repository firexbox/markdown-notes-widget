package com.hermes.mdnotes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget Receiver — 系统通过它管理 Widget 实例
 */
class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: NotesWidget = NotesWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            triggerUpdate(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.hermes.mdnotes.ACTION_REFRESH_WIDGET"

        /**
         * 主动触发 Widget 刷新（从 MainActivity.onResume 调用）
         */
        fun triggerUpdate(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    val widget = NotesWidget()
                    glanceIds.forEach { glanceId ->
                        widget.update(context, glanceId)
                    }
                } catch (e: Exception) {
                    // Widget 未添加时忽略
                }
            }
        }
    }
}
