package com.hermes.mdnotes.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WidgetRefreshService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(this@WidgetRefreshService)
                val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                val widget = NotesWidget()
                glanceIds.forEach { widget.update(this@WidgetRefreshService, it) }
            } catch (_: Exception) {}
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
