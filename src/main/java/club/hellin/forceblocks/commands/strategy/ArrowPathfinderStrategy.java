package club.hellin.forceblocks.commands.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.patheloper.api.pathing.strategy.PathValidationContext;
import org.patheloper.api.pathing.strategy.PathfinderStrategy;
import org.patheloper.api.snapshot.SnapshotManager;
import org.patheloper.api.wrapper.PathBlock;
import org.patheloper.mapping.bukkit.BukkitMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public final class ArrowPathfinderStrategy implements PathfinderStrategy {
    private final Location target;

    @Override
    public boolean isValid(@NonNull PathValidationContext pathValidationContext) {
        if (pathValidationContext == null) {
            throw new NullPointerException("pathValidationContext is marked non-null but is null");
        } else {
            final SnapshotManager manager = pathValidationContext.getSnapshotManager();

            final PathBlock block = manager.getBlock(pathValidationContext.getPosition());

            final PathBlock above = manager.getBlock(pathValidationContext.getPosition().clone().add(0, 1, 0));
            final PathBlock aboveAbove = manager.getBlock(pathValidationContext.getPosition().clone().add(0, 2, 0));
            final PathBlock below = manager.getBlock(pathValidationContext.getPosition().clone().subtract(0, 1, 0));

            final Block aboveBlock = BukkitMapper.toBlock(above);
            final Block belowBlock = BukkitMapper.toBlock(below);

            if (Arrays.asList(aboveBlock, belowBlock).stream().anyMatch(b -> b.getType().name().contains("LANTERN")))
                return block.isPassable();

            final List<Block> blocks = new ArrayList<>(Arrays.asList(
                    BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(1, 0, 1))),
                    BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(-1, 0, -1))),
                    BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(-1, 0, 1))),
                    BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(1, 0, -1)))
            ));

            if (!this.target.clone().subtract(0, -1, 0).getBlock().getType().isSolid()) {
                blocks.addAll(
                        Arrays.asList(
                                BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(1, -1, 1))),
                                BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(-1, -1, -1))),
                                BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(-1, -1, 1))),
                                BukkitMapper.toBlock(manager.getBlock(pathValidationContext.getPosition().clone().add(1, -1, -1)))
                        )
                );
            }

            final boolean isDoorway = !aboveBlock.isPassable() || !belowBlock.isPassable() || !aboveAbove.isPassable();

            if (blocks.stream().filter(b -> b.getType().isSolid()).count() >= 2 && !isDoorway)
                return false;

            return block.isPassable() && above.isPassable() && below.isPassable();
        }
    }
}