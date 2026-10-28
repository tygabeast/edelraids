package com.tygabeast.edelraids.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Finds a hunter camp's empty cage: a one-block cell with a fence gate on one side and iron bars on the other three,
 * open to stand in, with nobody inside. Works whatever way the camp was rotated. It scans blocks, so it only runs on
 * the rare event that needs it (hunters beating an Arc Aile agent), never per tick.
 */
public final class CampCages {
    private CampCages() {}

    @Nullable
    public static BlockPos findEmptyCage(ServerLevel level, BlockPos around, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = -4; y <= 4; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    m.set(around.getX() + x, around.getY() + y, around.getZ() + z);
                    if (!level.isLoaded(m)) continue;
                    BlockState gate = level.getBlockState(m);
                    if (!(gate.getBlock() instanceof FenceGateBlock)) continue;
                    Direction facing = gate.getValue(FenceGateBlock.FACING);
                    for (Direction side : new Direction[]{facing, facing.getOpposite()}) {
                        BlockPos cell = m.relative(side);
                        if (!isEmptyCell(level, cell, side)) continue;
                        double d = cell.distSqr(around);
                        if (d < bestDist) {
                            bestDist = d;
                            best = cell.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Standing room, bars on the three sides away from the gate, and no Edel Raid already inside. */
    private static boolean isEmptyCell(ServerLevel level, BlockPos cell, Direction fromGate) {
        if (!level.getBlockState(cell).isAir() || !level.getBlockState(cell.above()).isAir()) return false;
        int bars = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d == fromGate.getOpposite()) continue;
            if (level.getBlockState(cell.relative(d)).is(Blocks.IRON_BARS)) bars++;
        }
        return bars == 3 && level.getEntitiesOfClass(EdelRaidEntity.class, new AABB(cell)).isEmpty();
    }
}
