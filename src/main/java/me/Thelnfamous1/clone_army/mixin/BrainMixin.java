package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.CloneArmy;
import me.Thelnfamous1.clone_army.duck.EntityHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Brain.class)
public abstract class BrainMixin implements EntityHolder {

    @Unique
    @Nullable
    private Entity heldEntity;

    @Shadow protected abstract boolean activityRequirementsAreMet(Activity pActivity);

    @Shadow protected abstract void setActiveActivity(Activity pActivity);

    @Shadow public abstract void setActiveActivityIfPossible(Activity pActivity);

    @Inject(method = "updateActivityFromSchedule", at = @At("HEAD"), cancellable = true)
    private void handleFightActivity(long pDayTime, long pGameTime, CallbackInfo ci){
        if(this.heldEntity != null && CloneArmy.hasCustomAttackTarget(this.heldEntity)){
            if (this.activityRequirementsAreMet(Activity.FIGHT)) {
                ci.cancel();
                this.setActiveActivity(Activity.FIGHT);
            }
        }
    }

    @Inject(method = "setActiveActivityIfPossible", at = @At("HEAD"), cancellable = true)
    private void handleFightActivity(Activity pActivity, CallbackInfo ci){
        if(this.heldEntity != null && pActivity != Activity.FIGHT && CloneArmy.hasCustomAttackTarget(this.heldEntity)){
            if (this.activityRequirementsAreMet(Activity.FIGHT)) {
                ci.cancel();
                this.setActiveActivity(Activity.FIGHT);
            }
        }
    }

    @Inject(method = "setActiveActivityToFirstValid", at = @At("HEAD"), cancellable = true)
    private void handleFightActivity(List<Activity> pActivities, CallbackInfo ci){
        if(this.heldEntity != null && !pActivities.contains(Activity.FIGHT) && CloneArmy.hasCustomAttackTarget(this.heldEntity)){
            if (this.activityRequirementsAreMet(Activity.FIGHT)) {
                ci.cancel();
                this.setActiveActivity(Activity.FIGHT);
            }
        }
    }

    @Unique
    @Override
    public @Nullable Entity getHeldEntity() {
        return this.heldEntity;
    }

    @Unique
    @Override
    public void setHeldEntity(@Nullable Entity heldEntity) {
        this.heldEntity = heldEntity;
    }
}
