package pl.eve.evetrolle.api;

/**
 * Pojedynczy troll. Implementacje mogą być rejestrowane przez inne pluginy za pomocą
 * {@link EVETrolleAPI#registerTroll(Troll)}.
 * <p>
 * Troll powinien działać wyłącznie wizualnie / dźwiękowo i tylko dla gracza z {@link TrollContext#getTarget()}.
 * Wszystkie zadania i encje należy tworzyć przez {@link TrollContext}, aby zostały posprzątane po zakończeniu.
 */
public interface Troll {

    /** Unikalne ID (małe litery, bez spacji), np. "fake-ban". */
    String getId();

    /** Domyślne dane używane, gdy w config.yml brak sekcji dla tego trolla. */
    TrollInfo getDefaultInfo();

    /** Uruchamia trolla. Wywoływane zawsze w głównym wątku serwera. */
    void start(TrollContext context);
}
