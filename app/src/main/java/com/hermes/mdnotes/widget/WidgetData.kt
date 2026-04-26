package com.hermes.mdnotes.widget

import android.content.Context
import android.util.Log
import com.hermes.mdnotes.data.NotesRepository
import com.hermes.mdnotes.data.PreferencesManager

data class WidgetNote(
    val title: String,
    val preview: String,
    val filePath: String,
    val lastModified: Long,
)

object WidgetDataProvider {

    fun loadNotes(context: Context): List<WidgetNote> {
        return try {
            val repo = NotesRepository.getInstance(context)
            val savedDir = PreferencesManager.getNotesDirectory(context)
            if (savedDir != null) {
                repo.setNotesDirectory(savedDir)
            }
            repo.ensureDirectory()
            repo.refreshNotes()
            // 按修改时间降序，最新的在前
            repo.filteredNotes(sortByModified = true)
                .take(8)
                .map { n ->
                    WidgetNote(
                        title = n.title,
                        preview = n.preview,
                        filePath = n.filePath,
                        lastModified = n.lastModified,
                    )
                }
        } catch (e: Exception) {
            Log.w("WidgetDataProvider", "loadNotes failed", e)
            emptyList()
        }
    }
}
