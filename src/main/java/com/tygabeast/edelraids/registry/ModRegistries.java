package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Song;
import com.tygabeast.edelraids.data.WeaponForm;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Datapack registries: pack makers add elements, forms and songs as JSON without code. All sync to clients. */
public final class ModRegistries {
    public static final ResourceKey<Registry<Element>> ELEMENT = ResourceKey.createRegistryKey(EdelRaids.id("element"));
    public static final ResourceKey<Registry<WeaponForm>> WEAPON_FORM = ResourceKey.createRegistryKey(EdelRaids.id("weapon_form"));
    public static final ResourceKey<Registry<Song>> SONG = ResourceKey.createRegistryKey(EdelRaids.id("song"));

    private ModRegistries() {}

    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ELEMENT, Element.CODEC, Element.CODEC);
        event.dataPackRegistry(WEAPON_FORM, WeaponForm.CODEC, WeaponForm.CODEC);
        event.dataPackRegistry(SONG, Song.CODEC, Song.CODEC);
    }
}
