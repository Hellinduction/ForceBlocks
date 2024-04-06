package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReachCommand implements Listener {
    private static final String PERMISSION = "forceblock.reach";
    private static final String DEFAULT_REACH_STR = "3.0";
    private static final double DEFAULT_REACH = Double.parseDouble(DEFAULT_REACH_STR); // Default survival reach

    private static boolean registeredListeners = false;

    private final Map<UUID, Double> reachMap = new HashMap<>();

    public ReachCommand() {
        if (!registeredListeners) {
            Bukkit.getPluginManager().registerEvents(this, Main.instance);
            registeredListeners = true;
        }
    }

    @Command(name = "", desc = "Set your combat reach", usage = "<reach>")
    @Require(PERMISSION)
    public void setReach(final @Sender CommandSender sender, final @OptArg(DEFAULT_REACH_STR) double reach) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        if (reach < DEFAULT_REACH) {
            sender.sendMessage(ChatColor.RED + "That reach is too low.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();

        this.reachMap.put(uuid, reach);

        if (reach == DEFAULT_REACH) {
            player.sendMessage(ChatColor.GREEN + "You have reset your reach.");
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&aYou have set your reach to '&e%s&a'.", reach)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        final double reach = this.reachMap.getOrDefault(uuid, DEFAULT_REACH);

        if (reach <= DEFAULT_REACH)
            return;

        if (e.getAction() != Action.LEFT_CLICK_AIR)
            return;

        final BlockIterator iterator = new BlockIterator(player, 180);
        while (iterator.hasNext()) {
            final Block block = iterator.next();

//            if (block.getType().isSolid())
//                break;

            for (final Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 0.32, 0.32, 0.32)) {
                final double dist = player.getLocation().distance(entity.getLocation());

                if (entity instanceof LivingEntity && dist > DEFAULT_REACH && dist < reach) {
                    player.attack(entity);
                    return;
                }
            }
        }
    }
}