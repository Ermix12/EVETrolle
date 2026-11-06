package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Fałszywy ekran permanentnego bana (czat + title). Opcjonalnie prawdziwe rozłączenie (domyślnie wyłączone). */
public final class FakeBanTroll extends BaseTroll {
    private static final List<String> DEFAULT_LINES = List.of(
            "&8&m                                                ",
            "&c&lZOSTAŁEŚ ZBANOWANY PERMANENTNIE",
            " ",
            "&7Powód: &f{reason}",
            "&7Administrator: &f{admin}",
            "&7Data: &f{date}",
            "&7ID bana: &f#{ban-id}",
            " ",
            "&7Odwołanie: &bhttps://discord.gg/eve",
            "&8&m                                                ");

    public FakeBanTroll() {
        super("fake-ban", "&c&lFake Ban", Material.BARRIER, 10, 10,
                "&7Wyświetla fałszywy ekran", "&7permanentnego bana.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        Map<String, String> ph = ctx.placeholders();
        ph.put("reason", o.getString("reason", "Cheating (X-Ray)"));
        ph.put("ban-id", Integer.toHexString((int) (Math.random() * 0xFFFFFF)).toUpperCase());
        ph.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        List<String> lines = o.getStringList("lines");
        if (lines.isEmpty()) lines = DEFAULT_LINES;

        if (o.getBoolean("disconnect", false)) {
            Component msg = Component.empty();
            for (int i = 0; i < lines.size(); i++) {
                msg = msg.append(ctx.text(lines.get(i), ph));
                if (i < lines.size() - 1) msg = msg.append(Component.newline());
            }
            p.kick(msg);
            return;
        }

        FX.sound(p, o.getString("sound", "block.anvil.land 1 0.5"));
        for (int i = 0; i < o.getInt("clear-chat-lines", 25); i++) p.sendMessage(Component.empty());
        final List<String> out = lines;
        for (int i = 0; i < out.size(); i++) {
            final String line = out.get(i);
            ctx.runLater(1L + i * 2L, () -> ctx.message(Text.apply(line, ph, null)));
        }
        Component title = ctx.text(o.getString("title", "&4&lPERMANENTNY BAN"), ph);
        Component sub = ctx.text(o.getString("subtitle", "&cTwoje konto zostało zablokowane"), ph);
        p.showTitle(Title.title(title, sub, Title.Times.times(Duration.ofMillis(200),
                Duration.ofSeconds(o.getInt("title-stay", 6)), Duration.ofMillis(600))));
        ctx.countdownBar("&4&lKONTO ZABLOKOWANE &7({time}s)", BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        if (o.getBoolean("reveal", true)) {
            ctx.onEnd(() -> {
                Player q = ctx.getTarget();
                if (q != null) q.sendMessage(ctx.text(o.getString("reveal-message", "&a&lTo był tylko żart administracji! &7Nic Ci nie grozi.")));
            });
        }
    }
}
