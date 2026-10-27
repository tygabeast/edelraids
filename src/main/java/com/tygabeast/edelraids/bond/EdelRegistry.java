package com.tygabeast.edelraids.bond;

import com.tygabeast.edelraids.data.EdelRaidData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide record of where each Edel Raid is right now: in the world as an entity, or held as a weapon by a player.
 * Every React and reform bumps her stamp; any entity or weapon whose stamp or holder does not match is a stale copy.
 * While she is held, a snapshot of her data is kept so she can be restored if her weapon item is ever lost
 * (for example if a mod that added data to it is removed). Lookups are a single HashMap get.
 */
public final class EdelRegistry extends SavedData {
    private static final String NAME = "edelraids_registry";
    private static final Factory<EdelRegistry> FACTORY = new Factory<>(EdelRegistry::new, EdelRegistry::load);

    /**
     * @param held     true while she is a weapon; holder is then the player's UUID, otherwise her entity's UUID
     * @param snapshot her data while held, for recovery; null while she is in the world
     */
    public record Entry(boolean held, UUID holder, long stamp, @Nullable EdelRaidData snapshot) {}

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
        entries.put(id, new Entry(false, entity, stamp, null));
        setDirty();
    }

    public void setHeld(UUID id, UUID player, long stamp, EdelRaidData snapshot) {
        entries.put(id, new Entry(true, player, stamp, snapshot));
        setDirty();
    }

    /** Keep the recovery snapshot current (bond changes while she is held). */
    public void updateSnapshot(EdelRaidData data) {
        Entry e = entries.get(data.id());
        if (e != null && e.held() && e.stamp() == data.stamp()) {
            entries.put(data.id(), new Entry(true, e.holder(), e.stamp(), data));
            setDirty();
        }
    }

    /** Snapshots of every Edel Raid currently held by this player. Runs on login only, so a full scan is fine. */
    public List<EdelRaidData> heldBy(UUID player) {
        List<EdelRaidData> out = new ArrayList<>(2);
        for (Entry e : entries.values()) {
            if (e.held() && e.snapshot() != null && e.holder().equals(player)) out.add(e.snapshot());
        }
        return out;
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
            if (e.snapshot() != null) {
                EdelRaidData.CODEC.encodeStart(NbtOps.INSTANCE, e.snapshot()).result().ifPresent(t -> c.put("Snapshot", t));
            }
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
            if (!c.hasUUID("Id") || !c.hasUUID("Holder")) continue;
            EdelRaidData snap = c.contains("Snapshot")
                    ? EdelRaidData.CODEC.parse(NbtOps.INSTANCE, c.get("Snapshot")).result().orElse(null) : null;
            r.entries.put(c.getUUID("Id"), new Entry(c.getBoolean("Held"), c.getUUID("Holder"), c.getLong("Stamp"), snap));
        }
        return r;
    }
}
