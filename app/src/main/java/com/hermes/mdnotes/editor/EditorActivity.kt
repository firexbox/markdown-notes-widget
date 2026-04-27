package com.hermes.mdnotes.editor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.mdnotes.ui.theme.MdNotesTheme

class EditorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val isNew = filePath == null

        setContent {
            MdNotesTheme {
                EditorScreen(
                    filePath = filePath,
                    isNew = isNew,
                    onBack = { finish() },
                    viewModel = viewModel(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    filePath: String?,
    isNew: Boolean,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val showPreview by viewModel.showPreview.collectAsState()
    val note by viewModel.note.collectAsState()

    // 初始加载
    LaunchedEffect(filePath) {
        if (isNew) viewModel.initNewNote()
        else filePath?.let { viewModel.loadNote(it) }
    }

    // 系统返回键也触发保存
    BackHandler {
        viewModel.saveNow()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showPreview) {
                        Text(title.ifBlank { "预览" })
                    } else {
                        Text(title.ifBlank { if (isNew) "新建笔记" else "编辑笔记" })
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 保存状态指示
                    if (isSaving) {
                        Text("保存中…", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                    }

                    // 切换编辑/预览
                    IconButton(onClick = { viewModel.togglePreview() }) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (showPreview) "编辑" else "预览"
                        )
                    }

                    // 删除
                    if (!isNew) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("删除笔记") },
                                text = { Text("确定要删除「${title}」吗？此操作不可撤销。") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteNote()
                                        showDeleteDialog = false
                                        onBack()
                                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                                }
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        if (showPreview) {
            // ── 预览模式 ──────────────────────────
            MarkdownPreview(
                content = content,
                modifier = Modifier.padding(padding),
            )
        } else {
            // ── 编辑模式 ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                // 标题输入
                BasicTextField(
                    value = title,
                    onValueChange = viewModel::onTitleChanged,
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    "输入标题…",
                                    style = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 内容输入
                BasicTextField(
                    value = content,
                    onValueChange = viewModel::onContentChanged,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (content.isEmpty()) {
                                Text(
                                    "开始写 Markdown…\n\n**粗体** *斜体* `代码`\n- 列表项",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}
