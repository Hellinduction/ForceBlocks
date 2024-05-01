package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.inventory.InventoryItemProvider;
import club.hellin.forceblocks.utils.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@Getter
public enum ForceMode implements InventoryItemProvider {
    FORCE_FIELD(Material.EMERALD_BLOCK),
    MAGNET(Material.IRON_BLOCK),
    WHIRLPOOL(Material.SEA_LANTERN),
    OFF(Material.REDSTONE_BLOCK);

    private final Material material;

    ForceMode(final Material material) {
        this.material = material;
    }

    public ForceMode next() {
        int nextIndex = (this.ordinal() + 1) % ForceMode.values().length;
        return ForceMode.values()[nextIndex];
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        input = input.toLowerCase();

        final StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }

        return titleCase.toString();
    }

    @Override
    public ItemStack provide() {
        final ChatColor color = this == ForceMode.OFF ? ChatColor.RED : ChatColor.YELLOW;
        final String name = toTitleCase(this.name());
        return new ItemStackBuilder(this.getMaterial()).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName(String.format("&7&lMode: %s&l%s", color, name)).build();
    }
}