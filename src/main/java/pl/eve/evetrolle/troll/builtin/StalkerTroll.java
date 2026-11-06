package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Figure;

/** Czarna postać obserwuje gracza z różnych miejsc; znika, gdy gracz na nią spojrzy. */
public final class StalkerTroll extends BaseTroll {

    public StalkerTroll() {
        super("stalker", "&0&lStalker", Material.SPYGLASS, 20, 40,
                "&7Losowy NPC obserwuje gracza", "&7z różnych miejsc.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        final double minD = o.getDouble("min-distance", 16);
        final double maxD = o.getDouble("max-distance", 26);
        final double lookAngle = o.getDouble("look-angle", 14);
        final int lookTicks = Math.max(1, o.getInt("look-ticks", 3));
        final int reMin = o.getInt("reappear-min-ticks", 60);
        final int reMax = o.getInt("reappear-max-ticks", 140);
        final int lifetime = o.getInt("lifetime-ticks", 160);

        ctx.countdownBar("&8&lStalker &7({time}s)", BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        final Figure[] fig = {null};
        final int[] nextAt = {20};
        final int[] spawnedAt = {0};
        final int[] looked = {0};

        ctx.loop(5, 5, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            int now = i * 5;
            if (fig[0] == null) {
                if (now >= nextAt[0]) {
                    fig[0] = spawn(ctx, t, o, minD, maxD);
                    spawnedAt[0] = now;
                    looked[0] = 0;
                    if (fig[0] == null) nextAt[0] = now + 20;
                    else FX.sound(t, fig[0].getLocation(), o.getString("appear-sound", "block.grass.step 0.6 0.6"));
                }
                return true;
            }
            Figure f = fig[0];
            if (!f.isValid()) {
                fig[0] = null;
                nextAt[0] = now + FX.rndInt(reMin, reMax);
                return true;
            }
            f.face(t.getLocation());
            if (f.isLookedAt(t, lookAngle)) looked[0]++;
            else looked[0] = Math.max(0, looked[0] - 1);
            if (looked[0] >= lookTicks || now - spawnedAt[0] > lifetime) {
                FX.puff(t, f.getLocation());
                f.remove();
                fig[0] = null;
                nextAt[0] = now + FX.rndInt(reMin, reMax);
            }
            return true;
        });
    }

    private Figure spawn(TrollContext ctx, Player p, ConfigurationSection o, double minD, double maxD) {
        Location pl = p.getLocation();
        Vector dir = FX.flatDir(p);
        double view = Math.atan2(dir.getZ(), dir.getX());
        double ang = view + Math.toRadians(FX.rnd(60, 300));
        double dist = FX.rnd(minD, maxD);
        Location base = FX.grounded(p, pl.getX() + Math.cos(ang) * dist, pl.getZ() + Math.sin(ang) * dist);
        if (base == null || base.clone().subtract(0, 1, 0).getBlock().isLiquid()) return null;
        base.setDirection(pl.toVector().subtract(base.toVector()));
        return Figure.spawn(ctx, base, Figure.Style.STALKER, o);
    }
}
