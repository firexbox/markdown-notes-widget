package com.hermes.mdnotes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mukesh.MarkDown

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    MarkDown(
        text = content,
        modifier = modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF1C1B1F) else Color.White)
    )
}
