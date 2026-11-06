package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.Atmosphere;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Fakes;

import java.util.List;

/** Świecące oczy w oddali i wycie wilka (oczy to wyświetlane bloki widoczne tylko dla gracza). */
public final class WolfTroll extends BaseTroll {
    private static final List<String> HOWLS = List.of("entity.wolf.howl 3 0.7", "entity.wolf.growl 1.5 0.5");

    private static final class Eyes {
        final BlockDisplay a;
        final BlockDisplay b;
        final Location where;

        Eyes(BlockDisplay a, BlockDisplay b, Location where) {
            this.a = a;
            this.b = b;
            this.where = where;
        }
    }

    public WolfTroll() {
        super("wolf", "&8&lWilk w Ciemności", Material.BONE, 19, 20,
                "&7W oddali pojawiają się świecące", "&7oczy i słychać wycie.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        if (o.getBoolean("set-night", true)) Atmosphere.time(ctx, o.getLong("time", 18000L));
        ctx.title(o.getString("title", "&8&lCoś cię obserwuje..."), o.getString("subtitle", ""), 10, 40, 20);
        ctx.countdownBar("&8&lWilk w ciemności &7({time}s)", BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        List<String> howls = o.getStringList("howl-sounds");
        final List<String> howlPool = howls.isEmpty() ? HOWLS : howls;
        final double minD = o.getDouble("min-distance", 22);
        final double maxD = o.getDouble("max-distance", 34);
        final Eyes[] cur = {null};
        final int[] wait = {10};
        final int[] blink = {FX.rndInt(30, 80)};

        Runnable howl = () -> {
            Player t = ctx.getTarget();
            if (t == null) return;
            Location at = cur[0] != null ? cur[0].where : t.getLocation().add(FX.rnd(-25, 25), 0, FX.rnd(-25, 25));
            for (String s : howlPool) FX.sound(t, at, s);
        };
        ctx.runLater(30, howl);
        ctx.runLater(Math.max(40L, ctx.getDurationTicks() / 2L), howl);
        ctx.runLater(Math.max(60L, ctx.getDurationTicks() - 80L), howl);

        ctx.loop(5, 5, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            if (cur[0] == null) {
                wait[0] -= 5;
                if (wait[0] <= 0) cur[0] = spawn(ctx, t, o, minD, maxD);
                if (cur[0] == null) wait[0] = Math.max(wait[0], 10);
                return true;
            }
            Eyes e = cur[0];
            double d = t.getLocation().distance(e.where);
            if (!e.a.isValid() || !e.b.isValid() || d < minD * 0.5 || t.getWorld() != e.where.getWorld()) {
                ctx.untrack(e.a);
                ctx.untrack(e.b);
                cur[0] = null;
                wait[0] = FX.rndInt(40, 90);
                return true;
            }
            blink[0] -= 5;
            if (blink[0] <= 0) {
                blink[0] = FX.rndInt(40, 100);
                t.hideEntity(ctx.getPlugin(), e.a);
                t.hideEntity(ctx.getPlugin(), e.b);
                ctx.runLater(3, () -> {
                    Player q = ctx.getTarget();
                    if (q != null && e.a.isValid() && e.b.isValid()) {
                        q.showEntity(ctx.getPlugin(), e.a);
                        q.showEntity(ctx.getPlugin(), e.b);
                    }
                });
            }
            return true;
        });
    }

    private Eyes spawn(TrollContext ctx, Player p, ConfigurationSection o, double minD, double maxD) {
        double ang = FX.rnd(0, Math.PI * 2);
        double dist = FX.rnd(minD, maxD);
        Location pl = p.getLocation();
        Location g = FX.grounded(p, pl.getX() + Math.cos(ang) * dist, pl.getZ() + Math.sin(ang) * dist);
        if (g == null) return null;
        Location c = g.clone().add(0, o.getDouble("eye-height", 1.0), 0);
        c.setDirection(pl.toVector().subtract(c.toVector()));
        double yawR = Math.toRadians(c.getYaw());
        Vector lat = new Vector(Math.cos(yawR), 0, Math.sin(yawR)).multiply(o.getDouble("eye-spread", 0.22));
        Material m = Material.matchMaterial(o.getString("eye-material", "YELLOW_CONCRETE"));
        if (m == null) m = Material.YELLOW_CONCRETE;
        float w = (float) o.getDouble("eye-width", 0.3);
        float h = (float) o.getDouble("eye-height-size", 0.13);
        BlockDisplay a = Fakes.blockDisplay(ctx, c.clone().add(lat), m, w, h, 0.04f, true);
        BlockDisplay b = Fakes.blockDisplay(ctx, c.clone().subtract(lat), m, w, h, 0.04f, true);
        if (a == null || b == null) {
            if (a != null) ctx.untrack(a);
            if (b != null) ctx.untrack(b);
            return null;
        }
        return new Eyes(a, b, c);
    }
}
