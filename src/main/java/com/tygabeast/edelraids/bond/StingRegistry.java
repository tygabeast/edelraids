package com.tygabeast.edelraids.bond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Which freed Sting Raids each player looks after, loaded or not, and which of them are active (following) rather than
 * resting, so the active limit cannot be dodged by leaving some in unloaded chunks. Only freeing, resting, release
 * and removal change it.
 */
public final class StingRegistry extends SavedData {
    private static final String NAME = "edelraids_stings";
    private static final Factory<StingRegistry> FACTORY = new Factory<>(StingRegistry::new, StingRegistry::load);

    /** owner -> (Sting Raid entity UUID -> active). Resting ones are kept so the record survives toggling. */
    private final Map<UUID, Map<UUID, Boolean>> byOwner = new HashMap<>();

    public static StingRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Freed Sting Raids following this player (not resting). */
    public int activeCount(UUID owner) {
        Map<UUID, Boolean> m = byOwner.get(owner);
        if (m == null) return 0;
        int n = 0;
        for (boolean active : m.values()) if (active) n++;
        return n;
    }

    /** All freed Sting Raids of this player, active or resting. */
    public int count(UUID owner) {
        Map<UUID, Boolean> m = byOwner.get(owner);
        return m == null ? 0 : m.size();
    }

    /** Record her and whether she is active; only marks the data dirty when something changed. */
    public void put(UUID owner, UUID entity, boolean active) {
        Boolean old = byOwner.computeIfAbsent(owner, k -> new HashMap<>(4)).put(entity, active);
        if (old == null || old != active) setDirty();
    }

    public void remove(UUID owner, UUID entity) {
        Map<UUID, Boolean> m = byOwner.get(owner);
        if (m != null && m.remove(entity) != null) {
            if (m.isEmpty()) byOwner.remove(owner);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        byOwner.forEach((owner, map) -> map.forEach((e, active) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Owner", owner);
            c.putUUID("Entity", e);
            c.putBoolean("Active", active);
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
            if (!c.hasUUID("Owner") || !c.hasUUID("Entity")) continue;
            boolean active = !c.contains("Active") || c.getBoolean("Active");
            r.byOwner.computeIfAbsent(c.getUUID("Owner"), k -> new HashMap<>(4)).put(c.getUUID("Entity"), active);
        }
        return r;
    }
}
