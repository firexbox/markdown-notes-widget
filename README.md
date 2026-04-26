# Markdown 笔记 — Android Widget 应用

**session_id**: `2026-04-26-markdown-notes-widget`

一个 Android Markdown 笔记应用，核心功能是**桌面 Widget**：
- Widget 直接读取本地 .md 文件，以 Todo-list 风格展示
- 点击任意条目进入 Markdown 编辑器（编辑 + 预览双模式）
- 一键新建笔记
- 支持搜索、排序、重命名、删除

## 技术栈

| 层 | 选型 |
|---|------|
| 语言 | Kotlin |
| UI | Jetpack Compose (Material3) |
| Widget | Jetpack Glance |
| Markdown | CommonMark + 自绘预览 |
| 最低 SDK | Android 8.0 (API 26) |

## 构建

```bash
# 1. 安装 Android SDK (如果还没有)
#    - 下载 Android Studio 或命令行工具
#    - 设置 ANDROID_HOME 环境变量

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 安装

```bash
# 通过 ADB 安装到手机
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 打开应用 → 选择笔记存储目录
2. 创建笔记（Markdown 格式）
3. 长按桌面 → 添加 Widget → 找到「MD 笔记」
4. Widget 显示笔记列表，点击即可编辑

## 项目结构

```
app/src/main/java/com/hermes/mdnotes/
├── MdNotesApp.kt              # Application
├── MainActivity.kt            # 主界面（文件列表 + 搜索）
├── editor/
│   ├── EditorActivity.kt      # 编辑器（编辑/预览双模式）
│   └── EditorViewModel.kt     # 编辑器状态管理（自动保存）
├── widget/
│   ├── NotesWidget.kt         # Glance Widget 定义
│   ├── NotesWidgetReceiver.kt # Widget Provider
│   └── WidgetData.kt          # Widget 数据模型
├── data/
│   ├── Note.kt                # 笔记实体
│   ├── FileStorageManager.kt  # 文件系统 CRUD
│   └── NotesRepository.kt     # 数据仓库（单例）
└── ui/theme/
    ├── Color.kt               # 绿色主题色
    ├── Type.kt                # 字体排版
    └── Theme.kt               # Material3 主题
```

## License

MIT
