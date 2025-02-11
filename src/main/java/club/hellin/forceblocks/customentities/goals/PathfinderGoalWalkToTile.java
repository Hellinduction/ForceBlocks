package club.hellin.forceblocks.customentities.goals;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Location;

public final class PathfinderGoalWalkToTile extends Goal {
    private final Mob entity;
    private final Location loc;

    private Path path;

    public PathfinderGoalWalkToTile(final Mob entity, final Location loc) {
        this.entity = entity;
        this.loc = loc;
    }

    @Override
    public void start() {
        this.entity.getNavigation().moveTo(this.path, 2D);
    }

    @Override
    public boolean canUse() {
        final PathNavigation navigation = this.entity.getNavigation();

        this.path = navigation.createPath(loc.getX(), loc.getY(), loc.getZ(), 1);
        this.entity.getNavigation();

        if (this.path != null)
            this.start();

        return this.path != null;
    }
}