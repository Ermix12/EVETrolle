package pl.eve.evetrolle.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Skulls {
    private Skulls() {}

    /** Głowa z podaną nazwą profilu i opcjonalną teksturą (base64 "value" z textures). */
    public static ItemStack head(String name, String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;
        String profileName = (name == null || name.isBlank() || name.length() > 16) ? "Steve" : name;
        PlayerProfile profile = Bukkit.createProfile(
                UUID.nameUUIDFromBytes(("EVETrolle:" + profileName).getBytes(StandardCharsets.UTF_8)), profileName);
        if (texture != null && !texture.isBlank()) {
            profile.setProperty(new ProfileProperty("textures", texture.trim()));
        }
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack leather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }
}
