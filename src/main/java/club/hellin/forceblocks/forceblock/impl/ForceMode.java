package club.hellin.forceblocks.forceblock.impl;

import club.hellin.forceblocks.inventory.AbstractInventory;
import club.hellin.forceblocks.inventory.InventoryItemProvider;
import club.hellin.forceblocks.utils.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
        input = input.replace("_", " ");

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
    public ItemStack provide(final Player player, final AbstractInventory inventory) {
        final boolean isOff = this == ForceMode.OFF;
        final ChatColor color = isOff ? ChatColor.RED : ChatColor.YELLOW;
        final String name = isOff ? this.name() : toTitleCase(this.name()).replace(" ", "");
        int radius = -1;

        final AbstractInventory.OpenSession session = inventory.getSession(player);

        if (session != null) {
            final ForceBlock forceBlock = session.getAttachment();
            radius = forceBlock == null ? -1 : forceBlock.getConfig().getRadius();
        }

        final ItemStackBuilder builder = new ItemStackBuilder(this.getMaterial())
                .addEnchant(Enchantment.KNOCKBACK)
                .hideEnchants()
                .setDisplayName(String.format("&7&lMode: %s&l%s", color, name));

        if (radius != -1)
            builder.setLore(String.format("&7&lRadius:&b&l %s", radius));

        return builder.build();
    }
}