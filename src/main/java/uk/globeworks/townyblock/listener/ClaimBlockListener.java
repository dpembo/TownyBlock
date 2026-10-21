package uk.globeworks.townyblock.listener;

import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import uk.globeworks.townyblock.TownyBlockPlugin;
import uk.globeworks.townyblock.chunk.ChunkCoverage;

/**
 * Refuses town creation and town expansion when the claim covers a protected chunk.
 * TownPreClaimEvent is fired for a new town's homeblock and for later claims, including outposts and takeovers.
 */
public final class ClaimBlockListener implements Listener {

    private final TownyBlockPlugin plugin;

    public ClaimBlockListener(TownyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTownPreClaim(TownPreClaimEvent event) {
        WorldCoord coord = event.getTownBlock().getWorldCoord();
        boolean blocked = ChunkCoverage.overlapsBlocked(
                coord.getWorldName(),
                coord.getX(),
                coord.getZ(),
                Coord.getCellSize(),
                plugin.store()::isBlocked
        );
        if (!blocked) {
            return;
        }
        event.setCancelled(true);
        event.setCancelMessage(plugin.blockedClaimMessage());
    }
}
