package com.tygabeast.edelraids.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Edel Garden bookkeeping: where its arrival court was built, where each traveller came from (so the Return Gate takes
 * them back), and who forced their way in with an agent's sigil (Edel Garden's wardens will remember, from 0.8.0).
 */
public final class GardenData extends SavedData {
    private static final String NAME = "edelraids_garden";
    private static final Factory<GardenData> FACTORY = new Factory<>(GardenData::new, GardenData::load);

    public record ReturnPoint(ResourceKey<Level> dimension, BlockPos pos, float yaw) {}

    @Nullable private BlockPos arrival;
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();
    private final Set<UUID> forced = new HashSet<>();

    public static GardenData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable public BlockPos arrival() { return arrival; }

    public void setArrival(BlockPos pos) {
        arrival = pos;
        setDirty();
    }

    @Nullable public ReturnPoint returnPoint(UUID player) { return returns.get(player); }

    public void setReturn(UUID player, ReturnPoint point) {
        returns.put(player, point);
        setDirty();
    }

    public boolean forced(UUID player) { return forced.contains(player); }

    public void setForced(UUID player, boolean value) {
        if (value ? forced.add(player) : forced.remove(player)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (arrival != null) tag.putLong("Arrival", arrival.asLong());
        ListTag list = new ListTag();
        returns.forEach((id, r) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Player", id);
            c.putString("Dimension", r.dimension().location().toString());
            c.putLong("Pos", r.pos().asLong());
            c.putFloat("Yaw", r.yaw());
            c.putBoolean("Forced", forced.contains(id));
            list.add(c);
        });
        tag.put("Travellers", list);
        return tag;
    }

    private static GardenData load(CompoundTag tag, HolderLookup.Provider registries) {
        GardenData d = new GardenData();
        if (tag.contains("Arrival")) d.arrival = BlockPos.of(tag.getLong("Arrival"));
        ListTag list = tag.getList("Travellers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(c.getString("Dimension"));
            if (!c.hasUUID("Player") || dim == null) continue;
            UUID id = c.getUUID("Player");
            d.returns.put(id, new ReturnPoint(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(c.getLong("Pos")), c.getFloat("Yaw")));
            if (c.getBoolean("Forced")) d.forced.add(id);
        }
        return d;
    }
}
