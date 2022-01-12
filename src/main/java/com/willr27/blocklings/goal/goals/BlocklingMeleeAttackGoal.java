package com.willr27.blocklings.goal.goals;

import com.willr27.blocklings.entity.EntityUtil;
import com.willr27.blocklings.entity.entities.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.entities.blockling.BlocklingHand;
import com.willr27.blocklings.entity.entities.blockling.BlocklingTasks;
import com.willr27.blocklings.goal.IHasTargetGoal;
import com.willr27.blocklings.goal.BlocklingGoal;
import com.willr27.blocklings.goal.BlocklingTargetGoal;
import com.willr27.blocklings.whitelist.GoalWhitelist;
import com.willr27.blocklings.whitelist.Whitelist;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Contains common behaviour shared between melee attack goals.
 *
 * @param <T> the type of the corresponding target goal.
 */
public abstract class BlocklingMeleeAttackGoal<T extends BlocklingTargetGoal<?>> extends BlocklingGoal implements IHasTargetGoal<T>
{
    /**
     * The current path to the target.
     */
    @Nullable
    private Path path = null;

    /**
     * Counts the number of ticks elapsed between path recalcs.
     */
    private int recalcPathCounter = 0;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingMeleeAttackGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        GoalWhitelist whitelist = new GoalWhitelist("540241cd-085a-4c1f-9e90-8aea973568a8", "targets", Whitelist.Type.ENTITY, this);
        EntityUtil.VALID_ATTACK_TARGETS.keySet().forEach(type -> whitelist.put(type, true));
        whitelists.add(whitelist);
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        LivingEntity target = blockling.getTarget();

        if (target == null || !target.isAlive())
        {
            return false;
        }

        path = blockling.getNavigation().createPath(target, 0);

        if (path == null)
        {
            if (!isInRange(target))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        LivingEntity target = blockling.getTarget();

        if (target == null || !target.isAlive())
        {
            return false;
        }

        if (path == null)
        {
            if (!isInRange(target))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void start()
    {
        super.start();

        blockling.setAggressive(true);
        blockling.getNavigation().moveTo(path, 1.0);
    }

    @Override
    public void stop()
    {
        super.stop();

        blockling.setTarget(null);
        blockling.setAggressive(false);
        blockling.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        super.tick();

        LivingEntity target = blockling.getTarget();

        if (isInRange(target))
        {
            BlocklingHand attackingHand = blockling.getEquipment().findAttackingHand();
            attackingHand = attackingHand == BlocklingHand.BOTH ? blockling.getActions().attack.getRecentHand() == BlocklingHand.OFF ? BlocklingHand.MAIN : BlocklingHand.OFF : attackingHand;

            if (blockling.getActions().attack.tryStart(attackingHand))
            {
                blockling.doHurtTarget(target);
                path = blockling.getNavigation().createPath(target, 0);
                blockling.getNavigation().moveTo(path, 1.0);
                recalcPathCounter = 0;
            }
        }
        else if (recalcPathCounter >= 20)
        {
            path = blockling.getNavigation().createPath(target, 0);
            blockling.getNavigation().moveTo(path, 1.0);
            recalcPathCounter = 0;
        }

        recalcPathCounter++;
    }

    /**
     * @param target the target entity.
     * @return true if the blockling is in range of the target entity.
     */
    private boolean isInRange(LivingEntity target)
    {
        return blockling.distanceToSqr(target.getX(), target.getY() + target.getBbHeight() / 2.0f, target.getZ()) < 4.0f;
    }
}