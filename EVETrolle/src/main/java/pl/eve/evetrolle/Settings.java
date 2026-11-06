package pl.eve.evetrolle;

import org.bukkit.configuration.file.FileConfiguration;
import pl.eve.evetrolle.util.FX;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Zbuforowane ustawienia ogólne z config.yml (sekcja settings). */
public final class Settings {
    private final EVETrollePlugin plugin;

    private boolean logToConsole;
    private boolean notifyAdmins;
    private boolean allowSelfTroll;
    private boolean replaceActive;
    private int cooldownSeconds;
    private int maxActiveSessions;
    private int maxEntitiesPerSession;
    private boolean bossBarEnabled;
    private boolean stopOnWorldChange;
    private boolean stopOnDeath;
    private Set<String> disabledWorlds = new HashSet<>();

    public Settings(EVETrollePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration c = plugin.getConfig();
        logToConsole = c.getBoolean("settings.log-to-console", true);
        notifyAdmins = c.getBoolean("settings.notify-admins", true);
        allowSelfTroll = c.getBoolean("settings.allow-self-troll", true);
        replaceActive = c.getBoolean("settings.replace-active-troll", true);
        cooldownSeconds = Math.max(0, c.getInt("settings.cooldown-seconds", 5));
        maxActiveSessions = Math.max(1, c.getInt("settings.max-active-sessions", 25));
        maxEntitiesPerSession = Math.max(1, c.getInt("settings.max-entities-per-session", 40));
        bossBarEnabled = c.getBoolean("settings.bossbar-enabled", true);
        stopOnWorldChange = c.getBoolean("settings.stop-on-world-change", true);
        stopOnDeath = c.getBoolean("settings.stop-on-death", true);
        List<String> dw = c.getStringList("settings.disabled-worlds");
        disabledWorlds = new HashSet<>();
        for (String w : dw) disabledWorlds.add(w.toLowerCase());
        FX.setMultiplier(c.getDouble("settings.particle-multiplier", 1.0));
    }

    public boolean logToConsole() { return logToConsole; }
    public boolean notifyAdmins() { return notifyAdmins; }
    public boolean allowSelfTroll() { return allowSelfTroll; }
    public boolean replaceActive() { return replaceActive; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public int maxActiveSessions() { return maxActiveSessions; }
    public int maxEntitiesPerSession() { return maxEntitiesPerSession; }
    public boolean bossBarEnabled() { return bossBarEnabled; }
    public boolean stopOnWorldChange() { return stopOnWorldChange; }
    public boolean stopOnDeath() { return stopOnDeath; }
    public boolean isWorldDisabled(String world) { return disabledWorlds.contains(world.toLowerCase()); }
}
