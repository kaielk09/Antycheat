package pl.purple.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit event telemetry only. Raw network packets are deliberately not claimed here;
 * a packet-level decoder would require a separately reviewed protocol adapter.
 */
public final class PacketBehaviorEngine implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Deque<Long>> swings = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> interactions = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> inventoryClicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFlag = new ConcurrentHashMap<>();

    public PacketBehaviorEngine(PurpleAnticheat plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        track(event.getPlayer(), swings, "swing", plugin.getConfig().getInt("checks.packet.max-swings-per-second", 24), 1.0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;
        track(event.getPlayer(), interactions, "interaction",
                plugin.getConfig().getInt("checks.packet.max-interactions-per-second", 38), 0.75);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        track(player, inventoryClicks, "inventory",
                plugin.getConfig().getInt("checks.packet.max-inventory-clicks-per-second", 35), 0.75);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        track(event.getPlayer(), interactions, "command",
                plugin.getConfig().getInt("checks.packet.max-commands-per-second", 8), 0.75);
    }

    private void track(Player player, Map<UUID, Deque<Long>> source, String type, int limit, double weight) {
        if (!plugin.getConfig().getBoolean("checks.packet.enabled", true)) return;
        long now = System.currentTimeMillis();
        Deque<Long> deque = source.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        int count;
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) deque.removeFirst();
            deque.addLast(now);
            count = deque.size();
        }
        if (count <= Math.max(1, limit)) return;
        Long previous = lastFlag.put(player.getUniqueId(), now);
        if (previous != null && now - previous < 500L) return;
        plugin.reportEvidence(player, "packet", "event burst (" + type + "): " + count + " per second", weight);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        swings.remove(id); interactions.remove(id); inventoryClicks.remove(id); lastFlag.remove(id);
    }
}
