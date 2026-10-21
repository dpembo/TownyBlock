package uk.globeworks.townyblock.chunk;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns a centre chunk plus a radius, or a Towny town block, into the Minecraft chunks involved.
 * Radius uses Chebyshev distance, so a radius of 2 is every chunk within 2 of the centre, a 5 by 5 square.
 */
public final class ChunkCoverage {

    public static final int CHUNK_SIZE = 16;

    private ChunkCoverage() {
    }

    public static Set<ChunkPosition> square(String world, int centerX, int centerZ, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        int width = radius * 2 + 1;
        Set<ChunkPosition> chunks = new LinkedHashSet<>(width * width);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                chunks.add(new ChunkPosition(world, x, z));
            }
        }
        return chunks;
    }

    /**
     * A Towny town block overlaps a blocked chunk when any Minecraft chunk inside that town block is blocked.
     * Town block size defaults to 16, which lines up with chunks, but Towny can be configured otherwise.
     */
    public static boolean overlapsBlocked(String world, int townBlockX, int townBlockZ, int cellSize, Predicate<ChunkPosition> blocked) {
        int size = cellSize > 0 ? cellSize : CHUNK_SIZE;
        int minBlockX = townBlockX * size;
        int minBlockZ = townBlockZ * size;
        int minChunkX = Math.floorDiv(minBlockX, CHUNK_SIZE);
        int maxChunkX = Math.floorDiv(minBlockX + size - 1, CHUNK_SIZE);
        int minChunkZ = Math.floorDiv(minBlockZ, CHUNK_SIZE);
        int maxChunkZ = Math.floorDiv(minBlockZ + size - 1, CHUNK_SIZE);
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (blocked.test(new ChunkPosition(world, x, z))) {
                    return true;
                }
            }
        }
        return false;
    }
}
