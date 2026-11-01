# Changelog

## 1.1.0 — 2026-11-01

### Added
- Protected, skin-rendered corpse grave with owner unlock timer.
- Floating hologram: owner name, despawn countdown, protection status/countdown.
- Take All / Restore GUI and Shift + right-click quick restore.
- Smart despawn, owner-offline/chunk-unload pause and restart persistence.
- Configurable 50% XP recovery, void rescue, curse-of-vanishing support and command suite.
- Fabric 26.1–26.4, Fabric 1.21/1.21.1/1.20.1, Forge 1.20.1–26.3, and NeoForge 1.20.1–26.3 builds (23 jars).
- GitHub Pages site and all-families bundle.

### Fixed
- Fabric legacy menu registration (`GraveMenu.TYPE`).
- Legacy `HelloC2S` unit-codec handshake and hitbox debug name.
- NeoForge `HelloC2S` handshake instance.
- Fabric 26.x GUI title truncation when the owner name is long.
- NeoForge 1.20.1 compatibility via the Forge 47.1.x binary-compatible jar.

### Known gaps
- No Forge or NeoForge build for Minecraft 26.4 (not published upstream).
- No NeoForge 26.1/26.1.1 releases (not published upstream).
- Some 26.x builds are server-tested only; see `SOURCE-MAP.md`.
