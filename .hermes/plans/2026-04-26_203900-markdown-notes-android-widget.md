# Markdown Notes Android Widget App — 开发计划

## 目标

开发一个 Android Markdown 笔记应用，核心功能是桌面 Widget：
- Widget 读取本地目录中的 `.md` 文件，以 todo-list 风格展示（标题 + 内容预览）
- 点击条目进入编辑页面
- "新建"按钮快速创建笔记
- 完整 Markdown 编辑功能（编辑 + 预览）

## 技术选型

| 层级 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android 现代化开发首选 |
| 最低 SDK | API 26 (Android 8.0) | 覆盖 95%+ 设备 |
| UI 框架 | Jetpack Compose | 编辑页面现代化 UI |
| Widget | Jetpack Glance | Compose 风格 Widget API |
| 架构 | MVVM + Repository | 清晰分层，Livedata/StateFlow 驱动 |
| 依赖注入 | Hilt (可选，初期手动注入) | 简化复杂度 |
| Markdown | `io.noties.markwon` | 成熟稳定的渲染库 |
| 文件存储 | SAF / app 私有目录 | 用户可选目录 |

## 项目结构

```
markdown-notes-widget/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hermes/mdnotes/
│       │   ├── MdNotesApp.kt              # Application 类
│       │   ├── MainActivity.kt            # 主界面（文件列表）
│       │   ├── editor/
│       │   │   ├── EditorActivity.kt      # 编辑器页面
│       │   │   └── EditorViewModel.kt     # 编辑器逻辑
│       │   ├── widget/
│       │   │   ├── NotesWidget.kt         # Glance Widget 定义
│       │   │   ├── NotesWidgetReceiver.kt # Widget Provider
│       │   │   └── WidgetData.kt          # Widget 数据模型
│       │   ├── data/
│       │   │   ├── Note.kt                # 笔记实体
│       │   │   ├── NotesRepository.kt     # 数据仓库
│       │   │   └── FileStorageManager.kt  # 文件系统操作
│       │   └── ui/
│       │       ├── theme/                 # Material3 主题
│       │       └── components/            # 可复用组件
│       └── res/
│           ├── xml/
│           │   └── notes_widget_info.xml  # Widget 配置
│           └── values/
│               └── strings.xml
├── build.gradle.kts                       # 根构建文件
├── settings.gradle.kts
└── gradle.properties
```

## 分步实施计划

### Phase 1: 项目骨架搭建 (Step 1-3)

**Step 1 — 创建 Android 项目结构**
- 手动创建 Gradle 项目（无 Android Studio，纯命令行）
- 配置 `build.gradle.kts`：Kotlin, Compose, Glance, Markwon 依赖
- 设置 minSdk=26, targetSdk=34, compileSdk=34
- 创建 `AndroidManifest.xml`

**Step 2 — 数据层**
- `Note.kt`：数据模型（id, title, content, filePath, lastModified）
- `FileStorageManager.kt`：文件的 CRUD 操作
  - `listNotes(directory: File): List<Note>` — 扫描 `.md` 文件
  - `readNote(path: String): Note` — 读取完整内容
  - `saveNote(note: Note)` — 保存/更新
  - `createNote(directory: File, title: String): Note` — 新建
  - `deleteNote(path: String)` — 删除
- `NotesRepository.kt`：封装 FileStorageManager，提供 Flow

**Step 3 — 主题和基础组件**
- Material3 主题（支持深色模式）
- 基础颜色、字体设置

### Phase 2: 主界面和编辑器 (Step 4-6)

**Step 4 — MainActivity 文件列表**
- Compose 实现文件列表界面
- 显示每个笔记的标题（从第一行 `#` 提取）和内容预览（前 100 字符）
- 浮动按钮 "新建笔记"
- 点击条目 → 启动 EditorActivity

**Step 5 — EditorActivity 编辑器**
- 分两个标签：编辑 / 预览
- 编辑模式：多行文本输入框，自动保存
- 预览模式：Markwon 渲染 Markdown
- 返回时自动保存

**Step 6 — EditorViewModel**
- 管理编辑状态（title, content, isModified）
- 自动保存逻辑（防抖 2 秒）
- 加载/保存操作

### Phase 3: Widget 实现 (Step 7-9)

**Step 7 — Widget Provider & Config**
- `notes_widget_info.xml`：定义 Widget 尺寸、更新频率
- `NotesWidgetReceiver.kt`：GlanceAppWidgetReceiver

**Step 8 — Widget UI（Glance）**
- `NotesWidget.kt`：使用 Glance 定义 Widget 布局
- 显示笔记列表（标题 + 预览），支持滚动
- 每条目可点击（PendingIntent 打开 EditorActivity）
- 顶部 "新建" 按钮
- 底部显示笔记数量统计

**Step 9 — Widget 数据刷新**
- Widget 点击时自动刷新
- 定时刷新（AlarmManager + 30 分钟间隔）
- 从主 App 编辑保存后触发 Widget 更新

### Phase 4: 完善和优化 (Step 10-12)

**Step 10 — 目录选择**
- 首次启动引导用户选择笔记存储目录
- 使用 SAF (Storage Access Framework) 或内置预设路径
- 支持外部存储（需要权限处理）

**Step 11 — 搜索和排序**
- 搜索栏过滤笔记
- 按修改时间 / 标题排序

**Step 12 — 右键/长按菜单**
- 长按笔记：重命名、删除、分享
- 删除确认对话框

## 关键依赖

```kotlin
// build.gradle.kts (app)
dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Glance Widget
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")
    
    // Markdown
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:editor:4.6.2")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## 风险与注意事项

1. **Widget 限制**：Glance 不支持所有 Compose 组件，需确认列表滚动在 Glance 中的支持情况
2. **Android 存储权限**：Android 10+ 的分区存储限制，SAF 是推荐方案
3. **Widget 实时更新**：Widget 更新有频率限制，不能太频繁
4. **Markdown 渲染性能**：大文件在 Widget 中只显示摘要，完整渲染在 Editor 中
5. **Glance 状态管理**：Glance 使用 `GlanceStateDefinition`，与普通 Compose 不同

## 验证方式

1. 在 Android 设备/模拟器上安装 APK
2. 长按桌面 → 添加 Widget → 找到 "Markdown Notes"
3. 验证 Widget 显示笔记列表
4. 点击笔记 → 打开编辑器 → 编辑 → 保存 → Widget 更新
5. 点击 "新建" → 输入标题 → 保存 → Widget 显示新笔记

## 项目路径

`/home/hermes/projects/markdown-notes-widget/`
