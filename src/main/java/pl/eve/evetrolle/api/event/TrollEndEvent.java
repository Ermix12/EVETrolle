package pl.eve.evetrolle.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Wywoływany po zakończeniu (lub przerwaniu) trolla. */
public class TrollEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID targetId;
    private final String trollId;

    public TrollEndEvent(UUID targetId, String trollId) {
        this.targetId = targetId;
        this.trollId = trollId;
    }

    public UUID getTargetId() { return targetId; }
    public String getTrollId() { return trollId; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
