package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;

import java.util.List;

/** Title oraz ActionBary: "JA CIĘ WIDZĘ", "NIE OGLĄDAJ SIĘ", "JESTEM ZA TOBĄ". */
public final class ScreenTakeoverTroll extends BaseTroll {
    private static final List<String> MESSAGES = List.of("JA CIĘ WIDZĘ", "NIE OGLĄDAJ SIĘ", "JESTEM ZA TOBĄ");

    public ScreenTakeoverTroll() {
        super("screen-takeover", "&4&lPrzejęcie Ekranu", Material.ENDER_EYE, 21, 12,
                "&7Title i ActionBary:", "&c\"JA CIĘ WIDZĘ\"  \"NIE OGLĄDAJ SIĘ\"", "&c\"JESTEM ZA TOBĄ\"");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        List<String> msgs = o.getStringList("messages");
        final List<String> pool = msgs.isEmpty() ? MESSAGES : msgs;
        final int interval = Math.max(10, o.getInt("interval-ticks", 36));
        final String titleFmt = o.getString("title-format", "&4&l{msg}");
        final String barFmt = o.getString("actionbar-format", "&c&l{msg}");
        final List<String> sounds = o.getStringList("sounds").isEmpty()
                ? List.of("entity.warden.heartbeat 1 1", "entity.warden.nearby_close 0.8 1", "ambient.cave 1 0.8")
                : o.getStringList("sounds");

        ctx.countdownBar("&4&l{time}s", BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        ctx.loop(0, interval, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            String msg = pool.get(i % pool.size());
            ctx.title(titleFmt.replace("{msg}", msg), o.getString("subtitle", ""), 2, Math.max(4, interval - 8), 4);
            FX.sound(t, sounds.get(i % sounds.size()));
            return true;
        });
        ctx.loop(5, 10, i -> {
            if (ctx.getTarget() == null) return false;
            ctx.actionBar(barFmt.replace("{msg}", pool.get((i + 1) % pool.size())));
            return true;
        });
    }
}
