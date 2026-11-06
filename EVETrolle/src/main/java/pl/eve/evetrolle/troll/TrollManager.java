package pl.eve.evetrolle.troll;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.EVETrollePlugin;
import pl.eve.evetrolle.Messages;
import pl.eve.evetrolle.Settings;
import pl.eve.evetrolle.api.EVETrolleAPI;
import pl.eve.evetrolle.api.StartResult;
import pl.eve.evetrolle.api.Troll;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.api.TrollInfo;
import pl.eve.evetrolle.api.event.TrollEndEvent;
import pl.eve.evetrolle.api.event.TrollStartEvent;
import pl.eve.evetrolle.troll.builtin.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Rejestr trolli, sesje, cooldowny, uprawnienia, logowanie i powiadomienia. */
public final class TrollManager implements EVETrolleAPI {

    /** Zarejestrowany troll wraz z rozwiązanymi ustawieniami z configu. */
    public record Entry(Troll troll, TrollInfo info, boolean enabled, ConfigurationSection options, boolean custom) {}

    private final EVETrollePlugin plugin;
    private final Map<String, Entry> trolls = new LinkedHashMap<>();
    private final Map<UUID, TrollContext> active = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public TrollManager(EVETrollePlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ rejestr

    public void registerBuiltins() {
        List<Troll> list = List.of(
                new FakeBanTroll(), new FakeCrashTroll(), new HerobrineTroll(), new TntRainTroll(),
                new MeteorTroll(), new AbyssTroll(), new BloodMoonTroll(), new WolfTroll(),
                new StalkerTroll(), new ScreenTakeoverTroll(), new ScarySoundsTroll(), new StreamerModeTroll(),
                new HerobrineInvasionTroll(), new EndOfWorldTroll());
        for (Troll t : list) register(t, false);
    }

    public void loadCustomTrolls() {
        trolls.values().removeIf(Entry::custom);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("custom-trolls");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            try {
                register(new ConfigTroll(key.toLowerCase(Locale.ROOT), s), true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Niepoprawny własny troll '" + key + "' w config.yml", e);
            }
        }
    }

    private void register(Troll troll, boolean custom) {
        String id = troll.getId().toLowerCase(Locale.ROOT);
        trolls.put(id, resolve(troll, custom));
    }

    private Entry resolve(Troll troll, boolean custom) {
        String path = (custom ? "custom-trolls." : "trolls.") + troll.getId().toLowerCase(Locale.ROOT);
        ConfigurationSection s = plugin.getConfig().getConfigurationSection(path);
        TrollInfo def = troll.getDefaultInfo();
        if (s == null) return new Entry(troll, def, true, new MemoryConfiguration(), custom);
        Material mat = Material.matchMaterial(s.getString("material", def.material().name()));
        TrollInfo info = new TrollInfo(
                s.getString("name", def.name()),
                mat == null ? def.material() : mat,
                s.isList("lore") ? s.getStringList("lore") : def.lore(),
                s.getInt("slot", def.slot()),
                s.getInt("duration", def.durationSeconds()),
                s.getString("permission", def.permission()));
        ConfigurationSection opt = s.getConfigurationSection("options");
        return new Entry(troll, info, s.getBoolean("enabled", true), opt == null ? new MemoryConfiguration() : opt, custom);
    }

    public void reloadAll() {
        loadCustomTrolls();
        trolls.replaceAll((id, e) -> e.custom() ? e : resolve(e.troll(), false));
    }

    @Override
    public void registerTroll(Troll troll) {
        register(troll, false);
    }

    @Override
    public boolean unregisterTroll(String id) {
        return trolls.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    @Override public Collection<Troll> getTrolls() { return trolls.values().stream().map(Entry::troll).toList(); }
    public Collection<Entry> getEntries() { return trolls.values(); }
    public Entry getEntry(String id) { return id == null ? null : trolls.get(id.toLowerCase(Locale.ROOT)); }

    @Override
    public Optional<Troll> getTroll(String id) {
        Entry e = getEntry(id);
        return e == null ? Optional.empty() : Optional.of(e.troll());
    }

    @Override
    public Optional<TrollInfo> getInfo(String id) {
        Entry e = getEntry(id);
        return e == null ? Optional.empty() : Optional.of(e.info());
    }

    // ------------------------------------------------------------------ uprawnienia

    public boolean canUse(CommandSender sender, Entry e) {
        if (sender.hasPermission("evetrolle.admin")) return true;
        if (!sender.hasPermission("evetrolle.troll")) return false;
        return e.info().permission().isBlank() || sender.hasPermission(e.info().permission());
    }

    // ------------------------------------------------------------------ cooldown

    private String cdKey(CommandSender s, String id) {
        return (s instanceof Player p ? p.getUniqueId().toString() : "console") + ":" + id;
    }

    @Override
    public long getCooldownLeft(CommandSender executor, String trollId) {
        if (executor.hasPermission("evetrolle.admin")) return 0;
        Long until = cooldowns.get(cdKey(executor, trollId.toLowerCase(Locale.ROOT)));
        if (until == null) return 0;
        long left = (until - System.currentTimeMillis() + 999) / 1000;
        return Math.max(0, left);
    }

    // ------------------------------------------------------------------ start / stop

    @Override
    public StartResult startTroll(CommandSender executor, Player target, String trollId) {
        Settings st = plugin.getSettings();
        Entry entry = getEntry(trollId);
        if (entry == null) return StartResult.UNKNOWN_TROLL;
        if (!entry.enabled()) return StartResult.DISABLED;
        if (!canUse(executor, entry)) return StartResult.NO_PERMISSION;
        if (target == null || !target.isOnline()) return StartResult.TARGET_OFFLINE;
        if (executor instanceof Player p && p.getUniqueId().equals(target.getUniqueId()) && !st.allowSelfTroll()) {
            return StartResult.SELF_NOT_ALLOWED;
        }
        if (target.hasPermission("evetrolle.bypass")) return StartResult.TARGET_EXEMPT;
        if (st.isWorldDisabled(target.getWorld().getName())) return StartResult.WORLD_DISABLED;
        String id = entry.troll().getId().toLowerCase(Locale.ROOT);
        if (getCooldownLeft(executor, id) > 0) return StartResult.COOLDOWN;

        TrollContext existing = active.get(target.getUniqueId());
        if (existing != null) {
            if (!st.replaceActive()) return StartResult.ALREADY_ACTIVE;
            existing.end();
        }
        if (active.size() >= st.maxActiveSessions()) return StartResult.LIMIT_REACHED;

        TrollStartEvent event = new TrollStartEvent(executor, target, id);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return StartResult.CANCELLED;

        TrollContext ctx = new TrollContext(plugin, target, executor, entry.troll(), entry.info(), entry.options(),
                st.maxEntitiesPerSession(), st.bossBarEnabled(), this::onEnd);
        active.put(target.getUniqueId(), ctx);
        try {
            entry.troll().start(ctx);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Troll '" + id + "' zgłosił błąd przy starcie.", t);
            ctx.end();
            return StartResult.CANCELLED;
        }
        if (!ctx.isEnded()) {
            ctx.runLater(Math.max(1L, ctx.getDurationTicks()), ctx::end);
        }

        if (st.cooldownSeconds() > 0 && !executor.hasPermission("evetrolle.admin")) {
            if (cooldowns.size() > 2000) {
                long now = System.currentTimeMillis();
                cooldowns.values().removeIf(v -> v < now);
            }
            cooldowns.put(cdKey(executor, id), System.currentTimeMillis() + st.cooldownSeconds() * 1000L);
        }
        plugin.getStats().record(id, executor);
        announce(executor, target, entry);
        return StartResult.SUCCESS;
    }

    private void announce(CommandSender executor, Player target, Entry entry) {
        Messages m = plugin.getMessages();
        String trollName = pl.eve.evetrolle.util.Text.strip(entry.info().name());
        String prefix = plugin.getLuckPerms() == null ? "" : plugin.getLuckPerms().prefix(executor);
        String group = plugin.getLuckPerms() == null ? "" : plugin.getLuckPerms().group(executor);
        String[] kv = {"admin", executor.getName(), "target", target.getName(), "troll", trollName,
                "world", target.getWorld().getName(), "admin_prefix", prefix, "admin_group", group};
        if (plugin.getSettings().logToConsole()) {
            plugin.getLogger().info(m.plain("log.troll-used", kv));
        }
        if (plugin.getSettings().notifyAdmins()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("evetrolle.notify") && !p.equals(executor)) {
                    m.send(p, "notify.troll-used", kv);
                }
            }
        }
    }

    private void onEnd(TrollContext ctx) {
        active.remove(ctx.getTargetId(), ctx);
        Bukkit.getPluginManager().callEvent(new TrollEndEvent(ctx.getTargetId(), ctx.getTroll().getId()));
    }

    @Override
    public boolean stopTroll(Player target) {
        TrollContext ctx = active.get(target.getUniqueId());
        if (ctx == null) return false;
        ctx.end();
        return true;
    }

    @Override
    public void stopAll() {
        for (TrollContext ctx : new ArrayList<>(active.values())) ctx.end();
        active.clear();
    }

    public void shutdown() {
        stopAll();
    }

    @Override public boolean isTrolled(Player player) { return active.containsKey(player.getUniqueId()); }

    @Override
    public Optional<String> getActiveTroll(Player player) {
        TrollContext c = active.get(player.getUniqueId());
        return c == null ? Optional.empty() : Optional.of(c.getTroll().getId());
    }
}
