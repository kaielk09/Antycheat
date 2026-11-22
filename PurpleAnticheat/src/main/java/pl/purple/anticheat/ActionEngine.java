package pl.purple.anticheat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Placement-pattern and action-rate checks. Signals are evidence, not standalone proof. */
public final class ActionEngine implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Deque<Long>> placements = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> underfoot = new ConcurrentHashMap<>();

    public ActionEngine(PurpleAnticheat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("checks.actions.enabled", true)) return;
        long now = System.currentTimeMillis();
        Deque<Long> all = placements.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        int count;
        synchronized (all) {
            trim(all, now, 1000L);
            all.addLast(now);
            count = all.size();
        }
        int fastLimit = plugin.getConfig().getInt("checks.actions.max-placements-per-second", 14);
        if (count > fastLimit) {
            plugin.reportEvidence(player, "action", "high block-placement cadence: " + count + " placements/second", 1.0);
        }

        Location feet = player.getLocation();
        Location placed = event.getBlockPlaced().getLocation();
        boolean underPlayer = placed.getBlockX() == feet.getBlockX()
                && placed.getBlockZ() == feet.getBlockZ()
                && placed.getY() <= feet.getBlockY() - 1
                && placed.getY() >= feet.getBlockY() - 2;
        if (!underPlayer || player.isOnGround() || player.isInsideVehicle() || player.isGliding()) return;

        Deque<Long> scaffold = underfoot.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        int pattern;
        synchronized (scaffold) {
            trim(scaffold, now, 4000L);
            scaffold.addLast(now);
            pattern = scaffold.size();
        }
        double horizontal = player.getVelocity().setY(0).length();
        if (pattern >= 4 && horizontal > 0.12) {
            plugin.reportEvidence(player, "action",
                    "repeated mid-air block placement beneath a horizontally moving player", 1.25);
        }
    }

    private void trim(Deque<Long> deque, long now, long window) {
        while (!deque.isEmpty() && now - deque.peekFirst() > window) deque.removeFirst();
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        placements.remove(event.getPlayer().getUniqueId());
        underfoot.remove(event.getPlayer().getUniqueId());
    }
}
