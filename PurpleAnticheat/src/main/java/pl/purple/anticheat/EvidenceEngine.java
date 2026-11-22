package pl.purple.anticheat;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Independent evidence accumulator. A single noisy event never becomes a ban:
 * scores decay over time and a check must collect a configurable amount of evidence.
 */
public final class EvidenceEngine {
    private static final long DECAY_INTERVAL_MS = 1_000L;
    private static final double DECAY_PER_SECOND = 0.20;
    private static final int MAX_HISTORY = 12;

    private final PurpleAnticheat plugin;
    private final Map<UUID, Map<String, EvidenceState>> states = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> history = new ConcurrentHashMap<>();

    public EvidenceEngine(PurpleAnticheat plugin) {
        this.plugin = plugin;
    }

    public boolean record(Player player, String check, String evidence, double weight) {
        if (player == null || !player.isOnline() || check == null || check.isBlank()) return false;
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) return false;

        String id = check.toLowerCase(java.util.Locale.ROOT);
        long now = System.currentTimeMillis();
        EvidenceState state = states.computeIfAbsent(player.getUniqueId(), key -> new ConcurrentHashMap<>())
                .computeIfAbsent(id, key -> new EvidenceState());

        synchronized (state) {
            decay(state, now);
            state.score = Math.min(20.0, state.score + Math.max(0.1, Math.min(5.0, weight)));
            state.samples++;
            state.lastEvidence = now;
            double threshold = Math.max(1.0, plugin.getConfig().getDouble("evidence.thresholds." + id,
                    plugin.getConfig().getDouble("evidence.default-threshold", 5.0)));

            addHistory(player.getUniqueId(), id + " +" + String.format(java.util.Locale.ROOT, "%.1f", weight)
                    + " (score " + String.format(java.util.Locale.ROOT, "%.1f", state.score) + "): "
                    + sanitize(evidence));

            if (state.score < threshold || state.reported) return false;
            state.reported = true;
        }

        plugin.getLogger().warning("[Evidence] " + player.getName() + " flagged by " + id
                + " | " + String.format(java.util.Locale.ROOT, "%.1f", state.score) + " points"
                + " | " + sanitize(evidence));
        return plugin.getDetectionManager().reportSignal(player, id,
                "evidence threshold reached: " + sanitize(evidence));
    }

    private void decay(EvidenceState state, long now) {
        if (state.lastDecay == 0L) state.lastDecay = now;
        long seconds = Math.max(0L, (now - state.lastDecay) / DECAY_INTERVAL_MS);
        if (seconds == 0L) return;
        state.score = Math.max(0.0, state.score - seconds * DECAY_PER_SECOND);
        state.lastDecay += seconds * DECAY_INTERVAL_MS;
        if (state.score < 1.0) {
            state.reported = false;
            state.samples = 0;
        }
    }

    private void addHistory(UUID id, String line) {
        Deque<String> deque = history.computeIfAbsent(id, key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(System.currentTimeMillis() + " " + line);
            while (deque.size() > MAX_HISTORY) deque.removeLast();
        }
    }

    public java.util.List<String> getRecentEvidence(UUID id) {
        Deque<String> deque = history.get(id);
        if (deque == null) return java.util.List.of();
        synchronized (deque) { return java.util.List.copyOf(deque); }
    }

    private String sanitize(String value) {
        if (value == null) return "no details";
        String cleaned = value.replaceAll("[\\r\\n\\t]", " ");
        return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
    }

    public void remove(UUID id) {
        states.remove(id);
        history.remove(id);
    }

    public void shutdown() {
        states.clear();
        history.clear();
    }

    private static final class EvidenceState {
        double score;
        int samples;
        long lastEvidence;
        long lastDecay;
        boolean reported;
    }
}
