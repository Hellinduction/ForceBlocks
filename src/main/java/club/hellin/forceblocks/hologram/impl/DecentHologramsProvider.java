package club.hellin.forceblocks.hologram.impl;

import club.hellin.forceblocks.hologram.ForceBlockHologram;
import club.hellin.forceblocks.hologram.HologramProvider;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.List;

public final class DecentHologramsProvider implements HologramProvider {
    @Override
    public String getPluginName() {
        return "DecentHolograms";
    }

    @Override
    public ForceBlockHologram getOrCreate(final String name, final Location location, final List<String> lines) {
        Hologram hologram = DHAPI.getHologram(name);

        if (hologram == null) {
            final List<String> translated = lines.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList();

            hologram = DHAPI.createHologram(name, location, false, translated);
        }

        final Hologram finalHologram = hologram;

        return () -> {
            finalHologram.hideAll();
            finalHologram.delete();
        };
    }
}
