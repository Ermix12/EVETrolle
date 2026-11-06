package pl.eve.evetrolle.troll.builtin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.FX;

import java.util.List;

/** Dźwięki jaskiń, Wardena, szepty i kroki. */
public final class ScarySoundsTroll extends BaseTroll {
    private static final List<String> SOUNDS = List.of(
            "ambient.cave 1 1", "entity.warden.heartbeat 1 1", "entity.warden.nearby_close 0.8 1",
            "entity.warden.listening 0.7 1", "entity.warden.ambient 0.8 0.8", "entity.warden.sniff 0.8 1");
    private static final List<String> WHISPERS = List.of(
            "ambient.soul_sand_valley.additions 0.8 1", "entity.vex.ambient 0.6 0.5", "ambient.crimson_forest.additions 0.8 0.7");
    private static final List<String> STEPS = List.of(
            "block.stone.step 1 0.9", "block.gravel.step 1 0.8", "block.grass.step 1 0.8", "block.wood.step 1 0.9");

    public ScarySoundsTroll() {
        super("scary-sounds", "&5&lStraszne Dźwięki", Material.SCULK_SHRIEKER, 22, 20,
                "&7Cave sounds, Warden,", "&7szepty i kroki.");
    }

    private static List<String> pick(ConfigurationSection o, String key, List<String> def) {
        List<String> l = o.getStringList(key);
        return l.isEmpty() ? def : l;
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        final List<String> sounds = pick(o, "sounds", SOUNDS);
        final List<String> whispers = pick(o, "whispers", WHISPERS);
        final List<String> steps = pick(o, "footsteps", STEPS);
        final int minI = Math.max(10, o.getInt("interval-min-ticks", 25));
        final int maxI = Math.max(minI, o.getInt("interval-max-ticks", 70));
        final int[] next = {10};

        ctx.countdownBar("&5&lSzepty w ciemności &7({time}s)", net.kyori.adventure.bossbar.BossBar.Color.PURPLE,
                net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20);
        ctx.loop(10, 5, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            next[0] -= 5;
            if (next[0] > 0) return true;
            next[0] = FX.rndInt(minI, maxI);
            int roll = FX.rndInt(1, 100);
            if (roll <= 55) {
                FX.sound(t, around(t, FX.rnd(3, 14)), sounds.get(FX.rndInt(0, sounds.size() - 1)));
            } else if (roll <= 80) {
                footsteps(ctx, t, steps.get(FX.rndInt(0, steps.size() - 1)));
            } else {
                FX.sound(t, around(t, FX.rnd(2, 6)), whispers.get(FX.rndInt(0, whispers.size() - 1)));
            }
            return true;
        });
    }

    private static Location around(Player t, double dist) {
        return FX.around(t.getLocation(), FX.rnd(0, Math.PI * 2), dist).add(0, FX.rnd(0, 2), 0);
    }

    /** Kroki zbliżające się z jednej strony. */
    private static void footsteps(TrollContext ctx, Player t, String spec) {
        final double ang = FX.rnd(0, Math.PI * 2);
        for (int s = 0; s < 6; s++) {
            final double dist = 10 - s * 1.4;
            ctx.runLater(1L + s * 8L, () -> {
                Player q = ctx.getTarget();
                if (q != null) FX.sound(q, FX.around(q.getLocation(), ang, dist), spec);
            });
        }
    }
}
