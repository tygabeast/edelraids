package com.tygabeast.edelraids.data;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Lookups and the glittering-ring advantage rule. */
public final class Elements {
    private Elements() {}

    @Nullable
    public static Element element(RegistryAccess access, @Nullable ResourceLocation id) {
        if (id == null) return null;
        Registry<Element> reg = access.registry(ModRegistries.ELEMENT).orElse(null);
        return reg == null ? null : reg.get(id);
    }

    public static WeaponForm form(RegistryAccess access, @Nullable ResourceLocation id) {
        if (id == null) return WeaponForm.FALLBACK;
        Registry<WeaponForm> reg = access.registry(ModRegistries.WEAPON_FORM).orElse(null);
        WeaponForm f = reg == null ? null : reg.get(id);
        return f == null ? WeaponForm.FALLBACK : f;
    }

    /**
     * Same ring only: each element beats the next one and loses to the one before it; other pairs are neutral.
     * Pure arithmetic, no table search, so it is safe to call on every hit.
     */
    public static float multiplier(@Nullable Element attacker, @Nullable Element defender) {
        if (attacker == null || defender == null || attacker.ring() != defender.ring()) return 1f;
        int gap = Math.floorMod(defender.position() - attacker.position(), 6);
        if (gap == 1) return 1f + Config.ADVANTAGE_BONUS.get().floatValue();
        if (gap == 5) return 1f - Config.DISADVANTAGE_PENALTY.get().floatValue();
        return 1f;
    }
}
