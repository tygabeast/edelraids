package com.tygabeast.edelraids.registry;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.block.GateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The two gates. No block items: they only exist where Arc Aile built them (operators can /setblock them). */
public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(EdelRaids.MODID);

    private static BlockBehaviour.Properties gate(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1f, 3600000f).noLootTable()
                .lightLevel(s -> 12).sound(SoundType.AMETHYST).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<GateBlock> GARDEN_GATE = REGISTER.register("garden_gate",
            () -> new GateBlock(gate(MapColor.COLOR_LIGHT_BLUE), false));

    public static final DeferredBlock<GateBlock> RETURN_GATE = REGISTER.register("return_gate",
            () -> new GateBlock(gate(MapColor.COLOR_LIGHT_GREEN), true));

    private ModBlocks() {}
}
