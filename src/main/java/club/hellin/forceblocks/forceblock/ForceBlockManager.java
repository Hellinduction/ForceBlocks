package club.hellin.forceblocks.forceblock;

import club.hellin.forceblocks.Main;
import club.hellin.forceblocks.forceblock.impl.ForceBlock;
import club.hellin.forceblocks.forceblock.impl.ForceBlockConfig;
import club.hellin.forceblocks.utils.ComponentManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class ForceBlockManager extends ComponentManager<ForceBlock> {
    private static ForceBlockManager singletonInstance;

    private static final Map<Location, ForceBlock> resultMap = new HashMap<>();

    public static ForceBlockManager getInstance() {
        if (!(singletonInstance instanceof ForceBlockManager))
            singletonInstance = new ForceBlockManager();

        return singletonInstance;
    }

    @Override
    public void init() {
        this.loadForceBlocks();
        this.startSchedulers();
    }

    private void loadForceBlocks() {
        final File boostersDir = ForceBlock.DIR;

        if (!boostersDir.exists())
            boostersDir.mkdirs();

        final File[] configFiles = boostersDir.listFiles();

        for (final File configFile : configFiles) {
            final ForceBlockConfig config = ForceBlock.load(configFile);

            try {
                ForceBlock.class.getDeclaredConstructor(ForceBlockConfig.class).newInstance(config);
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void startSchedulers() {
        Bukkit.getScheduler().runTaskTimer(Main.instance, () -> {
            for (final ForceBlock block : super.get())
                block.everySecond();
        }, 20L, 20L);

        Bukkit.getScheduler().runTaskTimer(Main.instance, () -> {
            final List<ForceBlock> blocks = super.get();

            Collections.sort(blocks, Comparator.comparing(block -> block.getConfig().getRadius()));

            for (final ForceBlock block : blocks)
                block.tick();
        }, 0L, 1L);
    }

    public List<ForceBlock> getForceBlockWithinRadius(Location location) {
        final List<ForceBlock> forceBlocks = new ArrayList<>();
        location = location.getBlock().getLocation();

        for (final ForceBlock block : super.get()) {
            if (!location.getWorld().equals(block.getLocation().getWorld()))
                continue;

            if (location.distance(block.getLocation()) > block.getConfig().getRadius())
                continue;

            forceBlocks.add(block);
        }

        return forceBlocks;
    }

    public ForceBlock getClosestForceBlock(Location location, final Player owner) {
        location = location.getBlock().getLocation();

        if (resultMap.containsKey(location)) {
            final ForceBlock forceBlock = resultMap.get(location);

            if (!forceBlock.isDeleted())
                return forceBlock;
            else
                resultMap.remove(location);
        }

        List<ForceBlock> forceBlocks = this.getForceBlockWithinRadius(location);
        forceBlocks = forceBlocks.stream().filter(block -> block.isOwner(owner)).collect(Collectors.toList());

        final Location finalLocation = location;

        Collections.sort(forceBlocks, (block1, block2) -> {
            final double dist1 = block1.getLocation().distance(finalLocation);
            final double dist2 = block2.getLocation().distance(finalLocation);

            final int comparison = Double.compare(dist1, dist2);
            return comparison;
        });

        if (forceBlocks.isEmpty())
            return null;

        final ForceBlock forceBlock = forceBlocks.get(0);
        resultMap.put(location, forceBlock);
        return forceBlock;
    }

    public ForceBlock getClosestForceBlock(final Location location) {
        final List<ForceBlock> forceBlocks = this.getForceBlockWithinRadius(location);

        ForceBlock closest = null;
        double closestDist = Integer.MAX_VALUE;

        for (final ForceBlock block : forceBlocks) {
            final Location loc = block.getLocation();
            final double dist = location.distance(loc);

            if (dist < closestDist) {
                closestDist = dist;
                closest = block;
            }
        }

        return closest;
    }

    public ForceBlock getForceBlock(Location location) {
        location = location.getBlock().getLocation();

        for (final ForceBlock block : super.get()) {
            if (!block.getLocation().getBlock().getLocation().equals(location))
                continue;

            return block;
        }

        return null;
    }

    public void shutdown() {
        this.closeAllInventories();
        this.deleteHolograms();
    }

    public void closeAllInventories() {
        for (final ForceBlock block : super.get())
            block.closeAll();
    }

    public void deleteHolograms() {
        for (final ForceBlock block : super.get())
            block.deleteHologram();
    }
}