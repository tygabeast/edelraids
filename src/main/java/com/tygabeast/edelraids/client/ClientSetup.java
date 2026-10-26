package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.registry.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only wiring, called from the mod constructor only on the physical client. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EDEL_RAID.get(), EdelRaidRenderer::new);
    }
}
