package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.forceblock.ForceBlockBase;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.WorldEditUtils;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

@Getter
@ToString
public final class ForceBlock implements ForceBlockBase {
    private static final YamlConfigurationProperties PROPERTIES = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
    private static final double PUSH_DISTANCE = 1.2D;
    private static final double PULL_DISTANCE = 1.2D;
    public static final File DIR = new File(Main.instance.getDataFolder(), "blocks");

    private File configFile;
    private ForceBlockConfig config;

    private @ToString.Exclude List<Location> sphere;
    private long second = 0;
    private Hologram hologram;
    private List<BukkitTask> particleTasks = new ArrayList<>();
    private boolean deleted;

    public ForceBlock(final Location location, final int radius, final UUID owner) {
        this.config = new ForceBlockConfig();

        this.config.setLocation(location);
        this.config.setRadius(radius);
        this.config.setOwner(owner);

        this.hologram = this.spawnHologram();
        this.save();

        ForceBlockManager.getInstance().register(this);
    }

    public ForceBlock(final ForceBlockConfig config) {
        this.config = config;
        this.hologram = this.spawnHologram();

        ForceBlockManager.getInstance().register(this);
    }

    private Hologram spawnHologram() {
        Location loc = this.getLocation();

        loc = loc.clone().add(0, 2, 0);
        loc = ForceBlockListeners.center(loc);

        final String name = this.getHologramName(loc);
        final Hologram hologram = DHAPI.getHologram(name);

        if (hologram != null)
            return hologram;

        return this.createHologram(loc);
    }

    private Hologram createHologram(Location loc) {
        final String name = this.getHologramName(loc);
        final OfflinePlayer owner = Bukkit.getOfflinePlayer(this.config.getOwner());

        final Hologram hologram = DHAPI.createHologram(name, loc, Arrays.asList(ChatColor.translateAlternateColorCodes('&', String.format("&e%s&b%s Force Block", owner.getName(), owner.getName().toLowerCase().endsWith("s") ? "'" : "'s")),
                ChatColor.translateAlternateColorCodes('&', String.format("&bMode:&e %%forceblocks_mode_%s%%", convertLocationToFileName(this.config.getLocation())))));
        return hologram;
    }

    private String getHologramName(final Location loc) {
        final String name = convertLocationToFileName(loc);
        return name;
    }

    @Override
    public Location getLocation() {
        return this.config.getLocation();
    }

    @Override
    public void delete() {
        this.deleted = true;

        if (this.hologram != null) {
            this.hologram.hideAll();
            this.hologram.delete();
        }

        for (final BukkitTask task : this.particleTasks)
            task.cancel();

        this.getConfigFile().delete();
        ForceBlockManager.getInstance().remove(this);
    }

    @Override
    public void save() {
        final Class<ForceBlockConfig> configClass = (Class<ForceBlockConfig>) this.getConfig().getClass();
        final File config = this.getConfigFile();

        if (!config.exists())
            YamlConfigurations.update(config.toPath(), configClass, PROPERTIES);

        YamlConfigurations.save(config.toPath(), configClass, configClass.cast(this.getConfig()), PROPERTIES);
    }

    @Override
    public File getConfigFile() {
        if (this.configFile == null)
            this.configFile = new File(DIR, String.format("%s.yml", convertLocationToFileName(this.config.getLocation())));

        return this.configFile;
    }

    @Override
    public void everySecond() {
        if (this.second % 16 == 0)
            this.displayParticles();

        ++this.second;
    }

    @Override
    public void everyTick() {
        final Location loc = this.getLocation();
        final int radius = this.config.getRadius() * 2;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius / 2, radius)) {
            if (!(entity instanceof Projectile) && !(entity instanceof Monster) && !(entity instanceof Player))
                continue;

            final ForceBlock forceBlock = ForceBlockManager.getInstance().getClosestForceBlock(entity.getLocation());
            if (forceBlock != null && !forceBlock.equals(this))
                continue;

            if (this.isPermitted(entity.getUniqueId()))
                continue;

            if (entity instanceof Projectile) {
                final Projectile projectile = (Projectile) entity;
                final ProjectileSource source = projectile.getShooter();

                if (source instanceof Player) {
                    final Player shooter = (Player) source;
                    if (shooter != null && this.isPermitted(shooter))
                        continue;
                }

                if (source instanceof Mob && !(source instanceof Monster))
                    continue;
            }

            Location location = entity.getLocation();
            location = location.getBlock().getLocation();

            if (location.distance(this.getLocation()) > this.getConfig().getRadius())
                continue;

            if (entity.isInsideVehicle())
                entity = entity.getVehicle();

            switch (this.config.getMode()) {
                case MAGNET: {
                    this.magnet(entity);
                    break;
                }

                case FORCE_FIELD: {
                    this.forceField(entity);
                    break;
                }
            }
        }
    }

    @Override
    public void displayParticles() {
        final List<Location> blocks = this.getSphere();

        if (!this.particleTasks.isEmpty())
            return;

        if (blocks.isEmpty())
            return;

        Collections.sort(blocks, (loc1, loc2) -> {
            if (loc1.getY() != loc2.getY())
                return Double.compare(loc1.getY(), loc2.getY());
            if (loc1.getBlockX() != loc2.getBlockX())
                return Double.compare(loc1.getBlockX(), loc2.getBlockX());
            return Double.compare(loc1.getZ(), loc2.getZ());
        });

        final Particle particle = Particle.HEART;

        final int loopSize = blocks.size();
        final BukkitTask up = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= loopSize) {
                    super.cancel();
                    particleTasks.remove(this);
                    return;
                }

                final Location loc = blocks.get(index);
                loc.getWorld().spawnParticle(particle, loc, 1);
                index++;
            }
        }.runTaskTimer(Main.instance, 0, 3);

        final BukkitTask down = new BukkitRunnable() {
            int index = loopSize - 1;

            @Override
            public void run() {
                if (index < 0) {
                    super.cancel();
                    particleTasks.remove(this);
                    return;
                }

                final Location loc = blocks.get(index);
                loc.getWorld().spawnParticle(particle, loc, 1);
                index--;
            }
        }.runTaskTimer(Main.instance, 0, 3);

        this.particleTasks.add(up);
        this.particleTasks.add(down);
    }

    @Override
    public void trust(final UUID uuid) {
        if (!this.getTrusted().contains(uuid))
            this.getTrusted().add(uuid);

        this.save();
    }

    @Override
    public List<UUID> getTrusted() {
        return this.config.getTrusted();
    }

    @Override
    public void unTrust(final UUID uuid) {
        this.getTrusted().remove(uuid);
        this.save();
    }

    @Override
    public List<Location> getSphere() {
        if (this.sphere != null)
            return this.sphere;

        final Location center = this.getLocation();
        final List<Location> sphere = WorldEditUtils.makeSphere(center.getWorld(), center.toVector(), this.config.getRadius());
        return this.sphere = sphere;
    }

    @Override
    public boolean isOwner(final UUID uuid) {
        return this.config.getOwner().equals(uuid);
    }

    public static String convertLocationToFileName(final Location loc) {
        final StringBuilder builder = new StringBuilder();

        final int x = (int) Math.round(loc.getX());
        final int y = (int) Math.round(loc.getY());
        final int z = (int) Math.round(loc.getZ());

        builder.append(loc.getWorld().getName());
        builder.append("Q");
        builder.append(x);
        builder.append("Q");
        builder.append(y);
        builder.append("Q");
        builder.append(z);

        return builder.toString();
    }

    public static Location convertFileNameToLocation(final String name) {
        final String[] splits = name.split("Q");
        final String worldName = splits[0];
        final World world = Bukkit.getWorld(worldName);
        final int x = Integer.parseInt(splits[1]);
        final int y = Integer.parseInt(splits[2]);
        final int z = Integer.parseInt(splits[3]);

        final Location location = new Location(world, x, y, z);
        return location;
    }

    public static ForceBlockConfig load(final File configFile) {
        final Class<? extends ForceBlockConfig> configClass = ForceBlockConfig.class;

        final ForceBlockConfig config = YamlConfigurations.load(configFile.toPath(), configClass, PROPERTIES);
        return config;
    }

    @Override
    public void forceField(final Entity nearby) {
        final Location nearbyLoc = nearby.getLocation();
        final Location center = this.getLocation();

        final Vector awayFromCenter = new Vector(nearbyLoc.getX() - center.getX(), nearbyLoc.getY() - center.getY(), nearbyLoc.getZ() - center.getZ());
        awayFromCenter.normalize();
        awayFromCenter.setY(awayFromCenter.getY() + 0.1D);
        awayFromCenter.multiply(PUSH_DISTANCE);

        try {
            awayFromCenter.checkFinite();
        } catch (final RuntimeException ignored) {
            return;
        }

        try {
            nearby.setVelocity(awayFromCenter);
        } catch (final Exception ignored) {}
    }

    @Override
    public void magnet(final Entity nearby) {
        final Location nearbyLoc = nearby.getLocation();
        final Location center = this.getLocation();

        final double distance = center.distance(nearbyLoc);

        final double maxDistance = 10.0; // Adjust this value based on your preference
        double strength = PULL_DISTANCE * Math.exp(-distance / maxDistance);
//
//        if (distance < 3 * (this.config.getRadius() / 2))
//            strength *= (1 - Math.pow(distance / maxDistance, 2));
//        else
//            strength *= (1 - Math.pow(distance / maxDistance, 2)) / 4; // Gradual increase as it gets closer

        if (!nearby.isOnGround())
            strength *= 2;

        if (distance < 1.5)
            strength = 0.2;

        // Ensure strength is always positive
        strength = Math.abs(strength);

        // Calculate direction towards the center of the magnet
        final Vector towardsCenter = center.toVector().subtract(nearbyLoc.toVector());
        towardsCenter.normalize();

        // Multiply by strength
        towardsCenter.multiply(strength);

        try {
            towardsCenter.checkFinite();
        } catch (final RuntimeException ignored) {
            return;
        }

        nearby.setVelocity(towardsCenter);
    }

    @Override
    public boolean isPermitted(final UUID uuid) {
        return this.isOwner(uuid) || this.getConfig().getTrusted().contains(uuid);
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof ForceBlock))
            return false;

        final ForceBlock forceBlock = (ForceBlock) object;
        return forceBlock.getLocation().equals(this.getLocation());
    }
}