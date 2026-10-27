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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only wiring, called from the mod constructor only on the physical client. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onRegisterRenderers);
        modBus.addListener(ClientSetup::onClientSetup);
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
        });
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EDEL_RAID.get(), EdelRaidRenderer::new);
    }
}
