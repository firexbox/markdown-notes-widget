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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
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

    val body = remember(content, parser, renderer) {
        try {
            val doc = parser.parse(content)
            renderer.render(doc)
        } catch (e: Exception) { content }
    }

    val isDark = colorScheme.background.toArgb().let { c ->
        android.graphics.Color.luminance(c) < 0.5
    }
    val cs = if (isDark) "dark" else "light"

    val html = remember(body, bg, text, code, link, border, cs) {
        buildTemplate(body, cs, bg, text, code, link, border)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(bg)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
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
