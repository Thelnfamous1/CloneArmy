package me.Thelnfamous1.clone_army;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;

public class CACommands {

    public static final String TOGGLE_REMOTE_SUCCESS_KEY = "commands.clone_army.toggleremote.success";

    public static final String TOGGLE_HOSTILITY_SUCCESS_KEY = "commands.clone_army.togglehostility.success";

    public static final String TOGGLE_HOSTILITY_CLEAR_SUCCESS_KEY = "commands.clone_army.togglehostility.clear.success";

    public static final String TARGET_PLAYER_SUCCESS_KEY = "commands.clone_army.targetplayer.success";

    public static final String TARGET_PLAYER_CLEAR_SUCCESS_KEY = "commands.clone_army.targetplayer.clear.success";

    public static final String TARGET_PLAYER_CLEAR_ALL_SUCCESS_KEY = "commands.clone_army.targetplayer.clearall.success";

    public static final String MOB_COMBAT_SUCCESS_KEY = "commands.clone_army.mobcombat.success";

    public static final String MOB_COMBAT_CLEAR_SUCCESS_KEY = "commands.clone_army.mobcombat.clear.success";

    public static final String MOB_COMBAT_CLEAR_ALL_SUCCESS_KEY = "commands.clone_army.mobcombat.clearall.success";
    public static SuggestionProvider<CommandSourceStack> SUMMONABLE_CLONES;

    public static void registerSuggestionProviders() {
        SUMMONABLE_CLONES = registerSuggestionProvider("summonable_entities", (ctx, builder) ->
                SharedSuggestionProvider.suggestResource(
                        Registry.ENTITY_TYPE.stream().filter(CloneArmy::isBeenType),
                        builder,
                        EntityType::getKey,
                        (type) -> Component.translatable(Util.makeDescriptionId("entity", EntityType.getKey(type)))));
    }

    private static SuggestionProvider<CommandSourceStack> registerSuggestionProvider(String path, SuggestionProvider<SharedSuggestionProvider> provider) {
        return SuggestionProviders.register(new ResourceLocation(CloneArmy.MODID, path), provider);
    }

    public static final String NOT_HOLDING_REMOTE_EXCEPTION_KEY = "commands.clone_army.toggleremote.not_holding_remote";
    private static final SimpleCommandExceptionType NOT_HOLDING_REMOTE = new SimpleCommandExceptionType(Component.translatable(NOT_HOLDING_REMOTE_EXCEPTION_KEY));

    // Example: /toggle remote baby_nico
    public static void registerToggleRemote(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(Commands.literal("toggleremote")
                //.requires(CACommands::isPlayerHoldingRemote)
                .then(Commands.argument("entity", EntityToggleRemoteArgument.id())
                        .suggests(SUMMONABLE_CLONES)
                        .executes((context) -> toggleRemote(context.getSource(), EntityToggleRemoteArgument.getSummonableEntity(context, "entity")))));
    }

    private static boolean isPlayerHoldingRemote(CommandSourceStack sourceStack) {
        //noinspection ConstantConditions
        return sourceStack.isPlayer() && sourceStack.getPlayer().isHolding(is -> is.getItem() instanceof CloneRemoteItem);
    }

    private static int toggleRemote(CommandSourceStack pSource, ResourceLocation pType) throws CommandSyntaxException {
        if (!isPlayerHoldingRemote(pSource)) {
            throw NOT_HOLDING_REMOTE.create();
        } else {
            //noinspection ConstantConditions
            ItemStack stack = pSource.getPlayer().getItemInHand(ProjectileUtil.getWeaponHoldingHand(pSource.getPlayer(), i -> i instanceof CloneRemoteItem));
            CloneRemoteItem.setType(stack, pType);
            pSource.sendSuccess(Component.translatable(TOGGLE_REMOTE_SUCCESS_KEY, stack.getDisplayName(), Component.translatable(Util.makeDescriptionId("entity", pType))), true);
            return 1;
        }
    }


    // Example: /hostilitytoggle [mobName] [on/off]
    public static void registerToggleHostility(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(Commands.literal("togglehostility")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.argument("entity", EntitySummonArgument.id())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("toggle", BoolArgumentType.bool())
                                .executes((context) -> toggleHostility(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"), BoolArgumentType.getBool(context, "toggle"))))
                        .then(Commands.literal("clear")
                                .executes((context) -> clearHostility(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"))))));
    }

    private static int toggleHostility(CommandSourceStack pSource, ResourceLocation pType, boolean toggle) {
        EntityType.byString(pType.toString()).ifPresent(et -> CommandableCombat.setHostilityToggle(et, toggle));
        pSource.sendSuccess(Component.translatable(TOGGLE_HOSTILITY_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", pType)), toggle), true);
        return 1;
    }

    private static int clearHostility(CommandSourceStack pSource, ResourceLocation pType) {
        EntityType.byString(pType.toString()).ifPresent(CommandableCombat::clearHostilityToggle);
        pSource.sendSuccess(Component.translatable(TOGGLE_HOSTILITY_CLEAR_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", pType))), true);
        return 1;
    }


    // Example: /target [PlayerName] [MobName]
    public static void registerTargetPlayer(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(Commands.literal("targetplayer")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("clearall")
                        .executes(context -> clearAllPlayerTargets(context.getSource())))
                .then(Commands.argument("entity", EntitySummonArgument.id())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes((context) -> targetPlayer(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"), EntityArgument.getPlayer(context, "player"))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearTargetPlayer(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "entity"))))));
    }

    private static int targetPlayer(CommandSourceStack pSource, ResourceLocation pType, ServerPlayer player) {
        EntityType.byString(pType.toString()).ifPresent(et -> CommandableCombat.setPlayerTarget(et, player.getUUID()));
        pSource.sendSuccess(Component.translatable(TARGET_PLAYER_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", pType)), player.getDisplayName()), true);
        return 1;
    }

    private static int clearAllPlayerTargets(CommandSourceStack pSource) {
        CommandableCombat.clearAllPlayerTargets();
        pSource.sendSuccess(Component.translatable(TARGET_PLAYER_CLEAR_ALL_SUCCESS_KEY), true);
        return 1;
    }

    private static int clearTargetPlayer(CommandSourceStack pSource, ResourceLocation pType) {
        EntityType.byString(pType.toString()).ifPresent(CommandableCombat::clearPlayerTarget);
        pSource.sendSuccess(Component.translatable(TARGET_PLAYER_CLEAR_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", pType))), true);
        return 1;
    }


    // Example: /mobcombat [AttackingMobName] [TargetMobName]
    public static void registerMobCombat(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(Commands.literal("mobcombat")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("clearall")
                        .executes(context -> clearAllMobCombat(context.getSource())))
                .then(Commands.argument("attacker", EntitySummonArgument.id())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("target", EntitySummonArgument.id())
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes((context) -> mobCombat(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "attacker"), EntitySummonArgument.getSummonableEntity(context, "target"))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearMobCombat(context.getSource(), EntitySummonArgument.getSummonableEntity(context, "attacker"))))));
    }

    private static int mobCombat(CommandSourceStack pSource, ResourceLocation attacker, ResourceLocation target) {
        EntityType.byString(attacker.toString())
                .ifPresent(attackerType -> EntityType.byString(target.toString())
                        .ifPresent(targetType -> CommandableCombat.setMobCombat(attackerType, targetType)));
        pSource.sendSuccess(Component.translatable(MOB_COMBAT_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", attacker)), Component.translatable(Util.makeDescriptionId("entity", target))), true);
        return 1;
    }

    private static int clearAllMobCombat(CommandSourceStack pSource) {
        CommandableCombat.clearMobCombat();
        pSource.sendSuccess(Component.translatable(MOB_COMBAT_CLEAR_ALL_SUCCESS_KEY), true);
        return 1;
    }

    private static int clearMobCombat(CommandSourceStack pSource, ResourceLocation pType) {
        EntityType.byString(pType.toString()).ifPresent(CommandableCombat::clearMobCombat);
        pSource.sendSuccess(Component.translatable(MOB_COMBAT_CLEAR_SUCCESS_KEY, Component.translatable(Util.makeDescriptionId("entity", pType))), true);
        return 1;
    }
}
