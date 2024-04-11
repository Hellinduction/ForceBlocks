package club.hellin.forceblocks.listeners;

import club.hellin.forceblocks.inventory.AbstractInventory;
import club.hellin.forceblocks.inventory.InventoryManager;
import club.hellin.forceblocks.inventory.objects.InventoryClick;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class InventoryListeners implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;

        final Player player = (Player) e.getWhoClicked();
        final InventoryView view = e.getView();
        final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

        if (inventory == null)
            return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR)
            return;

        if (!clickedItem.hasItemMeta())
            return;

        if (inventory.getBackButton().isSimilar(clickedItem)) {
            inventory.back(player);
            return;
        }

        final InventoryClick click = new InventoryClick(e, player, view, clickedItem);
        inventory.handle(click);

        final InventoryView newView = player.getOpenInventory();
        final AbstractInventory currentInventory = InventoryManager.getInstance().getInventory(newView);

        if (currentInventory == null || !inventory.equals(currentInventory))
            return;

        currentInventory.setItems(player, newView.getTopInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent e) {
        final InventoryView view = e.getView();
        final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

        if (inventory == null)
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(final InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player))
            return;

        final Player player = (Player) e.getPlayer();
        final UUID uuid = player.getUniqueId();

        final InventoryView view = e.getView();
        final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

        if (inventory == null)
            return;

        InventoryManager.getInstance().getLastInventoryMap().put(uuid, inventory);
        inventory.close(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player))
            return;

        final Player player = (Player) e.getPlayer();

        final InventoryView view = e.getView();
        final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

        if (inventory == null)
            return;

        if (inventory.isOpen(player))
            return;

        inventory.addOpen(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        for (final AbstractInventory inventory : InventoryManager.getInstance().get()) {
            if (inventory.getProperties().updateOnJoin())
                inventory.updateAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent e) {
        for (final AbstractInventory inventory : InventoryManager.getInstance().get()) {
            if (inventory.getProperties().updateOnLeave())
                inventory.updateAll();
        }
    }
}