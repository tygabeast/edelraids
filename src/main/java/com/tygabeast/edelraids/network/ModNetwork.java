package com.tygabeast.edelraids.network;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.song.SongManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Two tiny client-to-server key presses. Everything they trigger is checked and done on the server. */
public final class ModNetwork {
    private ModNetwork() {}

    public record Sing() implements CustomPacketPayload {
        public static final Type<Sing> TYPE = new Type<>(EdelRaids.id("sing"));
        public static final StreamCodec<ByteBuf, Sing> CODEC = StreamCodec.unit(new Sing());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record NextSong() implements CustomPacketPayload {
        public static final Type<NextSong> TYPE = new Type<>(EdelRaids.id("next_song"));
        public static final StreamCodec<ByteBuf, NextSong> CODEC = StreamCodec.unit(new NextSong());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(EdelRaids.MODID).versioned("1");
        r.playToServer(Sing.TYPE, Sing.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) SongManager.sing(p);
        });
        r.playToServer(NextSong.TYPE, NextSong.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p) SongManager.cycle(p);
        });
    }
}
