package com.hermes.mdnotes.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.runBlocking

class WidgetRefreshService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ctx = this
        runBlocking {
            try {
                val manager = GlanceAppWidgetManager(ctx)
                val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                glanceIds.forEach { NotesWidget().update(ctx, it) }
            } catch (_: Exception) {}
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
