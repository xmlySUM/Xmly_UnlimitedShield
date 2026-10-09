package com.xmly.unlimitshield.network;

import com.xmly.unlimitshield.UnlimitedShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ShieldNetwork {

    private static final String PROTOCOL = "1";
    private static final ResourceLocation CHANNEL_NAME =
            ResourceLocation.fromNamespaceAndPath(UnlimitedShield.MODID, "shield");

    private static SimpleChannel channel;

    private ShieldNetwork() {
    }

    public static void register() {
        SimpleChannel created = NetworkRegistry.newSimpleChannel(
                CHANNEL_NAME, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

        created.registerMessage(0, ShieldRequestPacket.class,
                ShieldRequestPacket::encode, ShieldRequestPacket::decode, ShieldRequestPacket::handle);
        created.registerMessage(1, ShieldResponsePacket.class,
                ShieldResponsePacket::encode, ShieldResponsePacket::decode, ShieldResponsePacket::handle);
        created.registerMessage(2, ShieldStatePacket.class,
                ShieldStatePacket::encode, ShieldStatePacket::decode, ShieldStatePacket::handle);

        channel = created;
    }

    public static <MSG> void send(PacketDistributor.PacketTarget target, MSG message) {
        if (channel != null) {
            channel.send(target, message);
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        if (channel != null) {
            channel.sendToServer(message);
        }
    }
}
