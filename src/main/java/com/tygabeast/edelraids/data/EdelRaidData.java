package com.tygabeast.edelraids.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/**
 * Everything that makes one Edel Raid who she is. Stored on her weapon item as a data component while she is Reacted,
 * and copied back onto the entity when she reforms. Immutable: change it with the with* methods and set it again.
 *
 * @param id        stable identity, kept across every React and reform (not the entity UUID, which changes)
 * @param stamp     anti-duplication stamp; bumped on every React and reform and checked against {@code EdelRegistry}
 * @param awakened  derived from bond on the server and stored so clients can render the awakened model parts
 */
public record EdelRaidData(UUID id, String name, ResourceLocation element, ResourceLocation form, int quality,
                           float health, float maxHealth, Optional<UUID> pledger, int bond, boolean awakened, long stamp) {

    public static final Codec<EdelRaidData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(EdelRaidData::id),
            Codec.STRING.fieldOf("name").forGetter(EdelRaidData::name),
            ResourceLocation.CODEC.fieldOf("element").forGetter(EdelRaidData::element),
            ResourceLocation.CODEC.fieldOf("form").forGetter(EdelRaidData::form),
            Codec.intRange(1, 10).fieldOf("quality").forGetter(EdelRaidData::quality),
            Codec.FLOAT.fieldOf("health").forGetter(EdelRaidData::health),
            Codec.FLOAT.fieldOf("max_health").forGetter(EdelRaidData::maxHealth),
            UUIDUtil.CODEC.optionalFieldOf("pledger").forGetter(EdelRaidData::pledger),
            Codec.INT.fieldOf("bond").forGetter(EdelRaidData::bond),
            Codec.BOOL.optionalFieldOf("awakened", false).forGetter(EdelRaidData::awakened),
            Codec.LONG.fieldOf("stamp").forGetter(EdelRaidData::stamp)
    ).apply(i, EdelRaidData::new));

    /** Small, and only sent when the stack changes (React, a kill that raises bond), so the NBT-backed codec is fine. */
    public static final StreamCodec<ByteBuf, EdelRaidData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public EdelRaidData withHealth(float h) {
        return new EdelRaidData(id, name, element, form, quality, h, maxHealth, pledger, bond, awakened, stamp);
    }

    public EdelRaidData withBond(int b, boolean awake) {
        return new EdelRaidData(id, name, element, form, quality, health, maxHealth, pledger, b, awake, stamp);
    }

    public EdelRaidData withStamp(long s) {
        return new EdelRaidData(id, name, element, form, quality, health, maxHealth, pledger, bond, awakened, s);
    }

    public boolean isPledger(UUID player) {
        return pledger.isPresent() && pledger.get().equals(player);
    }
}
