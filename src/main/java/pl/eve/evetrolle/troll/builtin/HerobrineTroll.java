package pl.eve.evetrolle.troll.builtin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Figure;

/** Herobrine pojawia się za graczem i znika po kilku sekundach (lub gdy gracz się obróci). */
public final class HerobrineTroll extends BaseTroll {

    public HerobrineTroll() {
        super("herobrine", "&b&lHerobrine", Material.PLAYER_HEAD, 12, 12,
                "&7Pojawia się za graczem", "&7i znika po kilku sekundach.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        double dist = o.getDouble("distance", 4.0);
        Vector back = FX.flatDir(p).multiply(-dist);
        Location base = FX.grounded(p, p.getLocation().getX() + back.getX(), p.getLocation().getZ() + back.getZ());
        if (base == null) {
            ctx.end();
            return;
        }
        base.setDirection(p.getLocation().toVector().subtract(base.toVector()));
        Figure fig = Figure.spawn(ctx, base, Figure.Style.HEROBRINE, o);
        if (fig == null) {
            ctx.end();
            return;
        }
        FX.sound(p, base, o.getString("appear-sound", "ambient.cave 1 0.7"));
        FX.sound(p, base, o.getString("step-sound", "block.stone.step 1 0.6"));
        long stay = Math.max(20L, o.getInt("stay-seconds", 5) * 20L);
        boolean vanishOnLook = o.getBoolean("vanish-on-look", true);
        double angle = o.getDouble("look-angle", 20);
        ctx.loop(2, 2, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            if (!fig.isValid()) {
                ctx.end();
                return false;
            }
            fig.face(t.getLocation());
            boolean timeUp = i * 2L >= stay;
            boolean looked = vanishOnLook && i > 8 && fig.isLookedAt(t, angle);
            if (timeUp || looked) {
                FX.puff(t, fig.getLocation());
                FX.sound(t, fig.getLocation(), o.getString("vanish-sound", "entity.enderman.teleport 0.8 0.5"));
                fig.remove();
                ctx.runLater(10, ctx::end);
                return false;
            }
            return true;
        });
    }
}
