package com.tygabeast.edelraids.entity;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Arc Aile patrols: once a minute, each Overworld player has a small chance of a patrol of 1 or 2 agents appearing
 * 32 to 48 blocks away, unless one is already within 96 blocks. Patrol agents despawn when far from everyone, like
 * other wandering mobs, so they never pile up. The whole check is one modulo per server tick.
 */
public final class ArcAilePatrols {
    private ArcAilePatrols() {}

    public static void tick(ServerLevel level, int serverTick) {
        if (serverTick % 1200 != 0 || level.dimension() != Level.OVERWORLD) return;
        double chance = Config.ARC_AILE_PATROL_CHANCE.get();
        if (chance <= 0) return;
        RandomSource random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || random.nextDouble() >= chance) continue;
            if (!level.getEntitiesOfClass(ArcAileAgentEntity.class, p.getBoundingBox().inflate(96.0)).isEmpty()) continue;
            spawnPatrolNear(level, p, random);
        }
    }

    /** @return how many agents were spawned (0 if no spot was found) */
    public static int spawnPatrolNear(ServerLevel level, ServerPlayer p, RandomSource random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double dist = 32 + random.nextInt(17);
            int x = Mth.floor(p.getX() + Mth.cos(angle) * dist);
            int z = Mth.floor(p.getZ() + Mth.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getFluidState(pos.below()).isEmpty() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            int count = random.nextFloat() < 0.4f ? 2 : 1;
            int spawned = 0;
            for (int i = 0; i < count; i++) {
                ArcAileAgentEntity a = ModEntities.ARC_AILE_AGENT.get().create(level);
                if (a == null) continue;
                a.moveTo(x + 0.5 + i * 1.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
                a.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null);
                level.addFreshEntity(a);
                spawned++;
            }
            return spawned;
        }
        return 0;
    }
}
