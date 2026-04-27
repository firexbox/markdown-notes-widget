package com.hermes.mdnotes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hermes.mdnotes.editor.EditorActivity
import java.text.SimpleDateFormat
import java.util.*

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val allNotes = WidgetDataProvider.loadNotes(context)
        val newNoteIntent = Intent(context, EditorActivity::class.java)
        val refreshIntent = Intent(context, WidgetRefreshService::class.java)
        val dateFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        provideContent {
            GlanceTheme {
                // 计算可见条数：优先 LocalSize，失败用 AppWidgetManager，再失败默认 4
                val heightPx = LocalSize.current.height.value
                val maxItems = if (heightPx > 0) {
                    // 头部约 75dp(标题+计数)，每条约 56dp
                    val heightDp = heightPx / context.resources.displayMetrics.density
                    maxOf(1, ((heightDp - 75f) / 56f).toInt())
                } else {
                    getItemsFromOptions(context, id)
                }
                val displayNotes = allNotes.take(maxItems)
                val hidden = allNotes.size - displayNotes.size

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(ColorProvider(Color(0xFF1C1B1F))),
                ) {
                    // ── 标题栏 ──────────────────────
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "📝 MD 笔记",
                            style = TextStyle(color = ColorProvider(Color(0xFFA5D6A7)), fontSize = 16.sp),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(
                            text = "🔄",
                            style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 16.sp),
                            modifier = GlanceModifier.clickable(actionStartService(refreshIntent)),
                        )
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        Image(
                            provider = BitmapFactory.decodeResource(
                                context.resources, android.R.drawable.ic_input_add
                            ).let { ImageProvider(it) },
                            contentDescription = "新建",
                            modifier = GlanceModifier.size(28.dp)
                                .clickable(actionStartActivity(newNoteIntent)),
                        )
                    }

                    // ── 计数 ────────────────────────
                    val countText = when {
                        allNotes.isEmpty() -> "暂无笔记"
                        hidden > 0 -> "${displayNotes.size}/${allNotes.size} 条 · 按时间↓"
                        else -> "${allNotes.size} 条 · 按时间↓"
                    }
                    Text(
                        text = countText,
                        style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp),
                        modifier = GlanceModifier.padding(bottom = 4.dp),
                    )

                    // ── 笔记列表（无滚动，仅显示能容纳的条数）──
                    if (displayNotes.isEmpty()) {
                        Text(
                            text = "点击右上角 + 创建第一条笔记",
                            style = TextStyle(color = ColorProvider(Color(0xFF666666)), fontSize = 13.sp),
                            modifier = GlanceModifier.padding(vertical = 16.dp),
                        )
                    } else {
                        // 用 Column 而非 LazyColumn，不产生滚动
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            displayNotes.forEach { note ->
                                val editIntent = Intent(context, EditorActivity::class.java).apply {
                                    putExtra(EditorActivity.EXTRA_FILE_PATH, note.filePath)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                Column(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(ColorProvider(Color(0xFF2D2D30)))
                                        .padding(8.dp)
                                        .clickable(actionStartActivity(editIntent)),
                                ) {
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = note.title,
                                            style = TextStyle(color = ColorProvider(Color(0xFFE6E1E5)), fontSize = 14.sp),
                                            maxLines = 1,
                                            modifier = GlanceModifier.defaultWeight(),
                                        )
                                        Text(
                                            text = dateFmt.format(Date(note.lastModified)),
                                            style = TextStyle(color = ColorProvider(Color(0xFF666666)), fontSize = 10.sp),
                                        )
                                    }
                                    if (note.preview.isNotBlank()) {
                                        Text(
                                            text = note.preview,
                                            style = TextStyle(color = ColorProvider(Color(0xFF999999)), fontSize = 11.sp),
                                            maxLines = 2,
                                            modifier = GlanceModifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** AppWidgetManager 回退方案获取可见条数 */
    private fun getItemsFromOptions(context: Context, glanceId: GlanceId): Int {
        return try {
            val appWidgetId = glanceId.toString().toIntOrNull() ?: return 4
            val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            val h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200)
            maxOf(1, ((h - 75) / 56))
        } catch (_: Exception) {
            4
        }
    }
}
