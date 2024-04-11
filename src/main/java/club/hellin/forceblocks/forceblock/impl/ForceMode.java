package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.inventory.InventoryItemProvider;
import club.hellin.forceblocks.utils.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public enum ForceMode implements InventoryItemProvider {
    FORCE_FIELD,
    MAGNET,
    OFF;

    public ForceMode next() {
        int nextIndex = (this.ordinal() + 1) % ForceMode.values().length;
        return ForceMode.values()[nextIndex];
    }

    @Override
    public ItemStack provide() {
        switch (this) {
            case FORCE_FIELD: {
                return new ItemStackBuilder(Material.EMERALD_BLOCK).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName("&7&lMode: &e&lForceField").build();
            }

            case MAGNET: {
                return new ItemStackBuilder(Material.IRON_BLOCK).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName("&7&lMode: &e&lMagnet").build();
            }

            case OFF: {
                return new ItemStackBuilder(Material.REDSTONE_BLOCK).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName("&7&lMode: &c&lOFF").build();
            }

            default: {
                return null;
            }
        }
    }
}