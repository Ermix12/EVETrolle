package pl.eve.evetrolle.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eve.evetrolle.EVETrollePlugin;
import pl.eve.evetrolle.Messages;
import pl.eve.evetrolle.api.StartResult;
import pl.eve.evetrolle.troll.TrollManager;
import pl.eve.evetrolle.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** /evetrolle [reload | troll &lt;gracz&gt; [id] | stop &lt;gracz&gt; | list | help] */
public final class EveTrolleCommand implements CommandExecutor, TabCompleter {
    private final EVETrollePlugin plugin;

    public EveTrolleCommand(EVETrollePlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() { return plugin.getMessages(); }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("evetrolle.use")) {
            msg().send(sender, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                msg().send(sender, "errors.player-only");
                return true;
            }
            if (!p.hasPermission("evetrolle.troll")) {
                msg().send(sender, "errors.no-permission");
                return true;
            }
            plugin.getGuiManager().openPlayerMenu(p, 0);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "troll" -> troll(sender, args);
            case "stop" -> stop(sender, args);
            case "list" -> list(sender);
            default -> msg().sendList(sender, "help");
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("evetrolle.reload")) {
            msg().send(sender, "errors.no-permission");
            return;
        }
        try {
            plugin.reloadAll();
            msg().send(sender, "reload.success");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Błąd podczas przeładowania", e);
            msg().send(sender, "reload.failed", "error", String.valueOf(e.getMessage()));
        }
    }

    private void troll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evetrolle.troll")) {
            msg().send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            msg().send(sender, "usage.troll");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || (sender instanceof Player s && !s.canSee(target))) {
            msg().send(sender, "errors.player-not-found", "target", args[1]);
            return;
        }
        if (args.length >= 3) {
            String id = args[2].toLowerCase(Locale.ROOT);
            StartResult r = plugin.getTrollManager().startTroll(sender, target, id);
            plugin.reportResult(sender, r, id, target, target.getName());
            return;
        }
        if (!(sender instanceof Player p)) {
            msg().send(sender, "usage.troll-console");
            return;
        }
        plugin.getGuiManager().openTrollMenu(p, target);
    }

    private void stop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("evetrolle.troll")) {
            msg().send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            msg().send(sender, "usage.stop");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg().send(sender, "errors.player-not-found", "target", args[1]);
            return;
        }
        if (plugin.getTrollManager().stopTroll(target)) {
            msg().send(sender, "stop.success", "target", target.getName());
        } else {
            msg().send(sender, "stop.none", "target", target.getName());
        }
    }

    private void list(CommandSender sender) {
        msg().send(sender, "list.header");
        for (TrollManager.Entry e : plugin.getTrollManager().getEntries()) {
            if (!e.enabled()) continue;
            msg().send(sender, "list.entry", "id", e.troll().getId(), "troll", Text.strip(e.info().name()),
                    "duration", String.valueOf(e.info().durationSeconds()));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("evetrolle.use")) return out;
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "list"));
            if (sender.hasPermission("evetrolle.troll")) subs.addAll(List.of("troll", "stop"));
            if (sender.hasPermission("evetrolle.reload")) subs.add("reload");
            for (String s : subs) if (s.startsWith(last)) out.add(s);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("troll") || args[0].equalsIgnoreCase("stop"))
                && sender.hasPermission("evetrolle.troll")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if ((!(sender instanceof Player s) || s.canSee(p)) && p.getName().toLowerCase(Locale.ROOT).startsWith(last)) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("troll") && sender.hasPermission("evetrolle.troll")) {
            for (TrollManager.Entry e : plugin.getTrollManager().getEntries()) {
                if (e.enabled() && e.troll().getId().startsWith(last)) out.add(e.troll().getId());
            }
        }
        return out;
    }
}
