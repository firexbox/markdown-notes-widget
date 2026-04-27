package com.hermes.mdnotes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.hermes.mdnotes.data.PreferencesManager
import com.hermes.mdnotes.editor.EditorActivity
import com.hermes.mdnotes.ui.theme.MdNotesTheme
import com.hermes.mdnotes.widget.NotesWidgetReceiver
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: NotesRepository
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    // 编辑/新建后滚到底部
    private var pendingScrollToBottom = false

    // ── SAF 目录选择器 ──────────────────────────

    private val dirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> onDirectoryPicked(uri) }

    private fun onDirectoryPicked(uri: Uri?) {
        if (uri == null) return
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val newDir = uriToFile(uri)
            if (newDir == null) {
                Toast.makeText(this, "无法解析目录路径", Toast.LENGTH_SHORT).show()
                return
            }
            if (!newDir.exists()) newDir.mkdirs()

            // 迁移旧文件
            val oldDir = repository.getNotesDirectory()
            if (oldDir != null && oldDir != newDir && oldDir.exists()) {
                val fm = repository.getFileManager()
                fm.setNotesDirectory(newDir)
                val count = fm.migrateFilesFrom(oldDir)
                if (count > 0) {
                    Toast.makeText(this, "已迁移 $count 个文件到新目录", Toast.LENGTH_SHORT).show()
                }
            }

            // 切换到新目录
            repository.setNotesDirectory(newDir)
            PreferencesManager.saveNotesDirectory(this, newDir)
            PreferencesManager.markFirstLaunchDone(this)
            Toast.makeText(this, "✅ 笔记目录: ${newDir.name}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "切换目录失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToFile(uri: Uri): File? {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":", limit = 2)
        if (parts.size != 2) return null
        val (volume, path) = parts[0] to parts[1]
        return when {
            volume.equals("primary", true) -> File("/storage/emulated/0/$path")
            volume.matches(Regex("[A-F0-9]{4}-[A-F0-9]{4}")) -> File("/storage/$volume/$path")
            else -> File("/storage/$volume/$path")
        }
    }

    // ── Android 11+ 全部文件访问权限 ─────────────

    private fun hasFullStorageAccess(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return true // Android 10 以下用 legacy storage
    }

    private fun requestFullStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    // ── 生命周期 ────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as MdNotesApp).notesRepository
        repository.refreshNotes()

        setContent {
            MdNotesTheme {
                // 首次启动弹目录选择
                var showFirstLaunchDialog by remember {
                    mutableStateOf(PreferencesManager.isFirstLaunch(this))
                }

                if (showFirstLaunchDialog) {
                    FirstLaunchDialog(
                        onSelectDirectory = {
                            showFirstLaunchDialog = false
                            // Android 11+ 必须先开启全部文件访问权限
                            if (!hasFullStorageAccess()) {
                                requestFullStorageAccess()
                                // 标记首次启动未完成，下次 onResume 重新弹窗
                                Toast.makeText(
                                    this@MainActivity,
                                    "请开启「允许访问所有文件」权限后返回",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                dirPickerLauncher.launch(null)
                            }
                        },
                        onUseDefault = {
                            showFirstLaunchDialog = false
                            val defaultDir = File(filesDir, "notes").also { it.mkdirs() }
                            repository.setNotesDirectory(defaultDir)
                            PreferencesManager.saveNotesDirectory(this, defaultDir)
                            PreferencesManager.markFirstLaunchDone(this)
                        }
                    )
                }

                // 编辑/新建返回后滚到底部
                var scrollBottomTrigger by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) {
                    if (pendingScrollToBottom) {
                        pendingScrollToBottom = false
                        scrollBottomTrigger++
                    }
                }

                NotesListScreen(
                    repository = repository,
                    onNoteClick = { note -> openEditor(note.filePath) },
                    onNewNote = { openNewNote() },
                    onSelectDirectory = {
                        // Android 11+ 先检查全部文件访问权限
                        if (!hasFullStorageAccess()) {
                            requestFullStorageAccess()
                            Toast.makeText(
                                this,
                                "请开启「允许访问所有文件」，然后重新选择目录",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            dirPickerLauncher.launch(null)
                        }
                    },
                    dateFormat = dateFormat,
                    scrollBottomTrigger = scrollBottomTrigger,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置页返回时：权限刚开启且首次启动未完成 → 自动弹目录选择
        if (PreferencesManager.isFirstLaunch(this) && hasFullStorageAccess()) {
            dirPickerLauncher.launch(null)
            return
        }
        // 从设置页返回时更新权限状态
        if (hasFullStorageAccess()) {
            val savedDir = PreferencesManager.getNotesDirectory(this)
            if (savedDir != null) {
                repository.setNotesDirectory(savedDir)
                repository.refreshNotes()
            }
        }
        repository.refreshNotes()
        NotesWidgetReceiver.triggerUpdate(this)
    }

    private fun openEditor(filePath: String) {
        pendingScrollToBottom = true
        startActivity(Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, filePath)
        })
    }

    private fun openNewNote() {
        pendingScrollToBottom = true
        startActivity(Intent(this, EditorActivity::class.java))
    }
}

// ── 首次启动对话框 ──────────────────────────────

@Composable
fun FirstLaunchDialog(
    onSelectDirectory: () -> Unit,
    onUseDefault: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("📁 选择笔记存储目录") },
        text = {
            Column {
                Text("请选择 Markdown 笔记的存储位置。")
                Spacer(Modifier.height(8.dp))
                Text(
                    "建议选择「文档」或「下载」目录，方便文件管理。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSelectDirectory) {
                Text("选择目录")
            }
        },
        dismissButton = {
            TextButton(onClick = onUseDefault) {
                Text("使用默认")
            }
        },
    )
}

// ── 笔记列表界面 ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    repository: NotesRepository,
    onNoteClick: (Note) -> Unit,
    onNewNote: () -> Unit,
    onSelectDirectory: () -> Unit,
    dateFormat: SimpleDateFormat,
    scrollBottomTrigger: Int = 0,
) {
    val notes by repository.notes.collectAsState(initial = emptyList())
    val notesDir by repository.notesDir.collectAsState(initial = null)

    var searchQuery by remember { mutableStateOf("") }
    var sortByModified by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    val displayNotes = remember(notes, searchQuery, sortByModified) {
        repository.filteredNotes(searchQuery, sortByModified)
    }

    val listState = rememberLazyListState()

    // 编辑/新建返回后滚到底部
    LaunchedEffect(scrollBottomTrigger) {
        if (scrollBottomTrigger > 0 && displayNotes.isNotEmpty()) {
            listState.animateScrollToItem(displayNotes.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Markdown 笔记") },
                actions = {
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
                                leadingIcon = { if (sortByModified) Icon(Icons.Default.Check, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("按标题") },
                                onClick = { sortByModified = false; showSortMenu = false },
                                leadingIcon = { if (!sortByModified) Icon(Icons.Default.Check, null) },
                            )
                        }
                    }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; repository.search(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索笔记…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; repository.clearSearch() }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
            )

            notesDir?.let { dir ->
                Text(
                    "📁 ${dir.absolutePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (displayNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotEmpty()) "没有找到匹配的笔记" else "还没有笔记",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (searchQuery.isEmpty()) {
                            Text("点击右下角 + 创建第一个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(displayNotes, key = { it.filePath }) { note ->
                        NoteListItem(note, dateFormat, { onNoteClick(note) },
                            { repository.deleteNote(note.filePath) },
                            { repository.renameNote(note.filePath, it) })
                    }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Text("📄", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.preview.isNotBlank()) {
                    Text(note.preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                Text(dateFormat.format(Date(note.lastModified)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("重命名") }, onClick = { showMenu = false; showRenameDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("删除笔记") }, text = { Text("确定要删除「${note.title}」吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } })
    }
    if (showRenameDialog) {
        AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("重命名") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("新标题") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (renameText.isNotBlank()) onRename(renameText); showRenameDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("取消") } })
    }
}
