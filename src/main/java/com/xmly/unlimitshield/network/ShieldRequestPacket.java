package com.xmly.unlimitshield.network;

import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldManager;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class ShieldRequestPacket {

    private final int entityId;

    public ShieldRequestPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ShieldRequestPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
    }

    public static ShieldRequestPacket decode(FriendlyByteBuf buffer) {
        return new ShieldRequestPacket(buffer.readVarInt());
    }

    public static void handle(ShieldRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity target = ShieldManager.resolveParts(sender.level().getEntity(message.entityId));
            ShieldValue value = target instanceof LivingEntity living ? ShieldData.getShield(living) : null;

            boolean exponentVisible = !(target instanceof LivingEntity living)
                    || !ShieldData.isExponentDisabled(living);
            ShieldNetwork.send(PacketDistributor.PLAYER.with(() -> sender),
                    new ShieldResponsePacket(message.entityId, value != null,
                            value == null ? 0.0 : value.x(),
                            value == null ? 0L : value.y(),
                            exponentVisible));
        });
        context.setPacketHandled(true);
    }
}
