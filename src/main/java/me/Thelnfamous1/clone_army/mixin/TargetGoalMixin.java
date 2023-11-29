package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CommandableCombat;
import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {

    @Shadow @Nullable protected LivingEntity targetMob;

    @Shadow @Final protected Mob mob;

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void handleContinuedUse(CallbackInfoReturnable<Boolean> cir){
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            target = this.targetMob;
        }

        if(target != null && (CommandableCombat.isTargetInvalid(this.mob, target) || Summonable.isTargetInvalid(this.mob, target))){
            cir.setReturnValue(false);
        }
    }
}
