package pl.eve.evetrolle.util;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Efekty cząsteczkowe i dźwiękowe wysyłane wyłącznie do jednego gracza. */
public final class FX {
    private static volatile double multiplier = 1.0;

    private FX() {}

    public static void setMultiplier(double m) {
        multiplier = Math.max(0.05, Math.min(4.0, m));
    }

    public static int scale(int count) {
        return Math.max(1, (int) Math.round(count * multiplier));
    }

    /** Pobiera Particle po nazwie (odporne na zmiany enum/rejestr). */
    public static Particle particle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            Object o = Particle.class.getField(name.trim().toUpperCase(Locale.ROOT)).get(null);
            if (o instanceof Particle p) return p;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // zła nazwa w configu
        }
        return fallback;
    }

    public static void particle(Player p, Particle type, Location loc, int count, double dx, double dy, double dz, double speed) {
        if (p == null || loc == null || loc.getWorld() == null) return;
        new ParticleBuilder(type).location(loc).count(scale(count)).offset(dx, dy, dz).extra(speed)
                .receivers(p).force(true).spawn();
    }

    public static void dust(Player p, Location loc, int count, double spread, int rgb, float size) {
        if (p == null || loc == null || loc.getWorld() == null) return;
        new ParticleBuilder(Particle.DUST).location(loc).count(scale(count)).offset(spread, spread, spread)
                .data(new Particle.DustOptions(Color.fromRGB(rgb & 0xFFFFFF), size))
                .receivers(p).force(true).spawn();
    }

    /** Dźwięk w formacie "klucz głośność wysokość", np. "ambient.cave 1 0.8". */
    public static void sound(Player p, Location at, String spec) {
        if (p == null || spec == null || spec.isBlank()) return;
        String[] a = spec.trim().split("\\s+");
        float vol = a.length > 1 ? parseFloat(a[1], 1f) : 1f;
        float pitch = a.length > 2 ? parseFloat(a[2], 1f) : 1f;
        p.playSound(at == null ? p.getLocation() : at, a[0], SoundCategory.MASTER, vol, pitch);
    }

    public static void sound(Player p, String spec) {
        sound(p, p == null ? null : p.getLocation(), spec);
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double rnd(double min, double max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int rndInt(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Pozycja w danej odległości i pod danym kątem (radiany) od gracza, na jego wysokości. */
    public static Location around(Location center, double angle, double dist) {
        return center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
    }

    /** Poziomy kierunek, w którym patrzy gracz. */
    public static Vector flatDir(Player p) {
        Vector d = p.getLocation().getDirection().setY(0);
        if (d.lengthSquared() < 1.0E-4) d = new Vector(0, 0, 1);
        return d.normalize();
    }

    /**
     * Szuka powierzchni (blok stały + wolne miejsce nad nim) w kolumnie x,z w pobliżu wysokości fromY.
     * Zwraca null, jeśli chunk nie jest załadowany (nigdy nie ładujemy chunków).
     */
    public static Double groundY(World w, double x, double z, double fromY, int up, int down) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!w.isChunkLoaded(bx >> 4, bz >> 4)) return null;
        int top = Math.min(w.getMaxHeight() - 2, (int) fromY + up);
        int bottom = Math.max(w.getMinHeight(), (int) fromY - down);
        for (int y = top; y >= bottom; y--) {
            Block b = w.getBlockAt(bx, y, bz);
            if (!b.isPassable() && w.getBlockAt(bx, y + 1, bz).isPassable()) {
                return y + 1.0;
            }
        }
        return fromY;
    }

    /** Lokalizacja na powierzchni w pobliżu gracza lub null (chunk niezaładowany). */
    public static Location grounded(Player p, double x, double z) {
        World w = p.getWorld();
        Double gy = groundY(w, x, z, p.getLocation().getY(), 3, 12);
        return gy == null ? null : new Location(w, x, gy, z);
    }

    /** Mały obłok dymu - efekt "zniknięcia". */
    public static void puff(Player p, Location loc) {
        particle(p, Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 25, 0.3, 0.8, 0.3, 0.02);
        particle(p, Particle.SMOKE, loc.clone().add(0, 1, 0), 20, 0.4, 0.9, 0.4, 0.03);
    }
}
