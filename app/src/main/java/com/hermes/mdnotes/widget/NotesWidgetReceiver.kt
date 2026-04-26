package com.hermes.mdnotes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: NotesWidget = NotesWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    /**
     * Android 12+ resize 时系统发送 APPWIDGET_UPDATE 或 OPTIONS_CHANGED
     * 显式触发 update 确保 Widget 用新尺寸重绘
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // resize 时强制刷新
        CoroutineScope(Dispatchers.Main).launch {
            try {
                glanceAppWidget.update(context, androidx.glance.appwidget.GlanceAppWidgetManager(context)
                    .getGlanceIdBy(appWidgetId))
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            triggerUpdate(context)
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
