package com.hermes.mdnotes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件存储管理器 — 笔记文件的 CRUD 操作
 */
class FileStorageManager(private var notesDir: File) {

    // ── 目录管理 ──────────────────────────────────

    fun getNotesDirectory(): File = notesDir

    fun setNotesDirectory(dir: File) {
        notesDir = dir
    }

    fun ensureDirectory(): File {
        if (!notesDir.exists()) {
            notesDir.mkdirs()
        }
        return notesDir
    }

    // ── 读取 ──────────────────────────────────────

    /** 列出目录中所有 .md 文件，转为 Note 列表（Flow 方式） */
    fun listNotesFlow(): Flow<List<Note>> = flow {
        val notes = withContext(Dispatchers.IO) {
            listNotesSync()
        }
        emit(notes)
    }

    /** 同步列出笔记（供 Widget 调用） */
    fun listNotesSync(): List<Note> {
        val dir = ensureDirectory()
        return dir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) }
            ?.map { Note.fromFile(it) }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    /** 读取笔记完整内容 */
    fun readNote(filePath: String): String {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            ""
        }
    }

    /** 读取笔记完整内容（同步，含元数据） */
    fun readNoteFull(filePath: String): Note? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val content = file.readText()
                val title = content.lines()
                    .firstOrNull { it.trimStart().startsWith("# ") }
                    ?.trimStart()
                    ?.removePrefix("# ")
                    ?.trim()
                    ?: file.nameWithoutExtension
                Note(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    title = title,
                    preview = content.take(200),
                    lastModified = file.lastModified(),
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // ── 写入 ──────────────────────────────────────

    /** 保存笔记（覆盖写入） */
    fun saveNote(filePath: String, title: String, content: String) {
        val file = File(filePath)
        file.parentFile?.mkdirs()

        // 如果内容不以标题开头，自动添加标题
        val fullContent = if (content.trimStart().startsWith("# ")) {
            content
        } else {
            "# $title\n\n$content"
        }
        file.writeText(fullContent)
    }

    /** 新建笔记 */
    fun createNote(title: String, initialContent: String = ""): Note {
        ensureDirectory()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(50)
        val fileName = "${timestamp}_$safeTitle.md"
        val file = File(notesDir, fileName)
        val content = "# $title\n\n$initialContent"
        file.writeText(content)
        return Note(
            fileName = fileName,
            filePath = file.absolutePath,
            title = title,
            preview = initialContent.take(200),
            lastModified = file.lastModified(),
        )
    }

    // ── 删除与重命名 ──────────────────────────────

    fun deleteNote(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    fun renameNote(oldPath: String, newTitle: String): Note? {
        val oldFile = File(oldPath)
        if (!oldFile.exists()) return null
        val safeTitle = newTitle.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(50)
        val newName = safeTitle.endsWith(".md", ignoreCase = true).let {
            if (it) safeTitle else "$safeTitle.md"
        }
        val newFile = File(oldFile.parentFile, newName)
        return if (oldFile.renameTo(newFile)) {
            readNoteFull(newFile.absolutePath)
        } else null
    }

    // ── 搜索 ──────────────────────────────────────

    fun searchNotes(query: String): List<Note> {
        if (query.isBlank()) return listNotesSync()
        return listNotesSync().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.preview.contains(query, ignoreCase = true)
        }
    }
}
