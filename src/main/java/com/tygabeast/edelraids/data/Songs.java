package com.tygabeast.edelraids.data;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Songs by element and slot. The index is built once per song registry instance (datapack registries are fixed for a
 * world's lifetime), so every lookup is one HashMap get, on the server and on the client.
 */
public final class Songs {
    /** One loaded song with its id. */
    public record Entry(ResourceLocation id, Song song) {
        public Component name() {
            return Component.translatable("song." + id.getNamespace() + "." + id.getPath());
        }
    }

    private static Registry<Song> indexedRegistry;
    private static Map<ResourceLocation, Entry[]> index = Map.of();

    private Songs() {}

    /** Slots 1-3 at indexes 0-2; any may be null if a datapack leaves a slot empty. */
    public static Entry[] forElement(RegistryAccess access, ResourceLocation element) {
        Registry<Song> reg = access.registry(ModRegistries.SONG).orElse(null);
        if (reg == null) return new Entry[3];
        if (reg != indexedRegistry) rebuild(reg);
        Entry[] e = index.get(element);
        return e == null ? new Entry[3] : e;
    }

    private static synchronized void rebuild(Registry<Song> reg) {
        Map<ResourceLocation, Entry[]> map = new HashMap<>();
        reg.entrySet().forEach(e -> {
            Song s = e.getValue();
            map.computeIfAbsent(s.element(), k -> new Entry[3])[s.slot() - 1] = new Entry(e.getKey().location(), s);
        });
        index = map;
        indexedRegistry = reg;
    }

    /** Bond needed for a slot; the full song (slot 3) needs her weapon awakened. Jewels need neither. */
    public static boolean unlocked(EdelRaidData d, int slot) {
        if (d.quality() >= 4) return true; // a Glittering Jewel knows all three songs from the start
        return switch (slot) {
            case 1 -> d.bond() >= Config.SONG_BOND_1.get();
            case 2 -> d.bond() >= Config.SONG_BOND_2.get();
            default -> d.awakened();
        };
    }

    /** The slot to use: the selected one if unlocked, else the highest unlocked slot that has a song, else 0. */
    public static int effectiveSlot(EdelRaidData d, Entry[] songs, int selected) {
        if (selected >= 1 && selected <= 3 && songs[selected - 1] != null && unlocked(d, selected)) return selected;
        for (int s = 3; s >= 1; s--) if (songs[s - 1] != null && unlocked(d, s)) return s;
        return 0;
    }

    @Nullable
    public static Entry get(Entry[] songs, int slot) {
        return slot < 1 || slot > 3 ? null : songs[slot - 1];
    }
}
