package com.xmly.unlimitshield.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class ShieldHudOverlay implements IGuiOverlay {

    private static final int COLOR_NO_SHIELD = 0xB0B0B0;
    private static final int LABEL_COLOR = 0xFFFFFF;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientShieldState.isArmed() || !ClientShieldState.hasTarget()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Component name = describeTarget(minecraft, ClientShieldState.targetId());
        Component reading = ClientShieldState.targetShielded()
                ? Component.literal(ClientShieldState.value().describe(ClientShieldState.exponentVisible())).withStyle(ChatFormatting.AQUA)
                : Component.translatable("overlay.xmly_unlimitshield.no_shield").withStyle(ChatFormatting.GRAY);

        int x = screenWidth / 2;
        int y = screenHeight / 2 + 18;
        graphics.drawCenteredString(minecraft.font, name, x, y, LABEL_COLOR);
        graphics.drawCenteredString(minecraft.font, reading, x, y + 11, COLOR_NO_SHIELD);
    }

    private static Component describeTarget(Minecraft minecraft, int entityId) {
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        return entity == null ? Component.literal("?") : entity.getDisplayName();
    }
}
