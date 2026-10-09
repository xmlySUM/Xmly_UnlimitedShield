package com.xmly.unlimitshield.network;

import com.xmly.unlimitshield.client.ClientShieldState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ShieldResponsePacket {

    private final int entityId;
    private final boolean active;
    private final double x;
    private final long y;
    private final boolean exponentVisible;

    public ShieldResponsePacket(int entityId, boolean active, double x, long y, boolean exponentVisible) {
        this.entityId = entityId;
        this.active = active;
        this.x = x;
        this.y = y;
        this.exponentVisible = exponentVisible;
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean active() {
        return this.active;
    }

    public double x() {
        return this.x;
    }

    public long y() {
        return this.y;
    }

    public boolean exponentVisible() {
        return this.exponentVisible;
    }

    public static void encode(ShieldResponsePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.active);
        buffer.writeDouble(message.x);
        buffer.writeLong(message.y);
        buffer.writeBoolean(message.exponentVisible);
    }

    public static ShieldResponsePacket decode(FriendlyByteBuf buffer) {
        return new ShieldResponsePacket(buffer.readVarInt(), buffer.readBoolean(),
                buffer.readDouble(), buffer.readLong(), buffer.readBoolean());
    }

    public static void handle(ShieldResponsePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> ClientShieldState.accept(message));
        context.setPacketHandled(true);
    }
}
