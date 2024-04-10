package club.hellin.forceblocks.inventory.impl;

import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.forceblock.impl.ForceBlockConfig;
import club.hellin.forceblocks.inventory.InventoryProperties;
import club.hellin.forceblocks.inventory.InventoryToggleHandler;
import club.hellin.forceblocks.inventory.objects.InventoryClick;
import club.hellin.forceblocks.inventory.type.CaseByCaseInventory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@InventoryProperties(title = "&e&lForce Block Settings")
public final class ForceBlockSettingsInventory extends CaseByCaseInventory {
    @InventoryToggleHandler(name = "&bPlayers")
    public void toggleAffectPlayers(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectPlayers(!block.getConfig().isAffectPlayers());
        block.save();
    }

    @InventoryToggleHandler(name = "&bNone Hostile Mobs")
    public void toggleAffectNoneHostileMobs(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectNonHostileMobs(!block.getConfig().isAffectNonHostileMobs());
        block.save();
    }

    @InventoryToggleHandler(name = "&bExplosives")
    public void toggleAffectExplosives(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectExplosives(!block.getConfig().isAffectExplosives());
        block.save();
    }

    private ForceBlock getForceBlock(final Player player, final boolean sendFailure) {
        final ForceBlock forceBlock = super.getAttachment(player);

        if (forceBlock == null) {
            if (sendFailure)
                player.sendMessage(ChatColor.RED + "Failed to find Force Block :(");

            return null;
        }

        return forceBlock;
    }

    @Override
    public boolean isToggledOn(final Player player, final String rawName) {
        final ForceBlock block = this.getForceBlock(player, false);

        if (block == null)
            return false;

        final ForceBlockConfig config = block.getConfig();

        switch (rawName) {
            case "PLAYERS": {
                return config.isAffectPlayers();
            }

            case "NONE_HOSTILE_MOBS": {
                return config.isAffectNonHostileMobs();
            }

            case "EXPLOSIVES": {
                return config.isAffectExplosives();
            }
        }

        return false;
    }
}