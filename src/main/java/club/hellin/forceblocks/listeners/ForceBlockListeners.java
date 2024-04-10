package club.hellin.forceblocks.listeners;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.commands.ApplyForceBlockCommand;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.forceblock.impl.ForceBlockConfig;
import club.hellin.forceblocks.forceblock.impl.ForceMode;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONArray;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ForceBlockListeners implements Listener {
    private static final String NBT_TRUSTED_TAG = "force_block_trusted";
    private static final String NBT_MODE_TAG = "force_block_mode";
    private static final String NBT_AFFECT_PLAYERS_TAG = "force_block_affect_players";
    private static final String NBT_AFFECT_NON_HOSTILE_MOBS_TAG = "force_block_affect_non_hostile_mobs";
    private static final String NBT_AFFECT_EXPLOSIVES_TAG = "force_block_affect_explosives";

    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {
        final ItemStack item = e.getItemInHand();
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Block block = e.getBlock();

        final ForceBlock fb = ForceBlockManager.getInstance().getForceBlock(e.getBlockAgainst().getLocation());
        if (fb != null) {
            e.setCancelled(true);
            return;
        }

        if (item == null || item.getType() == Material.AIR)
            return;

        if (!item.getType().isBlock())
            return;

        final NBTItem nbt = new NBTItem(item);

        if (!nbt.hasKey(Main.NBT_RADIUS_TAG))
            return;

        final int radius = nbt.getInteger(Main.NBT_RADIUS_TAG);
        final ForceBlock forceBlock = new ForceBlock(block.getLocation(), radius, uuid);

        if (nbt.hasTag(NBT_TRUSTED_TAG) && nbt.hasTag(NBT_MODE_TAG)) {
            final String trustedJson = nbt.getString(NBT_TRUSTED_TAG);
            final JSONArray array = new JSONArray(trustedJson);
            final List<UUID> trusted = array.toList().stream().map(object -> UUID.fromString((String) object)).collect(Collectors.toList());

            if (trusted.contains(uuid))
                trusted.remove(uuid);

            final ForceMode mode = ForceMode.valueOf(nbt.getString(NBT_MODE_TAG));

            forceBlock.getConfig().setTrusted(trusted);
            forceBlock.getConfig().setMode(mode);

            forceBlock.save();
        }

        if (nbt.hasTag(NBT_AFFECT_PLAYERS_TAG) && nbt.hasTag(NBT_AFFECT_NON_HOSTILE_MOBS_TAG) && nbt.hasTag(NBT_AFFECT_EXPLOSIVES_TAG)) {
            final boolean affectPlayers = nbt.getBoolean(NBT_AFFECT_PLAYERS_TAG);
            final boolean affectNonHostileMobs = nbt.getBoolean(NBT_AFFECT_NON_HOSTILE_MOBS_TAG);
            final boolean affectExplosives = nbt.getBoolean(NBT_AFFECT_EXPLOSIVES_TAG);

            final ForceBlockConfig config = forceBlock.getConfig();

            config.setAffectPlayers(affectPlayers);
            config.setAffectNonHostileMobs(affectNonHostileMobs);
            config.setAffectExplosives(affectExplosives);

            forceBlock.save();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            return;
        }

        final Material type = block.getType();
        ItemStack item = new ItemStack(type);
        item = ApplyForceBlockCommand.apply(item, forceBlock.getConfig().getRadius());

        final NBTItem nbt = new NBTItem(item);

        final JSONArray array = new JSONArray(forceBlock.getConfig().getTrusted());
        final String json = array.toString();

        final ForceBlockConfig config = forceBlock.getConfig();

        nbt.setString(NBT_TRUSTED_TAG, json);
        nbt.setString(NBT_MODE_TAG, config.getMode().name());

        nbt.setBoolean(NBT_AFFECT_PLAYERS_TAG, config.isAffectPlayers());
        nbt.setBoolean(NBT_AFFECT_NON_HOSTILE_MOBS_TAG, config.isAffectNonHostileMobs());
        nbt.setBoolean(NBT_AFFECT_EXPLOSIVES_TAG, config.isAffectExplosives());

        item = nbt.getItem();

        final ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.GRAY + "" + ChatColor.ITALIC + "Mode: " + ChatColor.YELLOW + forceBlock.getConfig().getMode().name()));

        item.setItemMeta(meta);

        final PlayerInventory inv = player.getInventory();
        inv.addItem(item);
        player.updateInventory();

        e.setDropItems(false);
        forceBlock.delete(player);
    }

//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//    public void onPlayerMove(final PlayerMoveEvent e) {
//        if (this.floorLocation(e.getTo()).equals(this.floorLocation(e.getFrom())))
//            return;
//
//        final Player player = e.getPlayer();
//        final ForceBlock forceBlock = ForceBlockManager.getInstance().getClosestForceBlock(e.getTo());
//
//        if (forceBlock == null)
//            return;
//
//        if (forceBlock.isPermitted(player))
//            return;
//
//        switch (forceBlock.getConfig().getMode()) {
//            case MAGNET: {
//                forceBlock.magnet(player);
//                break;
//            }
//
//            case FORCE_FIELD: {
//                forceBlock.forceField(player);
//                break;
//            }
//        }
//    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final Action action = e.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK)
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

//    private Location floorLocation(final Location location) {
//        return new Location(location.getWorld(), Math.floor(location.getX()), Math.floor(location.getY()), Math.floor(location.getZ()));
//    }

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