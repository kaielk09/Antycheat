# PurpleAnticheat 1.3.1

Purple/black Bukkit-family server plugin source. The repository is the working source project; no JAR is committed or generated yet.

## Current modules
- `PurpleAnticheat.java` — plugin entry point and commands
- `AdminGUI.java` — OP-only purple admin menus, manual ban editor, history and response-profile editor
- `BanManager.java` — duration parsing, Bukkit name bans and YAML punishment history
- `DetectionManager.java` — rolling repeated-signal confirmation and automatic punishment cooldown
- `DetectionChecks.java` — conservative server-side speed and attack-reach heuristics
- `RateLimitGuard.java` — command/chat/interaction spam protection
- `config.yml` — check thresholds and punishment profiles

## Automatic punishment defaults
Automatic punishment is enabled only for the initial speed and combat-reach checks. A player must generate at least **6 signals from the same check within 30 seconds** before the plugin applies a **1-hour ban**. A one-hour per-player cooldown prevents repeated punishment loops. Other check categories remain disabled. Staff alerts are sent for every signal.

Movement checks ignore creative/spectator players, flight, vehicles, elytra, swimming, climbing, riptiding, speed/slowness effects and strong external velocity. Teleports reset movement samples. The reach check uses a conservative distance threshold and only considers uncancelled damage events from survival players.

## Important limitations
These are heuristics, not proof. Latency, server lag, knockback, unusual server mechanics, plugins, and Minecraft version differences can still create false positives. Repeated confirmation lowers risk but cannot guarantee that innocent players will never be banned. Before using this on a public server, test with normal players across different ping levels and review staff alerts. If false positives appear, turn off `autopunish.enabled` immediately and tune the thresholds.

This version does **not** detect every cheat. Aim assistance, autoclickers, packet manipulation, inventory automation, flight variants and other categories do not have complete detection modules yet. Disabled categories do not automatically ban.

## Commands
- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Build
Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.1.jar`.

## Compatibility
Compiled against Spigot API 1.21.4. Paper, Purpur and Spigot 1.21.x runtime compatibility still needs testing on each target server.
