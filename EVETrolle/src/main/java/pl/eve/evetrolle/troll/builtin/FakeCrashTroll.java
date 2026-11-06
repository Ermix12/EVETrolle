package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Realistyczny (fałszywy) raport crasha klienta wypisywany w czacie. */
public final class FakeCrashTroll extends BaseTroll {
    private static final List<String> DEFAULT_LINES = List.of(
            "&7---- Minecraft Crash Report ----",
            "&7// Don't do that.",
            " ",
            "&7Time: {date}",
            "&7Description: Ticking entity",
            " ",
            "&cjava.lang.NullPointerException: Cannot invoke \"net.minecraft.world.entity.Entity.getId()\" because \"entity\" is null",
            "&c\tat net.minecraft.client.multiplayer.ClientLevel.tickNonPassenger(ClientLevel.java:312)",
            "&c\tat net.minecraft.client.multiplayer.ClientLevel.tickEntities(ClientLevel.java:287)",
            "&c\tat net.minecraft.client.Minecraft.tick(Minecraft.java:1642)",
            "&c\tat net.minecraft.client.Minecraft.run(Minecraft.java:772)",
            " ",
            "&7-- Entity being ticked --",
            "&7Details:",
            "&7\tEntity Type: minecraft:player (net.minecraft.client.player.LocalPlayer)",
            "&7\tEntity Name: {player}",
            "&7\tEntity's Exact location: 128.50, 64.00, -312.50",
            " ",
            "&4Connection lost: &cInternal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host");

    public FakeCrashTroll() {
        super("fake-crash", "&e&lFake Crash", Material.COMMAND_BLOCK, 11, 10,
                "&7Wyświetla realistyczny komunikat", "&7crasha klienta.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        Map<String, String> ph = ctx.placeholders();
        ph.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        List<String> lines = o.getStringList("lines");
        if (lines.isEmpty()) lines = DEFAULT_LINES;

        if (o.getBoolean("disconnect", false)) {
            p.kick(ctx.text(o.getString("disconnect-message",
                    "&fInternal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host")));
            return;
        }

        FX.sound(p, o.getString("sound", "block.glass.break 1 0.6"));
        for (int i = 0; i < o.getInt("clear-chat-lines", 20); i++) p.sendMessage(Component.empty());
        int delay = Math.max(1, o.getInt("line-delay-ticks", 1));
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            ctx.runLater(1L + (long) i * delay, () -> ctx.message(Text.apply(line, ph, null)));
        }
        ctx.countdownBar("&c&lKRYTYCZNY BŁĄD KLIENTA &7({time}s)", BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        if (o.getBoolean("reveal", true)) {
            ctx.onEnd(() -> {
                Player q = ctx.getTarget();
                if (q != null) q.sendMessage(ctx.text(o.getString("reveal-message", "&a&lNic się nie stało - to tylko troll! &7Twoja gra działa normalnie.")));
            });
        }
    }
}
