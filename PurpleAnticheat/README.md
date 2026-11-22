# PurpleAnticheat 1.3.5

PurpleAnticheat is an independent Minecraft server anti-cheat plugin. It does not require GrimAC or another anti-cheat plugin and does not embed or copy another project's implementation.

## Five independent engines

1. **MovementEngine** — repeated horizontal displacement and sustained unsupported mid-air hovering. Common movement states (creative/spectator, flight, vehicles, elytra, swimming, climbing, effects and selected block mechanics) are excluded.
2. **CombatEngine** — distance to the target hitbox with bounded ping compensation, plus unusually dense successful-hit cadence.
3. **ActionEngine** — fast block-placement cadence and repeated mid-air placement beneath a moving player (scaffold-like evidence).
4. **PacketBehaviorEngine** — high-rate Bukkit event telemetry for swings, interactions, inventory clicks and commands. This is not raw packet decoding; Bukkit events do not expose every protocol detail.
5. **EvidenceEngine** — per-player/per-check weighted evidence, time decay, bounded recent history and configurable thresholds. Repeated evidence is required before staff are alerted.

## Important limitations

These are first independent implementations, not a claim of perfect or universal cheat detection. Bukkit/Spigot API does not expose enough information for robust packet-level checks, precise aim analysis, or full client movement simulation. Those require separately designed protocol/latency/world-state adapters and extensive testing. Client brand names are not treated as proof.

Automatic bans are **off by default**. Staff alerts remain enabled after an evidence threshold is reached. Validate the thresholds on a private test server before enabling punishment; custom server mechanics, latency, knockback and plugins can affect heuristics.

## Commands

- `/purpleanticheat banedit` (aliases: `/purpleac banedit`, `/pac banedit`)
- `/purpleanticheat console edit`

Both require OP and `purpleanticheat.admin`.

## Build

Requires JDK 21 and Maven. From this directory run:

```bash
mvn package
```

Expected output: `target/PurpleAnticheat-1.3.5.jar`.

## Compatibility

Configured against Spigot API 1.21.4. Test the resulting JAR on the exact Paper/Spigot/Purpur server build before using it publicly.
