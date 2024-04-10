package club.hellin.forceblocks.inventory;

import club.hellin.forceblocks.inventory.objects.InventoryClick;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface InventoryBase {
    String getTitle();
    String getRawName();
    int getSize(final Player player);
    default int getRows(final Player player) {
        return this.getSize(player) / 9;
    }
    List<ItemStack> getItems(final Player player);
    Inventory createInventory(final Player player);
    <T> Inventory createInventory(final Player player, final T attachment);
    void handle(final InventoryClick click);
    void setItems(final Player player, final Inventory inventory);
    boolean verify(final ItemStack item);
    ItemStack tag(final ItemStack item);
    boolean isInventory(final InventoryView view);
    void close(final Player player);
}