# 第三方依赖与许可

本项目参考了以下开源项目，特此声明。

## 参考项目

### OpenNote-Compose

- **项目**: [YangDai2003/OpenNote-Compose](https://github.com/YangDai2003/OpenNote-Compose)
- **许可**: GNU General Public License v3.0
- **用途**: 
  - Glance AppWidget 刷新机制（ActionCallback + triggerUpdate 模式）
  - Markdown 预览渲染方式（CommonMark 解析 + WebView 注入 Material3 颜色）
  - 文件存储架构设计参考

## 直接依赖

| 库 | 版本 | 许可 | 用途 |
|-----|------|------|------|
| **AndroidX Core** | 1.12.0 | Apache 2.0 | Android 核心扩展 |
| **Kotlin Stdlib** | — | Apache 2.0 | Kotlin 标准库 |
| **Kotlin Coroutines** | 1.7.3 | Apache 2.0 | 异步编程 |
| **Jetpack Compose BOM** | 2024.04.00 | Apache 2.0 | 声明式 UI 框架 |
| **Compose Material3** | — | Apache 2.0 | Material Design 3 组件 |
| **Compose Material Icons Extended** | 1.6.5 | Apache 2.0 | 扩展图标库 |
| **Jetpack Glance** | 1.1.1 | Apache 2.0 | 桌面 AppWidget 框架 |
| **Lifecycle ViewModel** | 2.7.0 | Apache 2.0 | MVVM 架构组件 |
| **Lifecycle Runtime Compose** | 2.7.0 | Apache 2.0 | Compose 生命周期集成 |
| **Activity Compose** | 1.8.2 | Apache 2.0 | Activity + Compose 桥接 |
| **Markwon** | 4.6.2 | Apache 2.0 | Markdown 解析与渲染 |
| **Markwon Editor** | 4.6.2 | Apache 2.0 | Markdown 编辑器组件 |
| **Markwon Ext: Strikethrough** | 4.6.2 | Apache 2.0 | 删除线扩展 |
| **Markwon Ext: Tables** | 4.6.2 | Apache 2.0 | 表格扩展 |
| **Markwon Ext: Tasklist** | 4.6.2 | Apache 2.0 | 任务列表扩展 |

## 传递依赖

| 库 | 许可 | 用途 |
|-----|------|------|
| CommonMark (com.atlassian.commonmark) | BSD 2-Clause | Markdown → HTML AST 解析 |
| CommonMark Ext: GFM Strikethrough | BSD 2-Clause | GFM 删除线语法 |
| CommonMark Ext: GFM Tables | BSD 2-Clause | GFM 表格语法 |
| AndroidX DataStore | Apache 2.0 | 键值存储（Glance 依赖） |
| AndroidX Room | Apache 2.0 | 本地数据库（传递依赖） |
| AndroidX WorkManager | Apache 2.0 | 后台任务调度（Glance 依赖） |
| Google Guava | Apache 2.0 | 工具集（传递依赖） |
| JetBrains Annotations | Apache 2.0 | 静态分析注解 |
| Protobuf | BSD 3-Clause | 序列化（Glance 传递依赖） |

## 构建工具

| 工具 | 许可 | 用途 |
|-----|------|------|
| Android Gradle Plugin | Apache 2.0 | 项目构建 |
| Kotlin Gradle Plugin | Apache 2.0 | Kotlin 编译 |
| R8 / ProGuard | Apache 2.0 / GPL | 代码压缩混淆（发布版） |

## 协议兼容

本项目承袭 OpenNote-Compose 的 GPL-3.0 许可，源代码在相同协议下发布。

Apache 2.0 组件（AndroidX、Glance、Compose、Markwon 等）属系统运行库，GPL-3.0 的"系统库例外"条款适用。BSD 许可组件（CommonMark）与 GPL-3.0 兼容。
