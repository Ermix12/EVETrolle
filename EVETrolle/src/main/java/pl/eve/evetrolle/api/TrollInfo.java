package pl.eve.evetrolle.api;

import org.bukkit.Material;

import java.util.List;

/**
 * Dane wyświetlane w GUI oraz czas trwania trolla. Wartości z config.yml nadpisują domyślne.
 *
 * @param name            nazwa (kody & lub MiniMessage)
 * @param material        ikona w GUI
 * @param lore            opis w GUI
 * @param slot            slot w GUI (-1 = automatycznie)
 * @param durationSeconds czas trwania w sekundach
 * @param permission      dodatkowe uprawnienie (puste = tylko evetrolle.troll)
 */
public record TrollInfo(String name, Material material, List<String> lore, int slot, int durationSeconds, String permission) {
    public TrollInfo {
        if (name == null) name = "Troll";
        if (material == null) material = Material.PAPER;
        lore = lore == null ? List.of() : List.copyOf(lore);
        if (permission == null) permission = "";
        if (durationSeconds < 1) durationSeconds = 1;
    }
}
