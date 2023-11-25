package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CommandableCombat;
import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StopAttackingIfTargetInvalid.class)
public abstract class StopAttackingIfTargetInvalidMixin<E extends Mob> {

    @Shadow protected abstract LivingEntity getAttackTarget(E pMemoryHolder);

    @Shadow protected abstract void clearAttackTarget(E pMemoryHolder);

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V", at = @At("HEAD"), cancellable = true)
    private void forceTargetErasure(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        if(CommandableCombat.isAlwaysPassive(pEntity.getType())){
            this.clearAttackTarget(pEntity);
            ci.cancel();
            return;
        }

        LivingEntity attackTarget = this.getAttackTarget(pEntity);
        if (!Summonable.cast(pEntity).canSummonableAttack(attackTarget)) {
            this.clearAttackTarget(pEntity);
            ci.cancel();
            return;
        }

        if (Summonable.cast(pEntity).isSummonableAlliedTo(attackTarget).orElse(false)) {
            this.clearAttackTarget(pEntity);
            ci.cancel();
        }
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/StopAttackingIfTargetInvalid;clearAttackTarget(Lnet/minecraft/world/entity/Mob;)V", ordinal = 4),
    cancellable = true)
    private void preventTargetErasureByPredicate(ServerLevel pLevel, E pEntity, long pGameTime, CallbackInfo ci){
        LivingEntity attackTarget = this.getAttackTarget(pEntity);
        if(CommandableCombat.isAllowedToAttack(pEntity, attackTarget, false)){
            ci.cancel();
        }
    }
}
