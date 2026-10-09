package com.xmly.unlimitshield.shield;

import com.xmly.unlimitshield.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

public final class ShieldData {

    private static final String ROOT = "XmlyUnlimitedShield";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_INITIALISED = "initialised";
    private static final String TAG_LAST_HEALTH = "lastHealth";
    private static final String TAG_PENDING = "pending";
    private static final String TAG_NATURAL = "natural";
    private static final String TAG_HITS_Y = "hitsY";

    public static final byte HITS_Y_DISABLED = -128;

    private static final double[] NO_DAMAGE = new double[0];

    private ShieldData() {
    }

    private static CompoundTag tag(LivingEntity entity, boolean create) {
        CompoundTag persistent = entity.getPersistentData();
        if (persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            return persistent.getCompound(ROOT);
        }
        if (!create) {
            return null;
        }
        CompoundTag created = new CompoundTag();
        persistent.put(ROOT, created);
        return created;
    }

    public static boolean isActive(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        return tag != null && tag.getBoolean(TAG_ACTIVE);
    }

    public static ShieldValue getShield(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        if (tag == null || !tag.getBoolean(TAG_ACTIVE)) {
            return null;
        }
        return new ShieldValue(tag.getDouble(TAG_X), tag.getLong(TAG_Y));
    }

    public static void setShield(LivingEntity entity, ShieldValue value) {
        CompoundTag tag = tag(entity, true);
        tag.putDouble(TAG_X, value.x());
        tag.putLong(TAG_Y, value.y());
        tag.putBoolean(TAG_ACTIVE, true);
    }

    public static boolean isNatural(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        return tag != null && tag.getBoolean(TAG_NATURAL);
    }

    public static void setNatural(LivingEntity entity, boolean natural) {
        tag(entity, true).putBoolean(TAG_NATURAL, natural);
    }

    public static void clearShield(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        if (tag == null) {
            return;
        }
        tag.putBoolean(TAG_ACTIVE, false);
        tag.putDouble(TAG_X, 0.0);
        tag.putLong(TAG_Y, 0L);
        tag.remove(TAG_PENDING);
    }

    public static boolean isInitialised(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        return tag != null && tag.getBoolean(TAG_INITIALISED);
    }

    public static void setInitialised(LivingEntity entity, boolean value) {
        tag(entity, true).putBoolean(TAG_INITIALISED, value);
    }

    public static byte getHitsY(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        if (tag == null || !tag.contains(TAG_HITS_Y, Tag.TAG_BYTE)) {
            return Config.defaultHitsY();
        }
        return tag.getByte(TAG_HITS_Y);
    }

    public static void setHitsY(LivingEntity entity, byte hitsY) {
        tag(entity, true).putByte(TAG_HITS_Y, hitsY);
    }

    public static boolean isExponentDisabled(LivingEntity entity) {
        return getHitsY(entity) == HITS_Y_DISABLED;
    }

    public static float getLastHealth(LivingEntity entity, float fallback) {
        CompoundTag tag = tag(entity, false);
        if (tag == null || !tag.contains(TAG_LAST_HEALTH, Tag.TAG_FLOAT)) {
            return fallback;
        }
        return tag.getFloat(TAG_LAST_HEALTH);
    }

    public static void setLastHealth(LivingEntity entity, float health) {
        tag(entity, true).putFloat(TAG_LAST_HEALTH, health);
    }

    public static double[] getPending(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        if (tag == null) {
            return NO_DAMAGE;
        }

        ListTag list = tag.getList(TAG_PENDING, Tag.TAG_DOUBLE);
        double[] values = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            values[i] = list.getDouble(i);
        }
        return values;
    }

    public static void addPending(LivingEntity entity, double amount) {
        if (!(amount > 0.0)) {
            return;
        }
        CompoundTag tag = tag(entity, true);
        ListTag list = tag.getList(TAG_PENDING, Tag.TAG_DOUBLE);
        list.add(DoubleTag.valueOf(amount));
        tag.put(TAG_PENDING, list);
    }

    public static void clearPending(LivingEntity entity) {
        CompoundTag tag = tag(entity, false);
        if (tag != null) {
            tag.remove(TAG_PENDING);
        }
    }

    public static void copy(LivingEntity from, LivingEntity to) {
        CompoundTag source = tag(from, false);
        if (source == null) {
            return;
        }
        tag(to, true).merge(source.copy());
    }
}
