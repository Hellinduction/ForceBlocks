package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.customentities.impl.EntityCarrierParrot;
import club.hellin.forceblocks.utils.GeneralConfig;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public final class CarrierParrotCommand implements Listener {
    private static final String PERMISSION = "forceblock.carrierparrot";
    private static final int REMOVE_AT_DISTANCE = 8;

    private static boolean registeredListeners = false;

    public CarrierParrotCommand() {
        if (!registeredListeners) {
            Bukkit.getPluginManager().registerEvents(this, Main.instance);
            registeredListeners = true;
        }
    }

    @Command(name = "", desc = "Make a parrot carry you to wherever your arrows land")
    @Require(PERMISSION)
    public void toggleCarrierParrot(final @Sender CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();

        final GeneralConfig config = GeneralConfig.getInstance();

        if (!config.getCarrierParrot().contains(uuid)) {
            config.getCarrierParrot().add(uuid);
            GeneralConfig.save(config);

            player.sendMessage(ChatColor.GREEN + "You have enabled carrier parrot.");
            return;
        }

        config.getCarrierParrot().remove(uuid);
        GeneralConfig.save(config);

        player.sendMessage(ChatColor.GREEN + "You have disabled carrier parrot.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent e) {
        if (e.getHitBlock() == null)
            return;

        final Block block = e.getHitBlock();

        if (block.getType() == Material.AIR)
            return;

        final Projectile projectile = e.getEntity();
        final ProjectileSource source = projectile.getShooter();

        if (!(source instanceof Player))
            return;

        final Player player = (Player) source;
        final UUID uuid = player.getUniqueId();

        final GeneralConfig config = GeneralConfig.getInstance();

        if (!config.getCarrierParrot().contains(uuid))
            return;

        final Location toTeleportTo = player.getLocation().clone().add(0, 2.5, 0);
        final Location loc = block.getLocation();
        final EntityCarrierParrot parrot = new EntityCarrierParrot(toTeleportTo, loc);
        final WorldServer world = ((CraftWorld) player.getWorld()).getHandle();

        parrot.init();
        world.addFreshEntity(parrot, CreatureSpawnEvent.SpawnReason.COMMAND);

        final Entity entity = parrot.getBukkitEntity();

        Bukkit.getScheduler().runTask(Main.instance, () -> parrot.addPassenger(player));

        this.checkIfReachedDestination(entity, loc);
    }

    private void checkIfReachedDestination(final Entity entity, final Location destination) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    super.cancel();
                    return;
                }

                if (entity.getPassengers().isEmpty()) {
                    entity.remove();
                    super.cancel();
                    return;
                }

                final Location loc = entity.getLocation();
                final double dist = loc.distance(destination);

                if (dist > REMOVE_AT_DISTANCE)
                    return;

                entity.eject();
                entity.remove();

                super.cancel();
            }
        }.runTaskTimer(Main.instance, 20L, 20L);
    }
}