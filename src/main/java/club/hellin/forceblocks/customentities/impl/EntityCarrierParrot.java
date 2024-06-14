package club.hellin.forceblocks.customentities.impl;

import club.hellin.forceblocks.customentities.goals.PathfinderGoalWalkToTile;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.animal.EntityParrot;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class EntityCarrierParrot extends EntityParrot {
    private final Location target;

    public EntityCarrierParrot(final Location loc, final Location target) {
        super(EntityTypes.ax, ((CraftWorld) loc.getWorld()).getHandle());

        this.target = target;

        super.a_(loc.getX(), loc.getY(), loc.getZ()); // Set position
        super.c(20F); // Set health
    }

    public void init() {
        this.clearSelectors();

        final PathfinderGoalSelector selector = super.bS;

        selector.a(1, new PathfinderGoalFloat(this));
        selector.a(2, new PathfinderGoalWalkToTile(this, target));
    }

    public void addPassenger(final Player player) {
        ((CraftPlayer) player).getHandle().n(this);
    }

    private void clearSelectors() {
        super.bS.b().clear();
        super.bS.b().clear();
    }
}