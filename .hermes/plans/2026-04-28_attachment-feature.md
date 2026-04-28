# 附件功能实现方案

**时间**: 2026-04-28  
**项目**: markdown-notes-widget

## 目标

在编辑器中增加插入附件功能，附件存储在笔记目录下的 `附件` 子目录，引用格式与 Obsidian 一致（`![[filename]]`），预览时正确渲染图片、音频、视频。

## 现有架构分析

| 文件 | 职责 |
|------|------|
| `FileStorageManager.kt` | 笔记文件 CRUD，目录管理 |
| `EditorViewModel.kt` | 编辑器状态，保存逻辑 |
| `EditorActivity.kt` | 编辑器 UI，TopAppBar + 输入区 |
| `MarkdownPreview.kt` | CommonMark → HTML，WebView 渲染 |

## 实现步骤

### 1. FileStorageManager — 附件管理

新增方法：
- `getAttachmentsDir()` → 返回 `<notesDir>/附件/`，自动创建
- `copyAttachment(sourceUri: Uri, context: Context): String?` — 从 content URI 复制文件到附件目录，返回文件名
- `resolveAttachment(filename: String): File?` — 解析附件文件路径

```
FileStorageManager
+ fun getAttachmentsDir(): File
+ fun copyAttachment(uri: Uri, context: Context): String?
+ fun resolveAttachment(filename: String): File?
```

### 2. EditorViewModel — 插入附件

新增方法：
- `insertAttachment(uri: Uri, context: Context)` — 调用 FileStorageManager 复制文件，在内容中插入 `![[filename]]`

```
EditorViewModel
+ fun insertAttachment(uri: Uri, context: Context)
```

### 3. EditorActivity — UI 变更

在编辑模式 Toolbar actions 中新增附件按钮（📎），调用系统文件选择器：

- 使用 `ActivityResultContracts.GetContent()` 或 `OpenMultipleDocuments` 
- 支持 image/*, audio/*, video/* MIME 类型
- 选中后调用 `viewModel.insertAttachment(uri)`
- 按钮位置：预览切换按钮左侧

```
EditorActivity
+ val attachmentPicker = rememberLauncherForActivityResult(GetContent())
+ action: IconButton(Attachment) → picker.launch("image/*;audio/*;video/*")
```

### 4. MarkdownPreview — 渲染 ![[...]]

在 `MarkdownPreview` 中增加预处理步骤：
- 解析内容中的 `![[filename]]` 语法
- 根据文件扩展名生成对应 HTML 标签
- 图片：`<img src="file://...">`
- 音频：`<audio controls src="file://...">`
- 视频：`<video controls width="100%" src="file://...">`
- 其他文件：文件链接
- WebView 的 `baseUrl` 设为附件目录路径，使相对路径可用

需要传入 `notesDir` 参数来解析附件路径。

```
MarkdownPreview
  signature: (content: String, notesDir: String?, modifier: Modifier)
  + 预处理 ![[...]] → HTML 标签
  + baseURL 指向附件目录
```

### 5. 传递 notesDir

- `EditorViewModel` 需要 `notesDir` 信息（从 repository 获取）
- `EditorScreen` 传递 `notesDir` 给 `MarkdownPreview`
- `EditorActivity` 从 intent/repository 获取 `notesDir`

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/FileStorageManager.kt` | 修改 | 新增 getAttachmentsDir, copyAttachment |
| `editor/EditorViewModel.kt` | 修改 | 新增 insertAttachment, 接收 notesDir |
| `editor/EditorActivity.kt` | 修改 | 新增附件按钮 + 文件选择器 |
| `editor/MarkdownPreview.kt` | 修改 | 新增 ![[...]] 解析, baseURL |
| `AndroidManifest.xml` | 检查 | 确认 READ_EXTERNAL_STORAGE 等权限 |

## 验证

1. 在编辑器中点击附件按钮 → 选择图片 → 内容中出现 `![[photo.jpg]]`
2. 切换到预览模式 → 图片正确显示
3. 检查 `<notesDir>/附件/` 目录下文件存在
4. 音频/视频文件同样可插入和预览
