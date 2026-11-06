package pl.eve.evetrolle.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Integracja z LuckPerms (prefix/grupa wykonawcy w powiadomieniach). Uprawnienia sprawdzane standardowo przez Bukkit. */
public final class LuckPermsHook {
    private final LuckPerms api;

    private LuckPermsHook(LuckPerms api) {
        this.api = api;
    }

    public static LuckPermsHook tryCreate() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return null;
        try {
            return new LuckPermsHook(LuckPermsProvider.get());
        } catch (Throwable t) {
            return null;
        }
    }

    private User user(Player p) {
        return api.getPlayerAdapter(Player.class).getUser(p);
    }

    public String prefix(CommandSender sender) {
        if (!(sender instanceof Player p)) return "";
        try {
            String prefix = user(p).getCachedData().getMetaData().getPrefix();
            return prefix == null ? "" : prefix;
        } catch (Throwable t) {
            return "";
        }
    }

    public String group(CommandSender sender) {
        if (!(sender instanceof Player p)) return "";
        try {
            return user(p).getPrimaryGroup();
        } catch (Throwable t) {
            return "";
        }
    }
}
