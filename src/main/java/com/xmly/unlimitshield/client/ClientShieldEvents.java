package com.xmly.unlimitshield.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xmly.unlimitshield.UnlimitedShield;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientShieldEvents {

    public static final String CATEGORY = "key.categories." + UnlimitedShield.MODID;
    public static final KeyMapping SHOW_SHIELD = new KeyMapping(
            "key." + UnlimitedShield.MODID + ".show_shield",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    private ClientShieldEvents() {
    }

    @Mod.EventBusSubscriber(modid = UnlimitedShield.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(SHOW_SHIELD);
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("shield_readout", new ShieldHudOverlay());
        }
    }

    @Mod.EventBusSubscriber(modid = UnlimitedShield.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class GameBus {

        private GameBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            ClientShieldRegistry.tick();

            boolean toggled = false;
            while (SHOW_SHIELD.consumeClick()) {
                toggled = true;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (toggled && minecraft.player != null && minecraft.screen == null) {
                boolean on = ClientShieldState.toggle();
                minecraft.player.displayClientMessage(Component.translatable(on
                        ? "overlay.xmly_unlimitshield.inspect_on"
                        : "overlay.xmly_unlimitshield.inspect_off"), true);
            }

            ClientShieldState.tick(minecraft);
        }
    }
}
