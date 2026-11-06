package pl.eve.evetrolle.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;

/** Wizualny meteoryt: blok magmy + particle, bez eksplozji i bez zmian w świecie. */
public final class MeteorFx {
    private MeteorFx() {}

    /**
     * @param impact     punkt uderzenia
     * @param outDir     poziomy kierunek "od gracza" przez punkt uderzenia (meteor nadlatuje zza punktu uderzenia)
     * @param backDist   odległość startu od punktu uderzenia (poziomo)
     * @param height     wysokość startu nad punktem uderzenia
     * @param flight     czas lotu w tickach
     * @param size       rozmiar bloku
     */
    public static void launch(TrollContext ctx, Location impact, Vector outDir, double backDist, double height,
                              int flight, float size, String flightSound, String impactSound) {
        Player p = ctx.getTarget();
        if (p == null) return;
        Vector dir = outDir.clone().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(1, 0, 0);
        dir.normalize();
        Location start = impact.clone().add(dir.multiply(backDist)).add(0, height, 0);
        Vector delta = impact.toVector().subtract(start.toVector());
        BlockDisplay d = Fakes.blockDisplay(ctx, start, Material.MAGMA_BLOCK, size, size, size, true);
        if (d == null) return;
        ctx.runLater(2, () -> Fakes.animate(d, delta, flight, size, size, size));
        Vector back = delta.clone().normalize().multiply(-size * 0.7);
        ctx.loop(2, 1, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            if (i >= flight) {
                impact(ctx, t, impact, size, impactSound);
                ctx.untrack(d);
                return false;
            }
            double f = (i + 1) / (double) flight;
            Location pos = start.clone().add(delta.clone().multiply(f));
            Location tail = pos.clone().add(back);
            double s = size * 0.35;
            FX.particle(t, Particle.FLAME, tail, 12, s, s, s, 0.04);
            FX.particle(t, Particle.LARGE_SMOKE, tail, 8, s, s, s, 0.02);
            FX.particle(t, Particle.LAVA, pos, 2, s, s, s, 0);
            if (i % 4 == 0) FX.sound(t, pos, flightSound);
            return true;
        });
    }

    private static void impact(TrollContext ctx, Player t, Location at, float size, String sound) {
        Location c = at.clone().add(0, 1, 0);
        FX.particle(t, Particle.EXPLOSION_EMITTER, c, 2, size * 0.25, 0.5, size * 0.25, 0);
        FX.particle(t, Particle.FLAME, c, 60, size * 0.4, 0.8, size * 0.4, 0.25);
        FX.particle(t, Particle.LAVA, c, 40, size * 0.4, 0.5, size * 0.4, 0);
        FX.particle(t, Particle.LARGE_SMOKE, c, 50, size * 0.4, 1.0, size * 0.4, 0.1);
        FX.particle(t, Particle.FIREWORK, c, 30, size * 0.3, 0.6, size * 0.3, 0.15);
        double r = Math.max(3, size * 0.6);
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            FX.dust(t, at.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 3, 0.15, 0x553311, 2.2f);
        }
        FX.sound(t, at, sound);
    }
}
