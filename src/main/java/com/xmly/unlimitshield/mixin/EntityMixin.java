package com.xmly.unlimitshield.mixin;

import com.xmly.unlimitshield.shield.ShieldManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void xmly$keepShieldedAlive(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED || !((Object) this instanceof LivingEntity living)) {
            return;
        }
        if (ShieldManager.blockRemoval(living)) {
            ci.cancel();
        }
    }
}
