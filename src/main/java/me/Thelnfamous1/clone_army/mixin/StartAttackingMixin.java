package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CloneArmy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(StartAttacking.class)
public class StartAttackingMixin<E extends Mob> {

    @Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)Z", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"), cancellable = true)
    private void handleConditions(ServerLevel pLevel, E pOwner, CallbackInfoReturnable<Boolean> cir){
        if (CloneArmy.hasCustomAttackTarget(pOwner)) {
            Optional<? extends LivingEntity> nearestAttackTarget = CloneArmy.findNearestAttackTarget(pOwner);
            cir.setReturnValue(nearestAttackTarget.filter(pOwner::canAttack).isPresent());
        }
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V", at = @At("HEAD"), cancellable = true)
    private void handleConditions(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        if (CloneArmy.hasCustomAttackTarget(pEntity)) {
            ci.cancel();
            CloneArmy.findNearestAttackTarget(pEntity).ifPresent((target) -> StartAttacking.setAttackTarget(pEntity, target));
        }
    }
}
