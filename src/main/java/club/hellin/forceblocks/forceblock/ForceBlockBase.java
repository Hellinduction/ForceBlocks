package club.hellin.forceblocks.forceblock;

import club.hellin.forceblocks.forceblock.impl.ForceBlockConfig;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface ForceBlockBase {
    Location getLocation();

    void delete(final Player player);

    void save();

    void openGui(final Player player);

    File getConfigFile();

    ForceBlockConfig getConfig();

    void everySecond();

    void tick();

    void displayParticles();

    void trust(final UUID uuid);

    default void trust(final OfflinePlayer player) {
        this.trust(player.getUniqueId());
    }

    List<UUID> getTrusted();

    void unTrust(final UUID uuid);

    default void unTrust(final OfflinePlayer player) {
        this.unTrust(player.getUniqueId());
    }

    List<Location> getSphere();

    boolean isOwner(final UUID uuid);

    default boolean isOwner(final OfflinePlayer player) {
        return this.isOwner(player.getUniqueId());
    }

    void forceField(final Entity entity);

    void magnet(final Entity entity);

    boolean isPermitted(final UUID uuid);

    default boolean isPermitted(final OfflinePlayer player) {
        return this.isPermitted(player.getUniqueId());
    }
}