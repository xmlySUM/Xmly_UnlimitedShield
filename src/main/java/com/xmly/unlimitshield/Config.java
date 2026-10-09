package com.xmly.unlimitshield;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {

    private static final double INVALID_FALLBACK = 1.0;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue MOB_SPAWN_X = BUILDER
            .comment("The x of the shield given to a naturally spawned hostile mob (z = x * 10^y).",
                    "Positive = the value itself, negative = a multiplier of max health.",
                    "Default: -7.0, so a 20 HP zombie spawns with x = 140.")
            .defineInRange("mobSpawnX", -7.0, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MOB_SPAWN_Y = BUILDER
            .comment("The y of the shield given to a naturally spawned hostile mob.",
                    "Positive = the value itself, negative = a multiplier of max health.",
                    "Default: -1.0, meaning y equals the mob's max health.")
            .defineInRange("mobSpawnY", -1.0, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MOB_MAX_X = BUILDER
            .comment("Upper limit for a mob's shield mantissa x. Same positive/negative rule as above.")
            .defineInRange("mobMaxX", 1.0e10, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MOB_MAX_Y = BUILDER
            .comment("Upper limit for a mob's shield exponent y. Same positive/negative rule as above.")
            .defineInRange("mobMaxY", 1.0e10, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue PLAYER_MAX_X = BUILDER
            .comment("Upper limit for a player's shield mantissa x. Same positive/negative rule as above.")
            .defineInRange("playerMaxX", 1.0e10, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue PLAYER_MAX_Y = BUILDER
            .comment("Upper limit for a player's shield exponent y. Same positive/negative rule as above.",
                    "Default: 1, which is roughly one solid hit's worth of counts.")
            .defineInRange("playerMaxY", 1.0, -Double.MAX_VALUE, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue SETTLE_INTERVAL = BUILDER
            .comment("How often, in ticks, the damage collected since the previous settlement is",
                    "applied to the shield. Default: 20.")
            .defineInRange("settleInterval", 20, 1, 72000);

    public static final ForgeConfigSpec.BooleanValue LOCK_HEALTH = BUILDER
            .comment("While a shield is up the entity's health is frozen. Healing is not lost though:",
                    "it is folded into the shield's x instead. Set to false to let healing through to",
                    "the entity as normal.")
            .define("lockHealth", true);

    public static final ForgeConfigSpec.BooleanValue GRANT_TO_MONSTERS = BUILDER
            .comment("Give newly spawned hostile mobs (implementors of",
                    "net.minecraft.world.entity.monster.Enemy) a shield. Neutral and passive",
                    "creatures never receive one.")
            .define("grantToMonsters", true);

    public static final ForgeConfigSpec.BooleanValue BLOCK_KNOCKBACK = BUILDER
            .comment("The shield swallows the impact of a blow, so a shielded entity is not knocked",
                    "back or launched by attacks and explosions.")
            .define("blockKnockback", true);

    public static final ForgeConfigSpec.BooleanValue BLOCK_DIRECT_REMOVAL = BUILDER
            .comment("Refuse Entity#remove(KILLED) while an entity is still shielded.",
                    "",
                    "This is OFF by default on purpose. Deleting an entity outright is not a kill:",
                    "no loot, no experience and no kill credit is awarded, so letting a mod do it",
                    "leaves the world in a state nobody asked for. Turning it on makes the shield",
                    "absolute, at the cost of shielded entities lingering through effects that were",
                    "never meant to be survivable.")
            .define("blockDirectRemoval", false);

    public static final ForgeConfigSpec.IntValue DEFAULT_HITS_Y = BUILDER
            .comment("The default hitsY of a shield, a signed byte (-128 to 127).",
                    "",
                    "    0        - y settles the usual way, from the damage values",
                    "    1..127   - y settles from the number of hits instead; see the README for the",
                    "               formula. Higher values make the shield shed counts far faster",
                    "   -1..-127  - same, but counting hits only and ignoring the damage values",
                    "   -128      - y is switched off: it neither settles nor counts, the shield",
                    "               behaves as a pure mantissa pool, and the readout hides it",
                    "",
                    "Can be overridden per entity with /shield add.")
            .defineInRange("defaultHitsY", 0, -128, 127);

    public static byte defaultHitsY() {
        return (byte) Math.max(-128, Math.min(127, DEFAULT_HITS_Y.get()));
    }

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("Log shield grants, removals, settlements and anything that bypasses the",
                    "damage events. Very chatty - for debugging only.")
            .define("debugSettlementLog", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static double resolve(double configured, float maxHealth) {
        if (Double.isNaN(configured)) {
            return INVALID_FALLBACK;
        }
        if (configured >= 0.0) {
            return configured;
        }
        double scaled = (double) maxHealth * Math.abs(configured);
        if (!Double.isFinite(scaled)) {
            return Double.MAX_VALUE;
        }
        return Math.max(scaled, 0.0);
    }

    public static double resolveX(double configured, float maxHealth) {
        return Math.max(1.0, resolve(configured, maxHealth));
    }

    public static long resolveY(double configured, float maxHealth) {
        double absolute = resolve(configured, maxHealth);
        if (absolute >= (double) Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) absolute);
    }
}
