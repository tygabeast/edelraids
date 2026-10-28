package com.tygabeast.edelraids.block;

import com.tygabeast.edelraids.garden.GardenTravel;
import com.tygabeast.edelraids.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Garden Gate at the heart of an Arc Aile fortress, and the Return Gate in Edel Garden's arrival court.
 * The Garden Gate opens to a Writ of Passage (kept) or an Agent's Sigil (spent: a forced entry); the Return Gate
 * always takes you home. Both are unbreakable, so neither can be carried off and placed somewhere easier.
 */
public class GateBlock extends Block {
    private final boolean returning;

    public GateBlock(Properties props, boolean returning) {
        super(props);
        this.returning = returning;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (returning) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean writ = stack.is(ModItems.WRIT_OF_PASSAGE.get());
        boolean sigil = stack.is(ModItems.AGENT_SIGIL.get());
        if (!writ && !sigil) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (sigil && !sp.isCreative()) stack.shrink(1);
            if (!GardenTravel.toGarden(sp, sigil)) sp.displayClientMessage(Component.translatable("message.edelraids.garden_missing"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (returning) GardenTravel.toWorld(sp);
            else sp.displayClientMessage(Component.translatable("message.edelraids.gate_sealed"), true);
        }
        return InteractionResult.CONSUME;
    }
}
