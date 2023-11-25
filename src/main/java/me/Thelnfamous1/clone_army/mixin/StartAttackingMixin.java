package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CommandableCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(StartAttacking.class)
public class StartAttackingMixin<E extends Mob> {

    @Shadow @Final private Predicate<E> canAttackPredicate;

    @Shadow @Final private Function<E, Optional<? extends LivingEntity>> targetFinderFunction;

    @Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)Z", at = @At(value = "HEAD"), cancellable = true)
    private void handleConditions(ServerLevel pLevel, E pOwner, CallbackInfoReturnable<Boolean> cir){
        if(this.canAttackPredicate != CommandableCombat.CAN_ATTACK
                && this.targetFinderFunction != CommandableCombat.COMMANDED_TARGET_FINDER
                && CommandableCombat.CAN_ATTACK.test(pOwner)
                && CommandableCombat.hasCommandedAttackTarget(pOwner)) {
            cir.setReturnValue(CommandableCombat.findNearestValidAttackTarget(pOwner)
                    .filter(pOwner::canAttack)
                    .isPresent());
        }
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V", at = @At("HEAD"), cancellable = true)
    private void handleConditions(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        if(this.targetFinderFunction == CommandableCombat.COMMANDED_TARGET_FINDER) return;
        if (CommandableCombat.hasCommandedAttackTarget(pEntity)) {
            ci.cancel();
            CommandableCombat.findNearestValidAttackTarget(pEntity)
                    .ifPresent((target) -> StartAttacking.setAttackTarget(pEntity, target));
        }
    }
}
