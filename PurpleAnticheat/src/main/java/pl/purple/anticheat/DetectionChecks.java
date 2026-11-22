package pl.purple.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Conservative server-side movement and reach signals. These are heuristics, not proof. */
public final class DetectionChecks implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Location> previous = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> speedSamples = new ConcurrentHashMap<>();
    private final BukkitTask movementTask;

    public DetectionChecks(PurpleAnticheat plugin) {
        this.plugin = plugin;
        this.movementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sampleMovement, 1L, 1L);
    }

    private void sampleMovement() {
        if (!plugin.getConfig().getBoolean("checks.movement.enabled", true)) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Location now = player.getLocation();
            Location before = previous.put(id, now.clone());
            if (before == null || before.getWorld() == null || !before.getWorld().equals(now.getWorld())) continue;
            if (isExempt(player)) {
                speedSamples.remove(id);
                continue;
            }
            double dx = now.getX() - before.getX();
            double dz = now.getZ() - before.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double threshold = plugin.getConfig().getDouble("checks.movement.horizontal-per-tick", 1.45);
            int required = Math.max(4, plugin.getConfig().getInt("checks.movement.samples", 8));
            if (horizontal > threshold) {
                int count = speedSamples.merge(id, 1, Integer::sum);
                if (count >= required) {
                    speedSamples.put(id, 0);
                    plugin.reportSignal(player, "speed",
                            String.format(java.util.Locale.ROOT, "repeated high horizontal movement (%.2f blocks/tick)", horizontal));
                }
            } else {
                speedSamples.computeIfPresent(id, (key, count) -> Math.max(0, count - 1));
            }
        }
    }

    private boolean isExempt(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isFlying()
                || player.isInsideVehicle()
                || player.isGliding()
                || player.isSwimming()
                || player.isClimbing()
                || player.isRiptiding()
                || player.hasPotionEffect(PotionEffectType.SPEED)
                || player.hasPotionEffect(PotionEffectType.SLOWNESS)
                || player.getVelocity().lengthSquared() > 0.85;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("checks.combat.reach.enabled", true)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.getGameMode() != GameMode.SURVIVAL || attacker.isInsideVehicle() || attacker.isGliding()) return;
        Entity target = event.getEntity();
        double distance = distanceToBox(attacker.getEyeLocation(), target.getBoundingBox());
        double maxReach = plugin.getConfig().getDouble("checks.combat.reach.max-distance", 4.35);
        if (distance > maxReach) {
            plugin.reportSignal(attacker, "combat",
                    String.format(java.util.Locale.ROOT, "attack outside configured reach (%.2f blocks)", distance));
        }
    }

    private double distanceToBox(Location point, BoundingBox box) {
        double x = Math.max(box.getMinX(), Math.min(point.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(point.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(point.getZ(), box.getMaxZ()));
        return point.toVector().distance(new org.bukkit.util.Vector(x, y, z));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        previous.remove(event.getPlayer().getUniqueId());
        speedSamples.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        previous.remove(event.getPlayer().getUniqueId());
        speedSamples.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        movementTask.cancel();
        previous.clear();
        speedSamples.clear();
    }
}