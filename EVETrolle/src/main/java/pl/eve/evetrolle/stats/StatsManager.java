package pl.eve.evetrolle.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.EVETrollePlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Proste statystyki użycia (pamięć + data.yml). Zapis asynchroniczny ze snapshotu. */
public final class StatsManager {
    private final EVETrollePlugin plugin;
    private final File file;
    private int total;
    private final Map<String, Integer> perTroll = new HashMap<>();
    private final Map<UUID, Integer> perExecutor = new HashMap<>();

    public StatsManager(EVETrollePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        total = y.getInt("total", 0);
        if (y.isConfigurationSection("troll")) {
            for (String k : y.getConfigurationSection("troll").getKeys(false)) perTroll.put(k, y.getInt("troll." + k));
        }
        if (y.isConfigurationSection("executor")) {
            for (String k : y.getConfigurationSection("executor").getKeys(false)) {
                try {
                    perExecutor.put(UUID.fromString(k), y.getInt("executor." + k));
                } catch (IllegalArgumentException ignored) {
                    // uszkodzony wpis
                }
            }
        }
    }

    public void record(String trollId, Object executor) {
        total++;
        perTroll.merge(trollId, 1, Integer::sum);
        if (executor instanceof Player p) perExecutor.merge(p.getUniqueId(), 1, Integer::sum);
    }

    public int getTotal() { return total; }
    public int getCount(String trollId) { return perTroll.getOrDefault(trollId, 0); }
    public int getUsedBy(UUID id) { return perExecutor.getOrDefault(id, 0); }

    private YamlConfiguration snapshot() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("total", total);
        perTroll.forEach((k, v) -> y.set("troll." + k, v));
        perExecutor.forEach((k, v) -> y.set("executor." + k, v));
        return y;
    }

    /** Zapis asynchroniczny (snapshot tworzony w głównym wątku). */
    public void saveAsync() {
        YamlConfiguration y = snapshot();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(y));
    }

    /** Zapis synchroniczny (onDisable). */
    public void saveNow() {
        write(snapshot());
    }

    private void write(YamlConfiguration y) {
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać data.yml: " + e.getMessage());
        }
    }
}
