package pl.eve.evetrolle.troll;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.Troll;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.api.TrollInfo;
import pl.eve.evetrolle.util.FX;

import java.util.List;
import java.util.Locale;

/**
 * Własny troll zdefiniowany w config.yml (sekcja custom-trolls). Akcje:
 * [TITLE] tytuł;podtytuł[;fadeIn;stay;fadeOut]  [ACTIONBAR] tekst  [CHAT] tekst
 * [SOUND] klucz głośność wysokość  [PARTICLE] NAZWA ilość rozrzut prędkość
 * [BOSSBAR] tekst  [DELAY] ticki
 */
public final class ConfigTroll implements Troll {
    private final String id;
    private final ConfigurationSection section;
    private final TrollInfo info;

    public ConfigTroll(String id, ConfigurationSection section) {
        this.id = id;
        this.section = section;
        Material mat = Material.matchMaterial(section.getString("material", "PAPER"));
        this.info = new TrollInfo(section.getString("name", id), mat == null ? Material.PAPER : mat,
                section.getStringList("lore"), section.getInt("slot", -1), section.getInt("duration", 5),
                section.getString("permission", ""));
    }

    @Override public String getId() { return id; }
    @Override public TrollInfo getDefaultInfo() { return info; }

    @Override
    public void start(TrollContext ctx) {
        List<String> actions = section.getStringList("actions");
        long t = 0;
        for (String raw : actions) {
            String line = raw.trim();
            int close = line.indexOf(']');
            if (!line.startsWith("[") || close < 0) continue;
            String tag = line.substring(1, close).toUpperCase(Locale.ROOT);
            String arg = line.substring(close + 1).trim();
            if (tag.equals("DELAY")) {
                try {
                    t += Math.max(0, Long.parseLong(arg));
                } catch (NumberFormatException ignored) {
                    // zła wartość - ignorujemy
                }
                continue;
            }
            if (t <= 0) {
                run(ctx, tag, arg);
            } else {
                ctx.runLater(t, () -> run(ctx, tag, arg));
            }
        }
    }

    private void run(TrollContext ctx, String tag, String arg) {
        Player p = ctx.getTarget();
        if (p == null) return;
        switch (tag) {
            case "TITLE" -> {
                String[] a = arg.split(";");
                ctx.title(a.length > 0 ? a[0] : "", a.length > 1 ? a[1] : "",
                        num(a, 2, 10), num(a, 3, 50), num(a, 4, 10));
            }
            case "ACTIONBAR" -> ctx.actionBar(arg);
            case "CHAT" -> ctx.message(arg);
            case "SOUND" -> FX.sound(p, arg);
            case "PARTICLE" -> {
                String[] a = arg.split("\\s+");
                Particle type = FX.particle(a[0], Particle.CLOUD);
                int count = num(a, 1, 20);
                double spread = dbl(a, 2, 0.5);
                double speed = dbl(a, 3, 0.02);
                FX.particle(p, type, p.getLocation().add(0, 1, 0), count, spread, spread, spread, speed);
            }
            case "BOSSBAR" -> {
                ctx.countdownBar(arg, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            }
            default -> { }
        }
    }

    private static int num(String[] a, int i, int def) {
        try {
            return i < a.length ? Integer.parseInt(a[i].trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(String[] a, int i, double def) {
        try {
            return i < a.length ? Double.parseDouble(a[i].trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
