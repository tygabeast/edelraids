package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.EdelRaidData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EdelRaids.MODID);

    /** The Edel Raid living inside a weapon item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EdelRaidData>> EDEL_RAID =
            REGISTER.registerComponentType("edel_raid", b -> b.persistent(EdelRaidData.CODEC).networkSynchronized(EdelRaidData.STREAM_CODEC));

    private ModComponents() {}
}
