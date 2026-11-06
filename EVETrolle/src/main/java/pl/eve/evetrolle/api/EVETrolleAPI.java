package pl.eve.evetrolle.api;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Publiczne API EVETrolle. Dostęp:
 * <pre>{@code EVETrolleAPI api = EVETrolleAPI.get();}</pre>
 */
public interface EVETrolleAPI {

    /** Zwraca API lub null, gdy EVETrolle nie jest włączony. */
    static EVETrolleAPI get() {
        var reg = Bukkit.getServicesManager().getRegistration(EVETrolleAPI.class);
        return reg == null ? null : reg.getProvider();
    }

    /** Rejestruje własnego trolla (zostanie dodany do GUI). Zastępuje trolla o tym samym ID. */
    void registerTroll(Troll troll);

    /** Wyrejestrowuje trolla. */
    boolean unregisterTroll(String id);

    Collection<Troll> getTrolls();

    Optional<Troll> getTroll(String id);

    Optional<TrollInfo> getInfo(String id);

    /** Uruchamia trolla na graczu, wykonując wszystkie sprawdzenia (uprawnienia, cooldown, limity). */
    StartResult startTroll(CommandSender executor, Player target, String trollId);

    /** Zatrzymuje aktywnego trolla gracza. */
    boolean stopTroll(Player target);

    /** Zatrzymuje wszystkie aktywne trolle. */
    void stopAll();

    boolean isTrolled(Player player);

    Optional<String> getActiveTroll(Player player);

    /** Pozostały cooldown w sekundach (0 = brak). */
    long getCooldownLeft(CommandSender executor, String trollId);
}
