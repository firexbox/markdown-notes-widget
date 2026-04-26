package com.hermes.mdnotes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermes.mdnotes.data.Note
import com.hermes.mdnotes.data.NotesRepository
import com.hermes.mdnotes.editor.EditorActivity
import com.hermes.mdnotes.ui.theme.MdNotesTheme
import com.hermes.mdnotes.widget.NotesWidgetReceiver
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: NotesRepository
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    // SAF 目录选择器
    private val dirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 将 content:// URI 转为实际路径
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val docId = it.lastPathSegment?.substringAfter("primary:")?.let { path ->
                "/storage/emulated/0/$path"
            } ?: return@let
            val dir = File(docId)
            repository.setNotesDirectory(dir)
            Toast.makeText(this, "笔记目录: ${dir.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as MdNotesApp).notesRepository
        repository.refreshNotes()

        setContent {
            MdNotesTheme {
                NotesListScreen(
                    repository = repository,
                    onNoteClick = { note -> openEditor(note.filePath) },
                    onNewNote = { openNewNote() },
                    onSelectDirectory = { dirPickerLauncher.launch(null) },
                    dateFormat = dateFormat,
                )
            }
        }
    }

    private fun openEditor(filePath: String) {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, filePath)
        }
        startActivity(intent)
    }

    private fun openNewNote() {
        startActivity(Intent(this, EditorActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        // 从编辑器返回时刷新列表
        repository.refreshNotes()
        // 通知 Widget 刷新
        NotesWidgetReceiver.triggerUpdate(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    repository: NotesRepository,
    onNoteClick: (Note) -> Unit,
    onNewNote: () -> Unit,
    onSelectDirectory: () -> Unit,
    dateFormat: SimpleDateFormat,
) {
    val notes by repository.notes.collectAsState()
    val notesDir by repository.notesDir.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortByModified by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 过滤和排序
    val displayNotes = remember(notes, searchQuery, sortByModified) {
        repository.filteredNotes(searchQuery, sortByModified)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Markdown 笔记") },
                actions = {
                    // 排序
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "排序")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("按修改时间") },
                                onClick = { sortByModified = true; showSortMenu = false },
                                leadingIcon = {
                                    if (sortByModified) Icon(Icons.Default.Check, null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("按标题") },
                                onClick = { sortByModified = false; showSortMenu = false },
                                leadingIcon = {
                                    if (!sortByModified) Icon(Icons.Default.Check, null)
                                },
                            )
                        }
                    }

                    // 选择目录
                    IconButton(onClick = onSelectDirectory) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "选择目录")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    repository.search(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索笔记…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            repository.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
            )

            // 目录提示
            notesDir?.let { dir ->
                Text(
                    text = "📁 ${dir.absolutePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // 笔记列表
            if (displayNotes.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "📭",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "没有找到匹配的笔记" else "还没有笔记",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                "点击右下角 + 创建第一个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(displayNotes, key = { it.filePath }) { note ->
                        NoteListItem(
                            note = note,
                            dateFormat = dateFormat,
                            onClick = { onNoteClick(note) },
                            onDelete = {
                                repository.deleteNote(note.filePath)
                            },
                            onRename = { newTitle ->
                                repository.renameNote(note.filePath, newTitle)
                            },
                        )
                    }

                    // 底部间距
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun NoteListItem(
    note: Note,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(note.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 图标
            Text("📄", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.width(12.dp))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.preview.isNotBlank()) {
                    Text(
                        text = note.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = dateFormat.format(Date(note.lastModified)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // 菜单按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = { showMenu = false; showRenameDialog = true },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showDeleteDialog = true },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }
    }

    // 删除对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确定要删除「${note.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    // 重命名对话框
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("新标题") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        onRename(renameText)
                    }
                    showRenameDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            },
        )
    }
}
