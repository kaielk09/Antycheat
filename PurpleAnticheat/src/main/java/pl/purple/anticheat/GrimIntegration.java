package pl.purple.anticheat;

import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.events.FlagEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Optional bridge to GrimAC's public API.
 * Grim callbacks may run off the Bukkit main thread, so all Bukkit operations
 * are scheduled back onto the server thread.
 */
public final class GrimIntegration {
    private final PurpleAnticheat plugin;
    private GrimPlugin grimPlugin;
    private boolean active;

    public GrimIntegration(PurpleAnticheat plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("grim.enabled", true)) {
            plugin.getLogger().info("GrimAC integration is disabled in config.yml.");
            return;
        }
        try {
            GrimAbstractAPI api = GrimAPIProvider.get();
            grimPlugin = api.getGrimPlugin(plugin);
            api.getEventBus().get(FlagEvent.class).onFlagSupplier(grimPlugin, (user, check, verbose, cancelled) -> {
                if (cancelled) return cancelled;

                String playerName = user.getName();
                String checkName = check.getCheckName();
                String evidence = verbose.get();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayerExact(playerName);
                    if (player != null && player.isOnline()) {
                        plugin.reportSignal(player, "grim",
                                checkName + (evidence.isBlank() ? "" : ": " + evidence));
                    }
                });
                return cancelled;
            });
            active = true;
            plugin.getLogger().info("GrimAC API connected. Grim flags are forwarded to PurpleAnticheat.");
        } catch (Throwable error) {
            plugin.getLogger().severe("Could not connect to GrimAC API: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            plugin.getLogger().warning("Grim-based detections are unavailable; built-in checks remain active.");
        }
    }

    public void shutdown() {
        if (!active || grimPlugin == null) return;
        try {
            GrimAPIProvider.get().getEventBus().unregisterAllListeners(grimPlugin);
        } catch (Throwable error) {
            plugin.getLogger().warning("Could not unregister GrimAC listeners cleanly: " + error.getMessage());
        }
        active = false;
    }
}
