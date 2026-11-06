package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.Atmosphere;
import pl.eve.evetrolle.util.FX;

import java.util.List;

/** Krwawy Księżyc: noc, czerwona poświata na ekranie, opady, "mgła" z particle. */
public final class BloodMoonTroll extends BaseTroll {
    private static final List<String> SOUNDS = List.of(
            "ambient.cave 0.9 0.7", "entity.wither.ambient 0.3 0.6", "entity.wolf.growl 0.6 0.5", "block.bell.use 0.4 0.5");

    public BloodMoonTroll() {
        super("blood-moon", "&4&lKrwawy Księżyc", Material.RED_DYE, 16, 15,
                "&7Niebo staje się czerwone,", "&7pojawia się mgła i particle.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        Atmosphere.time(ctx, o.getLong("time", 18000L));
        Atmosphere.weather(ctx, WeatherType.DOWNFALL);
        Atmosphere.vignette(ctx, true);
        ctx.title(o.getString("title", "&4&lKRWAWY KSIĘŻYC"), o.getString("subtitle", "&cNoc pełna krwi..."), 10, 60, 20);
        ctx.countdownBar("&4&lKrwawy Księżyc &7({time}s)", BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        FX.sound(p, o.getString("start-sound", "entity.wither.spawn 0.25 0.5"));
        List<String> sounds = o.getStringList("sounds");
        final List<String> pool = sounds.isEmpty() ? SOUNDS : sounds;
        final double density = o.getDouble("particle-density", 1.0);
        final int[] nextSound = {FX.rndInt(30, 60)};
        ctx.loop(5, 4, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            Location c = t.getLocation().add(0, 1.5, 0);
            FX.dust(t, c, (int) (14 * density), 7, 0x8A0000, 3.2f);
            FX.particle(t, Particle.ASH, c, (int) (30 * density), 8, 4, 8, 0.02);
            FX.particle(t, Particle.CLOUD, t.getLocation().add(0, 0.3, 0), (int) (6 * density), 8, 0.4, 8, 0.005);
            nextSound[0] -= 4;
            if (nextSound[0] <= 0) {
                nextSound[0] = FX.rndInt(60, 120);
                FX.sound(t, t.getLocation().add(FX.rnd(-10, 10), 0, FX.rnd(-10, 10)), pool.get(FX.rndInt(0, pool.size() - 1)));
            }
            return true;
        });
    }
}
