package me.Thelnfamous1.clone_army;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.marwinekk.been.BeenMod;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import me.Thelnfamous1.clone_army.duck.EntityHolder;
import me.Thelnfamous1.clone_army.duck.Summonable;
import me.Thelnfamous1.clone_army.mixin.BrainAccess;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.*;

@Mod(CloneArmy.MODID)
public class CloneArmy {
    public static final String MODID = "clone_army";
    public static final Logger LOGGER = LogUtils.getLogger();


    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(MODID, "sync_channel");
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel SYNC_CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME, () -> "1.0",
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int INDEX = 0;

    private static final Map<EntityType<?>, Boolean> HOSTILITY_TOGGLES = new HashMap<>();

    private static final Map<EntityType<?>, UUID> PLAYER_TARGETS = new HashMap<>();

    private static final Map<EntityType<?>, EntityType<?>> MOB_COMBAT = new HashMap<>();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> CLONE_REMOTE = ITEMS.register("clone_remote", () -> new CloneRemoteItem(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, MODID);

    private static final RegistryObject<SingletonArgumentInfo<EntityToggleRemoteArgument>> ENTITY_TOGGLE_REMOTE_COMMAND_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register("entity_toggle_remote", () ->
            ArgumentTypeInfos.registerByClass(EntityToggleRemoteArgument.class, SingletonArgumentInfo.contextFree(EntityToggleRemoteArgument::id)));

    public CloneArmy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(EventPriority.LOWEST, this::onModifyEntityAttributes);
        modEventBus.addListener(this::onCommonSetup);
        ITEMS.register(modEventBus);
        COMMAND_ARGUMENT_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onEntityJoinWorld);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLivingXpDrop);
        MinecraftForge.EVENT_BUS.addListener(this::onStartTracking);
    }

    public static void setHostile(EntityType<?> type, boolean hostile){
        HOSTILITY_TOGGLES.put(type, hostile);
    }

    public static boolean isHostile(EntityType<?> type){
        return HOSTILITY_TOGGLES.getOrDefault(type, true);
    }

    public static boolean isCloneType(EntityType<?> et) {
        return EntityType.getKey(et).getNamespace().equals(BeenMod.MODID);
    }

    public static void removePlayerTarget(EntityType<?> type) {
        PLAYER_TARGETS.remove(type);
    }

    public static void clearPlayerTargets() {
        PLAYER_TARGETS.clear();
    }

    public static void setPlayerTarget(EntityType<?> type, UUID uuid) {
        PLAYER_TARGETS.put(type, uuid);
        removeMobCombat(type);
    }

    public static Optional<UUID> getPlayerTarget(EntityType<?> attacker){
        return Optional.ofNullable(PLAYER_TARGETS.get(attacker));
    }

    public static void clearMobCombat() {
        MOB_COMBAT.clear();
    }

    public static void removeMobCombat(EntityType<?> attacker) {
        MOB_COMBAT.remove(attacker);
    }

    public static void setMobCombat(EntityType<?> attacker, EntityType<?> target) {
        MOB_COMBAT.put(attacker, target);
        removePlayerTarget(attacker);
    }

    public static Optional<EntityType<?>> getMobCombat(EntityType<?> attacker){
        return Optional.ofNullable(MOB_COMBAT.get(attacker));
    }

    public static boolean hasCustomAttackTarget(Entity entity) {
        return hasMobCombat(entity) || hasPlayerTarget(entity);
    }

    private void onModifyEntityAttributes(EntityAttributeModificationEvent event){
        event.getTypes().forEach(et -> {
            if(!event.has(et, Attributes.ATTACK_DAMAGE)){
                event.add(et, Attributes.ATTACK_DAMAGE);
            }
        });
    }

    private void onCommonSetup(FMLCommonSetupEvent event){
        event.enqueueWork(() -> {
            CACommands.registerSuggestionProviders();
            registerPackets();
        });
    }

    public static void registerPackets() {
        SYNC_CHANNEL.registerMessage(INDEX++, ClientboundSummonablePacket.class, ClientboundSummonablePacket::encode, ClientboundSummonablePacket::new, ClientboundSummonablePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private void onRegisterCommands(RegisterCommandsEvent event){
        CACommands.registerToggleRemote(event.getDispatcher());
        CACommands.registerToggleHostility(event.getDispatcher());
        CACommands.registerTargetPlayer(event.getDispatcher());
        CACommands.registerMobCombat(event.getDispatcher());
    }

    private void onEntityJoinWorld(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide) return;
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof Mob mob){
            ((EntityHolder)mob.getBrain()).setHeldEntity(mob);
            Map<Integer, Map<Activity, Set<Behavior<?>>>> availableBehaviorsByPriority = ((BrainAccess<?>) mob.getBrain()).clone_army_getAvailableBehaviorsByPriority();
            Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = ((BrainAccess<?>) mob.getBrain()).clone_army_getActivityRequirements();
            if(!availableBehaviorsByPriority.isEmpty()){
                if(!hasBehavior(availableBehaviorsByPriority, StartAttacking.class)){
                    addBehavior(availableBehaviorsByPriority, 0, Activity.CORE, new StartAttacking<>(le -> true, CloneArmy::findNearestAttackTarget));
                }
                if(!hasActivity(activityRequirements, Activity.FIGHT)) {
                    addMeleeAi(mob);
                }
            } else{
                if(mob instanceof PathfinderMob pf && mob.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).noneMatch(goal -> goal instanceof MeleeAttackGoal)){
                    mob.goalSelector.addGoal(4, new MeleeAttackGoal(pf, 1.0D, true));
                }
                mob.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, true, target ->
                        isValidAttackTarget(mob, target)));
            }
        }
    }

    private static void addMeleeAi(Mob mob) {
        mob.getBrain().getMemories().putIfAbsent(MemoryModuleType.ATTACK_TARGET, Optional.empty());
        mob.getBrain().getMemories().putIfAbsent(MemoryModuleType.ATTACK_COOLING_DOWN, Optional.empty());
        mob.getBrain().addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 0,
                ImmutableList.<Behavior<Mob>>of(new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F), new MeleeAttack(20), new StopAttackingIfTargetInvalid()), MemoryModuleType.ATTACK_TARGET);
    }

    private static void addBehavior(Map<Integer, Map<Activity, Set<Behavior<?>>>> availableBehaviorsByPriority, int targetPriority, Activity targetActivity, Behavior<?> behavior) {
        availableBehaviorsByPriority.computeIfAbsent(targetPriority, (priority) -> Maps.newHashMap())
                .computeIfAbsent(targetActivity, (activity) -> Sets.newLinkedHashSet())
                .add(behavior);
    }

    private static boolean hasActivity(Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements, Activity activity){
        return activityRequirements.containsKey(activity);
    }

    private static boolean hasBehavior(Map<Integer, Map<Activity, Set<Behavior<?>>>> availableBehaviorsByPriority, Class<? extends Behavior> behaviorClass) {
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

    public static Optional<LivingEntity> findNearestAttackTarget(Mob m) {
        if(!m.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)) return Optional.empty();

        return m.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty())
                .findClosest(le -> isValidAttackTarget(m, le));
    }

    private static boolean isValidAttackTarget(Entity m, Entity le) {
        return isPlayerTarget(m, le) || isMobCombat(m, le);
    }

    public static boolean hasPlayerTarget(Entity mob){
        return CloneArmy.getPlayerTarget(mob.getType()).isPresent();
    }

    public static boolean isPlayerTarget(Entity mob, Entity target) {
        return CloneArmy.getPlayerTarget(mob.getType()).map(uuid -> mob.level.getPlayerByUUID(uuid) == target).orElse(false);
    }

    public static boolean hasMobCombat(Entity mob){
        return CloneArmy.getMobCombat(mob.getType()).isPresent();
    }

    public static boolean isMobCombat(Entity mob, Entity target) {
        return CloneArmy.getMobCombat(mob.getType()).map(et -> target.getType() == et).orElse(false);
    }

    private void onLivingDrops(LivingDropsEvent event){
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof Mob && Summonable.cast(((Mob) event.getEntity())).isSummoned()){
            event.setCanceled(true);
        }
    }

    private void onLivingXpDrop(LivingExperienceDropEvent event){
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof Mob && Summonable.cast(((Mob) event.getEntity())).isSummoned()){
            event.setCanceled(true);
        }
    }

    private void onStartTracking(PlayerEvent.StartTracking event){
        if(event.getTarget() instanceof Mob && Summonable.cast((Mob) event.getTarget()).isSummoned()){
            Summonable.syncSummonerUUID((Mob) event.getTarget());
        }
    }
}
