package club.hellin.forceblocks.commands.strategy;

import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;
import de.metaphoriker.pathetic.bukkit.mapper.BukkitMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public final class ArrowPathfinderStrategy implements PathFilter {
    private final Location target;

    @Override
    public boolean filter(@NonNull PathValidationContext pathValidationContext) {
        if (pathValidationContext == null) {
            throw new NullPointerException("pathValidationContext is marked non-null but is null");
        } else {
            final PathPosition block = pathValidationContext.getPosition();

            final PathPosition above = pathValidationContext.getPosition().clone().add(0, 1, 0);
            final PathPosition aboveAbove = pathValidationContext.getPosition().clone().add(0, 2, 0);
            final PathPosition below = pathValidationContext.getPosition().clone().subtract(0, 1, 0);

            final Block aboveBlock = BukkitMapper.toLocation(above).getBlock();
            final Block belowBlock = BukkitMapper.toLocation(below).getBlock();

            if (Arrays.asList(aboveBlock, belowBlock).stream().anyMatch(b -> b.getType().name().contains("LANTERN")))
                return BukkitMapper.toLocation(block).getBlock().isPassable();

            final List<Block> blocks = new ArrayList<>(Arrays.asList(
                    BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(1, 0, 1)).getBlock(),
                    BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(-1, 0, -1)).getBlock(),
                    BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(-1, 0, 1)).getBlock(),
                    BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(1, 0, -1)).getBlock()
            ));

            if (!this.target.clone().subtract(0, -1, 0).getBlock().getType().isSolid()) {
                blocks.addAll(
                        Arrays.asList(
                                BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(1, -1, 1)).getBlock(),
                                BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(-1, -1, -1)).getBlock(),
                                BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(-1, -1, 1)).getBlock(),
                                BukkitMapper.toLocation(pathValidationContext.getPosition().clone().add(1, -1, -1)).getBlock()
                        )
                );
            }

            final boolean isDoorway = !aboveBlock.isPassable() || !belowBlock.isPassable() || !BukkitMapper.toLocation(aboveAbove).getBlock().isPassable();

            if (blocks.stream().filter(b -> b.getType().isSolid()).count() >= 2 && !isDoorway)
                return false;

            return BukkitMapper.toLocation(block).getBlock().isPassable() && BukkitMapper.toLocation(above).getBlock().isPassable() && BukkitMapper.toLocation(below).getBlock().isPassable();
        }
    }
}