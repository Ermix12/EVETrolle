package pl.eve.evetrolle.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.eve.evetrolle.api.TrollContext;

import java.util.function.Consumer;

/**
 * Encje widoczne WYŁĄCZNIE dla gracza-celu (setVisibleByDefault(false) + Player#showEntity).
 * Nie są zapisywane (persistent=false) i są usuwane przy zakończeniu trolla.
 */
public final class Fakes {
    private Fakes() {}

    public static <T extends Entity> T spawn(TrollContext ctx, Location loc, Class<T> type, Consumer<T> init) {
        Player target = ctx.getTarget();
        if (target == null || loc == null || loc.getWorld() == null || !ctx.canSpawnEntity()) return null;
        // nigdy nie wymuszamy ładowania chunków
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return null;
        T entity = loc.getWorld().spawn(loc, type, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            if (init != null) init.accept(e);
        });
        ctx.trackEntity(entity);
        target.showEntity(ctx.getPlugin(), entity);
        return entity;
    }

    /** BlockDisplay wyśrodkowany w loc o danym rozmiarze (w blokach). */
    public static BlockDisplay blockDisplay(TrollContext ctx, Location loc, Material mat,
                                            float sx, float sy, float sz, boolean fullBright) {
        Location l = loc.clone();
        l.setYaw(loc.getYaw());
        l.setPitch(0f);
        return spawn(ctx, l, BlockDisplay.class, d -> {
            d.setBlock(mat.createBlockData());
            d.setTransformation(new Transformation(new Vector3f(-sx / 2f, -sy / 2f, -sz / 2f),
                    new AxisAngle4f(), new Vector3f(sx, sy, sz), new AxisAngle4f()));
            d.setViewRange(4f);
            d.setShadowRadius(0f);
            d.setShadowStrength(0f);
            if (fullBright) d.setBrightness(new Display.Brightness(15, 15));
        });
    }

    /**
     * Płynnie przesuwa wyświetlany blok o delta w ciągu ticks (interpolacja po stronie klienta -
     * bez wysyłania pakietów co tick). Wywołać najwcześniej 2 ticki po spawnie.
     */
    public static void animate(BlockDisplay d, Vector delta, int ticks, float sx, float sy, float sz) {
        if (d == null || !d.isValid()) return;
        d.setInterpolationDuration(Math.max(1, ticks));
        d.setTransformation(new Transformation(
                new Vector3f(-sx / 2f + (float) delta.getX(), -sy / 2f + (float) delta.getY(), -sz / 2f + (float) delta.getZ()),
                new AxisAngle4f(), new Vector3f(sx, sy, sz), new AxisAngle4f()));
        d.setInterpolationDelay(0);
    }
}
