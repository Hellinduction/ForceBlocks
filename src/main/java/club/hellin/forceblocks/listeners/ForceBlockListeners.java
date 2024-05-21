package club.hellin.forceblocks.listeners;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.commands.ApplyForceBlockCommand;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.forceblock.impl.ForceBlockConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ForceBlockListeners implements Listener {
    private static final String NBT_JSON_CONFIG_TAG = "force_block_json_config";

    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent e) {
        this.handlePiston(e, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(final BlockPistonRetractEvent e) {
        this.handlePiston(e, e.getBlocks());
    }

    private void handlePiston(final BlockPistonEvent e, List<Block> blocks) {
        final Block block = e.getBlock();
        final BlockFace face = e.getDirection();

        blocks = new ArrayList<>(blocks);
        blocks.add(block);

        final boolean involved = blocks.stream().anyMatch(b -> ForceBlockManager.getInstance().getForceBlock(b.getLocation()) != null || ForceBlockManager.getInstance().getForceBlock(b.getRelative(face).getLocation()) != null);

        if (!involved)
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {
        final ItemStack item = e.getItemInHand();
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Block block = e.getBlock();

        final ForceBlock placedAgainstFb = ForceBlockManager.getInstance().getForceBlock(e.getBlockAgainst().getLocation());

        if (placedAgainstFb != null) {
            e.setCancelled(true);
            return;
        }

        if (item == null || item.getType() == Material.AIR)
            return;

        if (!item.getType().isBlock())
            return;

        final NBTItem nbt = new NBTItem(item);
        final boolean isForceBlock = nbt.hasKey(Main.NBT_RADIUS_TAG);

        final ForceBlock fb = ForceBlockManager.getInstance().getForceBlock(block.getLocation());

        if (fb != null && (isForceBlock || !ApplyForceBlockCommand.canBeForceBlock(item.getType()))) {
            if (isForceBlock)
                player.sendMessage(ChatColor.RED + "You cant do that :/");
            else
                player.sendMessage(ChatColor.RED + "The material is not hard enough to be a Force Block.");

            e.setCancelled(true);
            return;
        }

        if (!isForceBlock)
            return;

        final int radius = nbt.getInteger(Main.NBT_RADIUS_TAG);

        if (!nbt.hasTag(NBT_JSON_CONFIG_TAG)) {
            new ForceBlock(block.getLocation(), radius, uuid, item.getType());
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();
        final String json = nbt.getString(NBT_JSON_CONFIG_TAG);
        final ForceBlockConfig config;

        try {
            config = mapper.readValue(json, ForceBlockConfig.class);
        } catch (final JsonProcessingException exception) {
            exception.printStackTrace();
            player.sendMessage(ChatColor.RED + "Something went very wrong! Please report this to an administrator.");
            return;
        }

        config.setLocation(block.getLocation());
        config.setRadius(radius);
        config.setOwner(uuid);
        config.setMaterial(item.getType());

        final ForceBlock forceBlock = new ForceBlock(config);
        forceBlock.save();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Player player = e.getPlayer();

        final ForceBlock forceBlock = ForceBlockManager.getInstance().getForceBlock(block.getLocation());

        if (forceBlock == null)
            return;

        if (!forceBlock.isOwner(player)) {
            final OfflinePlayer owner = Bukkit.getOfflinePlayer(forceBlock.getConfig().getOwner());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&cYou cannot break &e%s%s&c Force Block!", owner.getName(), owner.getName().toLowerCase().endsWith("s") ? "'" : "'s")));
            e.setCancelled(true);
        }

        forceBlock.getConfig().setMaterial(block.getType());
        forceBlock.save();
    }

    private static ItemStack apply(ItemStack item, final ForceBlock forceBlock) {
        item = ApplyForceBlockCommand.apply(item, forceBlock.getConfig().getRadius());

        final NBTItem nbt = new NBTItem(item);
        final ForceBlockConfig config = forceBlock.getConfig();

        final ObjectMapper mapper = new ObjectMapper();
        final String json;

        try {
            json = mapper.writeValueAsString(config);
        } catch (final JsonProcessingException exception) {
            exception.printStackTrace();
            return item;
        }

        nbt.setString(NBT_JSON_CONFIG_TAG, json);
        item = nbt.getItem();

        final ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.GRAY + "" + ChatColor.ITALIC + "Mode: " + ChatColor.YELLOW + forceBlock.getConfig().getMode().name()));
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockDropItem(final BlockDropItemEvent e) {
        final Block block = e.getBlock();
        final Location loc = block.getLocation();
        final Player player = e.getPlayer();

        final ForceBlock forceBlock = ForceBlockManager.getInstance().getForceBlock(block.getLocation());

        if (forceBlock == null)
            return;

        e.setCancelled(true);

        final List<Item> itemEntities = e.getItems();
        final Map<Location, ItemStack> items = itemEntities.stream().map(itemEntity -> {
            final Location location = itemEntity.getLocation();
            final ItemStack itemStack = itemEntity.getItemStack();

            return new AbstractMap.SimpleEntry<>(location, ForceBlockListeners.apply(itemStack, forceBlock));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        itemEntities.clear();

        for (Map.Entry<Location, ItemStack> entry : new ArrayList<>(items.entrySet())) {
            final ItemStack item = entry.getValue();
            item.setType(forceBlock.getConfig().getMaterial());
            item.setAmount(1);

            final Map<Integer, ItemStack> result = player.getInventory().addItem(new ItemStack[]{ item });

            if (result.values().stream().anyMatch(i -> i.isSimilar(item))) {
                e.setCancelled(false);
                e.getItems().clear();

                loc.getWorld().dropItemNaturally(loc, item);
            }

            break;
        }

        forceBlock.delete(player);
    }

    private boolean isForceBlock(final Item itemEntity) {
        final ItemStack item = itemEntity.getItemStack();

        if (item == null || item.getType() == Material.AIR)
            return false;

        if (!item.getType().isBlock())
            return false;

        final NBTItem nbt = new NBTItem(item);
        final boolean isForceBlock = nbt.hasKey(Main.NBT_RADIUS_TAG);

        return isForceBlock;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent e) {
        final Entity entity = e.getEntity();

        if (!(entity instanceof Item))
            return;

        final Item itemEntity = (Item) entity;
        final boolean isForceBlock = this.isForceBlock(itemEntity);

        if (!isForceBlock)
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(final ItemDespawnEvent e) {
        final Entity entity = e.getEntity();

        if (!(entity instanceof Item))
            return;

        final Item itemEntity = (Item) entity;
        final boolean isForceBlock = this.isForceBlock(itemEntity);

        if (!isForceBlock)
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final Action action = e.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK || player.isSneaking())
            return;

        final long now = Instant.now().getEpochSecond();
        final long lastInteraction = this.lastInteraction.getOrDefault(player.getUniqueId(), 0L);
        final long timeSince = now - lastInteraction;

        if (timeSince < 2)
            return;

        this.lastInteraction.put(player.getUniqueId(), now);

        final Block block = e.getClickedBlock();
        final Location location = block.getLocation();
        final ForceBlock forceBlock = ForceBlockManager.getInstance().getForceBlock(location);

        if (forceBlock == null)
            return;

        if (!forceBlock.isPermitted(player)) {
            player.sendMessage(ChatColor.RED + "You are not permitted to interact with this Force Block.");
            return;
        }

        forceBlock.openGui(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        final Block block = e.getBlock();

        final ForceBlock forceBlock = ForceBlockManager.getInstance().getForceBlock(block.getLocation());

        if (forceBlock == null)
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent e) {
        this.handleExplosion(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent e) {
        this.handleExplosion(e.blockList());
    }

    private void handleExplosion(final List<Block> blocks) {
        for (final Block block : new ArrayList<>(blocks)) {
            final ForceBlock forceBlock = ForceBlockManager.getInstance().getForceBlock(block.getLocation());

            if (forceBlock == null)
                continue;

            blocks.remove(block);
        }
    }

    public static Location center(final Location loc) {
        final int x = (int) Math.floor(loc.getX());
        final int z = (int) Math.floor(loc.getZ());

        final float currentYaw = loc.getYaw();
        final float closestYaw = getClosestYaw(currentYaw);

        final Location newLoc = new Location(loc.getWorld(), x + 0.5, loc.getY(), z + 0.5, closestYaw, 0);
        return newLoc;
    }

    private static float getClosestYaw(final float yaw) {
        float closestYaw = Math.round(yaw / 90) * 90;
        closestYaw = (closestYaw + 180) % 360 - 180;
        return closestYaw;
    }
}