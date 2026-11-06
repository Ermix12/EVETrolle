package pl.eve.evetrolle;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import pl.eve.evetrolle.api.EVETrolleAPI;
import pl.eve.evetrolle.api.StartResult;
import pl.eve.evetrolle.command.EveTrolleCommand;
import pl.eve.evetrolle.gui.GuiManager;
import pl.eve.evetrolle.hook.LuckPermsHook;
import pl.eve.evetrolle.hook.PapiExpansion;
import pl.eve.evetrolle.listener.GuiListener;
import pl.eve.evetrolle.listener.PlayerListener;
import pl.eve.evetrolle.stats.StatsManager;
import pl.eve.evetrolle.troll.TrollManager;
import pl.eve.evetrolle.util.Text;

import java.io.File;
import java.util.Locale;

/** EVETrolle - wizualne trolle administracyjne (Paper 1.21.11, Java 21). */
public final class EVETrollePlugin extends JavaPlugin {
    private Settings settings;
    private Messages messages;
    private StatsManager stats;
    private TrollManager trollManager;
    private GuiManager guiManager;
    private LuckPermsHook luckPerms;
    private PapiExpansion papiExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);

        settings = new Settings(this);
        messages = new Messages(this);
        stats = new StatsManager(this);
        trollManager = new TrollManager(this);
        trollManager.registerBuiltins();
        trollManager.loadCustomTrolls();
        guiManager = new GuiManager(this);

        luckPerms = LuckPermsHook.tryCreate();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                papiExpansion = new PapiExpansion(this);
                papiExpansion.register();
                Text.setPapiEnabled(true);
            } catch (Throwable t) {
                getLogger().warning("Nie udało się zarejestrować rozszerzenia PlaceholderAPI: " + t.getMessage());
            }
        }

        EveTrolleCommand command = new EveTrolleCommand(this);
        PluginCommand pc = getCommand("evetrolle");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getServicesManager().register(EVETrolleAPI.class, trollManager, this, ServicePriority.Normal);

        // okresowy zapis statystyk (asynchronicznie, co 10 minut)
        getServer().getScheduler().runTaskTimer(this, stats::saveAsync, 20L * 600, 20L * 600);

        getLogger().info("EVETrolle " + getPluginMeta().getVersion() + " włączony. Trolli: " + trollManager.getTrolls().size()
                + " | LuckPerms: " + (luckPerms != null ? "tak" : "nie")
                + " | PlaceholderAPI: " + (papiExpansion != null ? "tak" : "nie"));
    }

    @Override
    public void onDisable() {
        if (trollManager != null) trollManager.shutdown();
        if (stats != null) stats.saveNow();
        getServer().getServicesManager().unregisterAll(this);
        if (papiExpansion != null) {
            try {
                papiExpansion.unregister();
            } catch (Throwable ignored) {
                // PlaceholderAPI już wyłączone
            }
        }
    }

    /** Przeładowuje config.yml i messages.yml; aktywne trolle kończą się naturalnie. */
    public void reloadAll() {
        reloadConfig();
        settings.reload();
        messages.reload();
        trollManager.reloadAll();
        guiManager.reload();
    }

    /** Wysyła wykonawcy komunikat odpowiadający wynikowi startTroll. */
    public void reportResult(CommandSender to, StartResult result, String trollId, Player target, String targetName) {
        String troll = trollManager.getInfo(trollId).map(i -> Text.strip(i.name())).orElse(trollId);
        if (result == StartResult.SUCCESS) {
            messages.send(to, "result.success", "troll", troll, "target", targetName);
            return;
        }
        String key = "errors." + result.name().toLowerCase(Locale.ROOT).replace('_', '-');
        messages.send(to, key, "troll", troll, "target", targetName,
                "seconds", String.valueOf(trollManager.getCooldownLeft(to, trollId)),
                "max", String.valueOf(settings.maxActiveSessions()));
    }

    public Settings getSettings() { return settings; }
    public Messages getMessages() { return messages; }
    public StatsManager getStats() { return stats; }
    public TrollManager getTrollManager() { return trollManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LuckPermsHook getLuckPerms() { return luckPerms; }
}
