# DawnGuiReader — 曙光无障碍阅读模组

![Minecraft](https://img.shields.io/badge/Minecraft-26.1--26.2-green)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2-blue)
![Cloth Config](https://img.shields.io/badge/Cloth_Config-required-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

> **DawnGuiReader**（曙光 GUI 阅读器）是一个为 Minecraft Fabric 客户端设计的无障碍模组，专为盲人及低视力玩家打造。模组会在容器 GUI 悬浮物品、切换物品栏、准星对准方块、或鼠标悬浮界面按钮时自动朗读相关内容。支持系统 TTS 双层回退，适配 Windows / macOS / Linux 全平台。

---

## ✨ 功能亮点

- **容器物品朗读** — 鼠标悬浮在背包、箱子、工作台、熔炉、铁砧、附魔台、村民交易等 GUI 中的物品上，达到配置延迟后自动朗读物品名称
- **详细信息朗读** — 可选开启，朗读物品来源模组名（如 "Minecraft"、"Create" 等蓝色文字），支持独立/顺序两种模式
- **附魔台朗读** — 朗读附魔台中各附魔选项的等级与描述
- **创造模式标签朗读** — 悬浮在创造模式分类标签上时触发朗读
- **物品栏切换朗读** — 切换快捷栏时自动朗读手持物品名称，独立延迟设置，空手时朗读"手"
- **准星方块朗读** — 三种模式：关闭 / 自动（延迟朗读）/ 手动（快捷键即时朗读）
- **界面文字朗读** — 朗读鼠标悬浮的 GUI 按钮文本，支持递归搜索嵌套控件（如设置界面内的选项）
- **音量控制** — 独立音量滑块（0–100），不干扰游戏音量
- **音色选择** — 预留音色切换接口，当前仅提供默认音色
- **双层 TTS 引擎** — 优先使用系统语音（Windows PowerShell / macOS say / Linux spd-say），不可用时自动回退到 Minecraft 内置 Narrator
- **持久化进程** — Windows 下采用持久化 PowerShell 进程 + stdin 管道通信，朗读延迟从数秒降至毫秒级
- **Cloth Config 设置界面** — 分标签页的现代设置 UI，支持输入框、开关、枚举选择、快捷键绑定
- **默认关闭** — 模组默认不启用朗读，插入其他整合包不影响正常玩家

---

## 📦 前置依赖

| 前置 | 版本要求 | 说明 |
|------|---------|------|
| **Minecraft** | 26.1.x / 26.2.x | |
| **Fabric Loader** | 0.19.2+ | |
| **Fabric API** | 包含 api-base / lifecycle-events / key-mapping-api | |
| **Cloth Config** | 对应 Minecraft 版本 | **必选前置**，提供设置界面 |

---

## 🚀 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/installer/) 0.19.2+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 到 `mods/` 目录
3. 安装 [Cloth Config](https://modrinth.com/mod/cloth-config) 到 `mods/` 目录
4. 将 `DawnGuiReader-1.2.0.jar` 复制到 `mods/` 目录
5. 启动游戏，模组自动生效

---

## ⌨️ 快捷键

所有快捷键默认**未绑定**，需在设置界面或 游戏设置 → 按键绑定 → Dawn 无障碍 中手动设置：

| 按键 | 功能 |
|------|------|
| 开关 GUI 朗读 | 切换朗读总开关 |
| 重复朗读当前物品 | 有 GUI 时重复鼠标悬浮物，无 GUI 时重复手持物 |
| 朗读准星方块 | 即时朗读准星所指方块（需准星模式非关闭） |

---

## ⚙️ 设置界面

在**主界面**左下角或**暂停菜单**中点击 **"Dawn 无障碍"** 进入设置，使用 Cloth Config 分标签页布局：

### 通用
| 设置项 | 范围 | 默认值 |
|--------|------|--------|
| 启用朗读 | ON/OFF | OFF |
| 朗读语速 | -10 ~ 10 | 0 |
| 朗读音量 | 0 ~ 100 | 100 |
| 语音音色 | 枚举选择 | 默认 |

### 容器
| 设置项 | 范围 | 默认值 |
|--------|------|--------|
| 容器朗读 | ON/OFF | ON |
| 悬浮延迟 | 100 ~ 3000ms | 500ms |
| 朗读详细信息 | ON/OFF | OFF |
| 详细信息模式 | 独立 / 顺序 | 独立 |
| 详细信息延迟 | 200 ~ 3000ms | 1000ms |

### 物品栏
| 设置项 | 范围 | 默认值 |
|--------|------|--------|
| 物品栏朗读 | ON/OFF | ON |
| 物品栏延迟 | 100 ~ 3000ms | 500ms |

### 准星
| 设置项 | 范围 | 默认值 |
|--------|------|--------|
| 准星模式 | OFF / AUTO / MANUAL | MANUAL |
| 准星延迟 | 100 ~ 3000ms | 500ms |

### 界面朗读
| 设置项 | 范围 | 默认值 |
|--------|------|--------|
| 界面文字朗读 | ON/OFF | OFF |
| 界面朗读延迟 | 100 ~ 3000ms | 500ms |

### 快捷键
在设置界面内直接绑定按键，点击按钮后按下目标按键即可。

---

## 🔊 TTS 引擎

| 平台 | 主引擎 | 回退方案 |
|------|--------|---------|
| Windows | PowerShell + System.Speech（持久化进程） | Minecraft Narrator |
| macOS | `say` 命令 | Minecraft Narrator |
| Linux | `spd-say` 命令 | Minecraft Narrator |

Windows 下首次运行会在 `.minecraft/config/` 目录生成 `dawn-tts-speak.ps1` 脚本。

---

## 🔨 本地构建

确保使用 **JDK 25** 和 **Gradle 9.4+**：

```bash
git clone https://github.com/aojiangQAQ/DawnGuiReader.git
cd DawnGuiReader

# 标准构建
.\gradlew.bat build

# 或使用 D 盘缓存构建（离线，更快）
.\build-d-drive.ps1

# 生成 build/libs/DawnGuiReader-1.2.0.jar
```

---

## 🛠 开发环境

- **Java:** 25
- **Build:** Gradle 9.4+ (Fabric Loom 1.16.3)
- **Minecraft:** 26.1.2
- **Fabric Loader:** 0.19.2
- **Fabric API:** 0.150.0+26.1.2
- **Cloth Config:** 26.2.155（构建依赖；运行时请安装对应 Minecraft 版本）

---

## 📁 目录结构

```
DawnGuiReader/
├── build.gradle
├── gradle.properties
├── README.md
├── LICENSE
└── src/
    ├── client/java/org/dawnteam/accessibility/
    │   ├── DawnAccessibilityClient.java           # 主入口 + 快捷键
    │   ├── config/DawnAccessibilityConfig.java     # JSON 配置
    │   ├── gui/
    │   │   ├── DawnClothConfigScreen.java          # Cloth Config 设置界面
    │   │   ├── EnchantmentScreenReader.java        # 附魔台朗读
    │   │   ├── HoveredItemReader.java              # 容器物品悬浮 + 模组名朗读
    │   │   ├── HoveredTextReader.java              # 创造标签悬浮
    │   │   ├── HotbarItemReader.java               # 物品栏切换
    │   │   ├── BlockTargetReader.java              # 准星方块
    │   │   └── GuiTextReader.java                  # 界面文字朗读
    │   ├── mixin/
    │   │   ├── AbstractContainerScreenAccessor.java
    │   │   ├── AbstractContainerScreenMixin.java
    │   │   ├── CreativeModeInventoryScreenMixin.java
    │   │   ├── GameNarratorAccessor.java
    │   │   ├── KeyMappingAccessor.java
    │   │   ├── PauseScreenMixin.java
    │   │   └── TitleScreenMixin.java
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

## 🤝 贡献

欢迎 Issue / PR：

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
> **版本**：1.2.0  
> **适用客户端**：Minecraft 26.1.x / 26.2.x Fabric 客户端  
> **主页**：[aojiang.space](https://aojiang.space)