package com.hermes.mdnotes.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hermes.mdnotes.MainActivity
import com.hermes.mdnotes.editor.EditorActivity

/**
 * Glance Widget — 桌面笔记小部件
 * 显示笔记列表，支持点击编辑和新建
 */
class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = WidgetDataProvider.loadNotes(context)

        provideContent {
            NotesWidgetContent(context, notes)
        }
    }

    @Composable
    private fun NotesWidgetContent(context: Context, notes: List<WidgetNote>) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(ColorProvider(Color(0xFF1C1B1F))),  // 深色背景
            ) {
                // ── 顶部标题栏 ──────────────────────
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 标题
                    Text(
                        text = "📝 Markdown 笔记",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA5D6A7)),
                            fontSize = 16.sp,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )

                    // 新建按钮
                    Image(
                        provider = BitmapFactory.decodeResource(
                            context.resources,
                            android.R.drawable.ic_input_add
                        ).let { ImageProvider(it) },
                        contentDescription = "新建",
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionStartActivity(
                                Intent(context, EditorActivity::class.java)
                            )),
                    )

                    // 打开应用按钮
                    Image(
                        provider = BitmapFactory.decodeResource(
                            context.resources,
                            android.R.drawable.ic_menu_edit
                        ).let { ImageProvider(it) },
                        contentDescription = "打开",
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionStartActivity(
                                Intent(context, MainActivity::class.java)
                            )),
                    )
                }

                // ── 笔记计数 ────────────────────────
                Text(
                    text = "${notes.size} 条笔记",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF888888)),
                        fontSize = 11.sp,
                    ),
                    modifier = GlanceModifier.padding(bottom = 4.dp),
                )

                // ── 笔记列表 ────────────────────────
                if (notes.isEmpty()) {
                    Text(
                        text = "暂无笔记\n长按桌面添加 Widget 后\n点击右下角 + 创建",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF666666)),
                            fontSize = 13.sp,
                        ),
                        modifier = GlanceModifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxWidth(),
                    ) {
                        items(notes) { note ->
                            NoteWidgetItem(context, note)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteWidgetItem(context: Context, note: WidgetNote) {
        // 点击打开编辑器
        val editIntent = Intent(context, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, note.filePath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .background(ColorProvider(Color(0xFF2D2D30)))
                .cornerRadius(6.dp)
                .clickable(actionStartActivity(editIntent))
                .padding(8.dp),
        ) {
            // 标题
            Text(
                text = note.title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE6E1E5)),
                    fontSize = 14.sp,
                ),
                maxLines = 1,
            )

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
