package uk.globeworks.townyblock.chunk;

/**
 * A Minecraft chunk, 16 by 16 blocks, identified by world and chunk coordinates.
 */
public record ChunkPosition(String world, int x, int z) {

    public ChunkPosition {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("World name is required");
        }
    }
}
