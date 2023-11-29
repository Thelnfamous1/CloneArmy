package me.Thelnfamous1.clone_army.duck;

import me.Thelnfamous1.clone_army.ClientboundSummonablePacket;
import me.Thelnfamous1.clone_army.CloneArmy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface Summonable {

    String SUMMONER_TAG = "Summoner";

    static Summonable cast(Mob entity){
        return (Summonable) entity;
    }

    static void syncSummonerUUID(Mob mob) {
        if(!mob.level.isClientSide){
            CloneArmy.SYNC_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), new ClientboundSummonablePacket(mob, Summonable.cast(mob).getSummonerUUID()));
        }
    }

    default boolean isSummoned() {
        return this.getSummonerUUID() != null;
    }

    @Nullable
    UUID getSummonerUUID();

    void setSummonerUUID(@Nullable UUID summonerUUID);

    default void writeSummonableInfo(CompoundTag tag){
        if(this.getSummonerUUID() != null){
            tag.putUUID(SUMMONER_TAG, this.getSummonerUUID());
        }
    }

    default void readSummonableInfo(CompoundTag tag){
        if(tag.hasUUID(SUMMONER_TAG)){
            this.setSummonerUUID(tag.getUUID(SUMMONER_TAG));
        }
    }

    default LivingEntity getSummoner(Level world){
        try {
            UUID summonerUUID = this.getSummonerUUID();
            return summonerUUID == null ? null : world.getPlayerByUUID(summonerUUID);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    default boolean canSummonableAttack(LivingEntity pTarget) {
        return !this.isSummonedBy(pTarget);
    }

    default boolean isSummonedBy(LivingEntity pEntity) {
        return pEntity == this.getSummoner(pEntity.level);
    }

    default boolean doesSummonableWantToAttack(LivingEntity target, LivingEntity mySummoner) {
        if(target instanceof Mob mob){
            Summonable targetSummonable = cast(mob);
            return !targetSummonable.isSummoned() || targetSummonable.getSummoner(mob.level) != mySummoner;
        } else{
            return true;
        }
    }

    default Optional<Boolean> isSummonableAlliedTo(Entity pEntity) {
        if (this.isSummoned()) {
            LivingEntity summoner = this.getSummoner(pEntity.level);
            if (pEntity == summoner) {
                return Optional.of(true);
            }

            if (summoner != null) {
                return Optional.of(summoner.isAlliedTo(pEntity));
            }
        }

        return Optional.empty();
    }

    static boolean isTargetInvalid(Mob attacker, LivingEntity target){
        if (!Summonable.cast(attacker).canSummonableAttack(target)) {
            return true;
        } else return Summonable.cast(attacker).isSummonableAlliedTo(target).orElse(false);
    }
}