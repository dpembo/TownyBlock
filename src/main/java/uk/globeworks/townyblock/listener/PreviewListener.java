package uk.globeworks.townyblock.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import uk.globeworks.townyblock.TownyBlockPlugin;

public final class PreviewListener implements Listener {

    private final TownyBlockPlugin plugin;

    public PreviewListener(TownyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.previews().discard(event.getPlayer());
    }
}
