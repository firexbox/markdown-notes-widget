package com.hermes.mdnotes.editor

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(content: String, notesDir: String? = null, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val bg = remember { colorScheme.surface.toArgb() }
    val text = remember { colorScheme.onSurface.toArgb() }
    val code = remember { colorScheme.surfaceVariant.toArgb() }
    val link = remember { colorScheme.primary.toArgb() }
    val border = remember { colorScheme.outline.toArgb() }

    val parser = remember {
        Parser.builder()
            .extensions(listOf(
                StrikethroughExtension.create(),
                TablesExtension.create()
            ))
            .build()
    }
    val renderer = remember { HtmlRenderer.builder().build() }

    // 预处理 ![[filename]] → HTML 标签
    val processed = remember(content, notesDir) {
        processEmbeds(content, notesDir)
    }

    val body = remember(processed, parser, renderer) {
        try {
            val doc = parser.parse(processed)
            renderer.render(doc)
        } catch (e: Exception) { processed }
    }

    val isDark = colorScheme.background.toArgb().let { c ->
        android.graphics.Color.luminance(c) < 0.5
    }
    val cs = if (isDark) "dark" else "light"

    val html = remember(body, bg, text, code, link, border, cs) {
        buildTemplate(body, cs, bg, text, code, link, border)
    }

    // 附件目录作为 base URL，使 file:// 路径可用
    val baseUrl = remember(notesDir) {
        notesDir?.let { "file://${it}/附件/" } ?: "about:blank"
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(bg)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.allowFileAccess = true
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
}

/** 将 ![[filename]] 替换为对应的 HTML 标签 */
private fun processEmbeds(content: String, notesDir: String?): String {
    val attachDir = notesDir?.let { File(it, "附件") }
    val regex = Regex("""!\[\[([^\]]+)\]\]""")
    return regex.replace(content) { match ->
        val filename = match.groupValues[1].trim()
        val file = attachDir?.let { File(it, filename) }
        val filePath = file?.absolutePath

        if (filePath != null && file.exists()) {
            val ext = filename.substringAfterLast('.', "").lowercase()
            when {
                ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") ->
                    """<img src="file://$filePath" alt="$filename" style="max-width:100%;border-radius:8px;margin:8px 0;">"""
                ext in listOf("mp4", "webm", "mkv", "avi", "mov") ->
                    """<video controls width="100%" style="max-width:100%;margin:8px 0;"><source src="file://$filePath" type="video/${if(ext=="mov")"mp4" else ext}"></video>"""
                ext in listOf("mp3", "wav", "ogg", "flac", "aac", "m4a") ->
                    """<audio controls style="width:100%;margin:8px 0;"><source src="file://$filePath" type="audio/${if(ext=="m4a")"mp4" else ext}"></audio>"""
                else -> {
                    val label = file.name
                    """<div style="padding:8px 12px;border:1px solid var(--border);border-radius:8px;margin:8px 0;"><a href="file://$filePath">📎 $label</a></div>"""
                }
            }
        } else {
            // 文件不存在，显示占位符
            """<div style="padding:8px 12px;border:1px dashed #999;border-radius:8px;margin:8px 0;color:#999;">📎 $filename (未找到)</div>"""
        }
    }
}

private fun buildTemplate(
    body: String, cs: String,
    bg: Int, text: Int, code: Int, link: Int, border: Int
): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="$cs">
<style>
:root { color-scheme: $cs; }
body {
    color: #${hex(text)}; background: #${hex(bg)};
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: 16px; line-height: 1.6; padding: 16px 20px; margin: 0;
}
h1,h2,h3,h4,h5,h6 { color: #${hex(link)}; margin: 16px 0 8px; }
h1 { font-size: 24px; border-bottom: 1px solid #${hex(border)}; padding-bottom: 8px; }
h2 { font-size: 20px; }
h3 { font-size: 18px; }
code { background: #${hex(code)}; padding: 2px 6px; border-radius: 4px; font-size: 14px; }
pre { background: #${hex(code)}; padding: 12px 16px; border-radius: 8px; overflow-x: auto; }
pre code { background: none; padding: 0; }
blockquote { border-left: 4px solid #${hex(link)}; padding-left: 16px; opacity: 0.7; margin: 12px 0; }
table { border-collapse: collapse; width: 100%; margin: 12px 0; }
th,td { border: 1px solid #${hex(border)}; padding: 8px 12px; text-align: left; }
th { background: #${hex(code)}; }
a { color: #${hex(link)}; }
img { max-width: 100%; }
ul,ol { padding-left: 24px; margin: 8px 0; }
li { margin: 4px 0; }
hr { border: none; border-top: 1px solid #${hex(border)}; margin: 16px 0; }
</style>
</head>
<body>$body</body>
</html>
""".trimIndent()

private fun hex(i: Int) = String.format("%06X", 0xFFFFFF and i)
