package uk.globeworks.townyblock.chunk;

import org.bukkit.configuration.file.YamlConfiguration;
import uk.globeworks.townyblock.TownyBlockPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protected chunks saved in plugins/TownyBlock/chunks.yml.
 */
public final class BlockedChunkStore {

    private static final Comparator<ChunkPosition> ORDER = Comparator
            .comparing(ChunkPosition::world)
            .thenComparingInt(ChunkPosition::x)
            .thenComparingInt(ChunkPosition::z);

    private final TownyBlockPlugin plugin;
    private final File file;
    private final Set<ChunkPosition> chunks = ConcurrentHashMap.newKeySet();
    private final List<BlockedArea> areas = new ArrayList<>();
    private boolean loaded;

    public BlockedChunkStore(TownyBlockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chunks.yml");
    }

    public int size() {
        long count = chunks.size();
        for (BlockedArea area : areas) {
            count += area.chunkCount();
        }
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    public boolean isBlocked(ChunkPosition chunk) {
        for (BlockedArea area : areas) {
            if (area.contains(chunk)) {
                return true;
            }
        }
        return chunks.contains(chunk);
    }

    public List<ChunkPosition> all() {
        return chunks.stream().sorted(ORDER).toList();
    }

    public List<BlockedArea> areas() {
        return areas.stream()
                .sorted(Comparator.comparing(BlockedArea::world)
                        .thenComparingInt(BlockedArea::minX)
                        .thenComparingInt(BlockedArea::minZ))
                .toList();
    }

    public boolean load() {
        if (!file.exists()) {
            chunks.clear();
            areas.clear();
            loaded = true;
            return true;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not read " + file.getName() + ": " + exception.getMessage());
            return false;
        }
        Set<ChunkPosition> parsed = ConcurrentHashMap.newKeySet();
        for (Map<?, ?> entry : yaml.getMapList("chunks")) {
            ChunkPosition chunk = parseChunk(entry);
            if (chunk != null) {
                parsed.add(chunk);
            }
        }
        List<BlockedArea> parsedAreas = new ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("areas")) {
            BlockedArea area = parseArea(entry);
            if (area != null) {
                parsedAreas.add(area);
            }
        }
        chunks.clear();
        chunks.addAll(parsed);
        areas.clear();
        areas.addAll(parsedAreas);
        loaded = true;
        return true;
    }

    /**
     * @return false when an identical area is already saved
     */
    public boolean addArea(BlockedArea area) {
        if (areas.contains(area)) {
            return false;
        }
        areas.add(area);
        save();
        return true;
    }

    public List<BlockedArea> removeAreasContaining(ChunkPosition chunk) {
        List<BlockedArea> removed = new ArrayList<>();
        areas.removeIf(area -> {
            if (!area.contains(chunk)) {
                return false;
            }
            removed.add(area);
            return true;
        });
        if (!removed.isEmpty()) {
            save();
        }
        return removed;
    }

    /**
     * Drops individual chunks inside the square, and saved areas that sit entirely inside it.
     *
     * @return two counts: areas removed, then individual chunks removed
     */
    public int[] removeSquare(String world, int minX, int maxX, int minZ, int maxZ) {
        int areasRemoved = 0;
        var areaIterator = areas.iterator();
        while (areaIterator.hasNext()) {
            BlockedArea area = areaIterator.next();
            if (area.isInside(world, minX, maxX, minZ, maxZ)) {
                areaIterator.remove();
                areasRemoved++;
            }
        }
        int chunksRemoved = 0;
        for (ChunkPosition chunk : List.copyOf(chunks)) {
            if (chunk.world().equals(world) && chunk.x() >= minX && chunk.x() <= maxX && chunk.z() >= minZ && chunk.z() <= maxZ) {
                if (chunks.remove(chunk)) {
                    chunksRemoved++;
                }
            }
        }
        if (areasRemoved > 0 || chunksRemoved > 0) {
            save();
        }
        return new int[] {areasRemoved, chunksRemoved};
    }

    /**
     * @return how many of the given chunks were not already blocked
     */
    public int addAll(Collection<ChunkPosition> toAdd) {
        int added = 0;
        for (ChunkPosition chunk : toAdd) {
            if (chunks.add(chunk)) {
                added++;
            }
        }
        if (added > 0) {
            save();
        }
        return added;
    }

    /**
     * @return how many of the given chunks were blocked and are now clear
     */
    public int removeAll(Collection<ChunkPosition> toRemove) {
        int removed = 0;
        for (ChunkPosition chunk : toRemove) {
            if (chunks.remove(chunk)) {
                removed++;
            }
        }
        if (removed > 0) {
            save();
        }
        return removed;
    }

    private void save() {
        if (!loaded) {
            plugin.getLogger().severe("Refusing to overwrite chunks.yml because it was not loaded successfully.");
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Chunks and areas that towns cannot claim.",
                "Coordinates are Minecraft chunk coordinates (16 blocks).",
                "A town claim is refused when any chunk the town block covers is listed here.",
                "Areas are squares saved by /townyblock radius. Chunks are single chunks saved by /townyblock add."
        ));
        List<Map<String, Object>> areaEntries = new ArrayList<>();
        for (BlockedArea area : areas()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", area.world());
            entry.put("min-x", area.minX());
            entry.put("max-x", area.maxX());
            entry.put("min-z", area.minZ());
            entry.put("max-z", area.maxZ());
            areaEntries.add(entry);
        }
        yaml.set("areas", areaEntries);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (ChunkPosition chunk : all()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", chunk.world());
            entry.put("x", chunk.x());
            entry.put("z", chunk.z());
            entries.add(entry);
        }
        yaml.set("chunks", entries);
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + exception.getMessage());
        }
    }

    private ChunkPosition parseChunk(Map<?, ?> entry) {
        Object world = entry.get("world");
        Object x = entry.get("x");
        Object z = entry.get("z");
        if (!(world instanceof String worldName) || worldName.isBlank() || !(x instanceof Number xNumber) || !(z instanceof Number zNumber)) {
            plugin.getLogger().warning("Skipping a chunks.yml chunk that is missing world, x, or z.");
            return null;
        }
        return new ChunkPosition(worldName, xNumber.intValue(), zNumber.intValue());
    }

    private BlockedArea parseArea(Map<?, ?> entry) {
        Object world = entry.get("world");
        Object minX = entry.get("min-x");
        Object maxX = entry.get("max-x");
        Object minZ = entry.get("min-z");
        Object maxZ = entry.get("max-z");
        if (!(world instanceof String worldName) || worldName.isBlank()
                || !(minX instanceof Number minXNumber) || !(maxX instanceof Number maxXNumber)
                || !(minZ instanceof Number minZNumber) || !(maxZ instanceof Number maxZNumber)) {
            plugin.getLogger().warning("Skipping a chunks.yml area that is missing world or bounds.");
            return null;
        }
        return new BlockedArea(worldName, minXNumber.intValue(), maxXNumber.intValue(), minZNumber.intValue(), maxZNumber.intValue());
    }
}
