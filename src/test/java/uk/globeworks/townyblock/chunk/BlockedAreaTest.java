package uk.globeworks.townyblock.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockedAreaTest {

    @Test
    void containsChunksInsideTheSquareOnly() {
        BlockedArea area = new BlockedArea("world", -1, 1, -1, 1);
        assertEquals(9, area.chunkCount());
        assertTrue(area.contains(new ChunkPosition("world", 0, 0)));
        assertTrue(area.contains(new ChunkPosition("world", -1, 1)));
        assertFalse(area.contains(new ChunkPosition("world", 2, 0)));
        assertFalse(area.contains(new ChunkPosition("world_nether", 0, 0)));
    }

    @Test
    void swappedBoundsAreNormalised() {
        BlockedArea area = new BlockedArea("world", 4, -2, 3, -1);
        assertEquals(-2, area.minX());
        assertEquals(4, area.maxX());
        assertEquals(-1, area.minZ());
        assertEquals(3, area.maxZ());
    }

    @Test
    void insideRequiresTheWholeArea() {
        BlockedArea area = new BlockedArea("world", 0, 2, 0, 2);
        assertTrue(area.isInside("world", -1, 3, -1, 3));
        assertFalse(area.isInside("world", 0, 1, 0, 2));
        assertFalse(area.isInside("world_nether", -5, 5, -5, 5));
    }
}
