package com.hermes.mdnotes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hermes.mdnotes.data.NotesRepository
import com.hermes.mdnotes.data.PreferencesManager
import com.hermes.mdnotes.editor.EditorActivity
import java.text.SimpleDateFormat
import java.util.*

class NotesWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = NotesRepository.getInstance(context)
        val savedDir = PreferencesManager.getNotesDirectory(context)
        if (savedDir != null) repo.setNotesDirectory(savedDir)
        repo.ensureDirectory()
        repo.refreshNotes()
        val notes = repo.filteredNotes(sortByModified = true).take(10)
        val dateFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        provideContent {
            GlanceTheme {
                Content(context = context, notes = notes, dateFmt = dateFmt)
            }
        }
    }

    @Composable
    private fun Content(
        context: Context,
        notes: List<com.hermes.mdnotes.data.Note>,
        dateFmt: SimpleDateFormat
    ) {
        Scaffold(
            titleBar = {
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 MD 笔记",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    IconRefresh(context)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    IconNewNote(context)
                }
            },
            horizontalPadding = 8.dp
        ) {
            Text(
                text = if (notes.isEmpty()) "暂无笔记"
                       else "${notes.size} 条 · 按时间↓",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.padding(bottom = 4.dp)
            )

            if (notes.isEmpty()) {
                Text(
                    text = "点击 ➕ 创建第一条笔记",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp
                    ),
                    modifier = GlanceModifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    items(notes) { note ->
                        NoteItem(context, note, dateFmt)
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteItem(
        context: Context,
        note: com.hermes.mdnotes.data.Note,
        dateFmt: SimpleDateFormat
    ) {
        val intent = Intent(context, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, note.filePath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(4.dp)
                .clickable(actionStartActivity(intent))
                .padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = dateFmt.format(Date(note.lastModified)),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
            if (note.preview.isNotBlank()) {
                Text(
                    text = note.preview,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    modifier = GlanceModifier.padding(top = 2.dp)
                )
            }
        }
    }

    @Composable
    private fun IconRefresh(context: Context) {
        Text(
            text = "🔄",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 16.sp
            ),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>())
        )
    }

    @Composable
    private fun IconNewNote(context: Context) {
        val intent = Intent(context, EditorActivity::class.java)
        Text(
            text = "➕",
            style = TextStyle(fontSize = 20.sp),
            modifier = GlanceModifier.clickable(actionStartActivity(intent))
        )
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NotesWidget().update(context, glanceId)
    }
}
