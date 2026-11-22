# PurpleAnticheat 1.2.1

Purple/black Bukkit-family server plugin source. The repository is the working source project; no JAR is committed or generated yet.

## Current structure
- `src/main/java/pl/purple/anticheat/PurpleAnticheat.java` — plugin entry point and commands
- `AdminGUI.java` — OP-only purple admin menus, manual ban editor, history and response-profile editor
- `BanManager.java` — duration parsing, Bukkit name bans and YAML punishment history
- `DetectionManager.java` — response/review pipeline for signals submitted by a validated server-side check
- `RateLimitGuard.java` — conservative command/chat/interaction rate limits against spam
- `src/main/resources/plugin.yml` — plugin metadata and permissions
- `src/main/resources/config.yml` — settings and defaults
- `pom.xml` — Maven project configuration (Java 21, Spigot API 1.21.4)

## Commands
- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Important scope
This version provides administration, moderation, conservative spam protection and a response API. It does **not** contain automatic cheat-detection algorithms. Response profiles are disabled by default and should only be enabled after connecting and testing trusted server-side validation modules. Do not treat a raw signal as proof of cheating; review false positives before enabling automatic punishment.

## Build later
Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.2.1.jar`.

## Compatibility
Compiled against the Spigot API 1.21.4. Paper, Purpur and Spigot 1.21.x compatibility still needs build and runtime testing on each target version.
