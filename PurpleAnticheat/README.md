# PurpleAnticheat 1.3.0

Purple/black Bukkit-family server plugin source. The repository is the working source project; no JAR is committed or generated yet.

## Current modules
- `PurpleAnticheat.java` — plugin entry point and commands
- `AdminGUI.java` — OP-only purple admin menus, manual ban editor, history and response-profile editor
- `BanManager.java` — duration parsing, Bukkit name bans and YAML punishment history
- `DetectionManager.java` — signal review pipeline and configurable response
- `DetectionChecks.java` — conservative server-side speed and attack-reach heuristics
- `RateLimitGuard.java` — command/chat/interaction spam protection
- `config.yml` — check thresholds and punishment profiles
- `plugin.yml` — plugin metadata and permissions

## Commands
- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Detection scope and limitations
This release adds initial speed and attack-reach heuristics plus rate guards. It does **not** detect every cheat. No server plugin can guarantee detection of every client modification: some behavior is indistinguishable from legitimate play, and latency, knockback, effects, vehicles, server performance and Minecraft version can affect measurements. Aim assistance, autoclickers, packet manipulation, inventory automation, flight variants and other categories need separate checks and careful testing.

Detections are signals for staff review, not proof. Automatic punishments for all checks are **disabled by default** in `config.yml`. Keep them disabled until you have tested on your own server with normal players and different ping levels.

## Build
Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.0.jar`.

## Compatibility
Compiled against Spigot API 1.21.4. Paper, Purpur and Spigot 1.21.x runtime compatibility still needs testing on each target server.
