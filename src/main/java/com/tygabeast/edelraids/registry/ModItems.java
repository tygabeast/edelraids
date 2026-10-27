package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.item.EdelTwinItem;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.item.StabilizerItem;
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

    /** The off-hand half of a paired form. A display and attack copy only: she lives in the main-hand item. */
    public static final DeferredItem<EdelTwinItem> EDEL_TWIN = REGISTER.register("edel_twin",
            () -> new EdelTwinItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));

    public static final DeferredItem<DeferredSpawnEggItem> EDEL_RAID_SPAWN_EGG = REGISTER.register("edel_raid_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EDEL_RAID, 0x7CE0B0, 0xF2E8D5, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> STING_RAID_SPAWN_EGG = REGISTER.register("sting_raid_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STING_RAID, 0x9FB0A8, 0x5A4A58, new Item.Properties()));

    /** Revives a subdued Sting Raid (and steadies one you have just freed). */
    public static final DeferredItem<StabilizerItem> STONE_STABILIZER = REGISTER.register("stone_stabilizer",
            () -> new StabilizerItem(new Item.Properties().stacksTo(16), "tooltip.edelraids.stone_stabilizer"));

    /** Used once on a freed Sting Raid: her stone strains half as fast from then on. */
    public static final DeferredItem<StabilizerItem> REFINED_STABILIZER = REGISTER.register("refined_stabilizer",
            () -> new StabilizerItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), "tooltip.edelraids.refined_stabilizer"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.edelraids"))
                    .icon(() -> new ItemStack(EDEL_RAID_SPAWN_EGG.get()))
                    .displayItems((params, out) -> {
                        out.accept(EDEL_RAID_SPAWN_EGG.get());
                        out.accept(STING_RAID_SPAWN_EGG.get());
                        out.accept(STONE_STABILIZER.get());
                        out.accept(REFINED_STABILIZER.get());
                    })
                    .build());

    private ModItems() {}
}
