package uk.globeworks.townyblock;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uk.globeworks.townyblock.chunk.BlockedChunkStore;
import uk.globeworks.townyblock.command.TownyBlockCommand;
import uk.globeworks.townyblock.listener.ClaimBlockListener;
import uk.globeworks.townyblock.listener.PreviewListener;
import uk.globeworks.townyblock.preview.PreviewManager;

import java.util.IllegalFormatException;

public final class TownyBlockPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private BlockedChunkStore store;
    private PreviewManager previews;

    @Override
    public void onEnable() {
        getLogger().info(Globeworks.logo("TownyBlock", getDescription().getVersion()));
        saveDefaultConfig();
        applyMessageDefaults();
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create the plugin data folder.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        store = new BlockedChunkStore(this);
        if (!store.load()) {
            getLogger().severe("TownyBlock is disabled because chunks.yml could not be read.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        TownyBlockCommand command = new TownyBlockCommand(this);
        PluginCommand pluginCommand = getCommand("townyblock");
        if (pluginCommand == null) {
            getLogger().severe("Command townyblock is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        previews = new PreviewManager(this);
        getServer().getPluginManager().registerEvents(new ClaimBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PreviewListener(this), this);
        getLogger().info("Protecting " + store.size() + " chunks from town claims.");
    }

    @Override
    public void onDisable() {
        if (previews != null) {
            previews.clearAll();
        }
    }

    public BlockedChunkStore store() {
        return store;
    }

    public PreviewManager previews() {
        return previews;
    }

    public int maxRadius() {
        return Math.min(256, Math.max(0, getConfig().getInt("max-radius", 48)));
    }

    public String blockedClaimMessage() {
        String message = getConfig().getString(
                "messages.blocked-claim",
                "This area is protected and cannot be claimed."
        );
        if (message == null || message.isBlank()) {
            return "This area is protected and cannot be claimed.";
        }
        try {
            // /town claim formats this with two integers. Reject a string that would throw.
            String.format(message, 1, 1);
            return message;
        } catch (IllegalFormatException exception) {
            getLogger().warning("messages.blocked-claim is not a valid message. Using the default.");
            return "This area is protected and cannot be claimed.";
        }
    }

    private void applyMessageDefaults() {
        var config = getConfig();
        config.addDefault("messages.preview-ready", "&aPreviewing &f%count% &achunks in a radius of &f%radius%&a. Red dust marks every block. Walk around, then &f/townyblock confirm &aor &f/townyblock cancel&a.");
        config.addDefault("messages.preview-replaced", "&eCleared the previous preview.");
        config.addDefault("messages.confirmed", "&aBlocked &f%count% &achunks. &7%already% were already blocked.");
        config.addDefault("messages.already-area", "&eThat area is already blocked.");
        config.addDefault("messages.nothing-to-confirm", "&eYou do not have a preview. Use &f/townyblock radius <radius>&e.");
        config.addDefault("messages.cancelled", "&aCleared the preview.");
        config.addDefault("messages.nothing-to-cancel", "&eYou do not have a preview to clear.");
        config.addDefault("messages.removed-area", "&aRemoved the protected area in &f%world% &7(%minx%, %minz% to %maxx%, %maxz%, %count% chunks)&a.");
        config.addDefault("messages.removed-square", "&aUnblocked &f%areas% &aareas and &f%chunks% &aindividual chunks.");
        config.addDefault("messages.list-area", "&7- &f%world% &7%minx%, %minz% &7to &f%maxx%, %maxz% &7(%count% chunks)");
        config.options().copyDefaults(true);
    }

    public void send(CommandSender sender, String key, Object... tokens) {
        String raw = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            raw = raw.replace("%" + tokens[i] + "%", String.valueOf(tokens[i + 1]));
        }
        String prefix = getConfig().getString("messages.prefix", "");
        for (String line : raw.split("\n", -1)) {
            sender.sendMessage(LEGACY.deserialize(prefix + line));
        }
    }
}
