package club.hellin.forceblocks.hologram.impl;

import club.hellin.forceblocks.hologram.ForceBlockHologram;
import club.hellin.forceblocks.hologram.HologramProvider;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;

import java.util.ArrayList;
import java.util.List;

public final class FancyHologramsProvider implements HologramProvider {
    private static final int TEXT_UPDATE_INTERVAL_MS = 1000; // Keeps the mode placeholder up to date

    @Override
    public String getPluginName() {
        return "FancyHolograms";
    }

    @Override
    public ForceBlockHologram getOrCreate(final String name, final Location location, final List<String> lines) {
        final HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        final Hologram hologram = manager.getHologram(name).orElseGet(() -> {
            final TextHologramData data = new TextHologramData(name, location);

            data.setText(new ArrayList<>(lines));
            data.setTextUpdateInterval(TEXT_UPDATE_INTERVAL_MS);
            data.setBillboard(Display.Billboard.CENTER);
            data.setPersistent(false);

            final Hologram created = manager.create(data);
            manager.addHologram(created);

            return created;
        });

        return () -> manager.removeHologram(hologram);
    }
}
