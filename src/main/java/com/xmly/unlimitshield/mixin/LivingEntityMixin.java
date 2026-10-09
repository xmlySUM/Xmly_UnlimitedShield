package com.xmly.unlimitshield.mixin;

import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.shield.ShieldHealthView;
import com.xmly.unlimitshield.shield.ShieldManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final AtomicBoolean xmly$reported = new AtomicBoolean();

    @Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
    private void xmly$shieldHealth(CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValueF() > 0.0F) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        float baseline = ShieldManager.shieldBaseline(self);
        if (baseline <= 0.0F) {
            baseline = ShieldHealthView.baselineFor(self.getId(), self.getMaxHealth());
        }
        if (baseline <= 0.0F) {
            return;
        }

        if (xmly$reported.compareAndSet(false, true)) {
            UnlimitedShield.LOGGER.info("[shield] read guard is live - a zeroed health was masked on {}",
                    self.getScoreboardName());
        }
        cir.setReturnValue(baseline);
    }
}
