package com.xmly.unlimitshield.compat.jade;

import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

public enum JadeShieldDataProvider implements IServerDataProvider<EntityAccessor> {

    INSTANCE;

    public static final String TAG_X = "XmlyShieldX";
    public static final String TAG_Y = "XmlyShieldY";
    public static final String TAG_HITS_Y = "XmlyShieldHitsY";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) {
            return;
        }
        ShieldValue value = ShieldData.getShield(living);
        if (value == null) {

            return;
        }
        data.putDouble(TAG_X, value.x());
        data.putLong(TAG_Y, value.y());
        data.putByte(TAG_HITS_Y, ShieldData.getHitsY(living));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(UnlimitedShield.MODID, "shield_data");
    }
}
