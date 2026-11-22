package pl.purple.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repeated-signal confirmation pipeline. A single heuristic signal can never cause a ban.
 * Each check must independently reach its configured count inside a rolling time window.
 */
public final class DetectionManager implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<String, SignalWindow> signals = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPunished = new ConcurrentHashMap<>();

    public DetectionManager(PurpleAnticheat plugin) {
        this.plugin = plugin;
    }

    public boolean reportSignal(Player player, String check, String evidence) {
        if (player == null || !player.isOnline() || check == null || check.isBlank()) return false;
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) return false;

        String id = check.toLowerCase(java.util.Locale.ROOT);
        String path = "autopunish.checks." + id;
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) return false;

        int required = Math.max(2, Math.min(20, plugin.getConfig().getInt(path + ".min-violations", 6)));
        long windowMs = Math.max(5, Math.min(120, plugin.getConfig().getInt(path + ".window-seconds", 30))) * 1000L;
        long now = System.currentTimeMillis();
        String key = player.getUniqueId() + ":" + id;
        SignalWindow state = signals.computeIfAbsent(key, ignored -> new SignalWindow());
        int count;
        synchronized (state) {
            while (!state.timestamps.isEmpty() && now - state.timestamps.peekFirst() > windowMs) {
                state.timestamps.removeFirst();
            }
            state.timestamps.addLast(now);
            count = state.timestamps.size();
        }

        if (plugin.getConfig().getBoolean("settings.notify-admins", true)) {
            notifyStaff(player, id, count, required, evidence);
        }

        if (count < required || !plugin.getConfig().getBoolean("autopunish.enabled", false)) return true;

        long cooldown = Math.max(60, plugin.getConfig().getLong("autopunish.player-cooldown-seconds", 3600)) * 1000L;
        Long last = lastPunished.get(player.getUniqueId());
        if (last != null && now - last < cooldown) return true;

        // Clear this check's window before punishment to prevent repeated ban attempts.
        synchronized (state) {
            state.timestamps.clear();
        }
        boolean punished = plugin.punishDetection(id, player.getName(),
                "Repeated " + id + " signals (" + count + "/" + required + " in " + (windowMs / 1000) + "s). Last: "
                        + (evidence == null || evidence.isBlank() ? "no extra evidence" : evidence));
        if (punished) {
            lastPunished.put(player.getUniqueId(), now);
            plugin.getLogger().warning("Confirmed repeated detection: check=" + id + ", player=" + player.getName()
                    + ", signals=" + count + "/" + required);
        }
        return true;
    }

    private void notifyStaff(Player target, String check, int count, int required, String evidence) {
        String message = PurpleAnticheat.color(plugin.getConfig().getString("messages.prefix", "&5Purple &dAnticheat &8» ")
                + "&cDetection &f" + check + " &cfor &f" + target.getName()
                + " &7(" + count + "/" + required + ")"
                + (evidence == null || evidence.isBlank() ? "" : " &8[" + evidence + "]"));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.isOp() && staff.hasPermission("purpleanticheat.admin")) staff.sendMessage(message);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String prefix = event.getPlayer().getUniqueId() + ":";
        signals.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static final class SignalWindow {
        private final java.util.ArrayDeque<Long> timestamps = new java.util.ArrayDeque<>();
    }
}
