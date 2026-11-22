# PurpleAnticheat 1.3.3

Purple/black Bukkit-family server plugin source. The repository is the working source project; no JAR is committed or generated yet.

## Detection engines

### Built-in Purple checks
- Speed: movement sampling with exemptions for common legitimate movement states.
- Reach: server-side distance-to-hitbox check on uncancelled player attacks.

### Optional GrimAC integration
PurpleAnticheat can connect to the official GrimAC public API when GrimAC is installed beside it. Grim's `FlagEvent` notifications are forwarded to PurpleAnticheat's punishment and OP-alert pipeline. This lets Grim perform its own supported checks while PurpleAnticheat controls the server's ban duration, punishment history, and staff messages.

Install GrimAC separately in the server's `plugins` folder. PurpleAnticheat does not bundle GrimAC or its implementation. The integration uses GrimAPI as a provided/compile-only dependency and is optional at runtime. If GrimAC is absent, Purple's built-in Speed and Reach checks still load.

The Grim integration currently forwards any non-cancelled Grim flag to the `grim` punishment profile. With the default configuration, one Grim flag causes an immediate ban for `1h`. This is intentionally configurable under `autopunish.checks.grim.duration` and `autopunish.checks.grim.enabled`. Because automatic punishment is immediate, test on a private server first and review Grim's own check configuration and alerts before enabling it on a public server. A check flag is a detector signal, not an infallible proof.

## Automatic punishment and staff alerts
- Auto-punish is enabled for built-in Speed, Reach, and forwarded Grim flags.
- The duration is read from the relevant profile in `/purpleanticheat console edit` or `config.yml`.
- Detection alerts are sent only to online OP players.
- Punishment history is saved in `plugins/PurpleAnticheat/punishments.yml`.

## Commands
- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Build
Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.3.jar`.

## Compatibility
Compiled against Spigot API 1.21.4 and GrimAPI 1.3.2.1. GrimAC is an optional runtime dependency. Paper, Purpur and Spigot versions should be tested on the exact server version before deployment.
