package club.hellin.forceblocks.inventory.impl;

import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.forceblock.impl.ForceMode;
import club.hellin.forceblocks.inventory.*;
import club.hellin.forceblocks.inventory.objects.InventoryClick;
import club.hellin.forceblocks.inventory.type.CaseByCaseInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@InventoryProperties(title = "&b&lForce Block")
@MainMenu
public final class ForceBlockInventory extends CaseByCaseInventory {
    @InventoryHandler(name = "SWITCH_MODE", switcher = true)
    public void switchMode(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock forceBlock = super.getAttachment(player);

        if (forceBlock == null) {
            player.sendMessage(ChatColor.RED + "Failed to find Force Block :(");
            return;
        }

        final ForceMode newMode = forceBlock.getConfig().getMode().next();

        forceBlock.getConfig().setMode(newMode); // Switch mode
        forceBlock.save();

        player.sendMessage(ChatColor.GREEN + String.format("Set the mode of this Force Block to %s.", newMode.name()));
    }

    @InventoryHandler(type = Material.SHULKER_BOX, name = "&eTrusted Players")
    public void viewTrustedPlayers(final InventoryClick click) {
        this.openInventory(click.getWhoClicked(), "TRUSTED_PLAYERS");
    }

    @InventoryHandler(type = Material.CHEST, name = "&eTrust New Players")
    public void viewPlayerSelector(final InventoryClick click) {
        this.openInventory(click.getWhoClicked(), "PLAYER_SELECTOR");
    }

    @InventoryHandler(type = Material.FEATHER, name = "&eSettings")
    public void viewSettings(final InventoryClick click) {
        this.openInventory(click.getWhoClicked(), "FORCE_BLOCK_SETTINGS");
    }

    private void openInventory(final Player player, final String inventoryName) {
        final ForceBlock block = super.getAttachment(player);

        if (!block.isOwner(player)) {
            player.sendMessage(ChatColor.RED + "You are not permitted to change these settings of this Force Block.");
            return;
        }

        player.closeInventory();

        final AbstractInventory abstractInventory = InventoryManager.getInstance().getInventory(inventoryName);
        final Inventory inventory = abstractInventory.createInventory(player, block);

        player.openInventory(inventory);
    }

    @Override
    public InventoryItemProvider getProvider(final Player player, final InventorySwitchItem item) {
        switch (item.getRawName()) {
            case "SWITCH_MODE": {
                final ForceBlock block = super.getAttachment(player);

                if (block == null)
                    return null;

                return block.getConfig().getMode();
            }
        }

        return null; // Boo hoo cope
    }
}