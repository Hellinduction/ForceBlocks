package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.utils.GeneralConfig;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public final class BypassForceBlockCommand {
    public static final String PERMISSION = "forceblock.bypass";

    @Command(name = "", desc = "Toggle command to bypass any restrictions created by a Force Block")
    @Require(PERMISSION)
    public void toggleBypass(final @Sender CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();

        final GeneralConfig config = GeneralConfig.getInstance();

        if (!config.getBypassList().contains(uuid)) {
            config.getBypassList().add(uuid);
            GeneralConfig.save(config);

            player.sendMessage(ChatColor.GREEN + "You are now bypassed to Force Blocks.");
            return;
        }

        config.getBypassList().remove(uuid);
        GeneralConfig.save(config);

        player.sendMessage(ChatColor.GREEN + "You are no longer bypassed to Force Blocks.");
    }
}