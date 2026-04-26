package com.hermes.mdnotes.widget

import android.content.Context
import android.util.Log
import com.hermes.mdnotes.data.NotesRepository
import com.hermes.mdnotes.data.PreferencesManager
import java.io.File

data class WidgetNote(
    val title: String,
    val preview: String,
    val filePath: String,
)

object WidgetDataProvider {

    fun loadNotes(context: Context): List<WidgetNote> {
        return try {
            val repo = NotesRepository.getInstance(context)
            // 同步目录：优先用持久化路径
            val savedDir = PreferencesManager.getNotesDirectory(context)
            if (savedDir != null) {
                repo.setNotesDirectory(savedDir)
            }
            repo.ensureDirectory()
            repo.refreshNotes()
            repo.filteredNotes(sortByModified = true)
                .take(8)
                .map { n ->
                    WidgetNote(
                        title = n.title,
                        preview = n.preview,
                        filePath = n.filePath,
                    )
                }
        } catch (e: Exception) {
            Log.w("WidgetDataProvider", "loadNotes failed", e)
            emptyList()
        }
    }
}
