# EVETrolle

Zaawansowany plugin **Paper 1.21.11 / Java 21** z systemem trolli administracyjnych.
Wszystkie trolle są **wyłącznie wizualne i dźwiękowe**, działają **tylko po stronie wybranego gracza**
i nie niszczą bloków, nie zadają obrażeń, nie wywołują eksplozji ani nie wpływają na rozgrywkę.

## Kompilacja

Wymagania: **JDK 21** i **Maven 3.8+** (dostęp do repozytoriów PaperMC, extendedclip i Maven Central).

```bash
mvn clean package
```

Gotowy plik: `target/EVETrolle-1.0.0.jar` → wrzuć do `plugins/` i uruchom serwer.
Pliki `config.yml`, `messages.yml` (oraz `data.yml` ze statystykami) powstaną w `plugins/EVETrolle/`.

## Komendy

| Komenda | Opis | Uprawnienie |
|---|---|---|
| `/evetrolle` | Otwiera menu wyboru gracza → menu trolli | `evetrolle.use` + `evetrolle.troll` |
| `/evetrolle troll <gracz>` | Otwiera menu trolli dla gracza | `evetrolle.troll` |
| `/evetrolle troll <gracz> <id>` | Uruchamia trolla bezpośrednio (działa też z konsoli) | `evetrolle.troll` |
| `/evetrolle stop <gracz>` | Zatrzymuje aktywnego trolla | `evetrolle.troll` |
| `/evetrolle list` | Lista trolli i ich ID | `evetrolle.use` |
| `/evetrolle reload` | Przeładowanie `config.yml` i `messages.yml` | `evetrolle.reload` |

Alias: `/etrolle`.

## Uprawnienia (LuckPerms)

| Uprawnienie | Opis |
|---|---|
| `evetrolle.use` | Dostęp do `/evetrolle` |
| `evetrolle.reload` | Dostęp do `/evetrolle reload` |
| `evetrolle.troll` | Możliwość używania trolli |
| `evetrolle.admin` | Dostęp do wszystkich funkcji (use, reload, troll, notify; pomija cooldown i uprawnienia per-troll) |
| `evetrolle.notify` | Powiadomienia o użyciu trolli |
| `evetrolle.bypass` | Ochrona przed trollami (domyślnie nikt) |
| *(opcjonalne)* `permission` w configu | Dodatkowe uprawnienie dla konkretnego trolla, np. `evetrolle.troll.meteor` |

Przykłady LuckPerms:

```
/lp group admin permission set evetrolle.admin true
/lp group moderator permission set evetrolle.use true
/lp group moderator permission set evetrolle.troll true
/lp group vip permission set evetrolle.bypass true
```

Integracja z LuckPerms jest opcjonalna (`softdepend`): uprawnienia są sprawdzane przez Bukkit/LuckPerms,
a prefiks i grupa wykonawcy trafiają do powiadomień (`{admin_prefix}`, `{admin_group}`).

## Trolle

| ID | Opis |
|---|---|
| `fake-ban` | Fałszywy ekran permanentnego bana (czat + title + BossBar) |
| `fake-crash` | Realistyczny raport crasha klienta w czacie |
| `herobrine` | Herobrine pojawia się za graczem i znika po kilku sekundach lub po spojrzeniu na niego |
| `tnt-rain` | Deszcz TNT z nieba (wyświetlane bloki, bez eksplozji) |
| `meteor` | Gigantyczny meteoryt lecący w stronę gracza |
| `abyss` | Czarne niebo (noc), fioletowe particle, dźwięki Endu |
| `blood-moon` | Noc, czerwona poświata na ekranie, opady i "mgła" z particle |
| `wolf` | Świecące oczy w oddali i wycie |
| `stalker` | NPC obserwuje gracza z różnych miejsc, znika gdy gracz na niego spojrzy |
| `screen-takeover` | Title/ActionBar: „JA CIĘ WIDZĘ”, „NIE OGLĄDAJ SIĘ”, „JESTEM ZA TOBĄ” |
| `scary-sounds` | Cave sounds, Warden, szepty i kroki |
| `streamer-mode` | Fałszywe wiadomości administracji na ekranie |
| `herobrine-invasion` | Kilku Herobrine wokół gracza |
| `end-of-world` | Meteoryty, błyskawice i ogrom particle |

### Jak to działa (bez wpływu na świat i innych graczy)

* Particle, dźwięki, title, ActionBar, BossBar, czas, pogoda i obramowanie (czerwona poświata) są wysyłane **wyłącznie do jednego gracza**
  (`Player#spawnParticle`/`ParticleBuilder.receivers`, `Player#playSound`, `setPlayerTime`, `setPlayerWeather`, `Player#setWorldBorder`).
  Po zakończeniu trolla wszystko jest przywracane.
* "Postacie" (Herobrine, Stalker) i obiekty (TNT, meteoryt, oczy) to encje **niewidoczne dla innych** (`setVisibleByDefault(false)` + `Player#showEntity`),
  niezapisywane (`persistent=false`), bez AI, bez kolizji, nieśmiertelne i usuwane po zakończeniu lub wyjściu gracza.
  Spadające obiekty są animowane interpolacją po stronie klienta, więc nie generują ruchu pakietów co tick.
* Plugin nigdy nie ładuje chunków i nie zmienia bloków.

### Ograniczenia (techniczne)

* **Fake Ban / Fake Crash** nie mogą wyświetlić prawdziwego ekranu „rozłączono” bez faktycznego rozłączenia gracza.
  Domyślnie pokazują realistyczny komunikat w czacie + title. Opcja `disconnect: true` w configu wykonuje prawdziwy kick
  (to **nie** jest ban, gracz może od razu wrócić) - domyślnie wyłączona.
* **Czerwone niebo / mgła** nie da się ustawić per-gracz w czystym API; Krwawy Księżyc używa nocy, opadów, czerwonej poświaty
  ekranu i particle. Opcjonalnie w `abyss` można włączyć efekt Darkness (`darkness-effect: true`), który jest realnym efektem potiona.
* **Skin Herobrine:** bez tekstury postać ma głowę gracza o podanej nazwie profilu (domyślnie wygląda jak Steve) w niebiesko-turkusowym ubraniu
  i **białymi, świecącymi oczami** (`eyes: true`). Aby ustawić własny skin, wklej w `texture` wartość base64 z tekstury (np. ze strony minecraft-heads.com
  → sekcja "Minecraft URL"/"Value") dla `herobrine`, `herobrine-invasion` lub `stalker`.
* Nazwy dźwięków podawaj w formacie vanilla (`klucz głośność wysokość`). Nieznany klucz jest po prostu cichy - nie powoduje błędów w konsoli.

## Konfiguracja

* `config.yml` - ustawienia ogólne (cooldown, limity, logowanie, powiadomienia), cały wygląd GUI (tytuł `&cEVETrolle`, rozmiar, wypełnienie,
  przyciski, sloty), oraz każdy troll osobno: `enabled`, `slot`, `material`, `name`, `lore`, `duration`, `permission`, `options`.
* `messages.yml` - wszystkie wiadomości czatu, powiadomienia dla adminów i format logu.
* Teksty: kody `&` / `&#RRGGBB` lub MiniMessage (`<red>`). Placeholdery: `{player}`, `{target}`, `{admin}`, `{troll}`, `{duration}`; z PlaceholderAPI także `%...%`.
* `/evetrolle reload` ładuje obie konfiguracje bez restartu (aktywne trolle dokończą się normalnie).

### Własne trolle w config.yml (bez programowania)

Sekcja `custom-trolls` - akcje: `[TITLE]`, `[ACTIONBAR]`, `[CHAT]`, `[SOUND]`, `[PARTICLE]`, `[BOSSBAR]`, `[DELAY]` (ticki). Przykład jest w pliku.

## API dla innych pluginów

Dodaj EVETrolle jako `provided` (lub `compileOnly`) i `softdepend: [EVETrolle]` w `plugin.yml`.

```java
EVETrolleAPI api = EVETrolleAPI.get();
if (api != null) {
    api.registerTroll(new Troll() {
        @Override public String getId() { return "moj-troll"; }
        @Override public TrollInfo getDefaultInfo() {
            return new TrollInfo("&a&lMój troll", Material.EMERALD, List.of("&7Opis"), -1, 8, "");
        }
        @Override public void start(TrollContext ctx) {
            Player p = ctx.getTarget();
            ctx.title("&a&lSiema!", "&7To mój troll", 5, 40, 10);
            ctx.runLater(60, () -> ctx.actionBar("&eKoniec"));
            // ctx.loop(...), ctx.onEnd(...), Fakes.spawn(...) - wszystko sprząta się samo
        }
    });
    api.startTroll(sender, target, "moj-troll");
}
```

Zdarzenia: `TrollStartEvent` (anulowalne) i `TrollEndEvent`. Własny troll dostaje sekcję `trolls.<id>` z configu (nadpisuje nazwę, ikonę, slot, czas)
oraz `options` dostępne przez `ctx.getOptions()`.

## PlaceholderAPI

| Placeholder | Wartość |
|---|---|
| `%evetrolle_active%` | `true`/`false` - czy gracz jest teraz trollowany |
| `%evetrolle_active_troll%` | ID aktywnego trolla lub `none` |
| `%evetrolle_total%` | Łączna liczba użyć trolli |
| `%evetrolle_used%` | Ile razy gracz użył trolli |
| `%evetrolle_count_<id>%` | Ile razy użyto danego trolla |
| `%evetrolle_trolls%` | Liczba zarejestrowanych trolli |

## Optymalizacja

* Każdy troll dotyczy jednego gracza; zadania kończą się automatycznie przy wyjściu gracza, zmianie świata i śmierci.
* Limity: `max-active-sessions`, `max-entities-per-session`, `particle-multiplier` (np. `0.5`).
* GUI buduje się na żądanie, lista graczy jest stronicowana. Statystyki zapisywane asynchronicznie.
* Brak ładowania chunków i brak pracy w tle, gdy nikt nie jest trollowany.

## Struktura projektu

```
pom.xml
src/main/resources/{plugin.yml, config.yml, messages.yml}
src/main/java/pl/eve/evetrolle/
  EVETrollePlugin, Settings, Messages
  api/        EVETrolleAPI, Troll, TrollContext, TrollInfo, StartResult, event/
  command/    EveTrolleCommand
  gui/        GuiManager, MenuHolder, Items
  hook/       LuckPermsHook, PapiExpansion
  listener/   GuiListener, PlayerListener
  stats/      StatsManager
  troll/      TrollManager, BaseTroll, ConfigTroll, builtin/ (14 trolli)
  util/       Text, FX, Fakes, Figure, Skulls, Atmosphere, MeteorFx
```
