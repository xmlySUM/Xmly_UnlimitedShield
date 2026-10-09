package com.xmly.unlimitshield.api;

import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldManager;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.world.entity.LivingEntity;

public final class UnlimitedShieldApi {

    private UnlimitedShieldApi() {
    }

    public static void setShield(LivingEntity entity, double x, long y) {
        ShieldManager.grant(entity, x, y, false);
    }

    public static void setNaturalShield(LivingEntity entity, double x, long y) {
        ShieldManager.grant(entity, x, y, true);
    }

    public static void clearShield(LivingEntity entity) {
        ShieldManager.revoke(entity, "removed through the API");
    }

    public static boolean hasShield(LivingEntity entity) {
        return ShieldData.isActive(entity);
    }

    public static double getX(LivingEntity entity) {
        ShieldValue value = ShieldData.getShield(entity);
        return value == null ? 0.0 : value.x();
    }

    public static long getY(LivingEntity entity) {
        ShieldValue value = ShieldData.getShield(entity);
        return value == null ? 0L : value.y();
    }

    public static double getMaxX(LivingEntity entity) {
        return ShieldManager.maxX(entity);
    }

    public static long getMaxY(LivingEntity entity) {
        return ShieldManager.maxY(entity);
    }

    public static double getMagnitude(LivingEntity entity) {
        ShieldValue value = ShieldData.getShield(entity);
        if (value == null) {
            return 0.0;
        }
        double log10 = Math.log10(Math.max(value.x(), 1.0)) + (double) value.y();
        if (log10 >= 308.0) {
            return Double.MAX_VALUE;
        }
        return log10 <= -323.0 ? 0.0 : Math.pow(10.0, log10);
    }
}
