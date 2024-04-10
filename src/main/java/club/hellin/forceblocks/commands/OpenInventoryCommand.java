package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.inventory.AbstractInventory;
import club.hellin.forceblocks.inventory.InventoryManager;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Test class
 */
public final class OpenInventoryCommand {
    private static final String PERMISSION = "forceblock.openinventory";

    @Command(name = "", desc = "Open an inventory", usage = "<inventory>")
    @Require(PERMISSION)
    public void openInventory(final @Sender CommandSender sender, final String name) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player player = (Player) sender;
        final AbstractInventory abstractInventory = InventoryManager.getInstance().getInventory(name);

        if (abstractInventory == null) {
            player.sendMessage(ChatColor.RED + "That inventory could not be found.");
            return;
        }

        try {
            final Inventory inventory = abstractInventory.createInventory(player);
            player.openInventory(inventory);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }
}