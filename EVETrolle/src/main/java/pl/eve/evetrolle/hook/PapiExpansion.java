package pl.eve.evetrolle.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eve.evetrolle.EVETrollePlugin;

/**
 * Placeholdery: %evetrolle_active%, %evetrolle_active_troll%, %evetrolle_total%,
 * %evetrolle_used%, %evetrolle_count_&lt;id&gt;%, %evetrolle_trolls%.
 */
public final class PapiExpansion extends PlaceholderExpansion {
    private final EVETrollePlugin plugin;

    public PapiExpansion(EVETrollePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "evetrolle"; }
    @Override public @NotNull String getAuthor() { return "EVE"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String p = params.toLowerCase();
        switch (p) {
            case "total":
                return String.valueOf(plugin.getStats().getTotal());
            case "trolls":
                return String.valueOf(plugin.getTrollManager().getTrolls().size());
            case "used":
                return player == null ? "0" : String.valueOf(plugin.getStats().getUsedBy(player.getUniqueId()));
            case "active":
                return player instanceof Player online && plugin.getTrollManager().isTrolled(online) ? "true" : "false";
            case "active_troll":
                if (player instanceof Player online) {
                    return plugin.getTrollManager().getActiveTroll(online).orElse("none");
                }
                return "none";
            default:
                if (p.startsWith("count_")) return String.valueOf(plugin.getStats().getCount(p.substring(6)));
                return null;
        }
    }
}
