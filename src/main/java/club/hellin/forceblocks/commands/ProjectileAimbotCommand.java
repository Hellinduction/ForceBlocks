package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class ProjectileAimbotCommand implements Listener {
    private static final String PERMISSION = "forceblock.projectileaimbot";
    private static final int DISTANCE = 50;
    private static final int FIELD_OF_VIEW = 75;
    private static boolean registeredListeners = false;
    private final List<UUID> toggledOn = new ArrayList<>();

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

        final boolean toggled = this.toggledOn.contains(uuid);

        if (toggled) {
            this.toggledOn.remove(uuid);
            p.sendMessage(ChatColor.GREEN + "Toggled Projectile Aimbot off.");
            return;
        }

        this.toggledOn.add(uuid);
        p.sendMessage(ChatColor.GREEN + "Toggled Projectile Aimbot on.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(final ProjectileLaunchEvent e) {
        final Projectile projectile = e.getEntity();
        final double speed = projectile.getVelocity().length();

        if (projectile instanceof EnderPearl)
            return;

        final ProjectileSource source = projectile.getShooter();

        if (!(source instanceof Player))
            return;

        final Player shooter = (Player) source;
        final UUID uuid = shooter.getUniqueId();

        if (!this.toggledOn.contains(uuid))
            return;

        final Entity target = this.getTargetEntity(projectile, shooter);

        if (target == null)
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                final Predicate<Entity> isValid = entity -> entity.isValid() && !entity.isDead();

                if (!toggledOn.contains(uuid)) {
                    super.cancel();
                    return;
                }

                if (target instanceof Player && !((Player) target).isOnline()) {
                    super.cancel();
                    return;
                }

                if (!isValid.test(projectile) || !isValid.test(target) || projectile.isOnGround()) {
                    super.cancel();
                    return;
                }

                if (!target.getWorld().equals(projectile.getWorld())) {
                    super.cancel();
                    return;
                }

                pushTowards(projectile, target, speed);
            }
        }.runTaskTimer(Main.instance, 0L, 1L);
    }

    private void pushTowards(final Projectile projectile, final Entity target, final double speed) {
        Location targetLoc;
        if (target instanceof Player)
            targetLoc = ((Player) target).getEyeLocation();
        else
            targetLoc = target.getLocation().add(0, target.getHeight() - 0.3, 0);

        this.pushTowards(projectile, targetLoc, speed);
    }

    private void pushTowards(final Projectile projectile, final Location targetLoc, final double speed) {
        final Location nearbyLoc = projectile.getLocation();

        final Vector direction = targetLoc.subtract(nearbyLoc).toVector().normalize();
        final Vector velocity = direction.multiply(speed);

        try {
            velocity.checkFinite();
        } catch (final RuntimeException ignored) {
            return;
        }

        projectile.setVelocity(velocity);
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
                if (this.isPathObstructed(projectile, projectile.getLocation(), entity.getLocation()))
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

                if (this.isPathObstructed(projectile, projectile.getLocation(), entity.getLocation()))
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