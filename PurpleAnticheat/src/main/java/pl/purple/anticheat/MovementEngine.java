package pl.purple.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Movement checks based on repeated server-observed movement, not client labels. */
public final class MovementEngine implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Location> last = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> speedSamples = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hoverTicks = new ConcurrentHashMap<>();
    private final BukkitTask task;

    public MovementEngine(PurpleAnticheat plugin) {
        this.plugin = plugin;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, 1L, 1L);
    }

    private void sample() {
        if (!plugin.getConfig().getBoolean("checks.movement.enabled", true)) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Location now = player.getLocation();
            Location before = last.put(id, now.clone());
            if (before == null || before.getWorld() == null || !before.getWorld().equals(now.getWorld())) continue;
            if (exempt(player)) {
                speedSamples.remove(id);
                airTicks.remove(id);
                hoverTicks.remove(id);
                continue;
            }

            double dx = now.getX() - before.getX();
            double dz = now.getZ() - before.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double speedLimit = plugin.getConfig().getDouble("checks.movement.horizontal-per-tick", 1.45);
            int needed = Math.max(3, plugin.getConfig().getInt("checks.movement.samples", 6));

            if (horizontal > speedLimit) {
                int count = speedSamples.merge(id, 1, Integer::sum);
                if (count >= needed) {
                    speedSamples.put(id, 0);
                    plugin.reportEvidence(player, "speed",
                            String.format(java.util.Locale.ROOT, "repeated horizontal displacement %.2f blocks/tick", horizontal), 1.5);
                }
            } else {
                speedSamples.computeIfPresent(id, (key, count) -> Math.max(0, count - 1));
            }

            if (player.isOnGround()) {
                airTicks.put(id, 0);
                hoverTicks.put(id, 0);
            } else {
                int airborne = airTicks.merge(id, 1, Integer::sum);
                double dy = now.getY() - before.getY();
                if (airborne > 18 && Math.abs(dy) < 0.018
                        && Math.abs(player.getVelocity().getY()) < 0.08
                        && player.getFallDistance() > 2.5f) {
                    int hovering = hoverTicks.merge(id, 1, Integer::sum);
                    if (hovering == 12) {
                        plugin.reportEvidence(player, "flight",
                                "sustained mid-air hovering without a supported movement state", 1.5);
                    }
                } else {
                    hoverTicks.put(id, 0);
                }
            }
        }
    }

    private boolean exempt(Player p) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return true;
        if (p.isFlying() || p.isInsideVehicle() || p.isGliding() || p.isSwimming()
                || p.isClimbing() || p.isRiptiding() || p.isSleeping()) return true;
        if (p.hasPotionEffect(PotionEffectType.SPEED) || p.hasPotionEffect(PotionEffectType.SLOWNESS)
                || p.hasPotionEffect(PotionEffectType.LEVITATION) || p.hasPotionEffect(PotionEffectType.JUMP_BOOST)) return true;
        if (p.getVelocity().lengthSquared() > 0.85) return true;
        Material feet = p.getLocation().getBlock().getType();
        Material below = p.getLocation().subtract(0, 1, 0).getBlock().getType();
        return feet == Material.WATER || feet == Material.LAVA || feet == Material.COBWEB
                || below == Material.SLIME_BLOCK || below == Material.HONEY_BLOCK
                || below == Material.BED || below == Material.SOUL_SAND
                || below == Material.SOUL_SOIL || below == Material.POWDER_SNOW;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) { clear(event.getPlayer().getUniqueId()); }

    @EventHandler public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer().getUniqueId()); }

    private void clear(UUID id) {
        last.remove(id); speedSamples.remove(id); airTicks.remove(id); hoverTicks.remove(id);
    }

    public void shutdown() {
        task.cancel();
        last.clear(); speedSamples.clear(); airTicks.clear(); hoverTicks.clear();
    }
}
