package club.hellin.forceblocks.customentities.goals;

import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.level.pathfinder.PathEntity;
import org.bukkit.Location;

public final class PathfinderGoalWalkToTile extends PathfinderGoal {
    private final EntityInsentient entity;
    private final Location loc;

    private PathEntity path;

    public PathfinderGoalWalkToTile(final EntityInsentient entity, final Location loc) {
        this.entity = entity;
        this.loc = loc;
    }

    @Override
    public boolean a() {
        final NavigationAbstract navigation = this.entity.K();

        this.path = navigation.a(loc.getX(), loc.getY(), loc.getZ(), 1);
        this.entity.K();

        if (this.path != null)
            this.c();

        return this.path != null;
    }

    @Override
    public void c() {
        this.entity.K().a(this.path, 2D);
    }
}