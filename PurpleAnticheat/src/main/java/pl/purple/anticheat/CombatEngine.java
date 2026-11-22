package pl.purple.anticheat;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BoundingBox;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Combat telemetry: hitbox distance and unusually dense successful-hit sequences. */
public final class CombatEngine implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReachFlag = new ConcurrentHashMap<>();

    public CombatEngine(PurpleAnticheat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.getGameMode() != GameMode.SURVIVAL || attacker.isInsideVehicle()
                || attacker.isGliding() || attacker.isDead()) return;

        Entity target = event.getEntity();
        if (plugin.getConfig().getBoolean("checks.combat.reach.enabled", true)) {
            double distance = distanceToBox(attacker, target.getBoundingBox());
            int ping = Math.max(0, Math.min(200, attacker.getPing()));
            double maxReach = plugin.getConfig().getDouble("checks.combat.reach.max-distance", 3.55)
                    + ping * plugin.getConfig().getDouble("checks.combat.reach.ping-compensation", 0.002);
            if (distance > maxReach) {
                long now = System.currentTimeMillis();
                Long previous = lastReachFlag.put(attacker.getUniqueId(), now);
                if (previous == null || now - previous > 350L) {
                    plugin.reportEvidence(attacker, "combat",
                            String.format(java.util.Locale.ROOT, "hitbox distance %.2f exceeded compensated limit %.2f", distance, maxReach), 2.0);
                }
            }
        }

        long now = System.currentTimeMillis();
        Deque<Long> deque = hits.computeIfAbsent(attacker.getUniqueId(), key -> new ArrayDeque<>());
        int count;
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) deque.removeFirst();
            deque.addLast(now);
            count = deque.size();
        }
        int limit = plugin.getConfig().getInt("checks.combat.max-successful-hits-per-second", 18);
        if (count > limit) {
            plugin.reportEvidence(attacker, "combat", "unusually dense successful-hit cadence: " + count + " hits/second", 1.0);
        }
    }

    private double distanceToBox(Player attacker, BoundingBox box) {
        var eye = attacker.getEyeLocation().toVector();
        double x = Math.max(box.getMinX(), Math.min(eye.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(eye.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(eye.getZ(), box.getMaxZ()));
        return eye.distance(new org.bukkit.util.Vector(x, y, z));
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        hits.remove(event.getPlayer().getUniqueId());
        lastReachFlag.remove(event.getPlayer().getUniqueId());
    }
}
