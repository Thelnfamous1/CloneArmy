package me.Thelnfamous1.clone_army;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BrainHelper {
    public static <E extends Mob> void addMeleeAi(E mob, int pPriorityStart) {
        Brain<E> typedBrain = getTypedBrain(mob);
        typedBrain.getMemories().putIfAbsent(MemoryModuleType.ATTACK_TARGET, Optional.empty());
        typedBrain.getMemories().putIfAbsent(MemoryModuleType.ATTACK_COOLING_DOWN, Optional.empty());
        typedBrain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, pPriorityStart,
                ImmutableList.of(new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F), new MeleeAttack(20), new StopAttackingIfTargetInvalid<>()), MemoryModuleType.ATTACK_TARGET);
    }

    public static <E extends Mob> Brain<E> getTypedBrain(E mob) {
        return (Brain<E>) mob.getBrain();
    }

    public static void addBehavior(Map<Integer, Map<Activity, Set<Behavior<?>>>> availableBehaviorsByPriority, int targetPriority, Activity targetActivity, Behavior<?> behavior) {
        availableBehaviorsByPriority.computeIfAbsent(targetPriority, (priority) -> Maps.newHashMap())
                .computeIfAbsent(targetActivity, (activity) -> Sets.newLinkedHashSet())
                .add(behavior);
    }

    public static boolean hasActivity(Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements, Activity activity){
        return activityRequirements.containsKey(activity);
    }

    public static boolean hasBehavior(Map<Integer, Map<Activity, Set<Behavior<?>>>> availableBehaviorsByPriority, Class<? extends Behavior> behaviorClass) {
        boolean foundMatch = false;
        for(Map<Activity, Set<Behavior<?>>> availableBehaviors : availableBehaviorsByPriority.values()) {
            for(Map.Entry<Activity, Set<Behavior<?>>> entry : availableBehaviors.entrySet()) {
                for(Behavior<?> behavior : entry.getValue()) {
                    if (behaviorClass.isInstance(behavior)) {
                        foundMatch = true;
                        break;
                    }
                }
                if(foundMatch) break;
            }
            if(foundMatch) break;
        }
        return foundMatch;
    }
}
