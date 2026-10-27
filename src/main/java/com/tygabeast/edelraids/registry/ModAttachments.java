package com.tygabeast.edelraids.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tygabeast.edelraids.EdelRaids;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, EdelRaids.MODID);

    /**
     * A Pledger's resonance (0-100) and selected song slot (1-3). Synced only to that player, and only when it changes
     * (a hit, a song, a drain step every half second while draining). Resets on death.
     */
    public record Resonance(int value, int selected) {
        public static final Resonance EMPTY = new Resonance(0, 1);
        public static final Codec<Resonance> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("value").forGetter(Resonance::value),
                Codec.INT.fieldOf("selected").forGetter(Resonance::selected)
        ).apply(i, Resonance::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Resonance> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Resonance::value, ByteBufCodecs.VAR_INT, Resonance::selected, Resonance::new);

        public Resonance withValue(int v) {
            return new Resonance(v, selected);
        }

        public Resonance withSelected(int s) {
            return new Resonance(value, s);
        }
    }

    public static final Supplier<AttachmentType<Resonance>> RESONANCE = REGISTER.register("resonance",
            () -> AttachmentType.builder(() -> Resonance.EMPTY)
                    .serialize(Resonance.CODEC)
                    .sync((holder, to) -> holder == to, Resonance.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
