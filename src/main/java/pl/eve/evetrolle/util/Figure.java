package pl.eve.evetrolle.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import pl.eve.evetrolle.api.TrollContext;

import java.util.ArrayList;
import java.util.List;

/** Humanoidalna "postać" (ArmorStand) widoczna tylko dla gracza-celu. */
public final class Figure {
    public enum Style { HEROBRINE, STALKER }

    private final TrollContext ctx;
    private final ArmorStand stand;
    private final ConfigurationSection opts;
    private final boolean withEyes;
    private final List<BlockDisplay> eyes = new ArrayList<>();
    private float yaw;

    private Figure(TrollContext ctx, ArmorStand stand, ConfigurationSection opts, boolean withEyes, float yaw) {
        this.ctx = ctx;
        this.stand = stand;
        this.opts = opts;
        this.withEyes = withEyes;
        this.yaw = yaw;
    }

    public static Figure spawn(TrollContext ctx, Location loc, Style style, ConfigurationSection opts) {
        ArmorStand stand = Fakes.spawn(ctx, loc, ArmorStand.class, as -> setup(as, style, opts));
        if (stand == null) return null;
        Figure f = new Figure(ctx, stand, opts, style == Style.HEROBRINE && opts.getBoolean("eyes", true), loc.getYaw());
        f.placeEyes(loc);
        return f;
    }

    private static void setup(ArmorStand as, Style style, ConfigurationSection o) {
        as.setGravity(false);
        as.setInvulnerable(true);
        as.setSilent(true);
        as.setArms(true);
        as.setBasePlate(false);
        as.setMarker(true);
        as.setCollidable(false);
        as.setCanPickupItems(false);
        as.setRightArmPose(EulerAngle.ZERO);
        as.setLeftArmPose(EulerAngle.ZERO);
        EntityEquipment eq = as.getEquipment();
        if (eq == null) return;
        boolean hero = style == Style.HEROBRINE;
        eq.setHelmet(Skulls.head(o.getString("head-name", hero ? "Herobrine" : "Steve"), o.getString("texture", "")));
        eq.setChestplate(Skulls.leather(Material.LEATHER_CHESTPLATE, hero ? Color.fromRGB(0, 170, 170) : Color.fromRGB(14, 14, 16)));
        eq.setLeggings(Skulls.leather(Material.LEATHER_LEGGINGS, hero ? Color.fromRGB(60, 60, 170) : Color.fromRGB(10, 10, 12)));
        eq.setBoots(Skulls.leather(Material.LEATHER_BOOTS, hero ? Color.fromRGB(90, 90, 90) : Color.fromRGB(8, 8, 10)));
    }

    private Location[] eyeLocations(Location base, float yawDeg) {
        double rad = Math.toRadians(yawDeg);
        Vector fwd = new Vector(-Math.sin(rad), 0, Math.cos(rad));
        Vector lat = new Vector(Math.cos(rad), 0, Math.sin(rad));
        double y = opts.getDouble("eye-y", 1.66);
        double fo = opts.getDouble("eye-forward", 0.30);
        double sp = opts.getDouble("eye-spread", 0.14);
        Location[] out = new Location[2];
        int i = 0;
        for (int s : new int[]{-1, 1}) {
            Location l = base.clone().add(fwd.clone().multiply(fo)).add(lat.clone().multiply(sp * s)).add(0, y, 0);
            l.setYaw(yawDeg);
            l.setPitch(0f);
            out[i++] = l;
        }
        return out;
    }

    private void placeEyes(Location base) {
        if (!withEyes) return;
        float w = (float) opts.getDouble("eye-width", 0.15);
        float h = (float) opts.getDouble("eye-height", 0.07);
        for (Location l : eyeLocations(base, yaw)) {
            BlockDisplay d = Fakes.blockDisplay(ctx, l, Material.WHITE_CONCRETE, w, h, 0.02f, true);
            if (d != null) eyes.add(d);
        }
    }

    public boolean isValid() { return stand.isValid(); }

    public Location getLocation() { return stand.getLocation(); }

    /** Obraca postać w stronę punktu (tylko jeśli zmiana kąta jest zauważalna). */
    public void face(Location target) {
        Location cur = stand.getLocation();
        Vector d = target.toVector().subtract(cur.toVector());
        float ny = (float) Math.toDegrees(Math.atan2(-d.getX(), d.getZ()));
        float diff = Math.abs(((ny - yaw + 540f) % 360f) - 180f);
        if (diff < 4f) return;
        yaw = ny;
        stand.setRotation(ny, 0f);
        if (!eyes.isEmpty()) {
            Location[] ls = eyeLocations(cur, ny);
            for (int i = 0; i < eyes.size() && i < ls.length; i++) {
                if (eyes.get(i).isValid()) eyes.get(i).teleport(ls[i]);
            }
        }
    }

    /** Czy gracz patrzy w stronę postaci (kąt w stopniach). */
    public boolean isLookedAt(Player p, double angleDeg) {
        Location eye = p.getEyeLocation();
        Location at = stand.getLocation().add(0, 1.5, 0);
        if (at.getWorld() != eye.getWorld()) return false;
        Vector to = at.toVector().subtract(eye.toVector());
        if (to.lengthSquared() < 0.01) return true;
        return to.normalize().dot(eye.getDirection()) >= Math.cos(Math.toRadians(angleDeg));
    }

    public void remove() {
        for (BlockDisplay e : eyes) ctx.untrack(e);
        eyes.clear();
        ctx.untrack(stand);
    }
}
