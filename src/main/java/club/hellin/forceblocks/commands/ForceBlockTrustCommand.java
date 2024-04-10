package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Sender;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ForceBlockTrustCommand {
    private final Map<UUID, Long> ranMap = new HashMap<>();

    @Command(name = "", desc = "Toggle trust on a player to your Force Block protected land", usage = "<player>")
    public void toggleTrust(final @Sender CommandSender sender, final Player toTrust) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        final Location loc = player.getLocation();
        final long now = Instant.now().getEpochSecond();

        final long then = this.ranMap.getOrDefault(uuid, 0L);
        final long diff = now - then;

        if (diff < 2) {
            player.sendMessage(ChatColor.RED + "Please wait a bit before doing this.");
            return;
        }

        final ForceBlock block = ForceBlockManager.getInstance().getClosestForceBlock(loc, player);

        this.ranMap.put(uuid, now);

        if (block == null) {
            player.sendMessage(ChatColor.RED + "It does not appear that you are within the radius of any existing Force Blocks owned by you.");
            return;
        }

        toggleTrust(block, player, toTrust);
    }

    public static void toggleTrust(final ForceBlock block, final Player player, final OfflinePlayer toTrust) {
        if (!block.isOwner(player)) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this Force Block.");
            return;
        }

        if (block.isOwner(toTrust)) {
            player.sendMessage(ChatColor.RED + "You cannot trust yourself.");
            return;
        }

        if (block.getTrusted().contains(toTrust.getUniqueId())) {
            block.unTrust(toTrust);
            player.sendMessage(ChatColor.GREEN + String.format("Untrusted %s to your Force Block!", toTrust.getName()));
            return;
        }

        block.trust(toTrust);
        player.sendMessage(ChatColor.GREEN + String.format("Trusted %s to your Force Block!", toTrust.getName()));
    }
}