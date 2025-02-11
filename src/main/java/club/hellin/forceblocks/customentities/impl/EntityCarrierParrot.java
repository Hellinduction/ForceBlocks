package club.hellin.forceblocks.customentities.impl;

import club.hellin.forceblocks.customentities.goals.PathfinderGoalWalkToTile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Parrot;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class EntityCarrierParrot extends Parrot {
    private final Location target;

    public EntityCarrierParrot(final Location loc, final Location target) {
        super(EntityType.PARROT, ((CraftWorld) loc.getWorld()).getHandle());

        this.target = target;

        super.teleportTo(loc.getX(), loc.getY(), loc.getZ()); // Set position
        super.setHealth(20F); // Set health
    }

    public void init() {
        this.clearSelectors();

        final GoalSelector selector = super.goalSelector;

        selector.addGoal(1, new FloatGoal(this));
        selector.addGoal(2, new PathfinderGoalWalkToTile(this, target));
    }

    public void addPassenger(final Player player) {
        ((CraftPlayer) player).getHandle().startRiding(this);
    }

    private void clearSelectors() {
        super.goalSelector.getAvailableGoals().clear();
        super.targetSelector.getAvailableGoals().clear();
    }
}