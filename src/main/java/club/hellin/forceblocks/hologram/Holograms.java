package club.hellin.forceblocks.hologram;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.hologram.impl.DecentHologramsProvider;
import club.hellin.forceblocks.hologram.impl.FancyHologramsProvider;
import club.hellin.forceblocks.utils.GeneralConfig;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Uses DecentHolograms or FancyHolograms, whichever is installed
 */
@UtilityClass
public final class Holograms {
    private HologramProvider provider;
    private boolean resolved;

    public ForceBlockHologram getOrCreate(final String name, final Location location, final List<String> lines) {
        final HologramProvider provider = getProvider();

        if (provider == null)
            return null;

        try {
            return provider.getOrCreate(name, location, lines);
        } catch (final Throwable throwable) {
            Main.instance.getLogger().warning(String.format("Failed to create hologram via %s: %s", provider.getPluginName(), throwable));
            return null;
        }
    }

    public HologramProvider getProvider() {
        if (!resolved) {
            provider = resolveProvider();
            resolved = true;

            if (provider != null)
                Main.instance.getLogger().info(String.format("Using %s for holograms", provider.getPluginName()));
        }

        return provider;
    }

    private HologramProvider resolveProvider() {
        final String preferred = GeneralConfig.getInstance().getHologramPlugin().trim().toLowerCase(Locale.ROOT);

        switch (preferred) {
            case "none":
                return null;
            case "decentholograms":
                return create("DecentHolograms", DecentHologramsProvider::new);
            case "fancyholograms":
                return create("FancyHolograms", FancyHologramsProvider::new);
            default: {
                final HologramProvider decent = create("DecentHolograms", DecentHologramsProvider::new);
                return decent != null ? decent : create("FancyHolograms", FancyHologramsProvider::new);
            }
        }
    }

    private HologramProvider create(final String pluginName, final Supplier<HologramProvider> supplier) {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        if (plugin == null || !plugin.isEnabled())
            return null;

        return supplier.get();
    }
}
