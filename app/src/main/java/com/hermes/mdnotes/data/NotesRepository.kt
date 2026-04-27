package com.hermes.mdnotes.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * 笔记仓库 — 应用层数据源
 * 单例封装 FileStorageManager，提供响应式数据流
 */
class NotesRepository private constructor(private val fileManager: FileStorageManager) {

    // ── 可观察状态 ────────────────────────────────

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: Flow<List<Note>> = _notes.asStateFlow()

    private val _notesDir = MutableStateFlow<File?>(null)
    val notesDir: Flow<File?> = _notesDir.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: Flow<String> = _searchQuery.asStateFlow()

    // ── 目录管理 ──────────────────────────────────

    fun setNotesDirectory(dir: File) {
        fileManager.setNotesDirectory(dir)
        _notesDir.value = dir
        refreshNotes()
    }

    fun getNotesDirectory(): File? = _notesDir.value

    fun getFileManager(): FileStorageManager = fileManager

    fun ensureDirectory(): File = fileManager.ensureDirectory()

    // ── 笔记操作 ──────────────────────────────────

    fun refreshNotes() {
        _notes.value = fileManager.listNotesSync()
    }

    fun createNote(title: String, content: String = ""): Note? {
        val note = fileManager.createNote(title, content)
        if (note != null) refreshNotes()
        return note
    }

    fun saveNote(filePath: String, title: String, content: String): String? {
        val newPath = fileManager.saveNote(filePath, title, content)
        if (newPath != null) refreshNotes()
        return newPath
    }

    fun deleteNote(filePath: String): Boolean {
        val result = fileManager.deleteNote(filePath)
        if (result) refreshNotes()
        return result
    }

    fun renameNote(oldPath: String, newTitle: String): Note? {
        val note = fileManager.renameNote(oldPath, newTitle)
        if (note != null) refreshNotes()
        return note
    }

    fun readNoteContent(filePath: String): String {
        return fileManager.readNote(filePath)
    }

    // ── 搜索 ──────────────────────────────────────

    fun search(query: String) {
        _searchQuery.value = query
        _notes.value = fileManager.searchNotes(query)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        refreshNotes()
    }

    /** 已过滤 + 已排序的笔记列表 */
    fun filteredNotes(
        query: String = "",
        sortByModified: Boolean = true
    ): List<Note> {
        val base = if (query.isBlank()) fileManager.listNotesSync()
        else fileManager.searchNotes(query)
        return if (sortByModified) {
            base.sortedByDescending { it.lastModified }
        } else {
            base.sortedBy { it.title.lowercase() }
        }
    }

    // ── 单例 ──────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: NotesRepository? = null

        fun getInstance(context: Context): NotesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val dir = PreferencesManager.getNotesDirectory(context)
                        ?: File(context.filesDir, "notes").also { it.mkdirs() }
                    val repo = NotesRepository(FileStorageManager(dir))
                    INSTANCE = repo
                    repo
                }
            }
        }
    }
}
