package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.forceblock.ForceBlockBase;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.inventory.AbstractInventory;
import club.hellin.forceblocks.inventory.InventoryManager;
import club.hellin.forceblocks.inventory.impl.ForceBlockInventory;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.GeneralConfig;
import de.exlll.configlib.YamlConfigurations;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.ToString;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import me.nahu.scheduler.wrapper.task.WrappedTask;
import org.apache.commons.codec.binary.Base32;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private final WrappedTask timer;

    private File configFile;
    private ForceBlockConfig config;

    private long second = 0;
    private Hologram hologram;
    private boolean particlesDisplaying; // Used to check if particles from previous BukkitRunnable are still being displayed
    private boolean deleted;
    private int ticks = 0;

    private Chunk chunk;
    private CompletableFuture<List<Chunk>> surroundingChunks;

    public ForceBlock(final Location location, final int radius, final UUID owner, final Material material) {
        this.config = new ForceBlockConfig();

        this.config.setLocation(location);
        this.config.setRadius(radius);
        this.config.setOwner(owner);
        this.config.setMaterial(material);

        this.hologram = this.spawnHologram();
        this.save();

        ForceBlockManager.getInstance().register(this);

        this.timer = this.getTimer();
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

        this.timer = this.getTimer();
    }

    private WrappedTask getTimer() {
        return Main.instance.getScheduler().runTaskTimerAsynchronously(() -> {
            if (this.ticks % 2 == 0)
                Main.instance.getScheduler().runTaskAsynchronously(this::tick);
            else
                this.tick();
        }, 1L, 1L);
    }

    private Hologram spawnHologram() {
        if (!this.config.isDisplayHologram())
            return null;

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

    public void toggleHologram() {
        this.config.setDisplayHologram(!this.config.isDisplayHologram());
        this.save();

        final boolean displayHologram = this.config.isDisplayHologram();

        if (displayHologram)
            this.hologram = this.spawnHologram();
        else
            this.deleteHologram();
    }

    @Override
    public Location getLocation() {
        return this.config.getLocation();
    }

    @Override
    public void delete(final Player player) {
        this.closeAll();

        this.deleted = true;

        this.deleteHologram();

        this.particlesDisplaying = false;

        this.getConfigFile().delete();
        ForceBlockManager.getInstance().remove(this);

        this.timer.cancel();
    }

    public void deleteHologram() {
        if (this.hologram != null) {
            this.hologram.hideAll();
            this.hologram.delete();
        }
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

        System.out.println(this.ticks);
        this.ticks = 0;
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

    private boolean isForceBlockCloser(final Entity entity) {
        final Location loc = entity.getLocation();
        final ForceBlock thisForceBlock = this;
        final ForceBlock closestForceBlock = ForceBlockManager.getInstance().getClosestForceBlock(loc);

        if (closestForceBlock == null || closestForceBlock.equals(thisForceBlock) || closestForceBlock.isOff())
            return false;

        final double thisDist = thisForceBlock.getLocation().distance(loc);
        final double closestDist = closestForceBlock.getLocation().distance(loc);
        final double thisRadius = thisForceBlock.getConfig().getRadius();
        final double closestRadius = closestForceBlock.getConfig().getRadius();

        if (thisRadius > closestRadius * 2)
            return false;

        // If the closest force block is larger and closer or equally close, return true
        if (closestRadius > thisRadius && closestDist <= closestRadius)
            return true;
            // If the current force block is larger and closer or equally close, return false
        else if (thisRadius > closestRadius && thisDist <= thisRadius)
            return false;
            // Otherwise, return based on distance only
        else
            return closestDist < thisDist;
    }

    private boolean allowedAsPermitted(final Entity entity) {
        final UUID uuid = entity.getUniqueId();

        final boolean isOwner = this.isOwner(uuid);
        final boolean affectOwner = this.config.isAffectOwner();
        final boolean affectedTrusted = this.config.isAffectTrustedPlayers();

        if (isOwner) {
            if (!affectOwner)
                return true;

            if (affectOwner && !affectedTrusted)
                return false;
        }

        final boolean isPermitted = this.isPermitted(uuid);

        if (isPermitted && !affectedTrusted)
            return true;

        return false;
    }

    private CompletableFuture<Chunk> getChunkAsync(World world, int x, int z) {
        CompletableFuture<Chunk> future = new CompletableFuture<>();
        Main.instance.getScheduler().runTaskAtLocation(new Location(world, x, 64, z), () -> {
            Chunk chunk = world.getChunkAt(x, z);
            future.complete(chunk);
        });

        return future;
    }

    private CompletableFuture<List<Chunk>> getChunksAsync(Chunk centerChunk, int radius) {
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        World world = centerChunk.getWorld();

        for (int x = centerChunk.getX() - radius; x <= centerChunk.getX() + radius; x++) {
            for (int z = centerChunk.getZ() - radius; z <= centerChunk.getZ() + radius; z++) {
                futures.add(getChunkAsync(world, x, z));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    private CompletableFuture<List<Chunk>> getChunks(final Chunk chunkCenter, final int radius) {
        if (this.surroundingChunks != null && this.surroundingChunks.isDone())
            return this.surroundingChunks;

        final CompletableFuture<List<Chunk>> future = this.getChunksAsync(chunkCenter, radius);

        if (this.surroundingChunks == null)
            this.surroundingChunks = future;

        return future;
    }

    private CompletableFuture<List<Entity>> getEntitiesAsync(final Chunk chunk) {
        final CompletableFuture<List<Entity>> future = new CompletableFuture<>();

        Main.instance.getScheduler().runTaskAtLocation(this.getChunkMidPoint(chunk, 64), () -> future.complete(Arrays.stream(chunk.getEntities()).collect(Collectors.toList())));

        return future;
    }

    public CompletableFuture<List<Entity>> getEntitiesAsync() {
        final Location loc = getLocation();
        int radius = this.config.getRadius(); // Assuming you have a config object

        if (!Main.isFolia()) {
            final CompletableFuture<List<Entity>> future = new CompletableFuture<>();

            Main.instance.getScheduler().runTask(() -> future.complete(loc.getWorld().getNearbyEntities(loc, radius, radius / 2, radius).stream().collect(Collectors.toList())));

            return future;
        }

        Chunk centerChunk = loc.getChunk();

        return getChunks(centerChunk, radius).thenCompose(chunks -> {
            List<CompletableFuture<List<Entity>>> futures = chunks.stream()
                    .map(this::getEntitiesAsync)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .flatMap(future -> future.join().stream())
                            .collect(Collectors.toList()));
        });
    }

    private Location getChunkMidPoint(final Chunk chunk, final int y) {
        World world = chunk.getWorld();

        final int midX = (chunk.getX() << 4) + 8;
        final int midZ = (chunk.getZ() << 4) + 8;

        return new Location(world, midX, y, midZ);
    }

    private CompletableFuture<Chunk> getChunkAsync() {
        final CompletableFuture<Chunk> future = new CompletableFuture<>();
        final Location loc = this.getLocation();

        Main.instance.getScheduler().runTaskAtLocation(loc, () -> future.complete(loc.getChunk()));

        return future;
    }

    private Chunk getChunk() {
        if (this.chunk != null)
            return this.chunk;

        Chunk chunk;

        try {
            chunk = this.getChunkAsync().get();
        } catch (final InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
            return null;
        }

        if (this.chunk != null)
            this.chunk = chunk;

        return chunk;
    }

    @Override
    public void tick() {
        final Chunk chunk = this.getChunk();

        if (!chunk.isLoaded())
            return;

        final List<Entity> entities;

        try {
            entities = this.getEntitiesAsync().get();
        } catch (final ExecutionException | InterruptedException e) {
            return;
        }

        if (entities == null)
            return;

        for (Entity entity : entities) {
            if (entity == null)
                continue;

            Main.instance.getScheduler().runTaskAtEntity(entity, () -> {
                if (!this.pushEntity(entity))
                    return;

                final boolean isForceBlockCloser = this.isForceBlockCloser(entity);

                if (isForceBlockCloser)
                    return;

                if (this.allowedAsPermitted(entity) || GeneralConfig.getInstance().getBypassList().contains(entity.getUniqueId()))
                    return;

                // Deflecting projectiles
                if (entity instanceof Projectile && this.config.isAffectProjectiles()) {
                    final Projectile projectile = (Projectile) entity;
                    final ProjectileSource source = projectile.getShooter();

                    if (source instanceof Player && !this.config.isAffectPlayers())
                        return;

                    if (source instanceof Player && this.config.isAffectPlayers()) {
                        final Player shooter = (Player) source;
                        final Player originalShooter = Bukkit.getPlayer(this.originalShooterMap.getOrDefault(projectile, shooter.getUniqueId()));

                        if (originalShooter != null && this.isPermitted(originalShooter) && !this.config.isAffectTrustedPlayers())
                            return;

                        final Player owner = Bukkit.getPlayer(this.config.getOwner());

                        if (owner != null && owner.isOnline() && !shooter.equals(owner) && !(projectile instanceof EnderPearl) && !(projectile instanceof Trident)) {
                            projectile.setShooter(owner);
                            this.originalShooterMap.put(projectile, shooter.getUniqueId());
                        }
                    }

                    final boolean isNonHostile = source instanceof Mob && !(source instanceof Monster);
                    final boolean isPlayer = source instanceof Player;

                    if (!isPlayer && isNonHostile && !this.config.isAffectNonHostileMobs())
                        return;

                    if (!isPlayer && !isNonHostile && !this.config.isAffectHostileMobs())
                        return;
                }

                Location location = entity.getLocation();
                location = location.getBlock().getLocation();

                if (location.distance(this.getLocation()) > this.getConfig().getRadius())
                    return;

                Entity currentEntity = entity;

                if (currentEntity.isInsideVehicle())
                    currentEntity = currentEntity.getVehicle();

                switch (this.config.getMode()) {
                    case MAGNET: {
                        this.magnet(currentEntity);
                        break;
                    }

                    case FORCE_FIELD: {
                        this.forceField(currentEntity);
                        break;
                    }

                    case WHIRLPOOL: {
                        this.whirlpool(currentEntity);
                        break;
                    }
                }
            });
        }

        ++this.ticks;
    }

    private boolean isOff() {
        return this.getConfig().getMode() == ForceMode.OFF || this.isDeleted();
    }

    @Override
    public void displayParticles() {
        if (this.isOff())
            return;

        if (this.particlesDisplaying)
            return;

        if (!this.config.isDisplayParticles())
            return;

        final Location loc = this.getLocation();
        final Chunk chunk = this.getChunk();

        if (!chunk.isLoaded())
            return;

        final Particle particle = Particle.HEART;
        final Location center = ForceBlockListeners.center(loc.clone());
        final Location originalCenter = center.clone();

        center.add(0, 5, 0);

        final int configRadius = this.getConfig().getRadius();
        final int radius = configRadius > MAX_SPHERE_RADIUS ? MAX_SPHERE_RADIUS : configRadius;

        new WrappedRunnable() {
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

                if (dist < 1.1D || isOff() || !config.isDisplayParticles()) {
                    super.cancel();
                    particlesDisplaying = false;
                    return;
                }

                Main.instance.getScheduler().runTaskAtLocation(this.loc, () -> this.loc.getWorld().spawnParticle(particle, this.loc, 1));
            }
        }.runTaskTimer(Main.instance, 1L, 3L);

        this.particlesDisplaying = true;
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

        double strength = getStrength(nearby, distance);

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

    private static double getStrength(Entity nearby, double distance) {
        final double maxDistance = 10.0; // Adjust this value based on your preference
        double strength = PULL_STRENGTH * Math.exp(-distance / maxDistance);

        if (!nearby.isOnGround())
            strength *= 2;

        if (distance < 1.5)
            strength = 0.2;

        // Ensure strength is always positive
        strength = Math.abs(strength);

        if (strength < 0.01)
            strength = 0.01;
        return strength;
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