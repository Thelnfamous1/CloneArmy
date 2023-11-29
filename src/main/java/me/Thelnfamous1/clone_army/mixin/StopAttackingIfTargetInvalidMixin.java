package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CommandableCombat;
import me.Thelnfamous1.clone_army.PredicateSecond;
import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(StopAttackingIfTargetInvalid.class)
public abstract class StopAttackingIfTargetInvalidMixin<E extends Mob> {

    @Shadow protected abstract LivingEntity getAttackTarget(E pMemoryHolder);

    @Shadow protected abstract void clearAttackTarget(E pMemoryHolder);

    @Shadow @Final private Predicate<LivingEntity> stopAttackingWhen;

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V", at = @At("HEAD"), cancellable = true)
    private void forceTargetErasure(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        LivingEntity attackTarget = this.getAttackTarget(pEntity);
        if(CommandableCombat.isTargetInvalid(pEntity, attackTarget) || Summonable.isTargetInvalid(pEntity, attackTarget)){
            this.clearAttackTarget(pEntity);
            ci.cancel();
        }
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/StopAttackingIfTargetInvalid;clearAttackTarget(Lnet/minecraft/world/entity/Mob;)V", ordinal = 4),
    cancellable = true)
    private void preventTargetErasureByPredicate(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        if(this.stopAttackingWhen instanceof PredicateSecond<?,?> predicateSecond && predicateSecond.getBiPredicate() == CommandableCombat.STOP_ATTACKING_WHEN) return;

        LivingEntity attackTarget = this.getAttackTarget(pEntity);
        if(CommandableCombat.isAllowedToAttack(pEntity, attackTarget, false)){
            ci.cancel();
        }
    }
}
