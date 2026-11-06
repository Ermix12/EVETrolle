package pl.eve.evetrolle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parsowanie tekstów: kody & / § (także &#RRGGBB) albo MiniMessage (gdy tekst zawiera tagi &lt;...&gt;).
 */
public final class Text {
    private static final Pattern MINI = Pattern.compile("</?[a-zA-Z#!][^<>]*>");
    private static final Pattern STRIP = Pattern.compile("(?i)&#[0-9a-f]{6}|&[0-9a-fk-or]");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private static volatile boolean papi;

    private Text() {}

    public static void setPapiEnabled(boolean enabled) { papi = enabled; }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        String s = input.replace('§', '&');
        if (MINI.matcher(s).find()) {
            return MiniMessage.miniMessage().deserialize(s);
        }
        return LEGACY.deserialize(s);
    }

    public static Component parse(String input, Map<String, String> placeholders, Player papiPlayer) {
        return parse(apply(input, placeholders, papiPlayer));
    }

    /** Podmienia {klucz} oraz (jeśli dostępne) placeholdery PlaceholderAPI. */
    public static String apply(String input, Map<String, String> placeholders, Player papiPlayer) {
        if (input == null) return "";
        String s = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        if (papi && papiPlayer != null && s.indexOf('%') >= 0) {
            try {
                s = PapiBridge.apply(papiPlayer, s);
            } catch (Throwable ignored) {
                // PlaceholderAPI niedostępne - pomijamy
            }
        }
        return s;
    }

    /** Komponent bez domyślnej kursywy (do nazw i opisów przedmiotów). */
    public static Component item(String input, Map<String, String> placeholders) {
        return parse(apply(input, placeholders, null)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> items(List<String> lines, Map<String, String> placeholders) {
        return lines.stream().map(l -> item(l, placeholders)).toList();
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Usuwa kody kolorów (do logów w konsoli). */
    public static String strip(String input) {
        if (input == null) return "";
        return plain(parse(STRIP.matcher(input.replace('§', '&')).replaceAll("")));
    }
}
