package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.WeaponForm;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Datapack registries: pack makers add elements and forms as JSON without code. Both sync to clients for rendering. */
public final class ModRegistries {
    public static final ResourceKey<Registry<Element>> ELEMENT = ResourceKey.createRegistryKey(EdelRaids.id("element"));
    public static final ResourceKey<Registry<WeaponForm>> WEAPON_FORM = ResourceKey.createRegistryKey(EdelRaids.id("weapon_form"));

    private ModRegistries() {}

    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ELEMENT, Element.CODEC, Element.CODEC);
        event.dataPackRegistry(WEAPON_FORM, WeaponForm.CODEC, WeaponForm.CODEC);
    }
}
