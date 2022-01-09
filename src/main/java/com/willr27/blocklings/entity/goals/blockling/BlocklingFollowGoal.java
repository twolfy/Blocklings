package com.willr27.blocklings.entity.goals.blockling;

import com.willr27.blocklings.entity.entities.blockling.BlocklingEntity;
import com.willr27.blocklings.goal.BlocklingGoal;
import com.willr27.blocklings.entity.entities.blockling.BlocklingTasks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Follows the blockling's owner when out of range.
 */
public class BlocklingFollowGoal extends BlocklingGoal
{
    /**
     * The speed modifier.
     */
    private final double speedModifier = 1.0;

    /**
     * The distance to stop following at.
     */
    private final float stopDistance = 2.0f;

    /**
     * The distance to start following at.
     */
    private final float startDistance = 4.0f;

    /**
     * The navigator used for pathing.
     */
    @Nonnull
    private final PathNavigator navigation;

    /**
     * The owner of the blockling.
     */
    private LivingEntity owner;

    /**
     * The counter used to work out when to recalc the path.
     */
    private int timeToRecalcPath;

    /**
     * The malus from water.
     */
    private float oldWaterCost;

    public BlocklingFollowGoal(UUID id, BlocklingEntity blockling, BlocklingTasks goals)
    {
        super(id, blockling, goals);

        this.navigation = blockling.getNavigation();

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        LivingEntity owner = blockling.getOwner();

        if (owner == null)
        {
            return false;
        }
        else if (owner.isSpectator())
        {
            return false;
        }
        else if (blockling.distanceToSqr(owner) < (double) (startDistance * startDistance))
        {
            return false;
        }
        else
        {
            this.owner = owner;

            return true;
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        if (navigation.isDone())
        {
            return false;
        }
        else
        {
            return !(blockling.distanceToSqr(owner) <= (double) (stopDistance * stopDistance));
        }
    }

    @Override
    public void start()
    {
        super.start();

        timeToRecalcPath = 0;
        oldWaterCost = blockling.getPathfindingMalus(PathNodeType.WATER);
        blockling.setPathfindingMalus(PathNodeType.WATER, 0.0f);
    }

    @Override
    public void stop()
    {
        super.stop();

        owner = null;
        navigation.stop();
        blockling.setPathfindingMalus(PathNodeType.WATER, oldWaterCost);
    }

    @Override
    public void tick()
    {
        super.tick();

        blockling.getLookControl().setLookAt(owner, 10.0f, (float) blockling.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0)
        {
            timeToRecalcPath = 10;

            if (!blockling.isLeashed() && !blockling.isPassenger())
            {
                navigation.stop();
                navigation.moveTo(owner, speedModifier);
            }
        }
    }
}
