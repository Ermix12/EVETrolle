package pl.eve.evetrolle.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.eve.evetrolle.EVETrollePlugin;
import pl.eve.evetrolle.Messages;
import pl.eve.evetrolle.api.StartResult;
import pl.eve.evetrolle.troll.TrollManager;
import pl.eve.evetrolle.util.FX;
import pl.eve.evetrolle.util.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Budowanie i obsługa GUI (całość konfigurowalna w config.yml, sekcja gui). */
public final class GuiManager {
    private final EVETrollePlugin plugin;

    public GuiManager(EVETrollePlugin plugin) {
        this.plugin = plugin;
    }

    /** GUI budowane jest na żądanie z aktualnego configu - reload nie wymaga dodatkowych kroków. */
    public void reload() {
        // brak cache - nic do zrobienia
    }

    private FileConfiguration c() { return plugin.getConfig(); }

    private int rows() { return Math.max(3, Math.min(6, c().getInt("gui.rows", 6))); }

    private void click(Player p) {
        FX.sound(p, c().getString("gui.click-sound", "ui.button.click 0.6 1.2"));
    }

    private ItemStack button(String key, Material defMat, String defName, Map<String, String> ph) {
        String path = "gui.buttons." + key + ".";
        Material m = Material.matchMaterial(c().getString(path + "material", defMat.name()));
        return Items.create(m == null ? defMat : m, c().getString(path + "name", defName),
                c().getStringList(path + "lore"), ph, false);
    }

    private int slot(String key, int def, int size) {
        int s = c().getInt("gui.buttons." + key + ".slot", def);
        return s >= 0 && s < size ? s : -1;
    }

    private void fill(Inventory inv) {
        if (!c().getBoolean("gui.filler.enabled", true)) return;
        Material m = Material.matchMaterial(c().getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE"));
        ItemStack filler = Items.create(m == null ? Material.BLACK_STAINED_GLASS_PANE : m, c().getString("gui.filler.name", " "), null, null, false);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private void put(MenuHolder h, Inventory inv, int slot, ItemStack item, java.util.function.Consumer<InventoryClickEvent> action) {
        if (slot < 0) return;
        inv.setItem(slot, item);
        if (action != null) h.action(slot, action);
    }

    // ------------------------------------------------------------------ wybór gracza

    public void openPlayerMenu(Player viewer, int page) {
        int rows = rows();
        int size = rows * 9;
        int perPage = (rows - 1) * 9;
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (viewer.canSee(p)) list.add(p);
        }
        list.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) perPage));
        int pg = Math.max(0, Math.min(page, pages - 1));

        Map<String, String> ph = Messages.map("page", String.valueOf(pg + 1), "pages", String.valueOf(pages),
                "online", String.valueOf(list.size()));
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, size, Text.parse(Text.apply(
                c().getString("gui.player-menu.title", "&cEVETrolle &8» &7Wybierz gracza &8({page}/{pages})"), ph, null)));
        holder.setInventory(inv);
        fill(inv);

        int from = pg * perPage;
        for (int i = 0; i < perPage && from + i < list.size(); i++) {
            Player target = list.get(from + i);
            UUID id = target.getUniqueId();
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            Map<String, String> hp = Messages.map("player", target.getName(), "world", target.getWorld().getName(),
                    "ping", String.valueOf(target.getPing()));
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(target);
                sm.displayName(Text.item(c().getString("gui.player-menu.head-name", "&e{player}"), hp));
                sm.lore(Text.items(c().getStringList("gui.player-menu.head-lore"), hp));
                head.setItemMeta(sm);
            }
            put(holder, inv, i, head, e -> {
                click(viewer);
                Player t = Bukkit.getPlayer(id);
                if (t == null) {
                    plugin.getMessages().send(viewer, "errors.target-offline");
                    return;
                }
                if (!viewer.hasPermission("evetrolle.troll")) {
                    plugin.getMessages().send(viewer, "errors.no-permission");
                    return;
                }
                openTrollMenu(viewer, t);
            });
        }
        put(holder, inv, slot("prev", size - 6, size), button("prev", Material.ARROW, "&e« Poprzednia strona", ph),
                pg > 0 ? e -> { click(viewer); openPlayerMenu(viewer, pg - 1); } : null);
        put(holder, inv, slot("next", size - 4, size), button("next", Material.ARROW, "&eNastępna strona »", ph),
                pg < pages - 1 ? e -> { click(viewer); openPlayerMenu(viewer, pg + 1); } : null);
        put(holder, inv, slot("close", size - 5, size), button("close", Material.BARRIER, "&cZamknij", ph),
                e -> { click(viewer); viewer.closeInventory(); });
        viewer.openInventory(inv);
    }

    // ------------------------------------------------------------------ wybór trolla

    public void openTrollMenu(Player viewer, Player target) {
        int rows = rows();
        int size = rows * 9;
        TrollManager manager = plugin.getTrollManager();
        UUID targetId = target.getUniqueId();
        Map<String, String> ph = Messages.map("target", target.getName(), "viewer", viewer.getName());

        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, size,
                Text.parse(Text.apply(c().getString("gui.title", "&cEVETrolle"), ph, null)));
        holder.setInventory(inv);
        fill(inv);

        Set<Integer> reserved = new HashSet<>();
        int infoSlot = slot("info", 4, size);
        int backSlot = slot("back", size - 9, size);
        int closeSlot = slot("close", size - 5, size);
        int stopSlot = slot("stop", size - 1, size);
        for (int s : new int[]{infoSlot, backSlot, closeSlot, stopSlot}) if (s >= 0) reserved.add(s);

        List<TrollManager.Entry> entries = new ArrayList<>();
        for (TrollManager.Entry e : manager.getEntries()) {
            if (e.enabled()) entries.add(e);
        }
        Set<Integer> used = new HashSet<>();
        Map<TrollManager.Entry, Integer> placed = new java.util.LinkedHashMap<>();
        for (TrollManager.Entry e : entries) {
            int s = e.info().slot();
            if (s >= 0 && s < size && !reserved.contains(s) && used.add(s)) placed.put(e, s);
        }
        for (TrollManager.Entry e : entries) {
            if (placed.containsKey(e)) continue;
            int found = -1;
            for (int r = 1; r < rows - 1 && found < 0; r++) {
                for (int col = 1; col <= 7; col++) {
                    int s = r * 9 + col;
                    if (!reserved.contains(s) && !used.contains(s)) {
                        found = s;
                        break;
                    }
                }
            }
            if (found < 0) {
                plugin.getLogger().warning("Brak miejsca w GUI dla trolla '" + e.troll().getId() + "'.");
                continue;
            }
            used.add(found);
            placed.put(e, found);
        }

        List<String> footer = c().getStringList("gui.troll-menu.lore-footer");
        List<String> locked = c().getStringList("gui.troll-menu.locked-lore");
        for (Map.Entry<TrollManager.Entry, Integer> pe : placed.entrySet()) {
            TrollManager.Entry e = pe.getKey();
            boolean allowed = manager.canUse(viewer, e);
            Map<String, String> tp = Messages.map("target", target.getName(), "duration", String.valueOf(e.info().durationSeconds()),
                    "troll", Text.strip(e.info().name()));
            List<String> lore = new ArrayList<>(e.info().lore());
            lore.addAll(allowed ? footer : locked);
            ItemStack item = Items.create(e.info().material(), e.info().name(), lore, tp, false);
            put(holder, inv, pe.getValue(), item, ev -> {
                click(viewer);
                Player t = Bukkit.getPlayer(targetId);
                if (c().getBoolean("gui.close-on-use", true)) viewer.closeInventory();
                StartResult r = manager.startTroll(viewer, t, e.troll().getId());
                plugin.reportResult(viewer, r, e.troll().getId(), t, target.getName());
            });
        }

        if (infoSlot >= 0) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(target);
                sm.displayName(Text.item(c().getString("gui.troll-menu.info-name", "&e{target}"), ph));
                sm.lore(Text.items(c().getStringList("gui.troll-menu.info-lore"), ph));
                head.setItemMeta(sm);
            }
            inv.setItem(infoSlot, head);
        }
        put(holder, inv, backSlot, button("back", Material.ARROW, "&e« Wybór gracza", ph),
                ev -> { click(viewer); openPlayerMenu(viewer, 0); });
        put(holder, inv, closeSlot, button("close", Material.BARRIER, "&cZamknij", ph),
                ev -> { click(viewer); viewer.closeInventory(); });
        put(holder, inv, stopSlot, button("stop", Material.REDSTONE_BLOCK, "&cZatrzymaj trolla", ph), ev -> {
            click(viewer);
            Player t = Bukkit.getPlayer(targetId);
            if (t != null && manager.stopTroll(t)) {
                plugin.getMessages().send(viewer, "stop.success", "target", target.getName());
            } else {
                plugin.getMessages().send(viewer, "stop.none", "target", target.getName());
            }
        });
        viewer.openInventory(inv);
    }
}
