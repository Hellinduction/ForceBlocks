package club.hellin.forceblocks.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface InventoryItemProvider {
    ItemStack provide(final Player player, final AbstractInventory inventory);
}