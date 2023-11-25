package me.Thelnfamous1.clone_army;

import me.Thelnfamous1.clone_army.duck.Summonable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundSummonablePacket {

    private final int id;
    @Nullable
    private final UUID summonerUUID;

    public ClientboundSummonablePacket(Mob mob, @Nullable UUID summonerUUID){
        this.id = mob.getId();
        this.summonerUUID = summonerUUID;
    }

    public ClientboundSummonablePacket(FriendlyByteBuf buffer){
        this.id = buffer.readInt();
        boolean hasSummonerUUID = buffer.readBoolean();
        if(hasSummonerUUID) {
            this.summonerUUID = buffer.readUUID();
        } else{
            this.summonerUUID = null;
        }
    }

    public static void encode(ClientboundSummonablePacket packet, FriendlyByteBuf buffer){
        buffer.writeInt(packet.id);
        boolean hasSummonerUUID = packet.summonerUUID != null;
        buffer.writeBoolean(hasSummonerUUID);
        if (hasSummonerUUID) {
            buffer.writeUUID(packet.summonerUUID);
        }
    }

    public static void handle(ClientboundSummonablePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(packet.id);
            if(entity instanceof Mob){
                Summonable.cast((Mob) entity).setSummonerUUID(packet.summonerUUID);
            }
        });
        context.get().setPacketHandled(true);
    }
}