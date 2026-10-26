package com.tygabeast.edelraids.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * A weapon form, loaded from data/<namespace>/edelraids/weapon_form/*.json and synced to clients.
 * The client model and texture are found by the form's id: geo/item/weapon/<path>.geo.json and textures/item/weapon/<path>.png.
 *
 * @param attackDamage       total attack damage at quality 1, like a vanilla tooltip shows it (a diamond sword is 7)
 * @param attackSpeed        attacks per second, like a vanilla tooltip shows it (a sword is 1.6)
 * @param sweeps             whether it performs sword sweep attacks
 * @param betterCombatPreset Better Combat weapon-attributes preset, used from phase 2 when Better Combat is installed
 */
public record WeaponForm(float attackDamage, float attackSpeed, boolean sweeps, Optional<String> betterCombatPreset) {
    public static final WeaponForm FALLBACK = new WeaponForm(7f, 1.6f, true, Optional.empty());

    public static final Codec<WeaponForm> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(1f, 100f).fieldOf("attack_damage").forGetter(WeaponForm::attackDamage),
            Codec.floatRange(0.1f, 4f).fieldOf("attack_speed").forGetter(WeaponForm::attackSpeed),
            Codec.BOOL.optionalFieldOf("sweeps", false).forGetter(WeaponForm::sweeps),
            Codec.STRING.optionalFieldOf("better_combat_preset").forGetter(WeaponForm::betterCombatPreset)
    ).apply(i, WeaponForm::new));
}
