package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.commands.strategy.ArrowPathfinderStrategy;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.GeneralConfig;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.patheloper.api.pathing.Pathfinder;
import org.patheloper.api.pathing.result.Path;
import org.patheloper.api.pathing.result.PathfinderResult;
import org.patheloper.api.wrapper.PathPosition;
import org.patheloper.mapping.bukkit.BukkitMapper;

import java.util.*;
import java.util.function.Predicate;

public final class ProjectileAimbotCommand implements Listener {
    private static final String PERMISSION = "forceblock.projectileaimbot";
    private static final int DISTANCE = 50;
    private static final int FIELD_OF_VIEW = 75;

    private static boolean registeredListeners = false;

    private final Map<Projectile, PathfinderResult> resultMap = new HashMap<>();
    private final Map<Projectile, Integer> indexMap = new HashMap<>();
    private final Map<Projectile, Location> lastLocMap = new HashMap<>();

    public ProjectileAimbotCommand() {
        if (!registeredListeners) {
            Bukkit.getPluginManager().registerEvents(this, Main.instance);
            registeredListeners = true;
        }
    }

    @Command(name = "", desc = "Make every projectile you launch hit the target")
    @Require(PERMISSION)
    public void toggleProjectileAimbot(final @Sender CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player p = (Player) sender;
        final UUID uuid = p.getUniqueId();
        final GeneralConfig config = GeneralConfig.getInstance();

        final boolean toggled = config.getProjectileAimbotToggledOn().contains(uuid);

        if (toggled) {
            config.getProjectileAimbotToggledOn().remove(uuid);
            GeneralConfig.save(config);
            p.sendMessage(ChatColor.GREEN + "Toggled Projectile Aimbot off.");
            return;
        }

        config.getProjectileAimbotToggledOn().add(uuid);
        GeneralConfig.save(config);
        p.sendMessage(ChatColor.GREEN + "Toggled Projectile Aimbot on.");
    }

    @Command(name = "pathfinding", desc = "Toggle path finding")
    @Require(PERMISSION)
    public void togglePathFinding(final @Sender CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command");
            return;
        }

        final GeneralConfig config = GeneralConfig.getInstance();
        config.setPathFind(!config.isPathFind());
        GeneralConfig.save(config);

        if (config.isPathFind()) {
            sender.sendMessage(ChatColor.GREEN + "Toggled path finding on.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Toggled path finding off.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(final ProjectileLaunchEvent e) {
        final Projectile projectile = e.getEntity();
        final double speed = projectile.getVelocity().length();
        final Vector originalVelocity = projectile.getVelocity();

        if (projectile instanceof EnderPearl)
            return;

        final ProjectileSource source = projectile.getShooter();

        if (!(source instanceof Player))
            return;

        final Player shooter = (Player) source;
        final UUID uuid = shooter.getUniqueId();
        final GeneralConfig config = GeneralConfig.getInstance();

        if (!config.getProjectileAimbotToggledOn().contains(uuid))
            return;

        if (config.isPathFind())
            projectile.setVelocity(projectile.getVelocity().multiply(0.4));

        final Entity target = this.getTargetEntity(projectile, shooter);

        if (target == null)
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                final Predicate<Entity> isValid = entity -> entity.isValid() && !entity.isDead();

                final Runnable cancel = () -> {
                    resultMap.remove(projectile);
                    indexMap.remove(projectile);
                    lastLocMap.remove(projectile);

                    super.cancel();
                };

                if (!config.getProjectileAimbotToggledOn().contains(uuid)) {
                    cancel.run();
                    return;
                }

                if (target instanceof Player && !((Player) target).isOnline()) {
                    cancel.run();
                    return;
                }

                if (!isValid.test(projectile) || !isValid.test(target) || projectile.isOnGround()) {
                    cancel.run();
                    return;
                }

                if (!target.getWorld().equals(projectile.getWorld())) {
                    cancel.run();
                    return;
                }

                pushTowards(projectile, target, speed, originalVelocity);
            }
        }.runTaskTimer(Main.instance, 0L, 1L);
    }

    private void pushTowards(final Projectile projectile, final Entity target, final double speed, final Vector originalVelocity) {
        Location targetLoc;
        if (target instanceof Player)
            targetLoc = ((Player) target).getEyeLocation();
        else
            targetLoc = target.getLocation().add(0, target.getHeight() - 0.3, 0);

        this.pushTowards(projectile, targetLoc, speed, originalVelocity);
    }

    private void pushTowards(final Projectile projectile, final Location targetLoc, final double speed, final Vector originalVelocity) {
        final Location nearbyLoc = projectile.getLocation();
        final GeneralConfig config = GeneralConfig.getInstance();

        if (nearbyLoc.distance(targetLoc) < 2 || !config.isPathFind()) {
            final Vector direction = targetLoc.subtract(nearbyLoc).toVector().normalize();
            final Vector velocity = direction.multiply(speed);

            try {
                velocity.checkFinite();
            } catch (final RuntimeException ignored) {
                return;
            }

            projectile.setVelocity(velocity);
            return;
        }

        final PathPosition start = BukkitMapper.toPathPosition(nearbyLoc);
        final PathPosition end = BukkitMapper.toPathPosition(targetLoc);

        final Pathfinder pathfinder = Main.instance.getPathfinder();
        final PathfinderResult result = this.resultMap.get(projectile);

        if (result != null) {
            final Path path = result.getPath();
            final Location target = BukkitMapper.toLocation(path.getEnd());

            if (this.floorLocation(targetLoc).equals(this.floorLocation(target)) || this.indexMap.getOrDefault(projectile, 0) < path.length() / 1.5) {
                this.handlePath(projectile, speed, result);
                return;
            }

            this.resultMap.remove(projectile);
            this.indexMap.remove(projectile);
        }

        pathfinder.findPath(start, end, new ArrowPathfinderStrategy(targetLoc))
                .thenAccept(pathfinderResult -> {
                    resultMap.put(projectile, pathfinderResult);
                    this.indexMap.remove(projectile);

                    this.handlePath(projectile, speed, pathfinderResult);
                });
    }

    private Location floorLocation(final Location location) {
        return new Location(location.getWorld(), Math.floor(location.getX()), Math.floor(location.getY()), Math.floor(location.getZ()));
    }

    private void handlePath(final Projectile projectile, double speed, final PathfinderResult result) {
        final int index = this.indexMap.getOrDefault(projectile, 0);
        final GeneralConfig config = GeneralConfig.getInstance();

        final Location nearbyLoc = projectile.getLocation();
        final Path path = result.getPath();

        PathPosition pos;
        final Iterator<PathPosition> iterator = path.iterator();

        pos = this.getElementAtIndex(iterator, index);

        if (pos == null)
            return;

        final Location loc = BukkitMapper.toLocation(pos);

        if (this.floorLocation(loc).distance(this.floorLocation(nearbyLoc)) > 2) {
            final Location lastLoc = this.lastLocMap.get(projectile);

            if (lastLoc != null)
                this.handle(loc, nearbyLoc, speed, projectile);

            return;
        }

        if (config.isPathFind() && speed > 0.4)
            speed = 0.4;

        this.handle(loc, nearbyLoc, speed, projectile);

        this.lastLocMap.put(projectile, nearbyLoc);
        this.indexMap.put(projectile, index + 1);
    }

    private void handle(Location loc, final Location nearbyLoc, double speed, final Projectile projectile) {
        loc = ForceBlockListeners.center(loc);

        final Vector direction = loc.toVector().subtract(nearbyLoc.toVector()).normalize();
        final Vector velocity = direction.multiply(speed);

        try {
            velocity.checkFinite();
        } catch (final RuntimeException ignored) {
            return;
        }

        projectile.setVelocity(velocity);
    }

    private <T> T getElementAtIndex(final Iterator<T> iterator, final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative");
        }

        int currentIndex = 0;
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (currentIndex == index) {
                return element;
            }
            currentIndex++;
        }

        return null;
    }

    /**
     * Checking if the player is vanished on SuperVanish/PremiumVanish
     *
     * @param player
     * @return
     */
    public static boolean isVanished(final Player player) {
        final List<MetadataValue> values = player.getMetadata("vanished");

        for (final MetadataValue meta : values)
            if (meta.asBoolean())
                return true;

        return false;
    }

    private Entity getTargetEntity(final Projectile projectile, final Player player) {
        Entity found;

        final Block block = player.getTargetBlockExact(DISTANCE);
        final World world = player.getWorld();
        final GeneralConfig config = GeneralConfig.getInstance();

        double closestDist = Double.MAX_VALUE;
        Entity closestEntity = null;

        final Predicate<Entity> shouldSkip = entity -> player.equals(entity) || !(entity instanceof Player) || entity.hasMetadata("NPC");

        if (block != null) {
            final Location loc = block.getLocation();

            for (final Entity entity : world.getNearbyEntities(loc, DISTANCE, DISTANCE, DISTANCE)) {
                if (shouldSkip.test(entity))
                    continue;

                if (entity instanceof Player && (((Player) entity).getGameMode() == GameMode.SPECTATOR || isVanished((Player) entity)))
                    continue;

                // Check if there's a clear path to the entity using Bukkit's ray tracing
                if (!config.isPathFind() && this.isPathObstructed(projectile, projectile.getLocation(), entity.getLocation()))
                    continue;

                final double dist = entity.getLocation().distance(loc);

                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = entity;
                }
            }
        } else {
            double smallestAngle = Double.MAX_VALUE;

            for (final Entity entity : world.getNearbyEntities(player.getLocation(), DISTANCE, DISTANCE, DISTANCE)) {
                if (shouldSkip.test(entity))
                    continue;

                if (entity instanceof Player && (((Player) entity).getGameMode() == GameMode.SPECTATOR || isVanished((Player) entity)))
                    continue;

                if (!config.isPathFind() && this.isPathObstructed(projectile, projectile.getLocation(), entity.getLocation()))
                    continue;

                final double fieldOfViewAngle = Math.toRadians(FIELD_OF_VIEW);
                final Vector playerDirection = player.getEyeLocation().getDirection();
                final Vector toEntity = entity.getLocation().toVector().subtract(player.getEyeLocation().toVector());
                final double angle = playerDirection.angle(toEntity);

                final boolean isBeyondPlayer = angle <= fieldOfViewAngle;

                if (isBeyondPlayer && angle < smallestAngle) {
                    closestEntity = entity;
                    smallestAngle = angle;
                }
            }
        }

        found = closestEntity;
        return found;
    }

    private boolean isPathObstructed(final Projectile projectile, final Location start, final Location end) {
        final RayTraceResult result = start.getWorld().rayTrace(start, end.toVector().subtract(start.toVector()).normalize(), DISTANCE, FluidCollisionMode.NEVER, true, 1.0, entity -> entity.getEntityId() != projectile.getEntityId());
        final boolean isObstructed = result != null && result.getHitBlock() != null;

        return isObstructed;
    }
}