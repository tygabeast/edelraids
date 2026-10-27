package com.tygabeast.edelraids.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * An Edel Raid song, loaded from data/<namespace>/edelraids/song/*.json and synced to clients (for names on the HUD).
 * Its display name is the lang key "song.<namespace>.<path>".
 *
 * @param element  the element that sings it
 * @param slot     1 and 2 unlock at the configured bond levels; 3 is the full song and unlocks with awakening
 * @param shape    how it reaches its targets
 * @param cost     resonance spent
 * @param power    damage per target (per pulse for Field; absorption points for Ward)
 * @param size     radius for Burst and Field, length for Wave, Dash and Call
 * @param duration ticks a Field or Ward lasts (ignored by other shapes)
 */
public record Song(ResourceLocation element, int slot, Shape shape, int cost, float power, float size, int duration) {

    public static final Codec<Song> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("element").forGetter(Song::element),
            Codec.intRange(1, 3).fieldOf("slot").forGetter(Song::slot),
            Shape.CODEC.fieldOf("shape").forGetter(Song::shape),
            Codec.intRange(0, 1000).fieldOf("cost").forGetter(Song::cost),
            Codec.floatRange(0f, 1000f).fieldOf("power").forGetter(Song::power),
            Codec.floatRange(0.5f, 64f).fieldOf("size").forGetter(Song::size),
            Codec.intRange(0, 72000).optionalFieldOf("duration", 0).forGetter(Song::duration)
    ).apply(i, Song::new));

    public enum Shape implements StringRepresentable {
        BURST, WAVE, DASH, WARD, FIELD, CALL;

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
