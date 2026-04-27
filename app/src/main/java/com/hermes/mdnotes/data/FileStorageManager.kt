package com.hermes.mdnotes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // ── 目录迁移 ──────────────────────────────────

    fun migrateFilesFrom(sourceDir: File): Int {
        if (!sourceDir.exists() || !sourceDir.isDirectory) return 0
        ensureDirectory()
        var count = 0
        sourceDir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) }
            ?.forEach { sourceFile ->
                val destFile = File(notesDir, sourceFile.name)
                if (!destFile.exists()) {
                    try {
                        sourceFile.copyTo(destFile)
                        count++
                    } catch (e: Exception) {
                        android.util.Log.w("FileStorageManager", "Failed to copy: ${sourceFile.name}", e)
                    }
                }
            }
        return count
    }

    // ── 读取 ──────────────────────────────────────

    fun listNotesFlow(): Flow<List<Note>> = flow {
        val notes = withContext(Dispatchers.IO) {
            listNotesSync()
        }
        emit(notes)
    }

    fun listNotesSync(): List<Note> {
        val dir = ensureDirectory()
        return dir.listFiles { f -> f.isFile && f.extension.equals("md", ignoreCase = true) }
            ?.map { Note.fromFile(it) }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    fun readNote(filePath: String): String {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            ""
        }
    }

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

    /** 保存笔记，内部标题变化时自动重命名文件，返回新路径 */
    fun saveNote(filePath: String, title: String, content: String): String? {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            val fullContent = if (content.trimStart().startsWith("# ")) {
                content
            } else {
                "# $title\n\n$content"
            }
            file.writeText(fullContent)

            // markdown 内部标题
            val internalTitle = fullContent.lines()
                .firstOrNull { it.trimStart().startsWith("# ") }
                ?.trimStart()?.removePrefix("# ")?.trim()
                ?: title

            // 新文件名：时间戳_内部标题
            val safeTitle = internalTitle.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(50)
            val ts = file.name.substringBefore('_')
            val newName = "${ts}_$safeTitle.md"

            if (newName != file.name) {
                val newFile = File(file.parentFile, newName)
                if (file.renameTo(newFile)) newFile.absolutePath else filePath
            } else {
                filePath
            }
        } catch (e: Exception) {
            android.util.Log.e("FileStorageManager", "saveNote failed: $filePath", e)
            null
        }
    }

    /** 新建笔记 */
    fun createNote(title: String, initialContent: String = ""): Note? {
        return try {
            ensureDirectory()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(50)
            val fileName = "${timestamp}_$safeTitle.md"
            val file = File(notesDir, fileName)
            val content = "# $title\n\n$initialContent"
            file.writeText(content)
            Note(
                fileName = fileName,
                filePath = file.absolutePath,
                title = title,
                preview = initialContent.take(200),
                lastModified = file.lastModified(),
            )
        } catch (e: Exception) {
            android.util.Log.e("FileStorageManager", "createNote failed", e)
            null
        }
    }

    // ── 删除与重命名 ──────────────────────────────

    fun deleteNote(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /** 手动重命名，保留时间戳前缀 */
    fun renameNote(oldPath: String, newTitle: String): Note? {
        val oldFile = File(oldPath)
        if (!oldFile.exists()) return null
        val safeTitle = newTitle.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(50)
        val ts = oldFile.name.substringBefore('_')
        val newName = "${ts}_$safeTitle.md"
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
