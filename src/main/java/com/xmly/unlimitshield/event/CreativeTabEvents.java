package com.xmly.unlimitshield.event;

import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.enchant.BitShieldRemovalEnchantment;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = UnlimitedShield.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {

    private static final Set<Integer> SHOWN_LEVELS = Set.of(1, 10, 127);

    private CreativeTabEvents() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        Enchantment enchantment = UnlimitedShield.BIT_SHIELD_REMOVAL.get();
        if (event.getTabKey() == CreativeModeTabs.SEARCH
                || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {

            List<ItemStack> outdated = new ArrayList<>();
            for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
                if (isBookOf(entry.getKey(), enchantment)) {
                    outdated.add(entry.getKey());
                }
            }
            outdated.forEach(event.getEntries()::remove);

            for (int level : SHOWN_LEVELS) {
                event.accept(EnchantedBookItem.createForEnchantment(
                        new EnchantmentInstance(enchantment, level)));
            }
        }
    }

    private static boolean isBookOf(ItemStack stack, Enchantment enchantment) {
        return stack.getItem() instanceof EnchantedBookItem
                && EnchantmentHelper.getEnchantments(stack).containsKey(enchantment);
    }
}
