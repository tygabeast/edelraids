package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.EdelRaidData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EdelRaids.MODID);

    /** The Edel Raid living inside a weapon item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EdelRaidData>> EDEL_RAID =
            REGISTER.registerComponentType("edel_raid", b -> b.persistent(EdelRaidData.CODEC).networkSynchronized(EdelRaidData.STREAM_CODEC));

    /** Markers set from her weapon form when she Reacts, so per-swing and per-use checks are a component lookup. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SWEEPS =
            REGISTER.registerComponentType("sweeps", b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> RANGED =
            REGISTER.registerComponentType("ranged", b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> PAIRED =
            REGISTER.registerComponentType("paired", b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    /** Whose keepsake this is and which one (see KeepsakeItem). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.tygabeast.edelraids.item.KeepsakeItem.Keepsake>> KEEPSAKE =
            REGISTER.registerComponentType("keepsake", b -> b.persistent(com.tygabeast.edelraids.item.KeepsakeItem.Keepsake.CODEC)
                    .networkSynchronized(com.tygabeast.edelraids.item.KeepsakeItem.Keepsake.STREAM_CODEC));

    private ModComponents() {}
}
