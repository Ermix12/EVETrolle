package pl.eve.evetrolle.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.eve.evetrolle.util.Text;

import java.util.List;
import java.util.Map;

final class Items {
    private Items() {}

    static ItemStack create(Material material, String name, List<String> lore, Map<String, String> ph, boolean glow) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Text.item(name, ph));
        if (lore != null && !lore.isEmpty()) meta.lore(Text.items(lore, ph));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (glow) meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }
}
