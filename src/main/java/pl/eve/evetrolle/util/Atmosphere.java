package pl.eve.evetrolle.util;

import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;

/** Zmiany nieba/pogody widoczne tylko dla jednego gracza (z automatycznym przywróceniem). */
public final class Atmosphere {
    private Atmosphere() {}

    public static void time(TrollContext ctx, long time) {
        Player p = ctx.getTarget();
        if (p == null) return;
        p.setPlayerTime(time, false);
        ctx.onEnd(() -> {
            Player q = Bukkit.getPlayer(ctx.getTargetId());
            if (q != null) q.resetPlayerTime();
        });
    }

    public static void weather(TrollContext ctx, WeatherType type) {
        Player p = ctx.getTarget();
        if (p == null) return;
        p.setPlayerWeather(type);
        ctx.onEnd(() -> {
            Player q = Bukkit.getPlayer(ctx.getTargetId());
            if (q != null) q.resetPlayerWeather();
        });
    }

    /**
     * Czerwona (kurcząca się granica) lub niebieska poświata na krawędziach ekranu.
     * Granica jest gigantyczna i wyłącznie wizualna dla tego gracza - nie ogranicza ruchu.
     */
    public static void vignette(TrollContext ctx, boolean red) {
        Player p = ctx.getTarget();
        if (p == null) return;
        WorldBorder previous = p.getWorldBorder();
        WorldBorder wb = Bukkit.createWorldBorder();
        wb.setCenter(p.getLocation().getX(), p.getLocation().getZ());
        wb.setSize(59_999_968D);
        wb.setWarningDistance(2_000_000_000);
        wb.setWarningTime(0);
        if (red) wb.setSize(59_999_000D, 7200L);
        p.setWorldBorder(wb);
        ctx.onEnd(() -> {
            Player q = Bukkit.getPlayer(ctx.getTargetId());
            if (q != null) q.setWorldBorder(previous);
        });
    }
}
