package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Element effects vanilla has no equivalent for. All are plain attribute effects or markers, so they cost nothing per
 * tick beyond vanilla's own effect countdown.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, EdelRaids.MODID);

    /** Earth: armor broken apart, -2 armor per level. */
    public static final DeferredHolder<MobEffect, MobEffect> SHATTERED = REGISTER.register("shattered",
            () -> new Effect(MobEffectCategory.HARMFUL, 0xA9773E)
                    .addAttributeModifier(Attributes.ARMOR, EdelRaids.id("effect.shattered"), -2.0, AttributeModifier.Operation.ADD_VALUE));

    /** Greenery: held in place by vines. */
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = REGISTER.register("rooted",
            () -> new Effect(MobEffectCategory.HARMFUL, 0x43A047)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, EdelRaids.id("effect.rooted"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Shield: raises the absorption cap so each hit can add a little absorption. */
    public static final DeferredHolder<MobEffect, MobEffect> WARDED = REGISTER.register("warded",
            () -> new Effect(MobEffectCategory.BENEFICIAL, 0x5C7FA3)
                    .addAttributeModifier(Attributes.MAX_ABSORPTION, EdelRaids.id("effect.warded"), 4.0, AttributeModifier.Operation.ADD_VALUE));

    /** Quiet: projectiles this entity fires are stopped before they spawn. */
    public static final DeferredHolder<MobEffect, MobEffect> SILENCED = REGISTER.register("silenced",
            () -> new Effect(MobEffectCategory.HARMFUL, 0x9FB0A8));

    /** Time: slower attacks and movement. */
    public static final DeferredHolder<MobEffect, MobEffect> TIMEWORN = REGISTER.register("timeworn",
            () -> new Effect(MobEffectCategory.HARMFUL, 0xC9A227)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, EdelRaids.id("effect.timeworn_attack"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, EdelRaids.id("effect.timeworn_move"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {}

    private static final class Effect extends MobEffect {
        Effect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
