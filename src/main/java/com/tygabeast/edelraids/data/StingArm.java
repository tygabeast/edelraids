package com.tygabeast.edelraids.data;

import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * The weapon a Sting Raid's forearm becomes when she Reacts by herself. Her form only picks one of these three, which
 * keeps her to one model with three hidden bone groups.
 *
 * @param bonusDamage    attack damage added while Reacted (before the config scale)
 * @param attackInterval ticks between attacks (melee) or shots (launcher)
 */
public enum StingArm {
    BLADE(5.0, 20),
    HEAVY(7.0, 30),
    LAUNCHER(1.0, 30);

    public final double bonusDamage;
    public final int attackInterval;

    StingArm(double bonusDamage, int attackInterval) {
        this.bonusDamage = bonusDamage;
        this.attackInterval = attackInterval;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static StingArm byId(String id) {
        for (StingArm a : values()) if (a.id().equals(id)) return a;
        return BLADE;
    }

    /** From a form's sting_arm field, defaulting to the launcher for ranged forms and the blade for everything else. */
    public static StingArm of(WeaponForm form) {
        if (form.stingArm().isEmpty()) return form.ranged() ? LAUNCHER : BLADE;
        return byId(form.stingArm());
    }

    public static StingArm of(RegistryAccess access, ResourceLocation formId) {
        WeaponForm f = access.registryOrThrow(ModRegistries.WEAPON_FORM).get(formId);
        return f == null ? BLADE : of(f);
    }
}
