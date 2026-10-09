package com.xmly.unlimitshield.shield;

import com.xmly.unlimitshield.Config;
import com.xmly.unlimitshield.UnlimitedShield;
import com.xmly.unlimitshield.api.ShieldGrantEvent;
import com.xmly.unlimitshield.enchant.BitShieldRemovalEnchantment;
import com.xmly.unlimitshield.network.ShieldNetwork;
import com.xmly.unlimitshield.network.ShieldStatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class ShieldManager {

    private static final Set<LivingEntity> TRACKED = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final Set<LivingEntity> RESTORATION_PENDING = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final Map<LivingEntity, Integer> HIT_MULTIPLIERS = new IdentityHashMap<>();

    private static final int COMBO_MAX_SHIFT = 16;

    private static final int VOID_DEPTH = 64;

    private static int tickCounter;

    private ShieldManager() {
    }

    public static boolean hasShield(LivingEntity entity) {
        return ShieldData.isActive(entity);
    }

    public static long maxY(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        return entity instanceof Player
                ? Config.resolveY(Config.PLAYER_MAX_Y.get(), maxHealth)
                : Config.resolveY(Config.MOB_MAX_Y.get(), maxHealth);
    }

    public static double maxX(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        return entity instanceof Player
                ? Config.resolveX(Config.PLAYER_MAX_X.get(), maxHealth)
                : Config.resolveX(Config.MOB_MAX_X.get(), maxHealth);
    }

    public static Entity resolveParts(Entity entity) {
        return entity instanceof PartEntity<?> part ? part.getParent() : entity;
    }

    public static boolean blockRemoval(LivingEntity entity) {
        if (!Config.BLOCK_DIRECT_REMOVAL.get() || entity.level().isClientSide() || !ShieldData.isActive(entity)) {
            return false;
        }
        if (entity.tickCount % 20 == 0) {
            log("{} was about to be deleted outright while still shielded - removal refused",
                    entity.getScoreboardName());
        }
        return true;
    }

    public static boolean isInVoid(LivingEntity entity) {
        return entity.getY() < entity.level().getMinBuildHeight() - VOID_DEPTH;
    }

    public static void grant(LivingEntity entity, double x, long y) {
        grant(entity, x, y, false);
    }

    public static void grant(LivingEntity entity, double x, long y, boolean natural) {
        if (entity.level().isClientSide()) {
            return;
        }
        double cappedX = Math.min(Math.max(x, 0.0), maxX(entity));
        long capped = Math.max(0L, Math.min(y, maxY(entity)));
        ShieldValue value = new ShieldValue(cappedX, capped);
        if (!value.isHolding()) {
            revoke(entity, "grant refused: x=" + x + " y=" + capped + " is already spent");
            return;
        }
        ShieldData.setShield(entity, value);
        ShieldData.setNatural(entity, natural);
        ShieldData.setLastHealth(entity, natural ? entity.getMaxHealth() : rawHealth(entity));
        ShieldData.clearPending(entity);
        TRACKED.add(entity);
        syncShieldState(entity, true);
        log("granted {} (x={}, y={}, y cap={}, natural={})",
                entity.getScoreboardName(), x, capped, maxY(entity), natural);
    }

    public static void revoke(LivingEntity entity, String reason) {
        boolean had = ShieldData.isActive(entity);
        ShieldData.clearShield(entity);
        TRACKED.remove(entity);
        if (had) {

            entity.dead = false;
            syncShieldState(entity, false);
            log("removed the shield from {} ({})", entity.getScoreboardName(), reason);
        }
    }

    public static void syncShieldState(LivingEntity entity, boolean shielded) {
        if (entity.level().isClientSide()) {
            return;
        }

        boolean natural = ShieldData.isNatural(entity);
        float baseline = ShieldData.getLastHealth(entity, entity.getMaxHealth());
        ShieldNetwork.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new ShieldStatePacket(entity.getId(), shielded, natural, baseline,
                        !ShieldData.isExponentDisabled(entity)));
    }

    public static void recordDamage(LivingEntity entity, double amount) {
        ShieldData.addPending(entity, amount);
    }

    public static void applyBitRemoval(LivingEntity attacker, LivingEntity victim) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = attacker.getItemInHand(hand);
            int level = EnchantmentHelper.getItemEnchantmentLevel(UnlimitedShield.BIT_SHIELD_REMOVAL.get(), stack);
            if (level > 0) {
                cutShield(victim, level);
            }
        }
    }

    public static void applySettleEnchantments(LivingEntity attacker, LivingEntity victim) {
        int combo = EnchantmentHelper.getItemEnchantmentLevel(UnlimitedShield.COMBO_SHIELD_BREAK.get(),
                attacker.getMainHandItem());
        if (combo > 0) {
            int multiplier = 1 << Math.min(combo, COMBO_MAX_SHIFT);
            HIT_MULTIPLIERS.merge(victim, multiplier, (a, b) -> Math.max(a, b));
        }

        if (EnchantmentHelper.getItemEnchantmentLevel(UnlimitedShield.COMBO_VULNERABILITY.get(),
                attacker.getMainHandItem()) > 0) {
            deepenHitsY(victim);
        }

        for (ItemStack armour : attacker.getArmorSlots()) {
            if (EnchantmentHelper.getItemEnchantmentLevel(UnlimitedShield.BIT_SHIELD_RESTORATION.get(), armour) > 0) {
                RESTORATION_PENDING.add(victim);
                break;
            }
        }
    }

    private static void deepenHitsY(LivingEntity victim) {
        if (!ShieldData.isActive(victim)) {
            return;
        }
        byte current = ShieldData.getHitsY(victim);
        if (current == ShieldData.HITS_Y_DISABLED) {
            return;
        }
        int magnitude = Math.abs(current) + 1;
        if (magnitude > 127) {
            return;
        }
        ShieldData.setHitsY(victim, (byte) (current < 0 ? -magnitude : magnitude));
        log("{}'s hitsY deepened to {}", victim.getScoreboardName(), ShieldData.getHitsY(victim));
    }

    private static long effectiveHits(LivingEntity entity, long hits) {
        Integer multiplier = HIT_MULTIPLIERS.remove(entity);
        if (multiplier == null) {
            return hits;
        }
        long scaled = hits * multiplier;
        return scaled < 0L ? Long.MAX_VALUE : scaled;
    }

    private static void cutShield(LivingEntity victim, int level) {
        ShieldValue value = ShieldData.getShield(victim);
        if (value == null) {
            return;
        }
        boolean exponentSpent = value.y() < 0L || ShieldData.isExponentDisabled(victim);
        BitShieldRemovalEnchantment.apply(value, level, exponentSpent);
        value.clampExponent(maxY(victim));
        value.clampMantissa(maxX(victim));
        if (value.isDepleted()) {
            revoke(victim, "x and y were cut away by the bit shield removal enchantment");
            return;
        }
        ShieldData.setShield(victim, value);
        log("{} was cut down to {} by bit shield removal level {}",
                victim.getScoreboardName(), value.describe(), level);
    }

    public static void absorbHeal(LivingEntity entity, double amount) {
        ShieldValue value = ShieldData.getShield(entity);
        if (value == null) {
            return;
        }
        value.addToMantissa(amount);
        ShieldData.setShield(entity, value);

        if (entity.tickCount % 20 == 0) {
            log("{} is being healed {} every tick - folded into the shield (x is now {})",
                    entity.getScoreboardName(), amount, value.describe());
        }
    }

    public static String describe(LivingEntity entity) {
        ShieldValue value = ShieldData.getShield(entity);
        if (value == null) {
            return "-";
        }
        return ShieldData.isExponentDisabled(entity) ? value.describeMantissaOnly() : value.describe();
    }

    public static float shieldBaseline(LivingEntity entity) {
        if (!ShieldData.isActive(entity)) {
            return 0.0F;
        }
        return ShieldData.getLastHealth(entity, 0.0F);
    }

    public static float rawHealth(LivingEntity entity) {
        return entity.getEntityData().get(LivingEntity.DATA_HEALTH_ID);
    }

    public static void restoreHealth(LivingEntity entity) {
        ShieldData.setLastHealth(entity, restore(entity, ShieldData.getLastHealth(entity, entity.getMaxHealth())));
    }

    public static void surviveDeath(LivingEntity entity, String source) {
        float restored = restore(entity, ShieldData.getLastHealth(entity, entity.getMaxHealth()));
        ShieldData.setLastHealth(entity, restored);
        entity.dead = true;
        log("{} was killed by {} but the shield held - health back to {}, death routine skipped",
                entity.getScoreboardName(), source, restored);
    }

    public static void onEntityJoin(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (!ShieldData.isInitialised(entity)) {
            ShieldData.setInitialised(entity, true);

            if (Config.GRANT_TO_MONSTERS.get() && entity instanceof Enemy) {
                float maxHealth = entity.getMaxHealth();
                double x = Config.resolveX(Config.MOB_SPAWN_X.get(), maxHealth);
                long y = Config.resolveY(Config.MOB_SPAWN_Y.get(), maxHealth);

                ShieldGrantEvent event = new ShieldGrantEvent(entity, x, y);
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    return;
                }
                grant(entity, event.getX(), event.getY(), true);
            }
        }
        if (ShieldData.isActive(entity)) {
            TRACKED.add(entity);
        }
    }

    public static void onEntityLeave(LivingEntity entity) {
        TRACKED.remove(entity);
    }

    public static void onLivingTick(LivingEntity entity) {
        if (!TRACKED.contains(entity)) {
            return;
        }
        if (entity.level().isClientSide() || entity.isRemoved() || !ShieldData.isActive(entity)) {
            TRACKED.remove(entity);
            return;
        }
        if (isInVoid(entity)) {
            revoke(entity, "fell into the void at y=" + entity.getY());
            return;
        }
        repair(entity);
    }

    public static void onServerTick(MinecraftServer server) {
        if (TRACKED.isEmpty()) {
            return;
        }

        for (LivingEntity entity : new ArrayList<>(TRACKED)) {
            if (entity.isRemoved() || entity.level().isClientSide() || !ShieldData.isActive(entity)) {
                TRACKED.remove(entity);
                continue;
            }
            repair(entity);
        }

        if (++tickCounter % Config.SETTLE_INTERVAL.get() != 0) {
            return;
        }

        for (LivingEntity entity : new ArrayList<>(TRACKED)) {
            if (entity.isRemoved() || entity.level().isClientSide() || !ShieldData.isActive(entity)) {
                continue;
            }
            settleDamage(entity);
        }
    }

    private static void repair(LivingEntity entity) {

        if (entity.dead) {
            entity.dead = false;
        }
        if (entity.getPose() == Pose.DYING) {
            entity.setPose(Pose.STANDING);
        }
        guardHealth(entity);
    }

    private static void settleDamage(LivingEntity entity) {
        double[] pending = ShieldData.getPending(entity);
        if (pending.length == 0) {
            return;
        }
        ShieldData.clearPending(entity);

        ShieldValue value = ShieldData.getShield(entity);
        if (value == null) {
            return;
        }

        if (RESTORATION_PENDING.remove(entity)) {
            value.doubleExponent();
            log("{}'s exponent was doubled by bit shield restoration before settling",
                    entity.getScoreboardName());
        }

        ShieldValue.Settlement settlement = value.applyDamage(pending, ShieldData.getHitsY(entity),
                effectiveHits(entity, pending.length));

        value.clampExponent(maxY(entity));
        value.clampMantissa(maxX(entity));
        if (settlement != null) {
            log("{} settled {}", entity.getScoreboardName(), settlement.describe(pending));
        }

        if (value.isDepleted()) {
            revoke(entity, "y=" + value.y() + " and x=" + value.x() + ", the shield is spent");
        } else {
            ShieldData.setShield(entity, value);
        }
    }

    private static void guardHealth(LivingEntity entity) {

        float health = rawHealth(entity);
        float last = ShieldData.getLastHealth(entity, health);
        float maxHealth = entity.getMaxHealth();

        if (ShieldData.isNatural(entity)) {

            last = maxHealth;
        } else if (last > maxHealth) {

            last = maxHealth;
        }

        if (Float.isNaN(health) || health <= 0.0F) {
            double lost = Math.max(last, 1.0F);
            ShieldData.addPending(entity, lost);
            log("{} had health {} (zeroed or overflowed) - counted {} as damage and stood it back up",
                    entity.getScoreboardName(), health, lost);
            health = restore(entity, last);
        } else if (health < last) {
            double lost = last - health;
            ShieldData.addPending(entity, lost);
            log("{} lost {} health outside the damage events - counted it as damage",
                    entity.getScoreboardName(), lost);
            health = restore(entity, last);
        } else if (Config.LOCK_HEALTH.get() && health > last) {
            health = restore(entity, last);
        }

        ShieldData.setLastHealth(entity, Math.max(health, last));
    }

    private static float restore(LivingEntity entity, float wanted) {
        float value = wanted;
        if (Float.isNaN(value) || value <= 0.0F) {
            value = Math.max(1.0F, entity.getMaxHealth());
        }
        entity.setHealth(value);

        entity.deathTime = 0;
        entity.hurtTime = 0;
        return rawHealth(entity);
    }

    public static void logSuppressedDrop(LivingEntity entity, int count) {
        log("{} tried to drop {} item stack(s) while still shielded - suppressed",
                entity.getScoreboardName(), count);
    }

    private static void log(String format, Object... args) {
        if (Config.DEBUG_LOG.get()) {
            UnlimitedShield.LOGGER.info("[shield] " + format, args);
        }
    }
}
