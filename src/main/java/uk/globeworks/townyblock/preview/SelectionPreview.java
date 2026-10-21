package uk.globeworks.townyblock.preview;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uk.globeworks.townyblock.TownyBlockPlugin;
import uk.globeworks.townyblock.chunk.BlockedArea;
import uk.globeworks.townyblock.chunk.ChunkCoverage;

import java.util.UUID;

/**
 * Shows the pending area to the player who started it, one dust particle on each block of ground.
 * Particles are only sent near that player, and they stop when the preview is confirmed or cancelled.
 */
public final class SelectionPreview {

    private static final int VIEW_RANGE = 32;
    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(255, 70, 40), 1.5f);

    private final TownyBlockPlugin plugin;
    private final UUID playerId;
    private final BlockedArea area;
    private BukkitTask task;
    private boolean closed;

    public SelectionPreview(TownyBlockPlugin plugin, Player player, BlockedArea area) {
        this.plugin = plugin;
        this.playerId = player.getUniqueId();
        this.area = area;
    }

    public BlockedArea area() {
        return area;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::draw, 0L, 10L);
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void draw() {
        if (closed) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        if (!world.getName().equals(area.world())) {
            return;
        }
        int px = player.getLocation().getBlockX();
        int pz = player.getLocation().getBlockZ();
        int minX = area.minX() * ChunkCoverage.CHUNK_SIZE;
        int maxX = area.maxX() * ChunkCoverage.CHUNK_SIZE + ChunkCoverage.CHUNK_SIZE - 1;
        int minZ = area.minZ() * ChunkCoverage.CHUNK_SIZE;
        int maxZ = area.maxZ() * ChunkCoverage.CHUNK_SIZE + ChunkCoverage.CHUNK_SIZE - 1;
        int fromX = Math.max(minX, px - VIEW_RANGE);
        int toX = Math.min(maxX, px + VIEW_RANGE);
        int fromZ = Math.max(minZ, pz - VIEW_RANGE);
        int toZ = Math.min(maxZ, pz + VIEW_RANGE);

        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                if (!world.isChunkLoaded(Math.floorDiv(x, 16), Math.floorDiv(z, 16))) {
                    continue;
                }
                int surface = world.getHighestBlockYAt(x, z);
                player.spawnParticle(Particle.DUST, x + 0.5, surface + 1.15, z + 0.5, 1, 0, 0, 0, 0, DUST);
            }
        }
    }
}
