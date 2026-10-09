package com.xmly.unlimitshield.event;

import com.xmly.unlimitshield.Config;
import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.command.ShieldCommand;
import com.xmly.unlimitshield.network.ShieldNetwork;
import com.xmly.unlimitshield.network.ShieldStatePacket;
import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = UnlimitedShield.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShieldEvents {

    private ShieldEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0.0F || !ShieldManager.hasShield(entity)) {
            return;
        }

        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) && ShieldManager.isInVoid(entity)) {
            ShieldManager.revoke(entity, "fell out of the world");
            return;
        }

        ShieldManager.recordDamage(entity, event.getAmount());
        event.setAmount(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !Config.LOCK_HEALTH.get() || !ShieldManager.hasShield(entity)) {
            return;
        }

        float amount = event.getAmount();
        event.setCanceled(true);

        if (amount > 0.0F && Float.isFinite(amount)) {
            ShieldManager.absorbHeal(entity, amount);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt_BitRemoval(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !ShieldManager.hasShield(victim)) {
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            ShieldManager.applyBitRemoval(attacker, victim);
            ShieldManager.applySettleEnchantments(attacker, victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !ShieldManager.hasShield(entity)) {
            return;
        }
        event.setCanceled(true);

        String source = event.getSource().typeHolder().unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown");
        ShieldManager.surviveDeath(entity, source);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getDrops().isEmpty() || !ShieldManager.hasShield(entity)) {
            return;
        }
        int suppressed = event.getDrops().size();
        event.getDrops().clear();
        ShieldManager.logSuppressedDrop(entity, suppressed);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getDroppedExperience() <= 0
                || !ShieldManager.hasShield(entity)) {
            return;
        }
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !Config.BLOCK_KNOCKBACK.get() || !ShieldManager.hasShield(entity)) {
            return;
        }
        event.setStrength(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            ShieldManager.onLivingTick(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        ShieldManager.onEntityJoin(living);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ShieldManager.onEntityLeave(living);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server != null) {
            ShieldManager.onServerTick(server);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living
                && event.getEntity() instanceof ServerPlayer tracker
                && ShieldData.isActive(living)) {
            ShieldNetwork.send(PacketDistributor.PLAYER.with(() -> tracker),
                    new ShieldStatePacket(living.getId(), true,
                            ShieldData.isNatural(living),
                            ShieldData.getLastHealth(living, living.getMaxHealth()),
                            !ShieldData.isExponentDisabled(living)));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ShieldData.copy(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ShieldCommand.register(event.getDispatcher());
    }
}
