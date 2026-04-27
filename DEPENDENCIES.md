# 第三方依赖与许可

本项目参考了以下开源项目，特此声明。

## OpenNote-Compose

- **项目**: [YangDai2003/OpenNote-Compose](https://github.com/YangDai2003/OpenNote-Compose)
- **许可**: GNU General Public License v3.0
- **用途**: 
  - Glance AppWidget 刷新机制（ActionCallback + triggerUpdate 模式）
  - Markdown 预览渲染方式（CommonMark 解析 + WebView 注入 Material3 颜色）
  - 文件存储架构设计参考

## 使用的开源库

| 库 | 许可 | 用途 |
|-----|------|------|
| Jetpack Compose BOM | Apache 2.0 | UI 框架 |
| Jetpack Glance | Apache 2.0 | 桌面 AppWidget |
| Material3 | Apache 2.0 | 主题 |
| Lifecycle ViewModel | Apache 2.0 | MVVM 架构 |
| CommonMark (org.commonmark) | BSD 2-Clause | Markdown → HTML 解析 |

## 协议兼容

本项目承袭 OpenNote-Compose 的 GPL-3.0 许可，源代码在相同协议下发布。

Apache 2.0 组件（AndroidX、Glance 等）属系统运行库，GPL-3.0 的"系统库例外"条款适用。
