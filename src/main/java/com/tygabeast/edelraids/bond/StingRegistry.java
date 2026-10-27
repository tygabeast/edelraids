package com.tygabeast.edelraids.bond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which freed Sting Raids each player looks after, loaded or not, so the companion limit cannot be dodged by leaving
 * some in unloaded chunks. Only freeing, release and removal touch it.
 */
public final class StingRegistry extends SavedData {
    private static final String NAME = "edelraids_stings";
    private static final Factory<StingRegistry> FACTORY = new Factory<>(StingRegistry::new, StingRegistry::load);

    private final Map<UUID, Set<UUID>> byOwner = new HashMap<>();

    public static StingRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public int count(UUID owner) {
        Set<UUID> s = byOwner.get(owner);
        return s == null ? 0 : s.size();
    }

    public boolean contains(UUID owner, UUID entity) {
        Set<UUID> s = byOwner.get(owner);
        return s != null && s.contains(entity);
    }

    public void add(UUID owner, UUID entity) {
        if (byOwner.computeIfAbsent(owner, k -> new HashSet<>(4)).add(entity)) setDirty();
    }

    public void remove(UUID owner, UUID entity) {
        Set<UUID> s = byOwner.get(owner);
        if (s != null && s.remove(entity)) {
            if (s.isEmpty()) byOwner.remove(owner);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        byOwner.forEach((owner, set) -> set.forEach(e -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Owner", owner);
            c.putUUID("Entity", e);
            list.add(c);
        }));
        tag.put("Stings", list);
        return tag;
    }

    private static StingRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        StingRegistry r = new StingRegistry();
        ListTag list = tag.getList("Stings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (c.hasUUID("Owner") && c.hasUUID("Entity")) r.byOwner.computeIfAbsent(c.getUUID("Owner"), k -> new HashSet<>(4)).add(c.getUUID("Entity"));
        }
        return r;
    }
}
