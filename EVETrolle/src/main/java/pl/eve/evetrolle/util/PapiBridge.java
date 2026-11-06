package pl.eve.evetrolle.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Wydzielone, aby klasa PlaceholderAPI ładowała się wyłącznie gdy plugin jest obecny. */
final class PapiBridge {
    private PapiBridge() {}

    static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
