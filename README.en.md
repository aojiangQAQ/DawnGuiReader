# DawnGuiReader — Minecraft Accessibility Reader

<p align="center">
  <a href="README.md">简体中文</a> ｜ <strong>English</strong>
</p>

<p align="center">
  <img src="src/main/resources/assets/dawn_accessibility/icon.png" alt="DawnGuiReader icon" width="128">
</p>

![Minecraft](https://img.shields.io/badge/Minecraft-26.1--26.2-green)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2-blue)
![Cloth Config](https://img.shields.io/badge/Cloth_Config-required-orange)
![Version](https://img.shields.io/badge/version-1.3.0-6f42c1)
![License](https://img.shields.io/badge/license-MIT-blue)

> DawnGuiReader is a client-side Minecraft Fabric accessibility mod for blind and low-vision players. It reads container items, hotbar items, targeted blocks, and GUI text aloud using system TTS on Windows, macOS, and Linux.

## What’s new in v1.3.0

- Added Minecraft 26.2 support while retaining compatibility with Minecraft 26.1.x.
- Added a compatibility layer for accessing the current screen across 26.1 and 26.2.
- Updated Cloth Config, build configuration, and Modrinth publishing metadata.
- Thanks to [@S-H-Go](https://github.com/S-H-Go) for contributing Minecraft 26.2 compatibility.

## Features

- **Container item reading:** Supports inventories, chests, crafting tables, furnaces, anvils, enchanting tables, villager trading, and other container screens.
- **Item detail reading:** Optionally reads the source mod name, with independent and sequential modes.
- **Enchanting table reading:** Reads the level and description of the hovered enchantment option.
- **Creative tab reading:** Reads hovered Creative Mode category tabs.
- **Hotbar reading:** Reads the held item when the selected slot changes; says “Hand” for an empty hand.
- **Crosshair block reading:** Off, automatic, and manual hotkey modes.
- **GUI text reading:** Reads hovered buttons, settings, world names, and server names.
- **Configurable speech:** Independent toggles and delays, plus speech rate and volume controls.
- **Cross-platform TTS:** Uses System.Speech on Windows, `say` on macOS, and `spd-say` on Linux, with Minecraft Narrator as a fallback.
- **Modern configuration UI:** Tabbed Cloth Config screen with built-in key binding controls.
- **Non-blocking speech:** TTS work runs on background daemon threads; Windows uses a persistent PowerShell process to reduce latency.

## Requirements

| Dependency | Requirement |
|---|---|
| Minecraft | 26.1.x or 26.2.x |
| Fabric Loader | 0.19.2+ |
| Fabric API | Version matching Minecraft |
| Cloth Config | Version matching Minecraft; required |
| Java | 25+ |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/).
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and [Cloth Config](https://modrinth.com/mod/cloth-config) in `mods/`.
3. Download and put `DawnGuiReader-1.3.0.jar` in `mods/`.
4. Start the game and open **Dawn Accessibility** from the bottom-left of the title screen or from the pause menu.
5. The master reader toggle is disabled by default. Enable the reading features you want in settings.

## Key bindings

All key bindings are unassigned by default. Configure them in the Dawn settings screen or Minecraft’s key binding screen.

| Key binding | Action |
|---|---|
| Toggle GUI reader | Toggles the master reader switch |
| Repeat current item | Repeats the hovered GUI item or the held in-game item |
| Read crosshair block | Immediately reads the block under the crosshair |

## Settings categories

| Category | Main options |
|---|---|
| General | Master toggle, speech rate, volume, voice |
| Container | Container reader, hover delay, details, independent/sequential mode |
| Hotbar | Hotbar reader and delay |
| Crosshair | Off/automatic/manual mode and delay |
| GUI reader | GUI text reader and hover delay |
| Key bindings | Bind keys directly in the mod settings |

## TTS engines

| Platform | Primary engine | Fallback |
|---|---|---|
| Windows | PowerShell + System.Speech (persistent process) | Minecraft Narrator |
| macOS | `say` | Minecraft Narrator |
| Linux | `spd-say` | Minecraft Narrator |

On first use, Windows creates `dawn-tts-speak.ps1` under `.minecraft/config/`.

## Building locally

JDK 25 is required. The Gradle Wrapper provides the project’s Gradle version.

```powershell
git clone https://github.com/aojiangQAQ/DawnGuiReader.git
cd DawnGuiReader
./gradlew.bat build
```

The output is written to `build/libs/DawnGuiReader-1.3.0.jar`.

## Contributing

Issues and pull requests are welcome. For Minecraft version compatibility changes, please include runtime checks for startup, title screen, pause screen, container reading, GUI reading, and the enchanting table.

## License and credits

DawnGuiReader is licensed under the [MIT License](LICENSE).

- Team: Dawn Team (曙光团队)
- Creator: Aojiang (鳌江)
- Homepage: [aojiang.space](https://aojiang.space)
- Source: [GitHub](https://github.com/aojiangQAQ/DawnGuiReader)
