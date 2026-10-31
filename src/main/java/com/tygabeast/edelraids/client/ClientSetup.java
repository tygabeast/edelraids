package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModEntities;
import com.tygabeast.edelraids.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only wiring, called from the mod constructor only on the physical client. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // NeoForge's built-in screen (Mods list > Edel Raids > Config) edits both the client and server files.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(ClientSetup::onRegisterRenderers);
        modBus.addListener(HunterRenderer::registerLayer);
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(SongClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(SongClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SongClient::onRenderGui);
    }

    /** The edelraids:form property picks each form's item model, which holds that form's hand positions. */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemPropertyFunction form = (stack, level, entity, seed) -> {
                EdelRaidData d = EdelWeaponItem.data(stack);
                return d == null ? 0f : FormIndex.of(d.form());
            };
            ItemProperties.register(ModItems.EDEL_WEAPON.get(), EdelRaids.id("form"), form);
            ItemProperties.register(ModItems.EDEL_TWIN.get(), EdelRaids.id("form"), form);
            // edelraids:story picks the keepsake's picture (letter, flower, drawing, ribbon).
            ItemProperties.register(ModItems.KEEPSAKE.get(), EdelRaids.id("story"), (stack, level, entity, seed) -> {
                var k = stack.get(com.tygabeast.edelraids.registry.ModComponents.KEEPSAKE.get());
                return k == null ? 0f : k.story();
            });
        });
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EDEL_RAID.get(), EdelRaidRenderer::new);
        event.registerEntityRenderer(ModEntities.STING_RAID.get(), StingRaidRenderer::new);
        event.registerEntityRenderer(ModEntities.HUNTER.get(), ctx -> new HunterRenderer(ctx, "hunter"));
        event.registerEntityRenderer(ModEntities.HUNTER_MARKSMAN.get(), ctx -> new HunterRenderer(ctx, "hunter_marksman"));
        event.registerEntityRenderer(ModEntities.ARC_AILE_AGENT.get(), AgentRenderer::new);
    }
}
