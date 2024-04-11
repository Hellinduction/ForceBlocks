package club.hellin.forceblocks.inventory;

import club.hellin.forceblocks.inventory.impl.ForceBlockInventory;
import club.hellin.forceblocks.inventory.impl.ForceBlockSettingsInventory;
import club.hellin.forceblocks.inventory.impl.PlayerSelectorInventory;
import club.hellin.forceblocks.inventory.impl.TrustListInventory;
import club.hellin.forceblocks.inventory.objects.Confirmation;
import club.hellin.forceblocks.inventory.type.VerifyInventory;
import club.hellin.forceblocks.utils.ComponentManager;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

@Getter
public final class InventoryManager extends ComponentManager<AbstractInventory> {
    private static InventoryManager singletonInstance;

    private final Map<UUID, AbstractInventory> lastInventoryMap = new HashMap<>();

    public static InventoryManager getInstance() {
        if (!(singletonInstance instanceof InventoryManager))
            singletonInstance = new InventoryManager();

        return singletonInstance;
    }

    @Override
    public void init() {
        super.register(new VerifyInventory());
        super.register(new PlayerSelectorInventory());
        super.register(new ForceBlockInventory());
        super.register(new TrustListInventory());
        super.register(new ForceBlockSettingsInventory());
    }

    public String toEnumName(String name) {
        name = ChatColor.stripColor(name);
        name = name.toUpperCase();
        name = name.replace(" ", "_");

        return name;
    }

    public String getInventoryName(final InventoryView view) {
        try {
            final Inventory inventory = view.getTopInventory();

            final Class<? extends Inventory> clazz = inventory.getClass();
            final Method method = clazz.getDeclaredMethod("getName");
            method.setAccessible(true);

            final Object nameObj = method.invoke(inventory);
            final String name = (String) nameObj;

            return name;
        } catch (final Exception ignored) {
        }

        try {
            final Class<? extends InventoryView> clazz = view.getClass();
            final Method method = clazz.getDeclaredMethod("getTitle");
            method.setAccessible(true);

            final Object nameObj = method.invoke(view);
            final String name = (String) nameObj;

            return name;
        } catch (final Exception ignored) {
        }

        return null;
    }

    public AbstractInventory getInventory(final InventoryView view) {
        AbstractInventory found = null;

        for (final AbstractInventory inventory : super.get()) {
            if (!inventory.isInventory(view))
                continue;

            found = inventory;
            break;
        }

        return found;
    }

    public AbstractInventory getInventory(String name) {
        AbstractInventory found = null;
        name = this.toEnumName(name);

        for (final AbstractInventory inventory : super.get()) {
            if (!inventory.getRawName().equals(name))
                continue;

            found = inventory;
            break;
        }

        return found;
    }

    public <T> List<AbstractInventory> getInventories(final T attachment) {
        final List<AbstractInventory> inventories = new ArrayList<>();

        for (final AbstractInventory inventory : super.get()) {
            for (final AbstractInventory.OpenSession session : inventory.getOpen().values()) {
                if (session == null)
                    continue;

                final Player player = session.getPlayer();

                if (player == null || !player.isOnline())
                    continue;

                if (!attachment.equals(inventory.getAttachment(player)))
                    continue;

                inventories.add(inventory);
            }
        }

        return inventories;
    }

    public void verify(final Player player, final Consumer<Confirmation> callback) {
        this.verify(player, callback, null);
    }

    public void verify(final Player player, final Consumer<Confirmation> callback, final String title) {
        final UUID uuid = player.getUniqueId();
        final InventoryView view = player.getOpenInventory();

        Object attachment = null;

        if (view != null) {
            final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

            if (inventory != null && inventory.getAttachment(player) != null)
                attachment = inventory.getAttachment(player);
        }

        final VerifyInventory verifyInventory = (VerifyInventory) this.getInventory("VERIFY");

        player.closeInventory();
        verifyInventory.getCallbackMap().put(uuid, callback);

        final Inventory inventory = verifyInventory.createInventory(player, title, attachment);
        player.openInventory(inventory);
    }
}