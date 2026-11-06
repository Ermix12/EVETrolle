package pl.eve.evetrolle.troll.builtin;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.eve.evetrolle.api.TrollContext;
import pl.eve.evetrolle.troll.BaseTroll;
import pl.eve.evetrolle.util.Atmosphere;
import pl.eve.evetrolle.util.FX;

import java.util.List;

/** Czarne niebo, fioletowe particle i dźwięki Endu. */
public final class AbyssTroll extends BaseTroll {
    private static final List<String> SOUNDS = List.of(
            "entity.enderman.stare 0.8 0.6", "block.portal.ambient 0.7 0.7", "entity.ender_dragon.growl 0.5 0.6",
            "entity.enderman.ambient 0.8 0.5", "ambient.cave 0.9 0.6");

    public AbyssTroll() {
        super("abyss", "&5&lOtchłań", Material.END_PORTAL_FRAME, 15, 15,
                "&7Czarne niebo, fioletowe particle", "&7i dźwięki Endu.");
    }

    @Override
    public void start(TrollContext ctx) {
        Player p = ctx.getTarget();
        if (p == null) return;
        ConfigurationSection o = ctx.getOptions();
        Atmosphere.time(ctx, o.getLong("time", 18000L));
        Atmosphere.weather(ctx, WeatherType.CLEAR);
        if (o.getBoolean("darkness-effect", false) && !p.hasPotionEffect(PotionEffectType.DARKNESS)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, (int) ctx.getDurationTicks() + 40, 0, true, false, false));
            ctx.onEnd(() -> {
                Player q = ctx.getTarget();
                if (q != null) q.removePotionEffect(PotionEffectType.DARKNESS);
            });
        }
        ctx.title(o.getString("title", "&5&lOTCHŁAŃ"), o.getString("subtitle", "&dEnd cię woła..."), 10, 50, 20);
        ctx.countdownBar("&5&lOtchłań &7({time}s)", BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_6);
        FX.sound(p, o.getString("start-sound", "block.end_portal.spawn 0.4 0.5"));
        List<String> sounds = o.getStringList("sounds");
        final List<String> pool = sounds.isEmpty() ? SOUNDS : sounds;
        final double density = o.getDouble("particle-density", 1.0);
        final int[] nextSound = {FX.rndInt(20, 50)};
        ctx.loop(5, 4, i -> {
            Player t = ctx.getTarget();
            if (t == null) return false;
            Location c = t.getLocation().add(0, 1, 0);
            FX.particle(t, Particle.PORTAL, c, (int) (40 * density), 6, 3, 6, 0.6);
            FX.particle(t, Particle.REVERSE_PORTAL, c, (int) (12 * density), 5, 2.5, 5, 0.05);
            FX.particle(t, Particle.WITCH, c, (int) (6 * density), 5, 2.5, 5, 0.02);
            FX.dust(t, c, (int) (8 * density), 4, 0x6A00B8, 2.4f);
            nextSound[0] -= 4;
            if (nextSound[0] <= 0) {
                nextSound[0] = FX.rndInt(40, 90);
                FX.sound(t, t.getLocation().add(FX.rnd(-8, 8), FX.rnd(0, 3), FX.rnd(-8, 8)), pool.get(FX.rndInt(0, pool.size() - 1)));
            }
            return true;
        });
    }
}
