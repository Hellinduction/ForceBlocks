package club.hellin.forceblocks.utils.papi;

import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ForceBlocksExpansion extends PlaceholderExpansion {
    private final Plugin plugin;

    public ForceBlocksExpansion(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return this.plugin.getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return this.plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer p, final String placeholder) {
        try {
            final String placeholderLower = placeholder.toLowerCase();
            final String[] splits = placeholder.split("_");

            if (placeholderLower.startsWith("mode_")) {
                final String name = splits[1];
                final Location location = ForceBlock.convertFileNameToLocation(name);
                final ForceBlock block = ForceBlockManager.getInstance().getForceBlock(location);

                if (block == null)
                    return "";

                return block.getConfig().getMode().name();
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
        }

        return "";
    }
}