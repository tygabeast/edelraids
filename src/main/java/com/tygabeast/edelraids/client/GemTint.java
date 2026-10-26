package com.tygabeast.edelraids.client;

import net.minecraft.client.renderer.LightTexture;

/** Shared by both renderers: the bone named "gem" is drawn in the element's color at full brightness. */
final class GemTint {
    static final String GEM_BONE = "gem";
    static final String AWAKENED_BONE = "awakened";
    static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private GemTint() {}

    static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
