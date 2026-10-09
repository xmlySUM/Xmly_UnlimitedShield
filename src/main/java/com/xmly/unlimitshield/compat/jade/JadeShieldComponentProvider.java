package com.xmly.unlimitshield.compat.jade;

import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum JadeShieldComponentProvider implements IEntityComponentProvider {

    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(JadeShieldDataProvider.TAG_X)) {
            return;
        }
        ShieldValue value = new ShieldValue(data.getDouble(JadeShieldDataProvider.TAG_X),
                data.getLong(JadeShieldDataProvider.TAG_Y));
        boolean exponentVisible = !data.contains(JadeShieldDataProvider.TAG_HITS_Y)
                || data.getByte(JadeShieldDataProvider.TAG_HITS_Y) != ShieldData.HITS_Y_DISABLED;
        tooltip.add(Component.translatable("jade.xmly_unlimitshield.shield",
                        Component.literal(value.describe(exponentVisible)).withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(UnlimitedShield.MODID, "shield_display");
    }
}
