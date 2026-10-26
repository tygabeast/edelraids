package com.tygabeast.edelraids;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config (per world, synced to clients by NeoForge). Every mechanic has a switch or a tunable with a safe default. */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static { B.push("reacting"); }
    public static final ModConfigSpec.IntValue TRANSFORM_TICKS = B
            .comment("How long the React and reform animations last, in ticks (20 = 1 second). She is invulnerable meanwhile.")
            .defineInRange("transformTicks", 20, 5, 100);
    public static final ModConfigSpec.DoubleValue REACT_RANGE = B
            .comment("The Pledger must stay within this many blocks for React to finish.")
            .defineInRange("reactRange", 16.0, 2.0, 64.0);
    public static final ModConfigSpec.IntValue WEAR_PER_HIT = B
            .comment("Weapon wear per hit. 10 wear = 1 point of her health. A broken weapon forces her out, wounded.")
            .defineInRange("wearPerHit", 1, 0, 100);
    static { B.pop().push("companion"); }
    public static final ModConfigSpec.IntValue WOUNDED_TICKS = B
            .comment("How long a wounded Edel Raid needs to recover, in ticks (6000 = 5 minutes).")
            .defineInRange("woundedTicks", 6000, 20, 72000);
    public static final ModConfigSpec.IntValue REGEN_INTERVAL = B
            .comment("While not wounded she heals 1 health every this many ticks. 0 disables natural healing.")
            .defineInRange("regenIntervalTicks", 100, 0, 6000);
    static { B.pop().push("bond"); }
    public static final ModConfigSpec.IntValue BOND_PER_KILL = B
            .comment("Bond gained when her Pledger kills a hostile mob with her weapon form. Placeholder until bond sources are designed.")
            .defineInRange("bondPerKill", 1, 0, 1000);
    public static final ModConfigSpec.IntValue AWAKEN_BOND = B
            .comment("Bond level at which her weapon awakens (cosmetic change + flat damage bonus).")
            .defineInRange("awakenBond", 100, 1, 100000);
    public static final ModConfigSpec.DoubleValue AWAKEN_DAMAGE = B
            .comment("Flat attack damage added by awakening. Design value is +1, raised to +2 only if testing needs it.")
            .defineInRange("awakenDamageBonus", 1.0, 0.0, 10.0);
    static { B.pop().push("balance"); }
    public static final ModConfigSpec.DoubleValue QUALITY_DAMAGE = B
            .comment("Flat attack damage added per quality tier above the first. Placeholder until tiers are designed.")
            .defineInRange("qualityDamagePerTier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue ADVANTAGE_BONUS = B
            .comment("Damage bonus against an element you beat within the same glittering ring (0.25 = +25%).")
            .defineInRange("advantageBonus", 0.25, 0.0, 5.0);
    public static final ModConfigSpec.DoubleValue DISADVANTAGE_PENALTY = B
            .comment("Damage penalty against an element that beats yours within the same ring (0.20 = -20%).")
            .defineInRange("disadvantagePenalty", 0.20, 0.0, 0.95);
    static { B.pop().push("technical"); }
    public static final ModConfigSpec.IntValue VALIDATE_INTERVAL = B
            .comment("How often (ticks) a held weapon is checked against the anti-duplication registry.")
            .defineInRange("validateIntervalTicks", 20, 5, 200);
    public static final ModConfigSpec.BooleanValue DEBUG = B
            .comment("Log React, revert and registry decisions.")
            .define("debug", false);
    static { B.pop(); }

    public static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
