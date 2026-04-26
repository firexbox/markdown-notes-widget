package com.hermes.mdnotes.widget

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
        val notes = WidgetDataProvider.loadNotes(context)
        val newNoteIntent = Intent(context, EditorActivity::class.java)
        val dateFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(ColorProvider(Color(0xFF1C1B1F))),
                ) {
                    // ── 标题栏 ──────────────────────
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "📝 MD 笔记",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFA5D6A7)),
                                fontSize = 16.sp,
                            ),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Image(
                            provider = BitmapFactory.decodeResource(
                                context.resources,
                                android.R.drawable.ic_input_add
                            ).let { ImageProvider(it) },
                            contentDescription = "新建",
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(actionStartActivity(newNoteIntent)),
                        )
                    }

                    // ── 计数 + 排序说明 ──────────────
                    Text(
                        text = if (notes.isEmpty()) "暂无笔记"
                               else "${notes.size} 条 · 按时间↓",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 11.sp,
                        ),
                        modifier = GlanceModifier.padding(bottom = 4.dp),
                    )

                    // ── 笔记列表 ────────────────────
                    if (notes.isEmpty()) {
                        Text(
                            text = "点击右上角 + 创建第一条笔记",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF666666)),
                                fontSize = 13.sp,
                            ),
                            modifier = GlanceModifier.padding(vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                            items(notes) { note ->
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
                                    // 标题行 + 时间
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = note.title,
                                            style = TextStyle(
                                                color = ColorProvider(Color(0xFFE6E1E5)),
                                                fontSize = 14.sp,
                                            ),
                                            maxLines = 1,
                                            modifier = GlanceModifier.defaultWeight(),
                                        )
                                        Text(
                                            text = dateFmt.format(Date(note.lastModified)),
                                            style = TextStyle(
                                                color = ColorProvider(Color(0xFF666666)),
                                                fontSize = 10.sp,
                                            ),
                                        )
                                    }
                                    // 预览
                                    if (note.preview.isNotBlank()) {
                                        Text(
                                            text = note.preview,
                                            style = TextStyle(
                                                color = ColorProvider(Color(0xFF999999)),
                                                fontSize = 11.sp,
                                            ),
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
}
