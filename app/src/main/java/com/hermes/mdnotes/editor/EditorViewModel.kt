package com.hermes.mdnotes.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mdnotes.MdNotesApp
import com.hermes.mdnotes.data.Note
import com.hermes.mdnotes.data.NotesRepository
import com.hermes.mdnotes.widget.NotesWidgetReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 编辑器 ViewModel — 管理编辑状态和自动保存
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotesRepository = (application as MdNotesApp).notesRepository

    // ── UI 状态 ──────────────────────────────────

    private val _note = MutableStateFlow<Note?>(null)
    val note = _note.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _showPreview = MutableStateFlow(false)
    val showPreview = _showPreview.asStateFlow()

    private val _isNewNote = MutableStateFlow(false)
    val isNewNote = _isNewNote.asStateFlow()

    // ── 自动保存 ─────────────────────────────────

    private var autoSaveJob: Job? = null
    private var hasChanges = false

    /**
     * 加载已有笔记进行编辑
     */
    fun loadNote(filePath: String) {
        val content = repository.readNoteContent(filePath)
        val note = repository.filteredNotes().find { it.filePath == filePath }
        if (note != null) {
            _note.value = note
            _title.value = note.title
            _content.value = content
            _isNewNote.value = false
        }
    }

    /**
     * 初始化新建笔记
     */
    fun initNewNote() {
        _isNewNote.value = true
        _title.value = ""
        _content.value = ""
        _note.value = null
    }

    // ── 编辑 ─────────────────────────────────────

    fun onTitleChanged(newTitle: String) {
        _title.value = newTitle
        // 已有笔记立即重命名文件
        val note = _note.value
        if (note != null && newTitle.isNotBlank()) {
            val renamed = repository.renameNote(note.filePath, newTitle)
            if (renamed != null) {
                _note.value = renamed
            }
        }
        scheduleAutoSave()
    }

    fun onContentChanged(newContent: String) {
        _content.value = newContent
        scheduleAutoSave()
    }

    fun togglePreview() {
        _showPreview.value = !_showPreview.value
    }

    // ── 保存 ─────────────────────────────────────

    fun saveNow() {
        val t = _title.value.ifBlank { "未命名笔记" }
        val c = _content.value

        val existing = _note.value
        if (existing != null) {
            repository.saveNote(existing.filePath, c)
        } else {
            // 新建笔记
            val newNote = repository.createNote(t, c)
            if (newNote != null) {
                _note.value = newNote
                _isNewNote.value = false
            }
        }
        _isSaving.value = true
        hasChanges = false
        // 通知 Widget 刷新
        NotesWidgetReceiver.triggerUpdate(getApplication())
        viewModelScope.launch {
            delay(500)
            _isSaving.value = false
        }
    }

    /** 仅在有修改时才保存，用于返回时 */
    fun saveIfChanged() {
        if (hasChanges) saveNow()
    }

    private fun scheduleAutoSave() {
        hasChanges = true
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // 2 秒防抖
            if (hasChanges) {
                saveNow()
            }
        }
    }

    // ── 删除 ─────────────────────────────────────

    fun deleteNote(): Boolean {
        val path = _note.value?.filePath ?: return false
        return repository.deleteNote(path)
    }

    // ── 生命周期 ─────────────────────────────────

    override fun onCleared() {
        if (hasChanges) {
            val t = _title.value.ifBlank { "未命名笔记" }
            val c = _content.value
            val existing = _note.value
            if (existing != null) {
                repository.saveNote(existing.filePath, c)
            } else if (c.isNotBlank()) {
                repository.createNote(t, c)
            }
            NotesWidgetReceiver.triggerUpdate(getApplication())
        }
        super.onCleared()
    }
}
