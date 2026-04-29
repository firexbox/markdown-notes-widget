# Markdown 笔记 — Android Widget 应用

## 为什么需要这个应用

[Obsidian](https://obsidian.md) 是强大的 Markdown 笔记工具，但它**没有桌面 Widget**。当你把 Obsidian 仓库里的某个文件夹当作「备忘录」使用时，每次查看和新建备忘都需要打开 Obsidian App，不够快捷。

本应用解决这个问题：**将 Obsidian 仓库中的任意文件夹（如 `备忘`）设为存储目录，桌面 Widget 直接展示其中的 .md 文件**。

- Widget 显示备忘列表，一目了然
- 一键新建备忘，自动遵循 Obsidian 命名规范（`yyyyMMdd_HHmmss_标题.md`）
- 编辑内容与 Obsidian 完全互通，两边都能打开同一个文件
- 附件引用格式与 Obsidian 一致（`![[filename]]`）

## 使用场景

1. 在 Obsidian 仓库下创建 `备忘` 文件夹（或其他任意名称）
2. 打开本应用 → 首次启动选择该文件夹作为笔记目录
3. 长按桌面 → 添加 Widget → 找到「MD 笔记」
4. Widget 即显示文件夹内的备忘列表，点击编辑、一键新建

## 功能

- **桌面 Widget**：直接读取本地 .md 文件，以列表风格展示
- **快速新建**：Widget 和应用内均可一键新建备忘
- **Markdown 编辑器**：编辑 + 预览双模式，Material3 深色主题
- **附件支持**：插入图片/音频/视频，存储在所选目录下的 `附件` 子目录，Obsidian 兼容 `![[filename]]` 语法
- **自动保存**：2 秒防抖自动保存，返回时仅修改才触发保存
- **搜索**：按标题或内容搜索笔记
- **文件管理**：重命名、删除，支持 SAF 目录选择

## 技术栈

| 层 | 选型 |
|---|------|
| 语言 | Kotlin |
| UI | Jetpack Compose (Material3) |
| Widget | Jetpack Glance |
| Markdown | Markwon + CommonMark + WebView 预览 |
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

## 项目结构

```
app/src/main/java/com/hermes/mdnotes/
├── MdNotesApp.kt              # Application
├── MainActivity.kt            # 主界面（文件列表 + 搜索）
├── editor/
│   ├── EditorActivity.kt      # 编辑器（编辑/预览双模式）
│   ├── EditorViewModel.kt     # 编辑器状态管理（自动保存）
│   └── MarkdownPreview.kt     # Markdown 预览（含附件渲染）
├── widget/
│   ├── NotesWidget.kt         # Glance Widget 定义
│   └── NotesWidgetReceiver.kt # Widget Provider + 刷新触发
├── data/
│   ├── Note.kt                # 笔记实体
│   ├── FileStorageManager.kt  # 文件系统 CRUD + 附件管理
│   ├── NotesRepository.kt     # 数据仓库
│   └── PreferencesManager.kt  # SharedPreferences 配置
└── ui/theme/
    ├── Color.kt               # 绿色主题色
    ├── Type.kt                # 字体排版
    └── Theme.kt               # Material3 主题
```

## License

[GNU General Public License v3.0](LICENSE)

本项目承袭 [OpenNote-Compose](https://github.com/YangDai2003/OpenNote-Compose) 的 GPL-3.0 协议，Widget 刷新机制和 Markdown 渲染方案参考自此项目。

完整第三方依赖及许可详见 [DEPENDENCIES.md](DEPENDENCIES.md)。
