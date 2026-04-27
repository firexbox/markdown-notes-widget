package com.hermes.mdnotes.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.*

class WidgetRefreshService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 主线程协程执行 Glance update（必须主线程）
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val manager = GlanceAppWidgetManager(this@WidgetRefreshService)
                val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                glanceIds.forEach { NotesWidget().update(this@WidgetRefreshService, it) }
            } catch (_: Exception) {}
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
