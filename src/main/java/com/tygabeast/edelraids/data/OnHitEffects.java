package com.tygabeast.edelraids.data;

import com.tygabeast.edelraids.EdelRaids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * On-hit effect handlers, keyed by the "type" in an element's on_hit block. Phase 1 ships knockback (Wind);
 * the rest of the 24 arrive in phase 2. Addons can register more with {@link #register}.
 */
public final class OnHitEffects {
    @FunctionalInterface
    public interface Handler {
        void apply(LivingEntity target, LivingEntity attacker, float strength);
    }

    private static final Map<ResourceLocation, Handler> HANDLERS = new HashMap<>();

    static {
        register(EdelRaids.id("knockback"), (target, attacker, strength) -> {
            float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(strength, Mth.sin(yaw), -Mth.cos(yaw));
        });
    }

    private OnHitEffects() {}

    public static void register(ResourceLocation type, Handler handler) {
        HANDLERS.put(type, handler);
    }

    public static void apply(Element.OnHit onHit, LivingEntity target, LivingEntity attacker) {
        Handler h = HANDLERS.get(onHit.type());
        if (h != null) h.apply(target, attacker, onHit.strength());
    }
}
