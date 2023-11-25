package me.Thelnfamous1.clone_army;

import com.marwinekk.been.BeenMod;
import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CloneRemoteItem extends Item {

    public static final String REMOTE_ENTITY_TYPE_KEY = "item.clone_army.clone_remote.entity_type";

    public CloneRemoteItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemInHand = pContext.getItemInHand();
            BlockPos clickedPos = pContext.getClickedPos();
            Direction clickedFace = pContext.getClickedFace();
            BlockState clickedState = level.getBlockState(clickedPos);

            BlockPos spawnPos;
            if (clickedState.getCollisionShape(level, clickedPos).isEmpty()) {
                spawnPos = clickedPos;
            } else {
                spawnPos = clickedPos.relative(clickedFace);
            }

            EntityType<?> spawnType = getType(itemInHand).orElse(null);
            if(spawnType == null) return InteractionResult.FAIL;
            Entity spawn = spawnType.spawn((ServerLevel) level, itemInHand, pContext.getPlayer(), spawnPos, MobSpawnType.MOB_SUMMONED, true, !Objects.equals(clickedPos, spawnPos) && clickedFace == Direction.UP);
            if (spawn != null) {
                if(pContext.getPlayer() != null){
                    if(spawn instanceof Mob mob) Summonable.cast(mob).setSummonerUUID(pContext.getPlayer().getUUID());
                    itemInHand.hurtAndBreak(1, pContext.getPlayer(), p -> p.broadcastBreakEvent(pContext.getHand()));
                }
                level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, clickedPos);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemInHand = pPlayer.getItemInHand(pHand);
        BlockHitResult playerPOVHitResult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
        if (playerPOVHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemInHand);
        } else if (!(pLevel instanceof ServerLevel)) {
            return InteractionResultHolder.success(itemInHand);
        } else {
            BlockPos targetedBlockPos = playerPOVHitResult.getBlockPos();
            if (!(pLevel.getBlockState(targetedBlockPos).getBlock() instanceof LiquidBlock)) {
                return InteractionResultHolder.pass(itemInHand);
            } else if (pLevel.mayInteract(pPlayer, targetedBlockPos) && pPlayer.mayUseItemAt(targetedBlockPos, playerPOVHitResult.getDirection(), itemInHand)) {
                EntityType<?> spawnType = getType(itemInHand).orElse(null);
                if(spawnType == null) return InteractionResultHolder.fail(itemInHand);
                Entity spawnedEntity = spawnType.spawn((ServerLevel)pLevel, itemInHand, pPlayer, targetedBlockPos, MobSpawnType.MOB_SUMMONED, false, false);
                if (spawnedEntity == null) {
                    return InteractionResultHolder.pass(itemInHand);
                } else {
                    if(spawnedEntity instanceof Mob mob) Summonable.cast(mob).setSummonerUUID(pPlayer.getUUID());
                    itemInHand.hurtAndBreak(1, pPlayer, p -> p.broadcastBreakEvent(pHand));

                    pPlayer.awardStat(Stats.ITEM_USED.get(this));
                    pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, spawnedEntity.position());
                    return InteractionResultHolder.consume(itemInHand);
                }
            } else {
                return InteractionResultHolder.fail(itemInHand);
            }
        }
    }

    public static void setType(ItemStack stack, ResourceLocation typeKey) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", typeKey.toString());
        stack.getOrCreateTag().put("EntityTag", entityTag);
    }

    public static Optional<EntityType<?>> getType(ItemStack stack) {
        if(!stack.hasTag()) return Optional.empty();

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("EntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag entityTag = tag.getCompound("EntityTag");
            if (entityTag.contains("id", Tag.TAG_STRING)) {
                String id = entityTag.getString("id");
                if(!id.startsWith(BeenMod.MODID)) return Optional.empty();
                return EntityType.byString(id);
            }
        }

        return Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @org.jetbrains.annotations.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        getType(pStack).ifPresent(et -> pTooltipComponents.add(Component.translatable(REMOTE_ENTITY_TYPE_KEY, Component.translatable(et.getDescriptionId()))));
    }
}
