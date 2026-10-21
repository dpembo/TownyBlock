package uk.globeworks.townyblock.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.globeworks.townyblock.TownyBlockPlugin;
import uk.globeworks.townyblock.chunk.BlockedArea;
import uk.globeworks.townyblock.chunk.ChunkPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TownyBlockCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = List.of("add", "radius", "confirm", "cancel", "remove", "list", "reload");

    private final TownyBlockPlugin plugin;

    public TownyBlockCommand(TownyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("townyblock.admin")) {
            plugin.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.send(sender, "usage");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> add(sender, args);
            case "radius" -> radius(sender, args);
            case "confirm" -> confirm(sender);
            case "cancel" -> cancel(sender);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender, args);
            case "reload" -> reload(sender);
            default -> {
                plugin.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean add(CommandSender sender, String[] args) {
        ChunkPosition chunk = parseSingle(sender, args);
        if (chunk == null) {
            return true;
        }
        int added = plugin.store().addAll(Set.of(chunk));
        if (added == 0) {
            plugin.send(sender, "already-blocked", "world", chunk.world(), "x", chunk.x(), "z", chunk.z());
        } else {
            plugin.send(sender, "added", "world", chunk.world(), "x", chunk.x(), "z", chunk.z());
        }
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("radius")) {
            return removeRadius(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        ChunkPosition chunk = parseSingle(sender, args);
        if (chunk == null) {
            return true;
        }
        List<BlockedArea> areas = plugin.store().removeAreasContaining(chunk);
        int removedChunk = plugin.store().removeAll(Set.of(chunk));
        if (areas.isEmpty() && removedChunk == 0) {
            plugin.send(sender, "not-blocked", "world", chunk.world(), "x", chunk.x(), "z", chunk.z());
            return true;
        }
        for (BlockedArea area : areas) {
            plugin.send(sender, "removed-area",
                    "world", area.world(),
                    "minx", area.minX(),
                    "minz", area.minZ(),
                    "maxx", area.maxX(),
                    "maxz", area.maxZ(),
                    "count", area.chunkCount());
        }
        if (removedChunk > 0) {
            plugin.send(sender, "removed", "world", chunk.world(), "x", chunk.x(), "z", chunk.z());
        }
        return true;
    }

    private boolean radius(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "players-only");
            return true;
        }
        if (args.length < 2) {
            plugin.send(sender, "invalid-radius", "max", plugin.maxRadius());
            return true;
        }
        Integer radius = parseRadius(args[1]);
        if (radius == null) {
            plugin.send(sender, "invalid-radius", "max", plugin.maxRadius());
            return true;
        }
        ChunkPosition centre = standingChunk(player);
        BlockedArea area = new BlockedArea(
                centre.world(),
                centre.x() - radius,
                centre.x() + radius,
                centre.z() - radius,
                centre.z() + radius
        );
        boolean replaced = plugin.previews().start(player, area);
        if (replaced) {
            plugin.send(sender, "preview-replaced");
        }
        plugin.send(sender, "preview-ready", "count", area.chunkCount(), "radius", radius);
        return true;
    }

    private boolean confirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "players-only");
            return true;
        }
        BlockedArea area = plugin.previews().confirm(player);
        if (area == null) {
            plugin.send(sender, "nothing-to-confirm");
            return true;
        }
        int already = 0;
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                if (plugin.store().isBlocked(new ChunkPosition(area.world(), x, z))) {
                    already++;
                }
            }
        }
        if (!plugin.store().addArea(area)) {
            plugin.send(sender, "already-area");
            return true;
        }
        plugin.send(sender, "confirmed", "count", area.chunkCount() - already, "already", already);
        return true;
    }

    private boolean cancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "players-only");
            return true;
        }
        if (!plugin.previews().discard(player)) {
            plugin.send(sender, "nothing-to-cancel");
            return true;
        }
        plugin.send(sender, "cancelled");
        return true;
    }

    private boolean removeRadius(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.send(sender, "invalid-radius", "max", plugin.maxRadius());
            return true;
        }
        Integer radius = parseRadius(args[1]);
        if (radius == null) {
            plugin.send(sender, "invalid-radius", "max", plugin.maxRadius());
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "players-only");
            return true;
        }
        ChunkPosition centre = standingChunk(player);
        int[] removed = plugin.store().removeSquare(
                centre.world(),
                centre.x() - radius,
                centre.x() + radius,
                centre.z() - radius,
                centre.z() + radius
        );
        if (removed[0] == 0 && removed[1] == 0) {
            plugin.send(sender, "none-blocked");
            return true;
        }
        plugin.send(sender, "removed-square", "areas", removed[0], "chunks", removed[1]);
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        List<BlockedArea> areas = plugin.store().areas();
        List<ChunkPosition> chunks = plugin.store().all();
        int total = areas.size() + chunks.size();
        if (total == 0) {
            plugin.send(sender, "list-empty");
            return true;
        }
        int pageSize = 8;
        int pages = (total + pageSize - 1) / pageSize;
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                page = 1;
            }
        }
        page = Math.max(1, Math.min(page, pages));
        plugin.send(sender, "list-header", "count", total, "page", page, "pages", pages);
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        for (int i = from; i < to; i++) {
            if (i < areas.size()) {
                BlockedArea area = areas.get(i);
                plugin.send(sender, "list-area",
                        "world", area.world(),
                        "minx", area.minX(),
                        "minz", area.minZ(),
                        "maxx", area.maxX(),
                        "maxz", area.maxZ(),
                        "count", area.chunkCount());
            } else {
                ChunkPosition chunk = chunks.get(i - areas.size());
                plugin.send(sender, "list-line", "world", chunk.world(), "x", chunk.x(), "z", chunk.z());
            }
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadConfig();
        if (!plugin.store().load()) {
            plugin.send(sender, "reload-failed");
            return true;
        }
        plugin.send(sender, "reloaded", "count", plugin.store().size());
        return true;
    }

    /**
     * No extra args uses the player's chunk. Two numbers use the player's world.
     * Three args are world, chunk x, chunk z, which also works from the console.
     *
     * @return null when the sender has already been told why the arguments were rejected
     */
    private ChunkPosition parseSingle(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                plugin.send(sender, "players-only");
                return null;
            }
            return standingChunk(player);
        }
        if (args.length == 3) {
            if (!(sender instanceof Player player)) {
                plugin.send(sender, "players-only");
                return null;
            }
            Integer x = parseInt(args[1]);
            Integer z = parseInt(args[2]);
            if (x == null || z == null) {
                plugin.send(sender, "invalid-coords");
                return null;
            }
            return new ChunkPosition(player.getWorld().getName(), x, z);
        }
        if (args.length == 4) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                plugin.send(sender, "unknown-world", "world", args[1]);
                return null;
            }
            Integer x = parseInt(args[2]);
            Integer z = parseInt(args[3]);
            if (x == null || z == null) {
                plugin.send(sender, "invalid-coords");
                return null;
            }
            return new ChunkPosition(world.getName(), x, z);
        }
        plugin.send(sender, "usage");
        return null;
    }

    private static ChunkPosition standingChunk(Player player) {
        return new ChunkPosition(player.getWorld().getName(), player.getChunk().getX(), player.getChunk().getZ());
    }

    private Integer parseRadius(String text) {
        Integer radius = parseInt(text);
        if (radius == null || radius < 0 || radius > plugin.maxRadius()) {
            return null;
        }
        return radius;
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("townyblock.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return partial(ROOT, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("radius") || sub.equals("list"))) {
            return partial(List.of("1", "2", "3", "5", "10"), args[1]);
        }
        if ((sub.equals("add") || sub.equals("remove")) && args.length == 2) {
            List<String> options = worldNames();
            if (sub.equals("remove")) {
                options.add(0, "radius");
            }
            return partial(options, args[1]);
        }
        return List.of();
    }

    private static List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }

    private static List<String> partial(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
