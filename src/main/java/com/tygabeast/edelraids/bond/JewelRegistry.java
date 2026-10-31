package com.tygabeast.edelraids.bond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Seven Glittering Jewels of this world. A Jewel sanctuary's candidate claims a place on her first tick (always on
 * the server thread); once seven exist the list is closed for good, even if a Jewel later leaves or is lost, and later
 * Jewel sanctuaries hold First Class Edel Raids instead. Each Jewel has her own element and name.
 */
public final class JewelRegistry extends SavedData {
    public static final int LIMIT = 7;
    private static final String NAME = "edelraids_jewels";
    private static final Factory<JewelRegistry> FACTORY = new Factory<>(JewelRegistry::new, JewelRegistry::load);

    public record Jewel(UUID id, String name, ResourceLocation element) {}

    private final List<Jewel> jewels = new ArrayList<>(LIMIT);

    public static JewelRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public boolean full() {
        return jewels.size() >= LIMIT;
    }

    public List<Jewel> jewels() {
        return Collections.unmodifiableList(jewels);
    }

    public Set<ResourceLocation> usedElements() {
        Set<ResourceLocation> s = new HashSet<>();
        for (Jewel j : jewels) s.add(j.element());
        return s;
    }

    public boolean nameTaken(String name) {
        for (Jewel j : jewels) if (j.name().equalsIgnoreCase(name)) return true;
        return false;
    }

    /** @return her place among the seven (1-7), or 0 if all seven already exist */
    public int claim(UUID id, String name, ResourceLocation element) {
        if (full()) return 0;
        jewels.add(new Jewel(id, name, element));
        setDirty();
        return jewels.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Jewel j : jewels) {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", j.id());
            c.putString("Name", j.name());
            c.putString("Element", j.element().toString());
            list.add(c);
        }
        tag.put("Jewels", list);
        return tag;
    }

    private static JewelRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        JewelRegistry r = new JewelRegistry();
        ListTag list = tag.getList("Jewels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && r.jewels.size() < LIMIT; i++) {
            CompoundTag c = list.getCompound(i);
            ResourceLocation el = ResourceLocation.tryParse(c.getString("Element"));
            if (!c.hasUUID("Id") || el == null) continue;
            r.jewels.add(new Jewel(c.getUUID("Id"), c.getString("Name"), el));
        }
        return r;
    }
}
