package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.customentities.impl.EntityCarrierParrot;
import club.hellin.forceblocks.utils.GeneralConfig;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class CarrierParrotCommand implements Listener {
    private static final String PERMISSION = "forceblock.carrierparrot";
    private static final float REMOVE_AT = 1F;
    private static final int REMOVE_AT_SECONDS_STATIONARY = 3;

    private static boolean registeredListeners = false;

    private final Map<Integer, Double> distanceMap = new HashMap<>();
    private final Map<Integer, Integer> countMap = new HashMap<>();

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
        final ServerLevel world = ((CraftWorld) player.getWorld()).getHandle();

        parrot.init();
        world.addFreshEntity(parrot, CreatureSpawnEvent.SpawnReason.COMMAND);

        final Entity entity = parrot.getBukkitEntity();

        if (entity instanceof Parrot) {
            final Parrot parrotEntity = (Parrot) entity;
            final Parrot.Variant variant = this.getRandomVariant();

            parrotEntity.setVariant(variant);
            parrotEntity.setAdult();
        }

        Bukkit.getScheduler().runTask(Main.instance, () -> parrot.addPassenger(player));

        this.check(entity, loc);
    }

    private Parrot.Variant getRandomVariant() {
        final Random random = new Random();
        return Parrot.Variant.values()[random.nextInt(Parrot.Variant.values().length)];
    }

    private void check(final Entity entity, final Location destination) {
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
                }

                final Location loc = entity.getLocation();
                final double dist = loc.distance(destination);
                final double lastDist = distanceMap.getOrDefault(entity.getEntityId(), -1D);

                if (lastDist >= 0 && dist == lastDist)
                    countMap.put(entity.getEntityId(), countMap.getOrDefault(entity.getEntityId(), 0) + 1);

                distanceMap.put(entity.getEntityId(), dist);

                final int count = countMap.getOrDefault(entity.getEntityId(), 0);

                if (dist > REMOVE_AT && count < REMOVE_AT_SECONDS_STATIONARY)
                    return;

                entity.eject();
            }
        }.runTaskTimer(Main.instance, 20L, 20L);
    }
}