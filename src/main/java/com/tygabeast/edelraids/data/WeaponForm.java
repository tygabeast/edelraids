package com.tygabeast.edelraids.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * A weapon form, loaded from data/<namespace>/edelraids/weapon_form/*.json and synced to clients.
 * The client model and texture are found by the form's id: geo/item/weapon/<path>.geo.json and textures/item/weapon/<path>.png.
 *
 * @param attackDamage       total melee damage at quality 1, like a vanilla tooltip shows it (a diamond sword is 7)
 * @param attackSpeed        attacks per second, like a vanilla tooltip shows it (a sword is 1.6)
 * @param sweeps             whether it performs sword sweep attacks
 * @param reachBonus         blocks added to melee reach; ignored when Better Combat is installed (its preset sets reach)
 * @param ranged             draws and fires like a bow, without ammunition
 * @param betterCombatPreset Better Combat weapon-attributes preset (for example bettercombat:claymore), used when it is installed
 * @param paired             a matching second piece appears in the off hand while she is in the main hand (twin blades, gauntlets)
 * @param stingArm           the arm weapon a Sting Raid of this form grows: "blade", "heavy" or "launcher"; empty picks
 *                           launcher for ranged forms and blade otherwise (see {@link StingArm#of})
 */
public record WeaponForm(float attackDamage, float attackSpeed, boolean sweeps, float reachBonus, boolean ranged,
                         Optional<ResourceLocation> betterCombatPreset, boolean paired, String stingArm) {
    public static final WeaponForm FALLBACK = new WeaponForm(7f, 1.6f, true, 0f, false, Optional.empty(), false, "");

    public static final Codec<WeaponForm> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(1f, 100f).fieldOf("attack_damage").forGetter(WeaponForm::attackDamage),
            Codec.floatRange(0.1f, 4f).fieldOf("attack_speed").forGetter(WeaponForm::attackSpeed),
            Codec.BOOL.optionalFieldOf("sweeps", false).forGetter(WeaponForm::sweeps),
            Codec.floatRange(-3f, 3f).optionalFieldOf("reach_bonus", 0f).forGetter(WeaponForm::reachBonus),
            Codec.BOOL.optionalFieldOf("ranged", false).forGetter(WeaponForm::ranged),
            ResourceLocation.CODEC.optionalFieldOf("better_combat_preset").forGetter(WeaponForm::betterCombatPreset),
            Codec.BOOL.optionalFieldOf("paired", false).forGetter(WeaponForm::paired),
            Codec.STRING.optionalFieldOf("sting_arm", "").forGetter(WeaponForm::stingArm)
    ).apply(i, WeaponForm::new));
}
