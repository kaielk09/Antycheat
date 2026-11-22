package pl.purple.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Routes evidence-threshold alerts to staff and optionally applies configured punishments. */
public final class DetectionManager implements Listener {
    private final PurpleAnticheat plugin;
    private final Map<UUID, Long> lastPunished = new ConcurrentHashMap<>();

    public DetectionManager(PurpleAnticheat plugin) { this.plugin = plugin; }

    public boolean reportSignal(Player player, String check, String evidence) {
        if (player == null || !player.isOnline() || check == null || check.isBlank()) return false;
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) return false;

        String id = check.toLowerCase(java.util.Locale.ROOT);
        String path = "autopunish.checks." + id;
        boolean punishmentEnabled = plugin.getConfig().getBoolean("autopunish.enabled", false)
                && plugin.getConfig().getBoolean(path + ".enabled", false);

        boolean punished = false;
        if (punishmentEnabled) {
            long now = System.currentTimeMillis();
            Long last = lastPunished.get(player.getUniqueId());
            long cooldown = Math.max(0, plugin.getConfig().getLong("autopunish.player-cooldown-seconds", 60)) * 1000L;
            if (last == null || cooldown == 0 || now - last >= cooldown) {
                String detail = "Detected " + id + (evidence == null || evidence.isBlank() ? "" : ": " + evidence);
                punished = plugin.punishDetection(id, player.getName(), detail);
                if (punished) {
                    lastPunished.put(player.getUniqueId(), now);
                    plugin.getLogger().warning("Automatic punishment applied: check=" + id + ", player="
                            + player.getName() + ", duration=" + plugin.getConfig().getString(path + ".duration", "1d"));
                }
            }
        }

        if (plugin.getConfig().getBoolean("settings.alerts", true)
                && plugin.getConfig().getBoolean("settings.notify-admins", true)) {
            notifyStaff(player, id, evidence, punished);
        }
        return punished;
    }

    private void notifyStaff(Player target, String check, String evidence, boolean punished) {
        String duration = plugin.getConfig().getString("autopunish.checks." + check + ".duration", "1d");
        String status = punished ? "&cAUTO-BAN &7(" + duration + ")" : "&eEvidence threshold reached";
        String message = PurpleAnticheat.color(
                plugin.getConfig().getString("messages.prefix", "&5Purple &dAnticheat &8» ")
                        + status + " &f" + target.getName() + " &8| &d" + check
                        + (evidence == null || evidence.isBlank() ? "" : " &8[" + evidence + "]"));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.isOp()) staff.sendMessage(message);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        lastPunished.remove(event.getPlayer().getUniqueId());
        if (plugin.getEvidenceEngine() != null) plugin.getEvidenceEngine().remove(event.getPlayer().getUniqueId());
    }
}
