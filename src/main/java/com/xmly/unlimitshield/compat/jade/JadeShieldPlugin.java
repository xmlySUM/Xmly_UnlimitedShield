package com.xmly.unlimitshield.compat.jade;

import com.xmly.unlimitshield.UnlimitedShield;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(UnlimitedShield.MODID)
public final class JadeShieldPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(JadeShieldDataProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(JadeShieldComponentProvider.INSTANCE, LivingEntity.class);
    }
}
