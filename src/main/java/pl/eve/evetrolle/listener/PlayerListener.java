package pl.eve.evetrolle.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.eve.evetrolle.EVETrollePlugin;

/** Sprzątanie trolli przy wyjściu, zmianie świata i śmierci gracza. */
public final class PlayerListener implements Listener {
    private final EVETrollePlugin plugin;

    public PlayerListener(EVETrollePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getTrollManager().stopTroll(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (plugin.getSettings().stopOnWorldChange()) plugin.getTrollManager().stopTroll(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (plugin.getSettings().stopOnDeath()) plugin.getTrollManager().stopTroll(e.getEntity());
    }
}
