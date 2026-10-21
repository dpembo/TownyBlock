package uk.globeworks.townyblock.chunk;

/**
 * A square of chunks saved together, so standing anywhere inside it can remove the whole area.
 */
public record BlockedArea(String world, int minX, int maxX, int minZ, int maxZ) {

    public BlockedArea {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("World name is required");
        }
        if (minX > maxX) {
            int swap = minX;
            minX = maxX;
            maxX = swap;
        }
        if (minZ > maxZ) {
            int swap = minZ;
            minZ = maxZ;
            maxZ = swap;
        }
    }

    public boolean contains(ChunkPosition chunk) {
        return world.equals(chunk.world())
                && chunk.x() >= minX && chunk.x() <= maxX
                && chunk.z() >= minZ && chunk.z() <= maxZ;
    }

    public boolean isInside(String worldName, int boundMinX, int boundMaxX, int boundMinZ, int boundMaxZ) {
        return world.equals(worldName)
                && minX >= boundMinX && maxX <= boundMaxX
                && minZ >= boundMinZ && maxZ <= boundMaxZ;
    }

    public int chunkCount() {
        long width = (long) maxX - minX + 1;
        long depth = (long) maxZ - minZ + 1;
        long count = width * depth;
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }
}
