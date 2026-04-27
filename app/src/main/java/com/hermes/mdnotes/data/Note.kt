package com.hermes.mdnotes.data

import java.io.File

/**
 * 笔记实体 — 从 .md 文件中读取的元数据
 */
data class Note(
    /** 文件名（不含路径） */
    val fileName: String,
    /** 文件完整路径 */
    val filePath: String,
    /** 标题（取自第一个 # heading，无则用文件名） */
    val title: String,
    /** 内容预览（前 200 字符，去除 Markdown 语法） */
    val preview: String,
    /** 最后修改时间 */
    val lastModified: Long,
) {
    /** 关联的 File 对象 */
    val file: File get() = File(filePath)

    companion object {
        /** 从 File 读取元数据（不加载全文） */
        fun fromFile(file: File): Note {
            // 只读头部 2KB，不读全文（大幅提速）
            val rawText = try {
                file.inputStream().bufferedReader().use { reader ->
                    val buf = CharArray(2048)
                    val n = reader.read(buf)
                    if (n > 0) String(buf, 0, n) else ""
                }
            } catch (e: Exception) {
                ""
            }

            // 提取标题：优先 # heading，回退到文件名（去时间戳前缀）
            val title = rawText.lines()
                .firstOrNull { it.trimStart().startsWith("# ") }
                ?.trimStart()
                ?.removePrefix("# ")
                ?.trim()
                ?: file.name.removeSuffix(".md").substringAfter('_')

            // 提取预览：跳过标题行，取前 200 字符
            val preview = rawText.lines()
                .dropWhile { it == title || it.trimStart().startsWith("# ") }
                .joinToString(" ")
                .replace(Regex("[#*`~>\\[\\]()]"), "")
                .trim()
                .take(200)

            return Note(
                fileName = file.name,
                filePath = file.absolutePath,
                title = title,
                preview = preview,
                lastModified = file.lastModified(),
            )
        }
    }
}
