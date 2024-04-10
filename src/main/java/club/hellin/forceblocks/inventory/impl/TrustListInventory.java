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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@InventoryProperties(title = "&eTrusted Players")
public final class TrustListInventory extends ListInventory<OfflinePlayer> {
    @Override
    public OfflinePlayer convertTo(final ItemStack item) {
        final ItemMeta itemMeta = item.getItemMeta();

        if (!(itemMeta instanceof SkullMeta))
            return null;

        final SkullMeta meta = (SkullMeta) itemMeta;
        final OfflinePlayer offlinePlayer = meta.getOwningPlayer();

        return offlinePlayer;
    }

    @Override
    public ItemStack convertFrom(final OfflinePlayer player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GREEN + player.getName());

        head.setItemMeta(meta);
        return head;
    }

    @Override
    public Collection<OfflinePlayer> provide(final Player player) {
        final ForceBlock block = super.getAttachment(player);
        final List<UUID> trusted = block.getTrusted();
        final List<OfflinePlayer> players = trusted.stream().map(uuid -> Bukkit.getOfflinePlayer(uuid)).filter(offlinePlayer -> !offlinePlayer.getUniqueId().equals(player.getUniqueId())).filter(offlinePlayer -> offlinePlayer != null).collect(Collectors.toList());

        return players;
    }

    @Override
    public void handle(final InventoryClick click, final OfflinePlayer target) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getAttachment(player);

        InventoryManager.getInstance().verify(player, result -> {
            if (!result.getStatus().isSuccessful())
                return;

            ForceBlockTrustCommand.toggleTrust(block, player, target);

            final Inventory inventory = InventoryManager.getInstance().getInventory("FORCE_BLOCK").createInventory(player, block);
            player.openInventory(inventory);
        }, String.format("&cRemove %s?", target.getName()));
    }
}