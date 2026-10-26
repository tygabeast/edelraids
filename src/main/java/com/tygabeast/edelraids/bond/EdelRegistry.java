package com.tygabeast.edelraids.bond;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide record of where each Edel Raid is right now: in the world as an entity, or held as a weapon by a player.
 * Every React and reform bumps her stamp; any entity or weapon whose stamp or holder does not match is a stale copy.
 * Saved once with the overworld data; lookups are a single HashMap get, so checks are cheap.
 */
public final class EdelRegistry extends SavedData {
    private static final String NAME = "edelraids_registry";
    private static final Factory<EdelRegistry> FACTORY = new Factory<>(EdelRegistry::new, EdelRegistry::load);

    /** @param held true while she is a weapon; holder is then the player's UUID, otherwise her entity's UUID */
    public record Entry(boolean held, UUID holder, long stamp) {}

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static EdelRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable
    public Entry get(UUID id) {
        return entries.get(id);
    }

    public long nextStamp(UUID id) {
        Entry e = entries.get(id);
        return e == null ? 1L : e.stamp() + 1L;
    }

    public void setWorld(UUID id, UUID entity, long stamp) {
        entries.put(id, new Entry(false, entity, stamp));
        setDirty();
    }

    public void setHeld(UUID id, UUID player, long stamp) {
        entries.put(id, new Entry(true, player, stamp));
        setDirty();
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((id, e) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", id);
            c.putBoolean("Held", e.held());
            c.putUUID("Holder", e.holder());
            c.putLong("Stamp", e.stamp());
            list.add(c);
        });
        tag.put("Entries", list);
        return tag;
    }

    private static EdelRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        EdelRegistry r = new EdelRegistry();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (c.hasUUID("Id") && c.hasUUID("Holder")) {
                r.entries.put(c.getUUID("Id"), new Entry(c.getBoolean("Held"), c.getUUID("Holder"), c.getLong("Stamp")));
            }
        }
        return r;
    }
}
