package com.tygabeast.edelraids;

import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import com.tygabeast.edelraids.entity.HunterEntity;
import com.tygabeast.edelraids.event.CommonEvents;
import com.tygabeast.edelraids.network.ModNetwork;
import com.tygabeast.edelraids.registry.ModAttachments;
import com.tygabeast.edelraids.registry.ModComponents;
import com.tygabeast.edelraids.registry.ModEffects;
import com.tygabeast.edelraids.registry.ModEntities;
import com.tygabeast.edelraids.registry.ModItems;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EdelRaids.MODID)
public final class EdelRaids {
    public static final String MODID = "edelraids";
    public static final Logger LOG = LoggerFactory.getLogger("Edel Raids");

    public EdelRaids(IEventBus modBus, ModContainer container) {
        ModComponents.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModEffects.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModItems.TABS.register(modBus);
        ModAttachments.REGISTER.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModRegistries::onNewRegistry);
        modBus.addListener(EdelRaids::onAttributes);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        CommonEvents.register(NeoForge.EVENT_BUS);
        if (FMLEnvironment.dist == Dist.CLIENT) com.tygabeast.edelraids.client.ClientSetup.init(modBus, container);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.EDEL_RAID.get(), EdelRaidEntity.createAttributes().build());
        event.put(ModEntities.STING_RAID.get(), StingRaidEntity.createAttributes().build());
        event.put(ModEntities.HUNTER.get(), HunterEntity.createAttributes().build());
        event.put(ModEntities.HUNTER_MARKSMAN.get(), HunterEntity.createAttributes().build());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static void debug(String msg, Object... args) {
        if (Config.DEBUG.get()) LOG.info(msg, args);
    }
}
