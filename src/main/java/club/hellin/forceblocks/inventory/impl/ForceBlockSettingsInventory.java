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

    @InventoryToggleHandler(name = "&bHostile Mobs")
    public void toggleAffectHostileMobs(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectHostileMobs(!block.getConfig().isAffectHostileMobs());
        block.save();
    }

    @InventoryToggleHandler(name = "&bTrusted Players")
    public void toggleAffectTrustedPlayers(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectTrustedPlayers(!block.getConfig().isAffectTrustedPlayers());
        block.save();
    }

    @InventoryToggleHandler(name = "&bProjectiles")
    public void toggleAffectProjectiles(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectProjectiles(!block.getConfig().isAffectProjectiles());
        block.save();
    }

    @InventoryToggleHandler(name = "&bHologram")
    public void toggleHologram(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.toggleHologram();
    }

    @InventoryToggleHandler(name = "&bParticles")
    public void toggleParticles(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setDisplayParticles(!block.getConfig().isDisplayParticles());
        block.save();
    }

    @InventoryToggleHandler(name = "&bOwner")
    public void toggleAffectOwner(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final ForceBlock block = this.getForceBlock(player, true);

        if (block == null)
            return;

        block.getConfig().setAffectOwner(!block.getConfig().isAffectOwner());
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

            case "HOSTILE_MOBS": {
                return config.isAffectHostileMobs();
            }

            case "TRUSTED_PLAYERS": {
                return config.isAffectTrustedPlayers();
            }

            case "PROJECTILES": {
                return config.isAffectProjectiles();
            }

            case "HOLOGRAM": {
                return config.isDisplayHologram();
            }

            case "PARTICLES": {
                return config.isDisplayParticles();
            }

            case "OWNER": {
                return config.isAffectOwner();
            }
        }

        return false;
    }

    @Override
    public int getAddToSize() {
        return 9; // Add extra row
    }
}