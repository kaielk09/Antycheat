# PurpleAnticheat 1.3.4

PurpleAnticheat is an independent Minecraft server anti-cheat plugin. It does not require GrimAC or any other anti-cheat plugin, and it does not embed or copy another project's implementation.

## Design direction

The project takes general engineering inspiration from modern server-side anti-cheats: movement simulation, state-aware checks, latency-aware combat analysis, independent check modules, evidence tracking, and configurable staff responses. All PurpleAnticheat checks and code are implemented independently.

## Current implemented checks

- **Speed:** movement sampling with exemptions for common legitimate movement states.
- **Reach:** server-side distance-to-hitbox check on uncancelled player attacks.

These are heuristics, not proof. In particular, the current Reach check is a basic first version and can be affected by server latency, custom hitboxes, and server mechanics. Automatic punishment should be tested on a private server before public use.

## Planned independent check families

The following are development targets, not currently implemented detections:
- Movement: fly, timer, air movement, ground spoof, step and unusual acceleration.
- Combat: reach improvements, attack cadence, aim consistency, target switching and critical-hit anomalies.
- Player actions: scaffold-like placement patterns, fast-place anomalies, inventory action patterns.
- Network/protocol: malformed or impossible action sequences and packet-rate anomalies.
- Defensive analysis: per-player evidence history, latency and world-state compensation, false-positive suppression.

A client brand/name such as Ventura is not reliable proof of cheating. The plugin focuses on observable server-side behavior rather than trying to identify a client's name.

## Commands

- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Build

Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.4.jar`.

## Compatibility

Compiled against Spigot API 1.21.4. Paper and Purpur should be tested on the exact server version before deployment.
