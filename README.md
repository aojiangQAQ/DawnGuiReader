# DawnGuiReader — 曙光无障碍阅读模组

<p align="center">
  <strong>简体中文</strong> ｜ <a href="README.en.md">English</a>
</p>

<p align="center">
  <img src="src/main/resources/assets/dawn_accessibility/icon.png" alt="DawnGuiReader 图标" width="128">
</p>

![Minecraft](https://img.shields.io/badge/Minecraft-26.1--26.2-green)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2-blue)
![Cloth Config](https://img.shields.io/badge/Cloth_Config-required-orange)
![Version](https://img.shields.io/badge/version-1.3.0-6f42c1)
![License](https://img.shields.io/badge/license-MIT-blue)

> DawnGuiReader 是一个面向盲人及低视力玩家的 Minecraft Fabric 客户端无障碍模组。它可以朗读容器物品、快捷栏物品、准星所指方块和 GUI 文本，并通过系统 TTS 在 Windows、macOS 和 Linux 上工作。

## v1.3.0 更新

- 新增 Minecraft 26.2 支持，同时保留 Minecraft 26.1.x 兼容性。
- 增加跨 26.1/26.2 的当前界面访问兼容层。
- 更新 Cloth Config、构建配置和 Modrinth 发布信息。
- 感谢 [@S-H-Go](https://github.com/S-H-Go) 提交 26.2 兼容支持。

## 功能

- **容器物品朗读**：支持背包、箱子、工作台、熔炉、铁砧、附魔台、村民交易等容器界面。
- **详细信息朗读**：可朗读物品来源模组名，支持独立和顺序两种模式。
- **附魔台朗读**：朗读鼠标所指附魔选项的等级和描述。
- **创造模式标签朗读**：朗读创造模式分类标签。
- **快捷栏朗读**：切换栏位时朗读手持物品；空手时朗读“手”。
- **准星方块朗读**：支持关闭、自动和快捷键手动朗读三种模式。
- **GUI 文本朗读**：朗读鼠标所指按钮、设置项、世界名称和服务器名称。
- **可调语速、音量和延迟**：各类朗读拥有独立开关与延迟设置。
- **跨平台 TTS**：Windows 使用 System.Speech，macOS 使用 `say`，Linux 使用 `spd-say`；不可用时回退到 Minecraft Narrator。
- **现代设置界面**：通过 Cloth Config 提供分页设置和快捷键绑定。
- **非阻塞语音处理**：TTS 工作在后台守护线程；Windows 使用持久 PowerShell 进程降低延迟。

## 前置依赖

| 前置 | 要求 |
|---|---|
| Minecraft | 26.1.x 或 26.2.x |
| Fabric Loader | 0.19.2+ |
| Fabric API | 对应 Minecraft 版本 |
| Cloth Config | 对应 Minecraft 版本，必选 |
| Java | 25+ |

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/installer/)。
2. 将 [Fabric API](https://modrinth.com/mod/fabric-api) 和 [Cloth Config](https://modrinth.com/mod/cloth-config) 放入 `mods/`。
3. 下载并将 `DawnGuiReader-1.3.0.jar` 放入 `mods/`。
4. 启动游戏，在主界面左下角或暂停菜单中打开 **Dawn 无障碍**。
5. 总开关默认关闭；请在设置中手动启用需要的朗读功能。

## 快捷键

所有快捷键默认未绑定，可在 Dawn 设置页或游戏按键绑定页面设置。

| 快捷键 | 功能 |
|---|---|
| 开关 GUI 朗读 | 切换朗读总开关 |
| 重复朗读当前物品 | GUI 中重复鼠标所指物品；游戏中重复手持物品 |
| 朗读准星方块 | 立即朗读准星所指方块 |

## 设置分类

| 分类 | 主要选项 |
|---|---|
| 通用 | 总开关、语速、音量、音色 |
| 容器 | 容器朗读、悬浮延迟、详细信息、独立/顺序模式 |
| 物品栏 | 物品栏朗读、切换延迟 |
| 准星 | 关闭/自动/手动模式、朗读延迟 |
| 界面朗读 | GUI 文本朗读、悬浮延迟 |
| 快捷键 | 在模组设置内直接绑定按键 |

## TTS 引擎

| 平台 | 主引擎 | 回退方案 |
|---|---|---|
| Windows | PowerShell + System.Speech（持久进程） | Minecraft Narrator |
| macOS | `say` | Minecraft Narrator |
| Linux | `spd-say` | Minecraft Narrator |

Windows 首次运行时会在 `.minecraft/config/` 中生成 `dawn-tts-speak.ps1`。

## 本地构建

需要 JDK 25。Gradle Wrapper 会负责使用项目指定的 Gradle 版本。

```powershell
git clone https://github.com/aojiangQAQ/DawnGuiReader.git
cd DawnGuiReader
./gradlew.bat build
```

产物位于 `build/libs/DawnGuiReader-1.3.0.jar`。

## 贡献

欢迎提交 Issue 和 Pull Request。兼容新 Minecraft 版本时，请附上启动、标题页、暂停页、容器朗读、GUI 朗读和附魔台的实际测试结果。

## 许可与作者

DawnGuiReader 使用 [MIT License](LICENSE)。

- 制作团队：曙光（Dawn）团队
- 制作人：鳌江
- 主页：[aojiang.space](https://aojiang.space)
- 源码：[GitHub](https://github.com/aojiangQAQ/DawnGuiReader)
