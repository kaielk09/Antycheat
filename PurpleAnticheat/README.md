# PurpleAnticheat 1.3.2

Purple/black Bukkit-family server plugin source. The repository is the working source project; no JAR is committed or generated yet.

## Current modules
- `PurpleAnticheat.java` — plugin entry point and commands
- `AdminGUI.java` — OP-only purple admin menus, manual ban editor, history and response-profile editor
- `BanManager.java` — duration parsing, Bukkit name bans and YAML punishment history
- `DetectionManager.java` — immediate automatic punishment after a check reports a validated detection, plus OP-only alerts
- `DetectionChecks.java` — server-side speed and attack-reach heuristics
- `RateLimitGuard.java` — command/chat/interaction spam protection
- `config.yml` — check thresholds and punishment profiles

## Automatic punishment
Auto-punish is enabled for the implemented speed and combat-reach checks. One **validated detection report** immediately triggers the configured punishment; there is no six-signal counter. The duration comes from the selected check's `duration` setting in the response-profile GUI (`/purpleanticheat console edit`) or the matching `autopunish.checks.<check>.duration` config value. Supported formats include `30m`, `2h`, `3d`, `1w`, and `perm`.

Detection announcements are sent only to online OP players. The message includes the player, check, and configured ban duration. The default duration for speed and combat is `1h`; change it in the response-profile editor to use the value you selected.

A speed report is produced only after the movement sampler observes the configured number of consecutive high-speed samples. Reach reports are produced from an uncancelled player attack beyond the configured distance. These are still heuristics, not proof: latency, knockback, server lag, custom movement mechanics, and other plugins can cause false positives. Immediate auto-bans make careful threshold testing especially important. Test on a private server first and keep a way to disable `autopunish.enabled`.

## Detection coverage
This version implements basic speed and reach checks only. Aim assistance, autoclickers, packet manipulation, inventory automation, flight variants and other cheat categories do not have complete detection modules yet. Their profiles remain disabled and do not automatically ban.

## Commands
- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Build
Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.2.jar`.

## Compatibility
Compiled against Spigot API 1.21.4. Paper, Purpur and Spigot 1.21.x runtime compatibility still needs testing on each target server.
