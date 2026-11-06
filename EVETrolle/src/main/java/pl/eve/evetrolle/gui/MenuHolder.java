package pl.eve.evetrolle.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Holder rozpoznający inventory EVETrolle i przechowujący akcje przycisków. */
public final class MenuHolder implements InventoryHolder {
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @NotNull @Override
    public Inventory getInventory() { return inventory; }

    public void action(int slot, Consumer<InventoryClickEvent> action) { actions.put(slot, action); }

    public void handle(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getSlot());
        if (a != null) a.accept(e);
    }
}
