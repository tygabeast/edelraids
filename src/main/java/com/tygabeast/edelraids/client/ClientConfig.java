package com.tygabeast.edelraids.client;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

/** Client-only settings (config/edelraids-client.toml). Nothing here affects gameplay or other players. */
public final class ClientConfig {
    private ClientConfig() {}

    /** Where the resonance meter is pinned. The two hotbar spots sit beside the hotbar, clear of the off-hand slot. */
    public enum Anchor implements TranslatableEnum {
        HOTBAR_RIGHT, HOTBAR_LEFT,
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("edelraids.configuration.anchor." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public enum Orientation implements TranslatableEnum {
        HORIZONTAL, VERTICAL;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("edelraids.configuration.orientation." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("meter");
    }

    public static final ModConfigSpec.BooleanValue SHOW_METER = B
            .comment("Show the resonance meter while you hold an Edel Raid.")
            .define("showMeter", true);
    public static final ModConfigSpec.BooleanValue SHOW_SONG_NAME = B
            .comment("Show the selected song's name and cost next to the meter.")
            .define("showSongName", true);
    public static final ModConfigSpec.EnumValue<Anchor> ANCHOR = B
            .comment("Where the meter is pinned on screen.")
            .defineEnum("anchor", Anchor.HOTBAR_RIGHT);
    public static final ModConfigSpec.EnumValue<Orientation> ORIENTATION = B
            .comment("Horizontal fills left to right; vertical fills bottom to top.")
            .defineEnum("orientation", Orientation.HORIZONTAL);
    public static final ModConfigSpec.IntValue LENGTH = B
            .comment("Length of the bar in GUI pixels.")
            .defineInRange("length", 62, 24, 182);
    public static final ModConfigSpec.IntValue OFFSET_X = B
            .comment("Moves the meter right (positive) or left (negative) from its anchor, in GUI pixels.")
            .defineInRange("offsetX", 0, -1000, 1000);
    public static final ModConfigSpec.IntValue OFFSET_Y = B
            .comment("Moves the meter down (positive) or up (negative) from its anchor, in GUI pixels.")
            .defineInRange("offsetY", 0, -1000, 1000);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();
}
