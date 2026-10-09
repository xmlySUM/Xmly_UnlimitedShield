package com.xmly.unlimitshield.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ComboEnchantments {

    private ComboEnchantments() {
    }

    public static class ComboShieldBreak extends Enchantment {

        public static final int MAX_LEVEL = 5;

        public ComboShieldBreak() {
            super(Rarity.RARE, EnchantmentCategory.WEAPON,
                    new EquipmentSlot[]{EquipmentSlot.MAINHAND});
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
            return 15 + (level - 1) * 9;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 50;
        }

        @Override
        public boolean isTreasureOnly() {
            return true;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }
    }

    public static class ComboVulnerability extends Enchantment {

        public ComboVulnerability() {
            super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON,
                    new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        }

        @Override
        public int getMinLevel() {
            return 1;
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }

        @Override
        public int getMinCost(int level) {
            return 25;
        }

        @Override
        public int getMaxCost(int level) {
            return 75;
        }

        @Override
        public boolean isTreasureOnly() {
            return true;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }
    }

    public static class BitShieldRestoration extends Enchantment {

        public BitShieldRestoration() {
            super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR,
                    new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                            EquipmentSlot.LEGS, EquipmentSlot.FEET});
        }

        @Override
        public int getMinLevel() {
            return 1;
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }

        @Override
        public int getMinCost(int level) {
            return 20;
        }

        @Override
        public int getMaxCost(int level) {
            return 60;
        }

        @Override
        public boolean isCurse() {
            return true;
        }

        @Override
        public boolean isTreasureOnly() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return true;
        }
    }
}
