package com.hermes.mdnotes.widget

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.runBlocking

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: NotesWidget = NotesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            Log.d(TAG, "Received ACTION_REFRESH broadcast")
            doUpdate(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.hermes.mdnotes.ACTION_REFRESH_WIDGET"
        private const val TAG = "WidgetReceiver"

        /**
         * 同步触发 Widget 刷新 — 使用 runBlocking 确保执行完毕
         * 调用方应在后台线程调用避免阻塞 UI
         */
        fun triggerUpdate(context: Context) {
            Thread {
                try {
                    doUpdate(context)
                    Log.d(TAG, "triggerUpdate completed")
                } catch (e: Exception) {
                    Log.e(TAG, "triggerUpdate failed", e)
                }
            }.start()
        }

        private fun doUpdate(context: Context) {
            runBlocking {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(NotesWidget::class.java)
                    Log.d(TAG, "Updating ${glanceIds.size} widget instances")
                    glanceIds.forEach { NotesWidget().update(context, it) }
                } catch (e: Exception) {
                    Log.e(TAG, "doUpdate failed", e)
                }
            }
        }
    }
}
