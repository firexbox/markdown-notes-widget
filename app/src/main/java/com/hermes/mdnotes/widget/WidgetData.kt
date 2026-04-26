package com.hermes.mdnotes.widget

import android.content.Context
import com.hermes.mdnotes.data.NotesRepository
import java.io.File

/**
 * Widget 数据 — 简化版笔记信息，用于 Widget 显示
 */
data class WidgetNote(
    val title: String,
    val preview: String,
    val filePath: String,
)

/**
 * Widget 数据提供者 — 从 Repository 加载数据
 */
object WidgetDataProvider {

    fun loadNotes(context: Context): List<WidgetNote> {
        return try {
            val repo = NotesRepository.getInstance(context)
            // 确保目录存在后刷新
            repo.ensureDirectory()
            repo.refreshNotes()
            // 取前 8 条（Widget 空间有限）
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
            emptyList()
        }
    }
}
