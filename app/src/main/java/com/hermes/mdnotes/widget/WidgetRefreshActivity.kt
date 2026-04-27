package com.hermes.mdnotes.widget

import android.app.Activity
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.runBlocking

class WidgetRefreshActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 同步执行更新，确保在 finish 前完成
        runBlocking {
            try {
                val manager = GlanceAppWidgetManager(this@WidgetRefreshActivity)
                val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                glanceIds.forEach { NotesWidget().update(this@WidgetRefreshActivity, it) }
            } catch (_: Exception) {}
        }
        finish()
    }
}
