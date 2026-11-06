package pl.eve.evetrolle.api;

/** Wynik próby uruchomienia trolla. */
public enum StartResult {
    SUCCESS,
    UNKNOWN_TROLL,
    DISABLED,
    NO_PERMISSION,
    TARGET_OFFLINE,
    TARGET_EXEMPT,
    SELF_NOT_ALLOWED,
    WORLD_DISABLED,
    COOLDOWN,
    ALREADY_ACTIVE,
    LIMIT_REACHED,
    CANCELLED
}
