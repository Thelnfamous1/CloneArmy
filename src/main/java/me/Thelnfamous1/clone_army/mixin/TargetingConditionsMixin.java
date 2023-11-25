package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CloneArmy;
import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public class TargetingConditionsMixin {

    @Shadow @Final private boolean isCombat;

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void handleTest(LivingEntity pAttacker, LivingEntity pTarget, CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue() && pAttacker instanceof Mob mobAttacker){
            if(!this.isCombat) return;


            if(!CloneArmy.isHostile(mobAttacker.getType())){
                cir.setReturnValue(false);
                return;
            }

            if(CloneArmy.hasPlayerTarget(mobAttacker) && !CloneArmy.isPlayerTarget(mobAttacker, pTarget)
                    || CloneArmy.hasMobCombat(mobAttacker) && !CloneArmy.isMobCombat(mobAttacker, pTarget)){
                cir.setReturnValue(false);
                return;
            }

            if (!Summonable.cast(mobAttacker).canSummonableAttack(pTarget)) {
                cir.setReturnValue(false);
                return;
            }

            if (Summonable.cast(mobAttacker).isSummonableAlliedTo(pTarget).orElse(false)) {
                cir.setReturnValue(false);
            }
        }
    }
}
