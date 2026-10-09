package com.xmly.unlimitshield.network;

import com.xmly.unlimitshield.client.ClientShieldRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ShieldStatePacket {

    private final int entityId;
    private final boolean shielded;
    private final boolean natural;
    private final float baseline;
    private final boolean exponentVisible;

    public ShieldStatePacket(int entityId, boolean shielded, boolean natural, float baseline,
                             boolean exponentVisible) {
        this.entityId = entityId;
        this.shielded = shielded;
        this.natural = natural;
        this.baseline = baseline;
        this.exponentVisible = exponentVisible;
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean shielded() {
        return this.shielded;
    }

    public boolean natural() {
        return this.natural;
    }

    public float baseline() {
        return this.baseline;
    }

    public boolean exponentVisible() {
        return this.exponentVisible;
    }

    public static void encode(ShieldStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.shielded);
        buffer.writeBoolean(message.natural);
        buffer.writeFloat(message.baseline);
        buffer.writeBoolean(message.exponentVisible);
    }

    public static ShieldStatePacket decode(FriendlyByteBuf buffer) {
        return new ShieldStatePacket(buffer.readVarInt(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(ShieldStatePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientShieldRegistry.setShielded(
                message.entityId, message.shielded, message.natural, message.baseline,
                message.exponentVisible));
        context.setPacketHandled(true);
    }
}
