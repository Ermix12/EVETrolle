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
import pl.eve.evetrolle.util.MeteorFx;

/** Gigantyczny meteoryt lecący w stronę gracza (uderza w pewnej odległości, bez żadnych zniszczeń). */
public final class MeteorTroll extends BaseTroll {

    public MeteorTroll() {
        super("meteor", "&6&lMeteoryt", Material.MAGMA_BLOCK, 14, 10,
                "&7Gigantyczny meteoryt", "&7lecący w stronę gracza.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        double impactDist = o.getDouble("impact-distance", 24);
        Vector dir = FX.flatDir(p);
        double angle = Math.toRadians(FX.rnd(-25, 25));
        Vector out = dir.clone().rotateAroundY(angle);
        Location impact = FX.grounded(p, p.getLocation().getX() + out.getX() * impactDist,
                p.getLocation().getZ() + out.getZ() * impactDist);
        if (impact == null) impact = p.getLocation().clone().add(out.clone().multiply(impactDist));

        ctx.title(o.getString("title", "&6&lMETEORYT"), o.getString("subtitle", "&7Nadlatuje..."), 5, 30, 10);
        ctx.countdownBar("&6&lMeteoryt &7({time}s)", BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        FX.sound(p, o.getString("start-sound", "entity.ghast.shoot 1 0.5"));
        MeteorFx.launch(ctx, impact, out, o.getDouble("start-distance", 32), o.getDouble("start-height", 42),
                Math.max(10, o.getInt("flight-ticks", 70)), (float) o.getDouble("size", 9),
                o.getString("flight-sound", "item.firecharge.use 3 0.5"),
                o.getString("impact-sound", "entity.generic.explode 5 0.6"));
    }
}
