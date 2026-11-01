# Corpse Graves

**Never lose your loot again.** When a player dies, their inventory and XP are stored in a protected grave with a body that renders the player's real skin, a live hologram, and a Take All / Restore GUI.

> **Unofficial mod.** Not affiliated with Mojang or Microsoft. Test on a backup world/server first. Licensed under MIT.
>
> **العربية:** [README.ar.md](README.ar.md)

## Features

- Real player skin: vanilla Mannequin on 26.x, custom skin-rendered body on older versions.
- Owner protection, unlock timer and operator bypass.
- Live hologram: owner name, despawn countdown, protection status and countdown.
- Custom GUI with **Take All** and **Restore**; **Shift + right-click** restores everything to its original slots.
- 50% XP recovery by default (0–100 configurable).
- Smart despawn: pauses while the owner is offline or the chunk is unloaded; survives restarts.
- Void rescue, curse-of-vanishing support and a configurable grave limit per player.
- Deep JSON config for timers, colors, hologram scale/height, corpse pose, XP and void rescue.
- Server commands: `locate`, `reload`, `unlock`, `remove`, `open`, `restore`.

## Supported versions (23 builds)

| Loader | Versions | Builds |
|---|---|---|
| Fabric | 26.1, 26.1.1, 26.1.2, 26.2, 26.3, 26.4-alpha.6, 1.21.1, 1.21, 1.20.1 | 9 |
| Forge | 1.20.1, 1.21, 1.21.1, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 8 |
| NeoForge | 1.20.1 (compatibility), 1.21, 1.21.1, 26.1.2, 26.2, 26.3 | 6 |

**Upstream gaps:** Minecraft 26.4 has no Forge or NeoForge release, and NeoForge never published 26.1/26.1.1. The NeoForge 1.20.1 build is a binary-compatible copy of the Forge 47.1.x jar. See [SOURCE-MAP.md](SOURCE-MAP.md).

## Download

- **Website:** https://kadoew.github.io/corpse-graves/
- **Release with all jars:** https://github.com/kadoew/corpse-graves/releases/tag/v1.1.0
- **Complete bundle:** `docs/downloads/corpsegraves-all-families.zip` (23 jars + 7 Fabric API jars + config + docs, ~18 MB).
- **Checksums:** `docs/downloads/SHA256SUMS.txt`.

Use exactly **one** Corpse Graves jar per `mods` folder. Do not mix Fabric, Forge and NeoForge jars.

## Install

### Fabric
1. Install Fabric Loader: 0.19.3+ for 26.x, 0.16+ for 1.21.x, 0.15+ for 1.20.1.
2. Put the matching mod jar and **Fabric API** build in `mods` on the client and server.
3. Use Java 25 for 26.x, Java 21 for 1.21/1.21.1, Java 17 for 1.20.1.

### Forge / NeoForge
1. Install the loader version listed in the download row.
2. Put only the mod jar in `mods` on the client and server. No Fabric API is required.
3. Do not mix loader families.

## Configuration

`config/corpsegraves.json` (sample: `docs/downloads/corpsegraves.sample.json`). Apply changes with `/corpsegraves reload`.

```json
{
  "despawnSeconds": 3600,
  "unlockSeconds": 1800,
  "xpRecoveryPercent": 50.0,
  "hologram": {
    "scale": 1.4,
    "height": 1.45,
    "background": "#66000000",
    "nameColor": "#FFAA00",
    "despawnColor": "#55FFFF",
    "protectedColor": "#FF5555",
    "unlockedColor": "#55FF55"
  }
}
```

## Build from source

- JDK 17 (1.20.1), JDK 21 (1.21.x), JDK 25 (26.x).
- Each project contains its own Gradle wrapper: `./gradlew build` or `gradlew.bat build`.
- Build everything:
  ```powershell
  $env:JAVA17_HOME="C:\path\to\jdk17"
  $env:JAVA21_HOME="C:\path\to\jdk21"
  $env:JAVA25_HOME="C:\path\to\jdk25"
  ./build-all.ps1
  ```
- Details: [BUILDING.md](BUILDING.md). Source map: [SOURCE-MAP.md](SOURCE-MAP.md). Maintenance workflow: [MAINTAINERS.md](MAINTAINERS.md).

## Testing

- All 23 builds boot their dedicated servers to `Done` and create graves.
- Full real-client GUI/Take All passes: Fabric 26.3/1.21.1/1.20.1, Forge 26.3/1.20.1, NeoForge 26.3/1.21.1.
- Vanilla-client quick loot: Forge 1.21/1.21.1/26.3 and NeoForge 1.20.1.
- The remaining 26.x builds are server-tested (boot + grave creation) without a full visual pass.

## Contributing

Open an issue with your Minecraft version, loader/version, exact jar name and `latest.log`. Use the bug report template. Keep changes MIT-compatible and do not commit worlds, logs, tokens or secrets. See [MAINTAINERS.md](MAINTAINERS.md) for the fix-and-release workflow.

## License

MIT © 2026 Kador. See [LICENSE](LICENSE) and [THIRD-PARTY.md](THIRD-PARTY.md).
