package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Fakes;

/** Deszcz TNT (tylko wizualny: BlockDisplay widoczny dla gracza, brak eksplozji i zmian w świecie). */
public final class TntRainTroll extends BaseTroll {

    public TntRainTroll() {
        super("tnt-rain", "&c&lFake TNT Rain", Material.TNT, 13, 12,
                "&7Deszcz TNT spadający z nieba.", "&7Bez eksplozji.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        final double radius = o.getDouble("radius", 14);
        final double height = o.getDouble("height", 28);
        final int perWave = Math.max(1, o.getInt("per-wave", 3));
        final int interval = Math.max(1, o.getInt("wave-interval-ticks", 3));
        final double speed = Math.max(0.3, o.getDouble("fall-speed", 1.0));
        final int maxDrops = Math.max(1, o.getInt("max-drops", 60));
        final String landSound = o.getString("land-sound", "block.stone.break 0.4 0.8");

        FX.sound(p, o.getString("start-sound", "entity.tnt.primed 1 0.8"));
        ctx.title(o.getString("title", "&c&lDESZCZ TNT"), o.getString("subtitle", "&7Uciekaj... a może nie?"), 5, 40, 10);
        ctx.countdownBar("&c&lDeszcz TNT &7({time}s)", BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        final int[] alive = {0};
        final long stopSpawning = ctx.getDurationTicks() - 50L;
        ctx.loop(10, interval, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            if (10L + (long) i * interval > stopSpawning) return false;
            for (int k = 0; k < perWave && alive[0] < maxDrops; k++) {
                drop(ctx, t, radius, height, speed, landSound, alive);
            }
            if (i % 6 == 0) FX.sound(t, t.getLocation(), "entity.tnt.primed 0.4 1.4");
            return true;
        });
    }

    private void drop(TrollContext ctx, Player t, double radius, double height, double speed, String landSound, int[] alive) {
        double ang = FX.rnd(0, Math.PI * 2);
        double r = Math.sqrt(FX.rnd(0, 1)) * radius;
        Location tl = t.getLocation();
        double x = tl.getX() + Math.cos(ang) * r;
        double z = tl.getZ() + Math.sin(ang) * r;
        Location land = FX.grounded(t, x, z);
        if (land == null) return;
        double startY = tl.getY() + height;
        double dist = startY - land.getY();
        if (dist < 3) return;
        int ticks = Math.max(5, (int) Math.ceil(dist / speed));
        BlockDisplay d = Fakes.blockDisplay(ctx, new Location(land.getWorld(), x, startY, z), Material.TNT, 1f, 1f, 1f, false);
        if (d == null) return;
        alive[0]++;
        ctx.runLater(2, () -> Fakes.animate(d, new Vector(0, -dist, 0), ticks, 1f, 1f, 1f));
        ctx.runLater(2L + ticks, () -> {
            alive[0]--;
            Player q = ctx.getTarget();
            if (q != null) {
                FX.particle(q, Particle.CLOUD, land.clone().add(0, 0.4, 0), 6, 0.3, 0.2, 0.3, 0.02);
                FX.sound(q, land, landSound);
            }
            ctx.untrack(d);
        });
    }
}
