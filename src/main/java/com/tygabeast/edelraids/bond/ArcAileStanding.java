package com.tygabeast.edelraids.bond;

import com.tygabeast.edelraids.Config;
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
 * Each player's standing with Arc Aile. Sparing their agents and freeing hunters' captives raise it; killing an agent
 * lowers it sharply. At the truce threshold their agents stop confronting that player. One small map, touched only
 * when an agent yields or a captive is freed.
 */
public final class ArcAileStanding extends SavedData {
    private static final String NAME = "edelraids_arc_aile";
    private static final Factory<ArcAileStanding> FACTORY = new Factory<>(ArcAileStanding::new, ArcAileStanding::load);

    private final Map<UUID, Integer> standing = new HashMap<>();
    /** Players already given a Writ of Passage, so a truce regained later does not hand out another. */
    private final Set<UUID> writGiven = new HashSet<>();

    public static ArcAileStanding get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public int standing(UUID player) {
        return standing.getOrDefault(player, 0);
    }

    public boolean truce(UUID player) {
        return standing(player) >= Config.ARC_AILE_TRUCE.get();
    }

    /** @return the new standing */
    public int add(UUID player, int delta) {
        int v = standing(player) + delta;
        standing.put(player, v);
        setDirty();
        return v;
    }

    public void set(UUID player, int value) {
        standing.put(player, value);
        setDirty();
    }

    /** True the first time only. */
    public boolean claimWrit(UUID player) {
        boolean first = writGiven.add(player);
        if (first) setDirty();
        return first;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        standing.forEach((id, v) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Player", id);
            c.putInt("Standing", v);
            c.putBoolean("Writ", writGiven.contains(id));
            list.add(c);
        });
        writGiven.stream().filter(id -> !standing.containsKey(id)).forEach(id -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Player", id);
            c.putInt("Standing", 0);
            c.putBoolean("Writ", true);
            list.add(c);
        });
        tag.put("Players", list);
        return tag;
    }

    private static ArcAileStanding load(CompoundTag tag, HolderLookup.Provider registries) {
        ArcAileStanding s = new ArcAileStanding();
        ListTag list = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("Player")) continue;
            UUID id = c.getUUID("Player");
            s.standing.put(id, c.getInt("Standing"));
            if (c.getBoolean("Writ")) s.writGiven.add(id);
        }
        return s;
    }
}
