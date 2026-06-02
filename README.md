# DawnGuiReader — 曙光无障碍阅读模组

![Minecraft](https://img.shields.io/badge/Minecraft-26.1-green)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

> **DawnGuiReader**（曙光 GUI 阅读器）是一个为 Minecraft Fabric 客户端设计的无障碍模组，专为盲人及低视力玩家打造。当玩家在容器 GUI 中悬停物品、切换物品栏、或准星对准方块时，模组会自动朗读名称，并在界面上显示大字号高对比文字。支持系统 TTS 及 Minecraft 内置语音双层回退，适配 Windows / macOS / Linux 全平台。

---

## ✨ 功能亮点

- **容器物品朗读**
  鼠标悬停在背包、箱子、工作台等 GUI 中的物品上，达到配置延迟后自动朗读物品名称

- **创造模式页签朗读**
  悬停在创造模式分类标签上时同样触发朗读

- **大字号物品名显示**
  在容器 GUI 右侧空白区域显示高对比大字号物品名，支持自动换行和缩放

- **物品栏切换朗读**
  切换快捷栏时自动朗读手持物品名称，独立延迟设置

- **准星方块朗读**
  三种模式：关闭 / 自动（延迟朗读）/ 手动（快捷键即时朗读）

- **双层 TTS 引擎**
  优先使用系统语音（Windows PowerShell / macOS say / Linux spd-say），不可用时自动回退到 Minecraft 内置 Narrator

- **持久化进程**
  Windows 下采用持久化 PowerShell 进程 + stdin 管道通信，朗读延迟从数秒降至毫秒级

- **GUI 内快捷键**
  所有快捷键在打开 GUI 时仍然可用，使用 GLFW 原生键状态检测

- **完整设置界面**
  ESC 暂停菜单中可调整语速、音色、各项延迟、字号、功能开关

---

## 📂 目录结构

```
DawnGuiReader/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── build-d-drive.ps1
├── README.md
├── LICENSE
└── src/
    ├── client/java/org/dawnteam/accessibility/
    │   ├── DawnAccessibilityClient.java          # 主入口 + 快捷键
    │   ├── config/DawnAccessibilityConfig.java    # JSON 配置
    │   ├── gui/
    │   │   ├── DawnAccessibilityOptionsScreen.java # 设置界面
    │   │   ├── HoveredItemReader.java              # 容器物品悬停
    │   │   ├── HoveredTextReader.java              # 创造页签悬停
    │   │   ├── HotbarItemReader.java               # 物品栏切换
    │   │   ├── BlockTargetReader.java              # 准星方块
    │   │   └── LargeItemNameOverlay.java           # 大字号覆盖层
    │   ├── mixin/
    │   │   ├── AbstractContainerScreenMixin.java
    │   │   ├── CreativeModeInventoryScreenMixin.java
    │   │   ├── GameNarratorAccessor.java
    │   │   ├── KeyMappingAccessor.java
    │   │   └── PauseScreenMixin.java
    │   └── tts/
    │       ├── SystemTtsEngine.java
    │       ├── TtsEngine.java
    │       ├── TtsOptions.java
    │       └── Voice.java
    └── main/resources/
        ├── fabric.mod.json
        ├── dawn_accessibility.client.mixins.json
        └── assets/dawn_accessibility/lang/
            ├── en_us.json
            └── zh_cn.json
```

---

## 🛠 本地构建

确保使用 **JDK 25** 和 **Gradle 9.4+**：

```bash
# 克隆仓库
git clone https://github.com/aojiangQAQ/DawnGuiReader.git
cd DawnGuiReader

# 标准构建
.\gradlew.bat build

# 或使用 D 盘缓存构建（离线，更快）
.\build-d-drive.ps1

# 生成 build/libs/dawn-gui-reader-1.0.0.jar
```

---

## 🚀 安装

### 前置依赖

| 前置 | 版本要求 |
|------|---------|
| **Minecraft** | 26.1.x |
| **Fabric Loader** | 0.19.2+ |
| **Fabric API** | 完整版或包含 api-base / lifecycle-events / key-mapping-api |

### 安装步骤

1. 安装 [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.2+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 到 `mods/` 目录
3. 将 `dawn-gui-reader-1.0.0.jar` 复制到 `mods/` 目录
4. 启动游戏，模组自动生效

---

## ⌨️ 快捷键

所有快捷键默认**未绑定**，需在 游戏设置 → 按键绑定 → Dawn 无障碍 中手动设置：

| 按键 | 功能 |
|------|------|
| 开关 GUI 朗读 | 切换朗读总开关 |
| 重复朗读当前物品 | 有 GUI 时重复鼠标悬停物，无 GUI 时重复手持物 |
| 开关大字号物品名 | 切换大字显示 |
| 朗读准星方块 | 即时朗读准星所指方块（需准星模式非关闭） |

---

## 🔧 设置界面

在 ESC 暂停菜单中点击 **"Dawn 无障碍"** 进入设置：

| 设置项 | 范围 | 默认值 | 说明 |
|--------|------|--------|------|
| 朗读 | ON/OFF | ON | 总开关 |
| 大字号 | ON/OFF | ON | 大字覆盖层开关 |
| 朗读语速 | -10 ~ 10 | 0 | 系统 TTS 语速 |
| 容器悬停延迟 | 100 ~ 3000ms | 500ms | 鼠标悬停后多久朗读 |
| 大字号大小 | 24 ~ 96 | 48 | 大字字号 |
| 音色 | 系统语音列表 | Default | TTS 音色选择 |
| 物品栏朗读 | ON/OFF | ON | 切换快捷栏时朗读 |
| 准星朗读 | OFF/AUTO/MANUAL | MANUAL | AUTO=自动延迟 / MANUAL=按键即时 |
| 物品栏延迟 | 100 ~ 3000ms | 500ms | 物品栏切换后延迟 |
| 准星延迟 | 100 ~ 3000ms | 500ms | 准星 AUTO 模式延迟 |

设置支持鼠标滚轮滚动，适配高 UI 缩放比。

---

## 🌐 TTS 引擎

| 平台 | 主引擎 | 回退方案 |
|------|--------|---------|
| Windows | PowerShell + System.Speech（持久化进程） | Minecraft Narrator |
| macOS | `say` 命令 | Minecraft Narrator |
| Linux | `spd-say` 命令 | Minecraft Narrator |

Windows 下首次运行会在 `.minecraft/config/` 目录生成 `dawn-tts-speak.ps1` 脚本，用于持久化语音合成。

---

## 🔧 开发环境

- **Java:** 25
- **Build:** Gradle 9.4+ (Fabric Loom 1.16.3)
- **Minecraft:** 26.1.2
- **Fabric Loader:** 0.19.2
- **Fabric API:** 0.150.0+26.1.2

---

## 🤝 贡献

欢迎 Issue / PR！

1. Fork 本仓库
2. 创建新分支: `git checkout -b feature/awesome`
3. 提交更改: `git commit -m "Add awesome feature"`
4. 推送分支: `git push origin feature/awesome`
5. 发起 Pull Request

---

## ⚖️ License

DawnGuiReader 使用 **MIT License**，详见 [LICENSE](LICENSE)。

---

> **制作团队**：曙光（Dawn）团队
> **制作人**：鳌江
> **适用客户端**：Minecraft 26.1.x Fabric 客户端
