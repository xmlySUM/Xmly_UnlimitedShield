package com.xmly.unlimitshield.enchant;

import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class BitShieldRemovalEnchantment extends Enchantment {

    public static final int MAX_LEVEL = 255;

    public static final EnchantmentCategory ANY_ITEM =
            EnchantmentCategory.create("xmly_any_item", item -> true);

    public BitShieldRemovalEnchantment() {
        super(Rarity.VERY_RARE, ANY_ITEM, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    public static void apply(ShieldValue shield, int level, boolean exponentSpent) {
        int l = Math.max(1, Math.min(level, MAX_LEVEL)) - 1;

        if (exponentSpent) {

            double step = power16(l);
            if (shield.x() > step) {
                shield.subtractMantissa(step);
            }
            return;
        }

        long orderStep = 1L << (4 * (l / 16));
        if (shield.y() > orderStep) {
            shield.subtractExponent(saturatedMultiply(orderStep, (l % 16) + 1));
        }
    }

    private static double power16(int l) {
        return l >= 255 ? Double.MAX_VALUE : Math.pow(16.0, l);
    }

    private static long saturatedMultiply(long value, long factor) {
        long result = value * factor;
        if (value != 0L && (result / value != factor || result < 0L)) {
            return Long.MAX_VALUE;
        }
        return result;
    }
}
