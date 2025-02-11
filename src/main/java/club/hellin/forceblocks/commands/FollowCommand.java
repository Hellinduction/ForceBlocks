package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import de.metaphoriker.pathetic.api.pathing.Pathfinder;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;
import de.metaphoriker.pathetic.bukkit.mapper.BukkitMapper;
import de.metaphoriker.pathetic.engine.result.PathImpl;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

public final class FollowCommand {
    @Getter
    @Setter
    public static final class FollowData {
        private final UUID follower;
        private final UUID following;

        private BukkitTask task;
        private Location previousTargetLocation;

        public FollowData(final UUID follower, final UUID following) {
            this.follower = follower;
            this.following = following;
        }
    }

    /**
     * Uses PathFinding to push you to the player you are following
     */
    private static final String PERMISSION = "forceblock.follow";
    private static final int STOP_FOLLOWING_DISTANCE = 3; // Blocks
    private static final int MAX_RADIUS = 100;
    private static final Predicate<Player> IS_ONLINE = player -> player != null && player.isOnline();
    private static final double SPEED = 0.6;

    private final Map<UUID, FollowData> followMap = new HashMap<>(); // Follower -> Following

    public FollowCommand() {
        Bukkit.getScheduler().runTaskTimer(Main.instance, this::performLogic, 60L, 60L);
    }

    private void performLogic() {
        for (final Map.Entry<UUID, FollowData> entry : this.followMap.entrySet()) {
            FollowData data = entry.getValue();

            final UUID followerUuid = data.getFollower();
            final UUID followingUuid = data.getFollowing();

            final Player follower = Bukkit.getPlayer(followerUuid);
            final Player following = Bukkit.getPlayer(followingUuid);

            if (!IS_ONLINE.test(follower) || !IS_ONLINE.test(following))
                continue;

            final Location from = follower.getLocation();
            final Location target = following.getLocation();

            if (!from.getWorld().equals(target.getWorld()))
                continue;

            if (from.distance(target) < STOP_FOLLOWING_DISTANCE)
                continue;

            final Location prev = data.getPreviousTargetLocation();
            final BukkitTask oldTask = data.getTask();
            final boolean shouldCancel = prev != null && oldTask != null && !oldTask.isCancelled() && !this.floorLocation(prev).equals(this.floorLocation(target));

            if (!shouldCancel && prev != null && oldTask != null && !oldTask.isCancelled())
                continue;

            final PathPosition start = BukkitMapper.toPathPosition(from);
            final PathPosition end = BukkitMapper.toPathPosition(target);

            final Pathfinder pathfinder = Main.instance.getPlayerPathfinder();
            pathfinder.findPath(start, end, Collections.emptyList())
                    .thenAccept(result -> {
                        final PathImpl path = (PathImpl) result.getPath();
                        final Iterator<PathPosition> iterator = path.getPositions().iterator();

                        if (shouldCancel)
                            oldTask.cancel();

                        if (shouldCancel || oldTask == null || oldTask.isCancelled()) {
                            final BukkitTask task = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!iterator.hasNext()) {
                                        super.cancel();
                                        return;
                                    }

                                    if (follower.getLocation().distance(data.getPreviousTargetLocation()) < STOP_FOLLOWING_DISTANCE) {
                                        super.cancel();
                                        return;
                                    }

                                    final PathPosition pos = iterator.next();
                                    final Location loc = BukkitMapper.toLocation(pos);

                                    final Vector direction = loc.toVector().subtract(follower.getLocation().toVector()).normalize();
                                    final Vector velocity = direction.multiply(SPEED);

                                    try {
                                        velocity.checkFinite();
                                    } catch (final RuntimeException ignored) {
                                        return;
                                    }

                                    follower.setVelocity(velocity);
                                }
                            }.runTaskTimer(Main.instance, 0L, 2L);

                            data.setTask(task);
                            data.setPreviousTargetLocation(target);
                        }
                    });
        }
    }

    private Location floorLocation(final Location location) {
        return new Location(location.getWorld(), Math.floor(location.getX()), Math.floor(location.getY()), Math.floor(location.getZ()));
    }

    @Command(name = "", desc = "Use path finding to follow a player", usage = "<player>")
    @Require(PERMISSION)
    public void follow(final @Sender CommandSender sender, final Player toFollow) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        final UUID toFollowUuid = toFollow.getUniqueId();

        if (toFollowUuid.equals(uuid)) {
            player.sendMessage(ChatColor.RED + "You cannot follow yourself.");
            return;
        }

        if (this.followMap.containsKey(uuid)) {
            final FollowData data = this.followMap.remove(uuid);
            final UUID followingUuid = data.getFollowing();
            final OfflinePlayer following = Bukkit.getOfflinePlayer(followingUuid);

            if (data.getTask() != null)
                data.getTask().cancel();

            player.sendMessage(ChatColor.GREEN + String.format("You have stopped following %s.", following.getName()));
            return;
        }

        if (!toFollow.getWorld().equals(player.getWorld()) || toFollow.getLocation().distance(player.getLocation()) > MAX_RADIUS) {
            player.sendMessage(ChatColor.RED + "That player is too far away to start following them.");
            return;
        }

        this.followMap.put(uuid, new FollowData(uuid, toFollowUuid));
        player.sendMessage(ChatColor.GREEN + String.format("You have started following %s.", toFollow.getName()));

        this.performLogic();
    }
}