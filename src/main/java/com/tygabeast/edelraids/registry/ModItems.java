package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(EdelRaids.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EdelRaids.MODID);

    /** One item for every element and form: the form and element come from the stack's data component. */
    public static final DeferredItem<EdelWeaponItem> EDEL_WEAPON = REGISTER.register("edel_weapon",
            () -> new EdelWeaponItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));

    public static final DeferredItem<DeferredSpawnEggItem> EDEL_RAID_SPAWN_EGG = REGISTER.register("edel_raid_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EDEL_RAID, 0x7CE0B0, 0xF2E8D5, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.edelraids"))
                    .icon(() -> new ItemStack(EDEL_RAID_SPAWN_EGG.get()))
                    .displayItems((params, out) -> out.accept(EDEL_RAID_SPAWN_EGG.get()))
                    .build());

    private ModItems() {}
}
