package me.Thelnfamous1.clone_army.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(Brain.class)
public interface BrainAccess<E extends LivingEntity> {

    @Accessor("availableBehaviorsByPriority")
    Map<Integer, Map<Activity, Set<Behavior<?>>>> clone_army_getAvailableBehaviorsByPriority();

    @Accessor("activityRequirements")
    Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> clone_army_getActivityRequirements();

    @Accessor("activityMemoriesToEraseWhenStopped")
    Map<Activity, Set<MemoryModuleType<?>>> clone_army_getActivityMemoriesToEraseWhenStopped();
}
