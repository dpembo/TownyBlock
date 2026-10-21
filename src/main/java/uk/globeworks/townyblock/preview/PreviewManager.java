package uk.globeworks.townyblock.preview;

import org.bukkit.entity.Player;
import uk.globeworks.townyblock.TownyBlockPlugin;
import uk.globeworks.townyblock.chunk.BlockedArea;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PreviewManager {

    private final TownyBlockPlugin plugin;
    private final Map<UUID, SelectionPreview> pending = new HashMap<>();

    public PreviewManager(TownyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasPreview(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    /**
     * @return true when this player already had a preview and it was cleared first
     */
    public boolean start(Player player, BlockedArea area) {
        boolean replaced = pending.containsKey(player.getUniqueId());
        discard(player);
        SelectionPreview preview = new SelectionPreview(plugin, player, area);
        pending.put(player.getUniqueId(), preview);
        preview.start();
        return replaced;
    }

    public BlockedArea confirm(Player player) {
        SelectionPreview preview = pending.remove(player.getUniqueId());
        if (preview == null) {
            return null;
        }
        BlockedArea area = preview.area();
        preview.close();
        return area;
    }

    public boolean discard(Player player) {
        SelectionPreview preview = pending.remove(player.getUniqueId());
        if (preview == null) {
            return false;
        }
        preview.close();
        return true;
    }

    public void clearAll() {
        for (SelectionPreview preview : pending.values()) {
            preview.close();
        }
        pending.clear();
    }
}
