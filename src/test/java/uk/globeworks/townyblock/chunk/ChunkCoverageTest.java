package uk.globeworks.townyblock.chunk;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkCoverageTest {

    @Test
    void radiusZeroIsTheCentreChunk() {
        Set<ChunkPosition> chunks = ChunkCoverage.square("world", 4, -2, 0);
        assertEquals(Set.of(new ChunkPosition("world", 4, -2)), chunks);
    }

    @Test
    void radiusIsASquareIncludingDiagonals() {
        Set<ChunkPosition> chunks = ChunkCoverage.square("world", 0, 0, 1);
        assertEquals(9, chunks.size());
        assertTrue(chunks.contains(new ChunkPosition("world", -1, -1)));
        assertTrue(chunks.contains(new ChunkPosition("world", 1, 1)));
        assertFalse(chunks.contains(new ChunkPosition("world", 2, 0)));
    }

    @Test
    void defaultTownBlockSizeLinesUpWithChunks() {
        assertTrue(overlaps("world", 3, -4, 16, new ChunkPosition("world", 3, -4)));
        assertFalse(overlaps("world", 3, -4, 16, new ChunkPosition("world", 4, -4)));
    }

    @Test
    void largerTownBlockIsBlockedWhenAnyCoveredChunkIsProtected() {
        assertTrue(overlaps("world", 0, 0, 32, new ChunkPosition("world", 1, 1)));
        assertFalse(overlaps("world", 0, 0, 32, new ChunkPosition("world", 2, 0)));
    }

    @Test
    void negativeTownBlockStillUsesFloorDivision() {
        assertTrue(overlaps("world", -1, -1, 16, new ChunkPosition("world", -1, -1)));
        assertFalse(overlaps("world", -1, -1, 16, new ChunkPosition("world", 0, 0)));
    }

    private static boolean overlaps(String world, int townBlockX, int townBlockZ, int cellSize, ChunkPosition blocked) {
        return ChunkCoverage.overlapsBlocked(world, townBlockX, townBlockZ, cellSize, blocked::equals);
    }
}
