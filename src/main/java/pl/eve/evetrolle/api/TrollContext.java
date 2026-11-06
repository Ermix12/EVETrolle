package pl.eve.evetrolle.api;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.eve.evetrolle.util.Text;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.logging.Level;

/**
 * Kontekst jednego uruchomienia trolla. Zarządza zadaniami, encjami, bossbarami i sprzątaniem.
 * Po {@link #end()} wszystko jest automatycznie czyszczone. Używać wyłącznie z głównego wątku.
 */
public final class TrollContext {
    private final Plugin plugin;
    private final UUID targetId;
    private final String targetName;
    private final CommandSender executor;
    private final Troll troll;
    private final TrollInfo info;
    private final ConfigurationSection options;
    private final int maxEntities;
    private final boolean bossBarsEnabled;
    private final Consumer<TrollContext> endListener;

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<BossBar> bars = new ArrayList<>();
    private final Deque<Runnable> cleanups = new ArrayDeque<>();
    private boolean ended;

    /** Konstruktor wewnętrzny - kontekst tworzy wyłącznie EVETrolle. */
    public TrollContext(Plugin plugin, Player target, CommandSender executor, Troll troll, TrollInfo info,
                        ConfigurationSection options, int maxEntities, boolean bossBarsEnabled,
                        Consumer<TrollContext> endListener) {
        this.plugin = plugin;
        this.targetId = target.getUniqueId();
        this.targetName = target.getName();
        this.executor = executor;
        this.troll = troll;
        this.info = info;
        this.options = options;
        this.maxEntities = maxEntities;
        this.bossBarsEnabled = bossBarsEnabled;
        this.endListener = endListener;
    }

    // ------------------------------------------------------------------ dane

    public Plugin getPlugin() { return plugin; }
    public UUID getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public CommandSender getExecutor() { return executor; }
    public Troll getTroll() { return troll; }
    public TrollInfo getInfo() { return info; }
    /** Opcje trolla z config.yml (sekcja options) - nigdy null. */
    public ConfigurationSection getOptions() { return options; }
    public long getDurationTicks() { return info.durationSeconds() * 20L; }
    public boolean isEnded() { return ended; }

    /** Gracz-cel lub null, jeśli jest offline. */
    public Player getTarget() {
        Player p = Bukkit.getPlayer(targetId);
        return p != null && p.isOnline() ? p : null;
    }

    public boolean isActive() { return !ended && getTarget() != null; }

    public Map<String, String> placeholders() {
        Map<String, String> m = new HashMap<>();
        m.put("player", targetName);
        m.put("target", targetName);
        m.put("admin", executor.getName());
        m.put("troll", Text.plain(Text.parse(info.name())));
        m.put("duration", String.valueOf(info.durationSeconds()));
        return m;
    }

    public Component text(String raw) {
        return Text.parse(raw, placeholders(), getTarget());
    }

    public Component text(String raw, Map<String, String> ph) {
        return Text.parse(raw, ph, getTarget());
    }

    // ------------------------------------------------------------------ zadania

    /** Jednorazowe zadanie; nie wykona się po zakończeniu trolla. */
    public void runLater(long delayTicks, Runnable task) {
        if (ended) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ended) return;
            try {
                task.run();
            } catch (Throwable t) {
                fail(t);
            }
        }, Math.max(1L, delayTicks));
    }

    /** Powtarzane zadanie; body zwraca false, aby je zatrzymać. Kończy trolla, gdy gracz wyjdzie. */
    public BukkitTask loop(long delayTicks, long periodTicks, IntPredicate body) {
        if (ended) return null;
        BukkitTask task = new BukkitRunnable() {
            private int i = 0;

            @Override
            public void run() {
                if (ended) {
                    cancel();
                    return;
                }
                if (getTarget() == null) {
                    cancel();
                    end();
                    return;
                }
                try {
                    if (!body.test(i++)) cancel();
                } catch (Throwable t) {
                    cancel();
                    fail(t);
                }
            }
        }.runTaskTimer(plugin, Math.max(0L, delayTicks), Math.max(1L, periodTicks));
        tasks.add(task);
        return task;
    }

    /** Rejestruje akcję sprzątającą (wykonywana odwrotnie do kolejności rejestracji). */
    public void onEnd(Runnable cleanup) {
        if (ended) {
            safe(cleanup);
        } else {
            cleanups.push(cleanup);
        }
    }

    // ------------------------------------------------------------------ encje

    public boolean canSpawnEntity() {
        if (entities.size() < maxEntities) return true;
        entities.removeIf(e -> !e.isValid());
        return entities.size() < maxEntities;
    }

    public void trackEntity(Entity entity) {
        if (entity == null) return;
        if (ended) {
            entity.remove();
        } else {
            entities.add(entity);
        }
    }

    /** Usuwa encję natychmiast i przestaje ją śledzić. */
    public void untrack(Entity entity) {
        if (entity == null) return;
        entities.remove(entity);
        entity.remove();
    }

    // ------------------------------------------------------------------ UI

    public void title(String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Player p = getTarget();
        if (p == null) return;
        p.showTitle(Title.title(text(title), text(subtitle), Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L), Duration.ofMillis(stayTicks * 50L), Duration.ofMillis(fadeOutTicks * 50L))));
    }

    public void actionBar(String raw) {
        Player p = getTarget();
        if (p != null) p.sendActionBar(text(raw));
    }

    public void message(String raw) {
        Player p = getTarget();
        if (p != null) p.sendMessage(text(raw));
    }

    public void addBossBar(BossBar bar) {
        Player p = getTarget();
        if (p == null || ended) return;
        bars.add(bar);
        p.showBossBar(bar);
    }

    /**
     * BossBar z odliczaniem do końca trolla. Respektuje settings.bossbar-enabled oraz options.bossbar.
     * Tekst z options.bossbar-text (placeholder {time} = pozostałe sekundy).
     */
    public BossBar countdownBar(String defaultText, BossBar.Color defaultColor, BossBar.Overlay overlay) {
        if (!bossBarsEnabled || ended || !options.getBoolean("bossbar", true)) return null;
        String raw = options.getString("bossbar-text", defaultText);
        if (raw == null || raw.isBlank()) return null;
        BossBar.Color color = defaultColor;
        String c = options.getString("bossbar-color");
        if (c != null) {
            try {
                color = BossBar.Color.valueOf(c.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // zły kolor w configu
            }
        }
        final long total = Math.max(20L, getDurationTicks());
        final long[] left = {total};
        Map<String, String> ph = placeholders();
        ph.put("time", String.valueOf(total / 20L));
        BossBar bar = BossBar.bossBar(text(raw, ph), 1f, color, overlay);
        addBossBar(bar);
        loop(20, 20, i -> {
            left[0] -= 20;
            if (left[0] <= 0) return false;
            bar.progress(Math.max(0f, Math.min(1f, left[0] / (float) total)));
            Map<String, String> m = placeholders();
            m.put("time", String.valueOf(left[0] / 20L));
            bar.name(text(raw, m));
            return true;
        });
        return bar;
    }

    // ------------------------------------------------------------------ koniec

    /** Kończy trolla i sprząta wszystko. Można wywoływać wielokrotnie. */
    public void end() {
        if (ended) return;
        ended = true;
        for (BukkitTask t : tasks) {
            try {
                t.cancel();
            } catch (Throwable ignored) {
                // już anulowane
            }
        }
        tasks.clear();
        while (!cleanups.isEmpty()) {
            safe(cleanups.pop());
        }
        Player p = getTarget();
        for (BossBar bar : bars) {
            if (p != null) p.hideBossBar(bar);
        }
        bars.clear();
        for (Entity e : entities) {
            try {
                e.remove();
            } catch (Throwable ignored) {
                // encja już usunięta
            }
        }
        entities.clear();
        try {
            endListener.accept(this);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Błąd przy kończeniu trolla " + troll.getId(), t);
        }
    }

    private void fail(Throwable t) {
        plugin.getLogger().log(Level.WARNING, "Błąd w trollu '" + troll.getId() + "' - przerywam.", t);
        end();
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Błąd podczas sprzątania trolla '" + troll.getId() + "'", t);
        }
    }
}
