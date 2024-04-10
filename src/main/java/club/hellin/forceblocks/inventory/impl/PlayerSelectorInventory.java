package club.hellin.forceblocks.inventory.impl;

import club.hellin.forceblocks.commands.ForceBlockTrustCommand;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.inventory.InventoryManager;
import club.hellin.forceblocks.inventory.InventoryProperties;
import club.hellin.forceblocks.inventory.objects.InventoryClick;
import club.hellin.forceblocks.inventory.type.ListInventory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.stream.Collectors;

@InventoryProperties(title = "&ePlayer Selector", updateOnJoin = true, updateOnLeave = true)
public final class PlayerSelectorInventory extends ListInventory<Player> {
    @Override
    public Player convertTo(final ItemStack item) {
        final ItemMeta itemMeta = item.getItemMeta();

        if (!(itemMeta instanceof SkullMeta))
            return null;

        final SkullMeta meta = (SkullMeta) itemMeta;
        final OfflinePlayer offlinePlayer = meta.getOwningPlayer();

        if (!offlinePlayer.isOnline())
            return null;

        final Player player = offlinePlayer.getPlayer();
        return player;
    }

    @Override
    public ItemStack convertFrom(final Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.RED + player.getDisplayName());

        head.setItemMeta(meta);
        return head;
    }

    @Override
    public Collection<Player> provide(final Player player) {
        final ForceBlock block = super.getAttachment(player);
        return Bukkit.getOnlinePlayers().stream().filter(p -> !block.isPermitted(p) && !p.equals(player)).collect(Collectors.toList());
    }

    @Override
    public void handle(final InventoryClick click, final Player target) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getAttachment(player);

        InventoryManager.getInstance().verify(player, result -> {
            if (!result.getStatus().isSuccessful())
                return;

            ForceBlockTrustCommand.toggleTrust(block, player, target);

            final Inventory inventory = InventoryManager.getInstance().getInventory("FORCE_BLOCK").createInventory(player, block);
            player.openInventory(inventory);
        }, String.format("&bSelect %s?", target.getName()));
    }
}