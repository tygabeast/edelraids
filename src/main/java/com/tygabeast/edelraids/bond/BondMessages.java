package com.tygabeast.edelraids.bond;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Songs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Tells the Pledger when a bond change unlocks a song. Runs only when bond changes, never per tick. */
public final class BondMessages {
    private BondMessages() {}

    public static void announce(ServerPlayer pledger, EdelRaidData before, EdelRaidData after) {
        Songs.Entry[] songs = Songs.forElement(pledger.level().registryAccess(), after.element());
        int[] needs = {Config.SONG_BOND_1.get(), Config.SONG_BOND_2.get()};
        for (int i = 0; i < 2; i++) {
            if (before.bond() < needs[i] && after.bond() >= needs[i] && songs[i] != null) {
                pledger.displayClientMessage(Component.translatable("message.edelraids.learned_song", after.name(), songs[i].name()), false);
                pledger.level().playSound(null, pledger, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
            }
        }
        if (!before.awakened() && after.awakened() && songs[2] != null) {
            pledger.displayClientMessage(Component.translatable("message.edelraids.learned_song", after.name(), songs[2].name()), false);
        }
    }
}
