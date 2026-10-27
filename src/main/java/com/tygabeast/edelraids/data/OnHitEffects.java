package com.tygabeast.edelraids.data;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * On-hit effect handlers, keyed by the "type" in an element's on_hit block. A handler can act at three points:
 * before damage (change the amount or armor), on a vanilla critical hit, and after damage landed. Addons register more
 * with {@link #register}. All handlers run on the server only, and only for melee hits and arrows from an Edel Raid
 * weapon; damage a handler deals itself (Thunder's chain) is magic damage and never triggers effects again.
 */
public final class OnHitEffects {

    public interface Handler {
        /** Before damage: may change the amount or armor reduction. */
        default void incoming(LivingIncomingDamageEvent event, LivingEntity target, LivingEntity attacker, float strength) {}

        /** Melee vanilla critical hit by a player. */
        default void critical(CriticalHitEvent event, float strength) {}

        /** After damage landed; {@code damage} is the final amount. */
        default void afterHit(LivingEntity target, LivingEntity attacker, float damage, float strength) {}
    }

    private static final Map<ResourceLocation, Handler> HANDLERS = new HashMap<>();

    private OnHitEffects() {}

    public static void register(ResourceLocation type, Handler handler) {
        HANDLERS.put(type, handler);
    }

    public static void incoming(Element.OnHit onHit, LivingIncomingDamageEvent event, LivingEntity attacker) {
        Handler h = HANDLERS.get(onHit.type());
        if (h != null) h.incoming(event, event.getEntity(), attacker, onHit.strength());
    }

    public static void critical(Element.OnHit onHit, CriticalHitEvent event) {
        Handler h = HANDLERS.get(onHit.type());
        if (h != null) h.critical(event, onHit.strength());
    }

    public static void afterHit(Element.OnHit onHit, LivingEntity target, LivingEntity attacker, float damage) {
        Handler h = HANDLERS.get(onHit.type());
        if (h != null) h.afterHit(target, attacker, damage, onHit.strength());
    }

    // ------------------------------------------------------------------ helpers

    private static int ticks(float seconds) {
        return Math.max(1, Math.round(seconds * 20f));
    }

    private static void effect(LivingEntity e, Holder<MobEffect> effect, float seconds, int amplifier) {
        e.addEffect(new MobEffectInstance(effect, ticks(seconds), amplifier, false, true, true));
    }

    /** Effects whose tick behaviour restarts when refreshed (regeneration) are only re-added once nearly expired. */
    private static void effectIfLow(LivingEntity e, Holder<MobEffect> effect, float seconds, int amplifier, int lowTicks) {
        MobEffectInstance cur = e.getEffect(effect);
        if (cur == null || cur.getDuration() < lowTicks) effect(e, effect, seconds, amplifier);
    }

    private static void particles(LivingEntity e, net.minecraft.core.particles.ParticleOptions p, int count) {
        if (e.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, e.getX(), e.getY(0.6), e.getZ(), count, 0.25, 0.3, 0.25, 0.05);
        }
    }

    private static void id(String path, Handler h) {
        register(EdelRaids.id(path), h);
    }

    // ------------------------------------------------------------------ the 24 elements' effects
    // strength meanings: seconds for timed effects, chance (0-1) for chance effects, fraction or flat damage otherwise.

    static {
        // Ring 1
        id("shatter", new Handler() { // Earth: stacking armor break, up to 3 levels
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                MobEffectInstance cur = t.getEffect(ModEffects.SHATTERED);
                int amp = cur == null ? 0 : Math.min(2, cur.getAmplifier() + Math.max(1, Math.round(s)));
                effect(t, ModEffects.SHATTERED, 5f, amp);
            }
        });
        id("knockback", new Handler() { // Wind
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                float yaw = a.getYRot() * Mth.DEG_TO_RAD;
                t.knockback(s, Mth.sin(yaw), -Mth.cos(yaw));
            }
        });
        id("ignite", new Handler() { // Fire
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                t.igniteForSeconds(s);
            }
        });
        id("slow", new Handler() { // Water
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, MobEffects.MOVEMENT_SLOWDOWN, s, 0);
            }
        });
        id("root", new Handler() { // Greenery
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, ModEffects.ROOTED, s, 0);
            }
        });
        id("insight", new Handler() { // Wisdom: chance for +50% damage
            @Override public void incoming(LivingIncomingDamageEvent e, LivingEntity t, LivingEntity a, float s) {
                if (a.getRandom().nextFloat() < s) {
                    e.setAmount(e.getAmount() * 1.5f);
                    particles(t, ParticleTypes.ENCHANTED_HIT, 10);
                }
            }
        });

        // Ring 2
        id("charm", new Handler() { // Beauty: chance the target forgets who it was fighting
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                if (t instanceof Mob m && a.getRandom().nextFloat() < s) {
                    m.setTarget(null);
                    m.setLastHurtByMob(null);
                    particles(t, ParticleTypes.HEART, 4);
                }
            }
        });
        id("shield_break", new Handler() { // Sound: knocks a raised shield down, like an axe
            @Override public void incoming(LivingIncomingDamageEvent e, LivingEntity t, LivingEntity a, float s) {
                if (t instanceof Player p && p.isBlocking()) p.disableShield();
            }
        });
        id("ward", new Handler() { // Shield: the Pledger gains a little absorption per hit, up to 2 hearts
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effectIfLow(a, ModEffects.WARDED, 10f, 0, 100);
                a.setAbsorptionAmount(Math.min(a.getMaxAbsorption(), a.getAbsorptionAmount() + s));
            }
        });
        id("pierce", new Handler() { // Sword: ignores part of the target's armor
            @Override public void incoming(LivingIncomingDamageEvent e, LivingEntity t, LivingEntity a, float s) {
                e.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * (1f - s));
            }
        });
        id("lifesteal", new Handler() { // Love: heals the Pledger by a share of the damage dealt
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                if (d > 0f) a.heal(d * s);
            }
        });
        id("strip", new Handler() { // Void: removes beneficial effects from the target
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                int n = Math.max(1, Math.round(s));
                List<Holder<MobEffect>> remove = new java.util.ArrayList<>(n);
                for (MobEffectInstance inst : t.getActiveEffects()) {
                    if (inst.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                        remove.add(inst.getEffect());
                        if (remove.size() >= n) break;
                    }
                }
                for (Holder<MobEffect> h : remove) t.removeEffect(h);
            }
        });

        // Ring 3
        id("smite", new Handler() { // Light: bonus against the undead, and the target glows
            @Override public void incoming(LivingIncomingDamageEvent e, LivingEntity t, LivingEntity a, float s) {
                if (t.getType().is(EntityTypeTags.UNDEAD)) e.setAmount(e.getAmount() * (1f + s));
            }

            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, MobEffects.GLOWING, 3f, 0);
            }
        });
        id("blind", new Handler() { // Darkness
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, MobEffects.BLINDNESS, s, 0);
            }
        });
        id("haste", new Handler() { // Movement: the Pledger gets a burst of speed
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(a, MobEffects.MOVEMENT_SPEED, s, 0);
            }
        });
        id("silence", new Handler() { // Quiet
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, ModEffects.SILENCED, s, 0);
            }
        });
        id("regen", new Handler() { // Life
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effectIfLow(a, MobEffects.REGENERATION, s, 0, 10);
            }
        });
        id("wither", new Handler() { // Death
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, MobEffects.WITHER, s, 0);
            }
        });

        // Ring 4
        id("chain", new Handler() { // Thunder: arcs to the nearest other hostile within 4 blocks
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                LivingEntity next = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : t.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4.0),
                        e -> e != t && e != a && e instanceof Enemy && e.isAlive())) {
                    double dist = e.distanceToSqr(t);
                    if (dist < best) {
                        best = dist;
                        next = e;
                    }
                }
                if (next != null) {
                    next.hurt(a.damageSources().indirectMagic(a, a), s);
                    particles(next, ParticleTypes.ELECTRIC_SPARK, 12);
                }
            }
        });
        id("flat_damage", new Handler() { // Steel
            @Override public void incoming(LivingIncomingDamageEvent e, LivingEntity t, LivingEntity a, float s) {
                e.setAmount(e.getAmount() + s);
            }
        });
        id("freeze", new Handler() { // Ice: builds freezing like powder snow; strength is ticks per hit
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 160, t.getTicksFrozen() + Math.round(s)));
            }
        });
        id("timeworn", new Handler() { // Time
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, ModEffects.TIMEWORN, s, 0);
            }
        });
        id("drowse", new Handler() { // Dream
            @Override public void afterHit(LivingEntity t, LivingEntity a, float d, float s) {
                effect(t, MobEffects.CONFUSION, s, 0);
                effect(t, MobEffects.MOVEMENT_SLOWDOWN, s * 0.5f, 0);
            }
        });
        id("starfall", new Handler() { // Star: stronger critical hits
            @Override public void critical(CriticalHitEvent e, float s) {
                if (e.isVanillaCritical()) e.setDamageMultiplier(e.getDamageMultiplier() * (1f + s));
            }
        });
    }
}
