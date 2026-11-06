package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Figure;

import java.util.ArrayList;
import java.util.List;

/** Kilku Herobrine pojawia się wokół gracza. */
public final class HerobrineInvasionTroll extends BaseTroll {

    public HerobrineInvasionTroll() {
        super("herobrine-invasion", "&b&lInwazja Herobrine", Material.WITHER_SKELETON_SKULL, 24, 15,
                "&7Kilku Herobrine pojawia się", "&7wokół gracza.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        final int count = Math.max(1, o.getInt("count", 7));
        final double rMin = o.getDouble("radius-min", 5);
        final double rMax = Math.max(rMin, o.getDouble("radius-max", 9));
        final int spawnInterval = Math.max(1, o.getInt("spawn-interval-ticks", 10));
        final List<Figure> figures = new ArrayList<>();

        ctx.title(o.getString("title", "&b&lINWAZJA"), o.getString("subtitle", "&7Nie jesteś sam..."), 5, 40, 15);
        ctx.countdownBar("&b&lInwazja Herobrine &7({time}s)", BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
        ctx.onEnd(() -> {
            Player q = ctx.getTarget();
            for (Figure f : figures) {
                if (q != null && f.isValid()) FX.puff(q, f.getLocation());
            }
        });

        final double base = FX.rnd(0, Math.PI * 2);
        ctx.loop(5, spawnInterval, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            if (i < count) {
                double ang = base + i * (Math.PI * 2 / count) + Math.toRadians(FX.rnd(-15, 15));
                Location pl = t.getLocation();
                double dist = FX.rnd(rMin, rMax);
                Location l = FX.grounded(t, pl.getX() + Math.cos(ang) * dist, pl.getZ() + Math.sin(ang) * dist);
                if (l != null) {
                    l.setDirection(pl.toVector().subtract(l.toVector()));
                    Figure f = Figure.spawn(ctx, l, Figure.Style.HEROBRINE, o);
                    if (f != null) {
                        figures.add(f);
                        FX.sound(t, l, o.getString("appear-sound", "entity.enderman.stare 0.8 0.6"));
                    }
                }
            }
            for (Figure f : figures) {
                if (f.isValid()) f.face(t.getLocation());
            }
            return true;
        });
    }
}
