package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import com.tygabeast.edelraids.entity.HunterEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, EdelRaids.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EdelRaidEntity>> EDEL_RAID = REGISTER.register("edel_raid",
            () -> EntityType.Builder.of(EdelRaidEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build(EdelRaids.id("edel_raid").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<StingRaidEntity>> STING_RAID = REGISTER.register("sting_raid",
            () -> EntityType.Builder.of(StingRaidEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build(EdelRaids.id("sting_raid").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<HunterEntity>> HUNTER = REGISTER.register("hunter",
            () -> EntityType.Builder.of(HunterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(8)
                    .build(EdelRaids.id("hunter").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<HunterEntity>> HUNTER_MARKSMAN = REGISTER.register("hunter_marksman",
            () -> EntityType.Builder.of(HunterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(8)
                    .build(EdelRaids.id("hunter_marksman").toString()));

    private ModEntities() {}
}
