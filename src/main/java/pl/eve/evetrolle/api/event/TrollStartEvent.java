package pl.eve.evetrolle.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Wywoływany przed uruchomieniem trolla. Można anulować. */
public class TrollStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CommandSender executor;
    private final Player target;
    private final String trollId;
    private boolean cancelled;

    public TrollStartEvent(CommandSender executor, Player target, String trollId) {
        this.executor = executor;
        this.target = target;
        this.trollId = trollId;
    }

    public CommandSender getExecutor() { return executor; }
    public Player getTarget() { return target; }
    public String getTrollId() { return trollId; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
