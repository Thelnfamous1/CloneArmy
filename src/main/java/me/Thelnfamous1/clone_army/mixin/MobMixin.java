package me.Thelnfamous1.clone_army.mixin;

import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements Summonable {

    @Nullable
    @Unique
    private UUID summonerUUID;

    protected MobMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique
    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.summonerUUID;
    }

    @Unique
    @Override
    public void setSummonerUUID(@Nullable UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
        Summonable.syncSummonerUUID((Mob) (Object) this);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void handleReadSaveData(CompoundTag pCompound, CallbackInfo ci){
        this.readSummonableInfo(pCompound);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void handleAddSaveData(CompoundTag pCompound, CallbackInfo ci){
        this.writeSummonableInfo(pCompound);
    }
}