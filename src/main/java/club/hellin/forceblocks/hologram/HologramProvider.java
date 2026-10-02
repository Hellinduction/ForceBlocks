package club.hellin.forceblocks.hologram;

import org.bukkit.Location;

import java.util.List;

public interface HologramProvider {
    String getPluginName();

    ForceBlockHologram getOrCreate(final String name, final Location location, final List<String> lines);
}
