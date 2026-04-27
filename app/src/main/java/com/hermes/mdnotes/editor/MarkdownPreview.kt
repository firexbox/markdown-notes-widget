package com.hermes.mdnotes.editor

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()

    val bg = remember(colorScheme) { colorScheme.surface.toArgb() }
    val text = remember(colorScheme) { colorScheme.onSurface.toArgb() }
    val code = remember(colorScheme) { colorScheme.surfaceVariant.toArgb() }
    val link = remember(colorScheme) { colorScheme.primary.toArgb() }
    val border = remember(colorScheme) { colorScheme.outline.toArgb() }
    val heading = remember(colorScheme) { colorScheme.primary.toArgb() }

    val html = remember(content, dark, bg, text, code, link, border) {
        buildHtml(content, dark, bg, text, code, link, border, heading)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(if (dark) AndroidColor.TRANSPARENT else AndroidColor.WHITE)
                settings.javaScriptEnabled = true
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

private fun buildHtml(
    md: String, dark: Boolean,
    bg: Int, text: Int, code: Int, link: Int, border: Int, heading: Int
): String {
    val cs = if (dark) "dark" else "light"
    val b64 = android.util.Base64.encodeToString(md.toByteArray(), android.util.Base64.NO_WRAP)

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="$cs">
<meta name="theme-color" content="#${bg.toHex()}">
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<style>
:root { color-scheme: $cs; }
body {
    color: #${text.toHex()};
    background: #${bg.toHex()};
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: 16px; line-height: 1.6; padding: 16px 20px; margin:0;
}
h1,h2,h3,h4,h5,h6 { color: #${heading.toHex()}; margin: 16px 0 8px 0; }
h1 { font-size: 24px; border-bottom: 1px solid #${border.toHex()}; padding-bottom: 8px; }
h2 { font-size: 20px; }
h3 { font-size: 18px; }
code { background: #${code.toHex()}; padding: 2px 6px; border-radius: 4px; font-size: 14px; }
pre { background: #${code.toHex()}; padding: 12px 16px; border-radius: 8px; overflow-x:auto; }
pre code { background:none; padding:0; }
blockquote { border-left: 4px solid #${link.toHex()}; padding-left: 16px; opacity:0.7; margin:12px 0; }
table { border-collapse: collapse; width:100%; margin:12px 0; }
th,td { border:1px solid #${border.toHex()}; padding:8px 12px; text-align:left; }
th { background: #${code.toHex()}; }
a { color: #${link.toHex()}; }
img { max-width:100%; }
ul,ol { padding-left:24px; margin:8px 0; }
li { margin:4px 0; }
hr { border:none; border-top:1px solid #${border.toHex()}; margin:16px 0; }
</style>
</head>
<body>
<div id="content"></div>
<script>
var raw = atob("$b64");
var bytes = new Uint8Array(raw.length);
for (var i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
document.getElementById('content').innerHTML = marked.parse(new TextDecoder().decode(bytes));
</script>
</body>
</html>
""".trimIndent()
}

private fun Int.toHex() = String.format("%06X", 0xFFFFFF and this)
