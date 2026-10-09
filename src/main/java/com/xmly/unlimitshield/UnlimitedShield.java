package com.xmly.unlimitshield;

import com.mojang.logging.LogUtils;
import com.xmly.unlimitshield.enchant.BitShieldRemovalEnchantment;
import com.xmly.unlimitshield.enchant.ComboEnchantments;
import com.xmly.unlimitshield.network.ShieldNetwork;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(UnlimitedShield.MODID)
public final class UnlimitedShield {

    public static final String MODID = "xmly_unlimitshield";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<Enchantment> BIT_SHIELD_REMOVAL =
            ENCHANTMENTS.register("shield_bit_removal", BitShieldRemovalEnchantment::new);

    public static final RegistryObject<Enchantment> COMBO_SHIELD_BREAK =
            ENCHANTMENTS.register("combo_shield_break", ComboEnchantments.ComboShieldBreak::new);

    public static final RegistryObject<Enchantment> COMBO_VULNERABILITY =
            ENCHANTMENTS.register("combo_vulnerability", ComboEnchantments.ComboVulnerability::new);

    public static final RegistryObject<Enchantment> BIT_SHIELD_RESTORATION =
            ENCHANTMENTS.register("bit_shield_restoration", ComboEnchantments.BitShieldRestoration::new);

    public static final RegistryObject<RecipeSerializer<?>> BIT_ADJUST_SERIALIZER =
            RECIPE_SERIALIZERS.register("shield_bit_adjust",
                    () -> new SimpleCraftingRecipeSerializer<>(com.xmly.unlimitshield.crafting.BitShieldAdjustRecipe::new));

    public UnlimitedShield(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);
        ENCHANTMENTS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ShieldNetwork::register);
        LOGGER.info("Xmly's Unlimited Shield online");
    }
}
