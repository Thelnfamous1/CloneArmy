package me.Thelnfamous1.clone_army;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommandableCombat {
    public static final Predicate<Mob> CAN_ATTACK = le -> !isAlwaysPassive(le.getType());
    public static final Function<Mob, Optional<? extends LivingEntity>> COMMANDED_TARGET_FINDER = CommandableCombat::findNearestValidAttackTarget;
    private static final Map<EntityType<?>, Boolean> HOSTILITY_TOGGLES = new HashMap<>();
    private static final Map<EntityType<?>, UUID> PLAYER_TARGETS = new HashMap<>();
    private static final Map<EntityType<?>, EntityType<?>> MOB_COMBAT = new HashMap<>();

    public static Optional<Boolean> getHostilityToggle(EntityType<?> type){
        return Optional.ofNullable(HOSTILITY_TOGGLES.get(type));
    }

    public static void setHostilityToggle(EntityType<?> type, boolean hostilityToggle){
        HOSTILITY_TOGGLES.put(type, hostilityToggle);
    }

    public static void clearHostilityToggle(EntityType<?> type){
        HOSTILITY_TOGGLES.remove(type);
    }

    public static boolean isAlwaysHostile(EntityType<?> type){
        return getHostilityToggle(type).orElse(false);
    }

    public static boolean isAlwaysPassive(EntityType<?> type){
        return getHostilityToggle(type).isPresent() && !getHostilityToggle(type).get();
    }

    public static Optional<UUID> getPlayerTarget(EntityType<?> attacker){
        return Optional.ofNullable(PLAYER_TARGETS.get(attacker));
    }

    public static void setPlayerTarget(EntityType<?> type, UUID uuid) {
        PLAYER_TARGETS.put(type, uuid);
        clearMobCombat(type);
    }

    public static void clearPlayerTarget(EntityType<?> type) {
        PLAYER_TARGETS.remove(type);
    }

    public static void clearAllPlayerTargets() {
        PLAYER_TARGETS.clear();
    }

    public static Optional<EntityType<?>> getMobCombat(EntityType<?> attacker){
        return Optional.ofNullable(MOB_COMBAT.get(attacker));
    }

    public static void setMobCombat(EntityType<?> attacker, EntityType<?> target) {
        MOB_COMBAT.put(attacker, target);
        clearPlayerTarget(attacker);
    }

    public static void clearMobCombat() {
        MOB_COMBAT.clear();
    }

    public static void clearMobCombat(EntityType<?> attacker) {
        MOB_COMBAT.remove(attacker);
    }

    public static Optional<LivingEntity> findNearestValidAttackTarget(Mob m) {
        if(!m.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)) return Optional.empty();

        NearestVisibleLivingEntities nearestVisibleLivingEntities = m.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty());
        return nearestVisibleLivingEntities
                .findClosest(le -> isAllowedToAttack(m, le, false))
                .or(() -> isAlwaysHostile(m.getType()) ? nearestVisibleLivingEntities.findClosest(le -> true) : Optional.empty());
    }

    public static boolean isAllowedToAttack(Entity attacker, Entity target, boolean defaultOutcome){
        return isCommandedAttackTarget(attacker, target).orElse(defaultOutcome);
    }

    public static boolean hasCommandedAttackTarget(Entity entity) {
        return PLAYER_TARGETS.containsKey(entity.getType()) || MOB_COMBAT.containsKey(entity.getType());
    }

    public static Optional<Boolean> isCommandedAttackTarget(Entity attacker, Entity target) {
        return getPlayerTarget(attacker.getType())
                .map(uuid -> attacker.level.getPlayerByUUID(uuid) == target)
                .or(() -> getMobCombat(attacker.getType())
                        .map(et -> target.getType() == et));
    }

}
