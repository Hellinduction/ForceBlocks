package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.forceblock.ForceBlockBase;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.inventory.AbstractInventory;
import club.hellin.forceblocks.inventory.InventoryManager;
import club.hellin.forceblocks.inventory.impl.ForceBlockInventory;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.GeneralConfig;
import club.hellin.forceblocks.utils.WorldEditUtils;
import de.exlll.configlib.YamlConfigurations;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.codec.binary.Base32;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

@Getter
@ToString
public final class ForceBlock implements ForceBlockBase {
    public static final Material DEFAULT_MATERIAL = Material.CRYING_OBSIDIAN;

    private static final double PUSH_STRENGTH = 1.2D;
    private static final double PULL_STRENGTH = 1.2D;
    private static final double WHIRLPOOL_STRENGTH = 0.3;

    private static final int MAX_SPHERE_RADIUS = 100; // This is only for the particle sphere
    public static final File DIR = new File(Main.instance.getDataFolder(), "blocks");

    private final Map<Projectile, UUID> originalShooterMap = new HashMap<>();

    private File configFile;
    private ForceBlockConfig config;

//    private @ToString.Exclude List<Location> sphere;
    private long second = 0;
    private Hologram hologram;
//    private List<BukkitTask> particleTasks = new ArrayList<>();
    private boolean particlesDisplaying;
    private boolean deleted;

    public ForceBlock(final Location location, final int radius, final UUID owner, final Material material) {
        this.config = new ForceBlockConfig();

        this.config.setLocation(location);
        this.config.setRadius(radius);
        this.config.setOwner(owner);
        this.config.setMaterial(material);

        this.hologram = this.spawnHologram();
        this.save();

        ForceBlockManager.getInstance().register(this);
    }

    public ForceBlock(final ForceBlockConfig config) {
        this.config = config;
        this.hologram = this.spawnHologram();

        if (this.config.getMaterial() == null) {
            final Location loc = this.getLocation();
            final Block block = loc.getBlock();

            if (block == null || block.getType() == Material.AIR)
                this.config.setMaterial(DEFAULT_MATERIAL);
            else
                this.config.setMaterial(block.getType());

            this.save();
        }

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

    private Hologram createHologram(final Location loc) {
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
    public void delete(final Player player) {
        this.closeAll();

        this.deleted = true;

        if (this.hologram != null) {
            this.hologram.hideAll();
            this.hologram.delete();
        }

        this.particlesDisplaying = false;

        this.getConfigFile().delete();
        ForceBlockManager.getInstance().remove(this);
    }

    public void closeAll() {
        final List<AbstractInventory> inventories = InventoryManager.getInstance().getInventories(this);

        for (final AbstractInventory inventory : inventories) {
            for (final AbstractInventory.OpenSession session : inventory.getOpen().values()) {
                final Player p = session.getPlayer();

                if (p == null || !p.isOnline())
                    continue;

                p.closeInventory();
            }
        }
    }

    public void close(final Player player) {
        final UUID uuid = player.getUniqueId();
        final List<AbstractInventory> inventories = InventoryManager.getInstance().getInventories(this);

        for (final AbstractInventory inventory : inventories) {
            if (!inventory.getOpen().keySet().contains(uuid))
                continue;

            player.closeInventory();
            break;
        }
    }

    @Override
    public void save() {
        final Class<ForceBlockConfig> configClass = (Class<ForceBlockConfig>) this.getConfig().getClass();
        final File config = this.getConfigFile();

        if (!config.exists())
            YamlConfigurations.update(config.toPath(), configClass, GeneralConfig.PROPERTIES);

        YamlConfigurations.save(config.toPath(), configClass, configClass.cast(this.getConfig()), GeneralConfig.PROPERTIES);
    }

    @Override
    public void openGui(final Player player) {
        player.closeInventory();

        final ForceBlockInventory forceBlockInventory = (ForceBlockInventory) InventoryManager.getInstance().getInventory("FORCE_BLOCK");
        final Inventory inventory = forceBlockInventory.createInventory(player, this);

        player.openInventory(inventory);
    }

    @Override
    public File getConfigFile() {
        if (this.configFile == null)
            this.configFile = new File(DIR, String.format("%s.yml", convertLocationToFileName(this.config.getLocation())));

        return this.configFile;
    }

    @Override
    public void everySecond() {
        if (this.second % 8 == 0)
            this.displayParticles();

        ++this.second;
    }

    private boolean pushEntity(final Entity entity) {
        if (entity.hasMetadata("NPC"))
            return false;

        if (entity instanceof Projectile && this.config.isAffectProjectiles())
            return true;

        if (entity instanceof Monster && this.config.isAffectHostileMobs())
            return true;

        if (entity instanceof Player && this.config.isAffectPlayers())
            return true;

        if (entity instanceof Mob && this.config.isAffectNonHostileMobs())
            return (entity instanceof Monster && this.config.isAffectHostileMobs()) || !(entity instanceof Monster);

        if ((entity instanceof TNTPrimed || entity instanceof Explosive || entity instanceof ExplosiveMinecart) && this.config.isAffectExplosives())
            return true;

        return false;
    }

    @Override
    public void tick() {
        final Location loc = this.getLocation();
        final Chunk chunk = loc.getChunk();

        if (!chunk.isLoaded())
            return;

        final int radius = this.config.getRadius() * 2; // Times by 2 to ensure we capture all players within the sphere

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius / 2, radius)) {
            if (!this.pushEntity(entity))
                continue;

            final ForceBlock forceBlock = ForceBlockManager.getInstance().getClosestForceBlock(entity.getLocation());
            if (forceBlock != null && !forceBlock.equals(this) && !forceBlock.isOff())
                continue;

            if ((this.isPermitted(entity.getUniqueId()) && !this.config.isAffectTrustedPlayers()) || GeneralConfig.getInstance().getBypassList().contains(entity.getUniqueId()))
                continue;

            // Deflecting projectiles
            if (entity instanceof Projectile && this.config.isAffectProjectiles()) {
                final Projectile projectile = (Projectile) entity;
                final ProjectileSource source = projectile.getShooter();

                if (source instanceof Player && !this.config.isAffectPlayers())
                    continue;

                if (source instanceof Player && this.config.isAffectPlayers()) {
                    final Player shooter = (Player) source;
                    final Player originalShooter = Bukkit.getPlayer(this.originalShooterMap.getOrDefault(projectile, shooter.getUniqueId()));

                    if (originalShooter != null && this.isPermitted(originalShooter) && !this.config.isAffectTrustedPlayers())
                        continue;

                    final Player owner = Bukkit.getPlayer(this.config.getOwner());

                    if (owner != null && owner.isOnline() && !shooter.equals(owner) && !(projectile instanceof EnderPearl) && !(projectile instanceof Trident)) {
                        projectile.setShooter(owner);
                        this.originalShooterMap.put(projectile, shooter.getUniqueId());
                    }
                }

                final boolean isNonHostile = source instanceof Mob && !(source instanceof Monster);
                final boolean isPlayer = source instanceof Player;

                if (!isPlayer && isNonHostile && !this.config.isAffectNonHostileMobs())
                    continue;

                if (!isPlayer && !isNonHostile && !this.config.isAffectHostileMobs())
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

                case WHIRLPOOL: {
                    this.whirlpool(entity);
                    break;
                }
            }
        }
    }

    private boolean isOff() {
        return this.getConfig().getMode() == ForceMode.OFF || this.isDeleted();
    }

    @Override
    public void displayParticles() {
        if (this.isOff())
            return;

//        final List<Location> blocks = this.getSphere();

        if (this.particlesDisplaying)
            return;

        final Location loc = this.getLocation();
        final Chunk chunk = loc.getChunk();

        if (!chunk.isLoaded())
            return;

//        if (blocks.isEmpty())
//            return;

//        Collections.sort(blocks, (loc1, loc2) -> {
//            if (loc1.getY() != loc2.getY())
//                return Double.compare(loc1.getY(), loc2.getY());
//            if (loc1.getBlockX() != loc2.getBlockX())
//                return Double.compare(loc1.getBlockX(), loc2.getBlockX());
//            return Double.compare(loc1.getZ(), loc2.getZ());
//        });

        final Particle particle = Particle.HEART;
        final Location center = ForceBlockListeners.center(loc.clone());
        final Location originalCenter = center.clone();

        center.add(0, 5, 0);

        final int configRadius = this.getConfig().getRadius();
        final int radius = configRadius > MAX_SPHERE_RADIUS ? MAX_SPHERE_RADIUS : configRadius;

        new BukkitRunnable() {
            private Location loc = center.clone().add(radius, 0, 0);

            @Override
            public void run() {
                final Vector radiusVector = this.loc.toVector().subtract(center.toVector());

                final Vector axis = new Vector(0, 1, 0);
                final double angle = WHIRLPOOL_STRENGTH / radiusVector.length();
                final Vector rotatedRadius = radiusVector.clone().rotateAroundAxis(axis, angle);

                final Vector velocity = rotatedRadius.subtract(radiusVector);

                final double strength = 0.5;
                final Vector pullVector = radiusVector.normalize().multiply(strength);

                final Vector newVector = velocity.add(pullVector);

                final Location newLoc = center.clone().add(newVector).subtract(0, 0.1D, 0);
                this.loc = newLoc;
                center.subtract(0, 0.1D, 0);

                try {
                    newVector.checkFinite();
                } catch (final RuntimeException ignored) {
                    return;
                }

                final double dist = this.loc.distance(originalCenter);

                if (dist < 1.1D || isOff()) {
                    super.cancel();
                    particlesDisplaying = false;
                    return;
                }

                this.loc.getWorld().spawnParticle(particle, this.loc, 1);
            }
        }.runTaskTimer(Main.instance, 0L, 3L);

        this.particlesDisplaying = true;

//        final int loopSize = blocks.size();
//        final BukkitTask up = new BukkitRunnable() {
//            int index = 0;
//
//            @Override
//            public void run() {
//                if (index >= loopSize) {
//                    super.cancel();
//                    particleTasks.remove(this);
//                    return;
//                }
//
//                final Location loc = blocks.get(index);
//                loc.getWorld().spawnParticle(particle, loc, 1);
//                index++;
//            }
//        }.runTaskTimer(Main.instance, 0, 3);
//
//        final BukkitTask down = new BukkitRunnable() {
//            int index = loopSize - 1;
//
//            @Override
//            public void run() {
//                if (index < 0) {
//                    super.cancel();
//                    particleTasks.remove(this);
//                    return;
//                }
//
//                final Location loc = blocks.get(index);
//                loc.getWorld().spawnParticle(particle, loc, 1);
//                index--;
//            }
//        }.runTaskTimer(Main.instance, 0, 3);
//
//        this.particleTasks.add(up);
//        this.particleTasks.add(down);
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
        final Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline())
            this.close(player);

        this.getTrusted().remove(uuid);
        this.save();
    }

//    @Override
//    public List<Location> getSphere() {
//        final int configRadius = this.getConfig().getRadius();
//        final int radius = configRadius > MAX_SPHERE_RADIUS ? MAX_SPHERE_RADIUS : configRadius;
//
//        if (this.sphere != null)
//            return this.sphere;
//
//        final Location center = this.getLocation();
//        final List<Location> sphere = WorldEditUtils.makeSphere(center.getWorld(), center.toVector(), radius);
//        return this.sphere = sphere;
//    }

    @Override
    public boolean isOwner(final UUID uuid) {
        return this.config.getOwner().equals(uuid) || GeneralConfig.getInstance().getBypassList().contains(uuid);
    }

    private static final String SEPARATOR = ";";

    public static String convertLocationToFileName(final Location loc) {
        final StringBuilder builder = new StringBuilder();

        final int x = (int) Math.round(loc.getX());
        final int y = (int) Math.round(loc.getY());
        final int z = (int) Math.round(loc.getZ());

        builder.append(loc.getWorld().getName());
        builder.append(SEPARATOR);
        builder.append(x);
        builder.append(SEPARATOR);
        builder.append(y);
        builder.append(SEPARATOR);
        builder.append(z);

        final String str = builder.toString();
        return new Base32().encodeToString(str.getBytes()).replaceAll("=", "");
    }

    public static Location convertFileNameToLocation(String name) {
        int paddingLength = name.length() % 8;

        if (paddingLength > 0) {
            paddingLength = 8 - paddingLength;

            for (int i = 0; i < paddingLength; ++i)
                name += "=";
        }

        name = new String(new Base32().decode(name));

        final String[] splits = name.split(SEPARATOR);
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

        final ForceBlockConfig config = YamlConfigurations.load(configFile.toPath(), configClass, GeneralConfig.PROPERTIES);
        return config;
    }

    @Override
    public void forceField(final Entity nearby) {
        final Location nearbyLoc = nearby.getLocation();
        final Location center = this.getLocation();

        final Vector awayFromCenter = new Vector(nearbyLoc.getX() - center.getX(), nearbyLoc.getY() - center.getY(), nearbyLoc.getZ() - center.getZ());
        awayFromCenter.normalize();
        awayFromCenter.setY(awayFromCenter.getY() + 0.1D);
        awayFromCenter.multiply(PUSH_STRENGTH);

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
        double strength = PULL_STRENGTH * Math.exp(-distance / maxDistance);
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

        if (strength < 0.01)
            strength = 0.01;

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
    public void whirlpool(final Entity entity) {
        final Location center = ForceBlockListeners.center(this.getLocation());
        final Vector radius = entity.getLocation().toVector().subtract(center.toVector());

        final Vector axis = new Vector(0, 1, 0);
        final double angle = WHIRLPOOL_STRENGTH / radius.length();
        final Vector rotatedRadius = radius.clone().rotateAroundAxis(axis, angle);

        final Vector velocity = rotatedRadius.subtract(radius);

        final boolean isPlayer = entity instanceof Player;
        final double pullStrength = isPlayer ? 0.06 : 0.03;
        final Vector pullVector = radius.normalize().multiply(-pullStrength);

        final Vector newVector = velocity.add(pullVector);

        try {
            newVector.checkFinite();
        } catch (final RuntimeException ignored) {
            return;
        }

        entity.setVelocity(newVector);
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