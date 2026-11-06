package pl.eve.evetrolle;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.util.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Wiadomości z messages.yml (z fallbackiem do wersji wbudowanej w plugin). */
public final class Messages {
    private final EVETrollePlugin plugin;
    private YamlConfiguration cfg = new YamlConfiguration();

    public Messages(EVETrollePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) plugin.saveResource("messages.yml", false);
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(f);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się wczytać domyślnych wiadomości: " + e.getMessage());
        }
        cfg = loaded;
    }

    public String raw(String key) {
        String s = cfg.getString(key);
        return s == null ? "&c[brak wiadomości: " + key + "]" : s;
    }

    public List<String> rawList(String key) {
        return cfg.getStringList(key);
    }

    public static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private Map<String, String> withPrefix(String... kv) {
        Map<String, String> m = map(kv);
        m.putIfAbsent("prefix", raw("prefix"));
        return m;
    }

    public Component get(String key, String... kv) {
        return Text.parse(Text.apply(raw(key), withPrefix(kv), null));
    }

    public void send(CommandSender to, String key, String... kv) {
        String raw = raw(key);
        if (raw.isEmpty()) return;
        Player papi = to instanceof Player p ? p : null;
        to.sendMessage(Text.parse(raw, withPrefix(kv), papi));
    }

    public void sendList(CommandSender to, String key, String... kv) {
        Player papi = to instanceof Player p ? p : null;
        for (String line : rawList(key)) {
            to.sendMessage(Text.parse(line, withPrefix(kv), papi));
        }
    }

    /** Wiadomość jako zwykły tekst bez kolorów (do konsoli). */
    public String plain(String key, String... kv) {
        return Text.strip(Text.apply(raw(key), withPrefix(kv), null));
    }
}
