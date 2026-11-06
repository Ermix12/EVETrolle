package pl.eve.evetrolle.troll;

import org.bukkit.Material;
import pl.eve.evetrolle.api.Troll;
import pl.eve.evetrolle.api.TrollInfo;

import java.util.List;

/** Wspólna baza dla wbudowanych trolli. */
public abstract class BaseTroll implements Troll {
    private final String id;
    private final TrollInfo info;

    protected BaseTroll(String id, String name, Material material, int slot, int seconds, String... lore) {
        this.id = id;
        this.info = new TrollInfo(name, material, List.of(lore), slot, seconds, "");
    }

    @Override public String getId() { return id; }
    @Override public TrollInfo getDefaultInfo() { return info; }
}
