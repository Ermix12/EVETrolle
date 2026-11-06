package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.Atmosphere;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.MeteorFx;

/** Koniec Świata: meteoryty, błyskawice (wizualne) i ogromna ilość particle. Bez żadnych zniszczeń. */
public final class EndOfWorldTroll extends BaseTroll {

    public EndOfWorldTroll() {
        super("end-of-world", "&4&lKoniec Świata", Material.DRAGON_EGG, 25, 25,
                "&7Spadają meteoryty, błyskawice", "&7i pojawia się masa particle.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        Atmosphere.time(ctx, o.getLong("time", 16000L));
        Atmosphere.weather(ctx, WeatherType.DOWNFALL);
        Atmosphere.vignette(ctx, true);
        final double density = o.getDouble("particle-density", 1.0);
        final int mMin = o.getInt("meteor-interval-min-ticks", 18);
        final int mMax = Math.max(mMin, o.getInt("meteor-interval-max-ticks", 40));
        final int lMin = o.getInt("lightning-interval-min-ticks", 25);
        final int lMax = Math.max(lMin, o.getInt("lightning-interval-max-ticks", 55));
        final float meteorSize = (float) o.getDouble("meteor-size", 5);
        final int flight = Math.max(15, o.getInt("meteor-flight-ticks", 45));

        ctx.title(o.getString("title", "&4&lKONIEC ŚWIATA"), o.getString("subtitle", "&cTo już koniec..."), 10, 70, 20);
        ctx.countdownBar("&4&lKoniec Świata &7({time}s)", BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);
        FX.sound(p, o.getString("start-sound", "entity.wither.spawn 0.35 0.5"));
        FX.sound(p, o.getString("start-sound-2", "entity.ender_dragon.growl 0.8 0.6"));

        final int[] nextMeteor = {20};
        final int[] nextBolt = {40};
        ctx.loop(5, 4, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            Location c = t.getLocation().add(0, 2, 0);
            FX.particle(t, Particle.ASH, c, (int) (40 * density), 10, 5, 10, 0.03);
            FX.particle(t, Particle.FLAME, c, (int) (14 * density), 12, 6, 12, 0.02);
            FX.particle(t, Particle.LAVA, c, (int) (4 * density), 10, 4, 10, 0);
            FX.particle(t, Particle.LARGE_SMOKE, c, (int) (10 * density), 12, 5, 12, 0.02);

            nextMeteor[0] -= 4;
            if (nextMeteor[0] <= 0) {
                nextMeteor[0] = FX.rndInt(mMin, mMax);
                double ang = FX.rnd(0, Math.PI * 2);
                double dist = FX.rnd(14, 40);
                Vector out = new Vector(Math.cos(ang), 0, Math.sin(ang));
                Location impact = FX.grounded(t, t.getLocation().getX() + out.getX() * dist, t.getLocation().getZ() + out.getZ() * dist);
                if (impact != null) {
                    MeteorFx.launch(ctx, impact, out, 28, 36, flight, meteorSize,
                            o.getString("flight-sound", "item.firecharge.use 2 0.5"),
                            o.getString("impact-sound", "entity.generic.explode 4 0.7"));
                }
            }
            nextBolt[0] -= 4;
            if (nextBolt[0] <= 0) {
                nextBolt[0] = FX.rndInt(lMin, lMax);
                lightning(t, o);
            }
            return true;
        });
    }

    /** Wizualna błyskawica (linia particle + grzmot) - bez uderzenia w świat. */
    private static void lightning(Player t, ConfigurationSection o) {
        double ang = FX.rnd(0, Math.PI * 2);
        double dist = FX.rnd(12, 35);
        Location base = FX.grounded(t, t.getLocation().getX() + Math.cos(ang) * dist, t.getLocation().getZ() + Math.sin(ang) * dist);
        if (base == null) return;
        double x = base.getX();
        double z = base.getZ();
        for (double y = base.getY() + 45; y >= base.getY(); y -= 1.5) {
            x += FX.rnd(-0.6, 0.6);
            z += FX.rnd(-0.6, 0.6);
            Location l = new Location(base.getWorld(), x, y, z);
            FX.particle(t, Particle.ELECTRIC_SPARK, l, 3, 0.1, 0.3, 0.1, 0.02);
            FX.particle(t, Particle.END_ROD, l, 1, 0.05, 0.2, 0.05, 0);
        }
        FX.sound(t, base, o.getString("thunder-sound", "entity.lightning_bolt.thunder 8 1"));
        FX.sound(t, base, o.getString("bolt-impact-sound", "entity.lightning_bolt.impact 4 1"));
    }
}
