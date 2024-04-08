package club.hellin.forceblocks.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public final class BypassForceBlockCommand {
    private static final String PERMISSION = "forceblock.bypass";

    private final List<UUID> bypassList = new ArrayList<>();

    @Command(name = "", desc = "Toggle command to bypass any restrictions created by a Force Block")
    @Require(PERMISSION)
    public void toggleBypass(final @Sender CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();

        if (!this.bypassList.contains(uuid)) {
            this.bypassList.add(uuid);
            player.sendMessage(ChatColor.GREEN + "You are now bypassed to Force Blocks.");
            return;
        }

        this.bypassList.remove(uuid);
        player.sendMessage(ChatColor.GREEN + "You are no longer bypassed to Force Blocks.");
    }
}