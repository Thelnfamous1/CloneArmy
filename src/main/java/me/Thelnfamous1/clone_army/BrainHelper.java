package me.Thelnfamous1.clone_army;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

public class BrainHelper {
    public static <E extends Mob> void addMeleeAi(E mob, int pPriorityStart, BiPredicate<E, LivingEntity> stopAttackingWhen) {
        Brain<E> typedBrain = getTypedBrain(mob);
        typedBrain.getMemories().putIfAbsent(MemoryModuleType.ATTACK_TARGET, Optional.empty());
        typedBrain.getMemories().putIfAbsent(MemoryModuleType.ATTACK_COOLING_DOWN, Optional.empty());
        typedBrain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, pPriorityStart,
                ImmutableList.of(
                        new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F),
                        new MeleeAttack(20),
                        new StopAttackingIfTargetInvalid<>(new PredicateSecond<>(mob, stopAttackingWhen), BrainHelper::onTargetErased)),
                MemoryModuleType.ATTACK_TARGET);
    }

    // needed for villagers, as they only update their schedule via non-core tasks
    private static <E extends Mob> void onTargetErased(E attacker, LivingEntity target) {
        if(attacker.getBrain().getSchedule() != Schedule.EMPTY){
            attacker.getBrain().useDefaultActivity();
        }
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
