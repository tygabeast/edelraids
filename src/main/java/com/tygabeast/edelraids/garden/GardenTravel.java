package com.tygabeast.edelraids.garden;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Travel through the Garden Gate and back. The arrival court (with the Return Gate) is built once, on the first
 * crossing, on the ground at the Garden's centre. Companions who are following you (not sitting, not resting, not
 * wounded) cross with you.
 */
public final class GardenTravel {
    public static final ResourceKey<Level> GARDEN = ResourceKey.create(Registries.DIMENSION, EdelRaids.id("edel_garden"));
    private static final int COMPANION_RANGE = 16;

    private GardenTravel() {}

    /** @return false if Edel Garden is unavailable (for example a datapack removed it) */
    public static boolean toGarden(ServerPlayer p, boolean forced) {
        MinecraftServer server = p.getServer();
        ServerLevel garden = server.getLevel(GARDEN);
        if (garden == null) return false;
        GardenData data = GardenData.get(server);
        BlockPos arrival = data.arrival();
        if (arrival == null) {
            arrival = buildArrivalCourt(garden);
            data.setArrival(arrival);
        }
        data.setReturn(p.getUUID(), new GardenData.ReturnPoint(p.level().dimension(), p.blockPosition(), p.getYRot()));
        data.setForced(p.getUUID(), forced);
        Vec3 to = Vec3.atBottomCenterOf(arrival.south(2));
        move(p, garden, to, 180f);
        p.displayClientMessage(Component.translatable(forced ? "message.edelraids.garden_forced" : "message.edelraids.garden_entered"), false);
        return true;
    }

    public static void toWorld(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        GardenData data = GardenData.get(server);
        GardenData.ReturnPoint back = data.returnPoint(p.getUUID());
        ServerLevel level = back == null ? null : server.getLevel(back.dimension());
        Vec3 to;
        float yaw;
        if (level == null) {
            level = server.overworld();
            to = Vec3.atBottomCenterOf(level.getSharedSpawnPos());
            yaw = 0f;
        } else {
            to = Vec3.atBottomCenterOf(back.pos());
            yaw = back.yaw();
        }
        data.setForced(p.getUUID(), false);
        move(p, level, to, yaw);
        p.displayClientMessage(Component.translatable("message.edelraids.garden_left"), false);
    }

    /** The player and every companion following them cross together. */
    private static void move(ServerPlayer p, ServerLevel dest, Vec3 to, float yaw) {
        ServerLevel from = p.serverLevel();
        List<TamableAnimal> followers = from.getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(COMPANION_RANGE),
                t -> t.isOwnedBy(p) && !t.isOrderedToSit() && following(t));
        from.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.8f);
        p.changeDimension(new DimensionTransition(dest, to, Vec3.ZERO, yaw, 0f, DimensionTransition.DO_NOTHING));
        int i = 0;
        for (TamableAnimal t : followers) {
            double angle = (i++) * 1.3;
            Vec3 at = to.add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            t.changeDimension(new DimensionTransition(dest, at, Vec3.ZERO, yaw, 0f, DimensionTransition.DO_NOTHING));
        }
        dest.playSound(null, BlockPos.containing(to), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static boolean following(TamableAnimal t) {
        if (t instanceof EdelRaidEntity e) return !e.isBusy() && !e.isLoyal() && !e.isCaptive();
        if (t instanceof StingRaidEntity s) return !s.isResting() && !s.isSubdued();
        return true; // pets that follow, like wolves
    }

    /** Builds the arrival court on the ground at the Garden's centre; returns the Return Gate's position. */
    private static BlockPos buildArrivalCourt(ServerLevel garden) {
        int y = garden.getChunkSource().getGenerator().getBaseHeight(0, 0, Heightmap.Types.WORLD_SURFACE_WG, garden,
                garden.getChunkSource().randomState());
        Optional<StructureTemplate> template = garden.getStructureManager().get(EdelRaids.id("garden/arrival_court"));
        BlockPos corner = new BlockPos(-4, y - 1, -4);
        if (template.isPresent()) {
            template.get().placeInWorld(garden, corner, corner, new StructurePlaceSettings(), garden.getRandom(), 2);
        } else {
            EdelRaids.LOG.warn("Edel Garden arrival court template is missing; building a bare gate");
            garden.setBlockAndUpdate(new BlockPos(0, y, 0), com.tygabeast.edelraids.registry.ModBlocks.RETURN_GATE.get().defaultBlockState());
        }
        EdelRaids.debug("Built the Edel Garden arrival court at y {}", y);
        return new BlockPos(0, y, 0);
    }

    @Nullable
    public static ServerLevel garden(MinecraftServer server) {
        return server.getLevel(GARDEN);
    }
}
