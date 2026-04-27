package com.hermes.mdnotes.editor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val htmlContent = remember(content, darkTheme) {
        buildDarkHtml(content, darkTheme)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(if (darkTheme) 0xFF1C1B1F.toInt() else 0xFFFFFFFF.toInt())
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun buildDarkHtml(markdown: String, dark: Boolean): String {
    val bg = if (dark) "#1C1B1F" else "#FFFFFF"
    val text = if (dark) "#E6E1E5" else "#1C1B1F"
    val secondary = if (dark) "#999999" else "#666666"
    val code = if (dark) "#2D2D30" else "#F5F5F5"
    val border = if (dark) "#3D3D40" else "#E0E0E0"
    val link = if (dark) "#A5D6A7" else "#1B6D27"
    val heading = if (dark) "#A5D6A7" else "#1B5E20"

    val b64 = android.util.Base64.encodeToString(
        markdown.toByteArray(Charsets.UTF_8),
        android.util.Base64.NO_WRAP
    )

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { 
    background:$bg; color:$text; 
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: 16px; line-height: 1.6; padding: 16px 20px;
}
h1,h2,h3,h4,h5,h6 { color:$heading; margin: 16px 0 8px 0; }
h1 { font-size: 24px; border-bottom: 1px solid $border; padding-bottom: 8px; }
h2 { font-size: 20px; }
h3 { font-size: 18px; }
code { background:$code; padding: 2px 6px; border-radius: 4px; font-size: 14px; }
pre { background:$code; padding: 12px 16px; border-radius: 8px; overflow-x:auto; }
pre code { background:none; padding:0; }
blockquote { border-left: 4px solid $link; padding-left: 16px; color:$secondary; margin: 12px 0; }
table { border-collapse: collapse; width: 100%; margin: 12px 0; }
th, td { border: 1px solid $border; padding: 8px 12px; text-align: left; }
th { background:$code; }
a { color:$link; }
img { max-width: 100%; }
ul, ol { padding-left: 24px; margin: 8px 0; }
li { margin: 4px 0; }
hr { border: none; border-top: 1px solid $border; margin: 16px 0; }
</style>
</head>
<body>
<div id="content"></div>
<script>
var md = atob("$b64");
var decoded = new TextDecoder().decode(new Uint8Array([...md].map(c=>c.charCodeAt(0))));
document.getElementById('content').innerHTML = marked.parse(decoded);
</script>
</body>
</html>
""".trimIndent()
}
