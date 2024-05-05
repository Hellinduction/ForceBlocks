package club.hellin.forceblocks.commands;

import club.hellin.forceblocks.Main;
import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public final class ApplyForceBlockCommand {
    private static final String PERMISSION = "forceblock.apply";

    @Command(name = "", desc = "Apply Force Block on the item you are holding in your hand", usage = "<radius>")
    @Require(PERMISSION)
    public void applyForceBlock(final @Sender CommandSender sender, final int radius) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return;
        }

        final Player p = (Player) sender;
        final PlayerInventory inv = p.getInventory();
        ItemStack hand = inv.getItemInMainHand();

        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage(ChatColor.RED + "You are not holding anything.");
            return;
        }

        if (!hand.getType().isBlock()) {
            p.sendMessage(ChatColor.RED + "You must apply force to blocks only.");
            return;
        }

        if (!canBeForceBlock(hand.getType())) {
            p.sendMessage(ChatColor.RED + "The material is not hard enough to be a Force Block.");
            return;
        }

        hand = apply(hand, radius);

        inv.setItemInMainHand(hand);
        p.updateInventory();

        p.sendMessage(ChatColor.GREEN + String.format("Applied force to your %s.", hand.getType().name()));
    }

    public static boolean canBeForceBlock(final Material material) {
        return material.getHardness() >= Material.OBSIDIAN.getHardness();
    }

    public static ItemStack apply(ItemStack item, final int radius) {
        final NBTItem nbt = new NBTItem(item);
        nbt.setInteger(Main.NBT_RADIUS_TAG, radius);

        item = nbt.getItem();

        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "FORCE BLOCK");
        item.setItemMeta(meta);

        return item;
    }
}