package com.xmly.unlimitshield.crafting;

import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.enchant.BitShieldRemovalEnchantment;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BitShieldAdjustRecipe extends CustomRecipe {

    public BitShieldAdjustRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        return find(container) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        Adjustment adjustment = find(container);
        if (adjustment == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = adjustment.stack().copyWithCount(1);
        Enchantment enchantment = UnlimitedShield.BIT_SHIELD_REMOVAL.get();
        int current = EnchantmentHelper.getItemEnchantmentLevel(enchantment, result);
        int updated;
        if (adjustment.delta() == -100) {
            updated = Mth.clamp(current / 2, 1, BitShieldRemovalEnchantment.MAX_LEVEL);
        } else if (adjustment.delta() == 100) {
            updated = Mth.clamp(current * 2, 1, BitShieldRemovalEnchantment.MAX_LEVEL);
        } else {
            updated = Mth.clamp(current + adjustment.delta(), 1, BitShieldRemovalEnchantment.MAX_LEVEL);
        }

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(result);
        enchantments.put(enchantment, updated);
        EnchantmentHelper.setEnchantments(enchantments, result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return UnlimitedShield.BIT_ADJUST_SERIALIZER.get();
    }

    private static final int[][] DELTAS = {
            {32, 8, 2, 5, 7},
            {16, 4, 1, 3, 6},
            {100, 1, 0, -1, -100},
            {-16, -4, -1, -3, -6},
            {-32, -8, -2, -5, -7},
    };

    private Adjustment find(CraftingContainer container) {
        Enchantment enchantment = UnlimitedShield.BIT_SHIELD_REMOVAL.get();
        int itemSlot = -1;
        int starSlot = -1;
        ItemStack enchanted = null;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.NETHER_STAR)) {
                if (starSlot >= 0) {
                    return null;
                }
                starSlot = slot;
            } else if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0) {
                if (itemSlot >= 0) {
                    return null;
                }
                itemSlot = slot;
                enchanted = stack;
            } else {

                return null;
            }
        }
        if (itemSlot < 0 || starSlot < 0) {
            return null;
        }

        int width = container.getWidth();
        int dx = starSlot % width - itemSlot % width;
        int dy = starSlot / width - itemSlot / width;
        if (dx < -2 || dx > 2 || dy < -2 || dy > 2 || (dx == 0 && dy == 0)) {
            return null;
        }
        int delta = DELTAS[dy + 2][dx + 2];
        return delta == 0 ? null : new Adjustment(enchanted, delta);
    }

    private record Adjustment(ItemStack stack, int delta) {
    }
}
