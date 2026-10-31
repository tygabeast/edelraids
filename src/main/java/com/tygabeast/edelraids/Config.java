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
    public static final ModConfigSpec.IntValue FREED_LINGER_TICKS = B
            .comment("How long a freed captive Edel Raid you have not bonded, or a freed Sting Raid you had no active slot for, waits before she leaves (6000 = 5 minutes).")
            .defineInRange("freedLingerTicks", 6000, 20, 72000);
    static { B.pop().push("bond"); }
    public static final ModConfigSpec.IntValue BOND_PER_KILL = B
            .comment("Bond gained when her Pledger kills a hostile mob with her weapon form.")
            .defineInRange("bondPerKill", 1, 0, 1000);
    public static final ModConfigSpec.IntValue AWAKEN_BOND = B
            .comment("Bond level at which her weapon awakens (cosmetic change + flat damage bonus).")
            .defineInRange("awakenBond", 100, 1, 100000);
    public static final ModConfigSpec.DoubleValue AWAKEN_DAMAGE = B
            .comment("Flat attack damage added by awakening. Design value is +1, raised to +2 only if testing needs it.")
            .defineInRange("awakenDamageBonus", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.IntValue BOND_COMPANION_TICKS = B
            .comment("Bond gained by 1 for every this many ticks she spends within 16 blocks of her Pledger as a companion (6000 = 5 minutes). 0 disables.")
            .defineInRange("bondCompanionTicks", 6000, 0, 720000);
    static { B.pop().push("songs"); }
    public static final ModConfigSpec.BooleanValue SONGS_ENABLED = B
            .comment("Allow Pledgers to sing songs with the Sing key.")
            .define("songsEnabled", true);
    public static final ModConfigSpec.IntValue SONG_BOND_1 = B
            .comment("Bond needed for her first song.")
            .defineInRange("songBond1", 25, 0, 100000);
    public static final ModConfigSpec.IntValue SONG_BOND_2 = B
            .comment("Bond needed for her second song. Her full song unlocks with awakening (see bond.awakenBond).")
            .defineInRange("songBond2", 50, 0, 100000);
    public static final ModConfigSpec.IntValue RESONANCE_PER_HIT = B
            .comment("Resonance gained per hit with her weapon form (the meter holds 100).")
            .defineInRange("resonancePerHit", 5, 0, 100);
    public static final ModConfigSpec.IntValue RESONANCE_DRAIN_DELAY = B
            .comment("Ticks without landing a hit before resonance starts to drain (200 = 10 seconds).")
            .defineInRange("resonanceDrainDelayTicks", 200, 0, 72000);
    public static final ModConfigSpec.IntValue RESONANCE_DRAIN_PER_SECOND = B
            .comment("Resonance lost per second while draining. 0 disables draining.")
            .defineInRange("resonanceDrainPerSecond", 2, 0, 100);
    public static final ModConfigSpec.IntValue SONG_COOLDOWN = B
            .comment("Ticks between songs, shared by all of a Pledger's songs.")
            .defineInRange("songCooldownTicks", 20, 0, 1200);
    public static final ModConfigSpec.DoubleValue SONG_DAMAGE_SCALE = B
            .comment("Multiplier on all song damage and absorption.")
            .defineInRange("songDamageScale", 1.0, 0.0, 10.0);
    static { B.pop().push("sting_raids"); }
    public static final ModConfigSpec.IntValue STING_LIMIT = B
            .comment("How many freed Sting Raids one player can have active (following) at once. Others rest where they were left (sneak + right-click to switch). 0 means freed Sting Raids can only rest.")
            .defineInRange("stingLimit", 2, 0, 64);
    public static final ModConfigSpec.DoubleValue STING_STRAIN_PER_SECOND = B
            .comment("Strain her artificial stone builds per second while she is Reacted. At 100 she is forced out and staggered.")
            .defineInRange("strainPerSecond", 5.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue STING_STABILIZED_FACTOR = B
            .comment("Strain build-up multiplier once a Refined Stabilizer has been used on her (0.5 = half as fast).")
            .defineInRange("stabilizedStrainFactor", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue STING_STRAIN_RECOVERY = B
            .comment("Strain lost per second while she is not Reacted. She can React again once strain is at 50 or less.")
            .defineInRange("strainRecoveryPerSecond", 10.0, 0.1, 100.0);
    public static final ModConfigSpec.IntValue STING_STAGGER_TICKS = B
            .comment("How long she is staggered when strain forces her out of React (60 = 3 seconds).")
            .defineInRange("staggerTicks", 60, 0, 600);
    public static final ModConfigSpec.IntValue STING_REVERT_TICKS = B
            .comment("Ticks without a target before she reverts from React (200 = 10 seconds).")
            .defineInRange("revertDelayTicks", 200, 20, 6000);
    public static final ModConfigSpec.IntValue STING_ESCAPE_TICKS = B
            .comment("How long a subdued hunter's Sting Raid waits to be freed before she escapes (2400 = 2 minutes).")
            .defineInRange("subduedEscapeTicks", 2400, 20, 72000);
    public static final ModConfigSpec.DoubleValue STING_DAMAGE_SCALE = B
            .comment("Multiplier on the damage her arm weapon adds while Reacted, and on her launcher's shots.")
            .defineInRange("stingDamageScale", 1.0, 0.0, 10.0);
    static { B.pop().push("arc_aile"); }
    public static final ModConfigSpec.IntValue ARC_AILE_SPARE = B
            .comment("Standing with Arc Aile gained by sparing one of their agents.")
            .defineInRange("standingPerSpare", 2, 0, 100);
    public static final ModConfigSpec.IntValue ARC_AILE_KILL = B
            .comment("Standing with Arc Aile lost by killing one of their agents.")
            .defineInRange("standingLostPerKill", 6, 0, 1000);
    public static final ModConfigSpec.IntValue ARC_AILE_CAPTIVE = B
            .comment("Standing with Arc Aile gained by freeing a captive from a hunter camp.")
            .defineInRange("standingPerCaptive", 1, 0, 100);
    public static final ModConfigSpec.IntValue ARC_AILE_TRUCE = B
            .comment("Standing at which Arc Aile's agents stop confronting a player (truce). Killing an agent usually breaks it.")
            .defineInRange("truceStanding", 10, 1, 1000);
    public static final ModConfigSpec.IntValue ARC_AILE_YIELD_TICKS = B
            .comment("How long a yielded agent waits for you to spare him (right-click) before he withdraws on his own (1200 = 1 minute).")
            .defineInRange("yieldTicks", 1200, 100, 12000);
    public static final ModConfigSpec.DoubleValue ARC_AILE_PATROL_CHANCE = B
            .comment("Chance per player per minute (in the Overworld) that an Arc Aile patrol of 1 or 2 agents appears nearby, if none is already within 96 blocks. 0 disables patrols.")
            .defineInRange("patrolChancePerMinute", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.IntValue ARC_AILE_PERSUADE = B
            .comment("Standing with Arc Aile lost each time you talk a sheltered Edel Raid into leaving Edel Garden.")
            .defineInRange("standingLostPerPersuade", 2, 0, 100);
    static { B.pop().push("edel_garden"); }
    public static final ModConfigSpec.DoubleValue UNEASY_CHANCE = B
            .comment("Chance that a sheltered Edel Raid in a sanctuary is uneasy (willing to leave with you) rather than content.")
            .defineInRange("uneasyChance", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue KEEPSAKE_CHANCE = B
            .comment("Chance that a sheltered Edel Raid who agrees to leave gives you a keepsake from before she entered Edel Garden.")
            .defineInRange("keepsakeChance", 0.35, 0.0, 1.0);
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
