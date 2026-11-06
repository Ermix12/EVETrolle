package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;

import java.util.List;

/** Fałszywe wiadomości administracji pojawiające się na ekranie (czat + ActionBar). */
public final class StreamerModeTroll extends BaseTroll {
    private static final List<String> MESSAGES = List.of(
            "Wykryto podejrzaną aktywność na Twoim koncie.",
            "Trwa weryfikacja Twojego klienta przez AntiCheat.",
            "Nie wylogowuj się - sprawa jest w toku.",
            "Zrób zrzut ekranu (F3) i wyślij go na kanał #support.",
            "Czy to na pewno Ty grasz na tym koncie?",
            "Jesteś na żywo - 12 438 widzów obserwuje ten stream.");

    public StreamerModeTroll() {
        super("streamer-mode", "&d&lFake Streamer Mode", Material.GLOW_ITEM_FRAME, 23, 20,
                "&7Na ekranie pojawiają się", "&7fałszywe wiadomości administracji.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        List<String> msgs = o.getStringList("messages");
        final List<String> pool = msgs.isEmpty() ? MESSAGES : msgs;
        final String format = o.getString("format", "&8[&4Administracja&8] &c{msg}");
        final int interval = Math.max(20, o.getInt("interval-ticks", 60));
        final String sound = o.getString("sound", "block.note_block.pling 1 1.5");

        ctx.title(o.getString("title", "&c&l● LIVE"), o.getString("subtitle", "&7Wiadomości od administracji"), 5, 40, 10);
        ctx.countdownBar("&4&lTRYB STREAMERA &7({time}s)", BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
        ctx.loop(30, interval, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            String msg = pool.get(i % pool.size());
            ctx.message(format.replace("{msg}", msg));
            ctx.actionBar("&c&l" + msg);
            FX.sound(t, sound);
            return true;
        });
    }
}
