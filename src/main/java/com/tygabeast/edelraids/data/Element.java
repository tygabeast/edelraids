package com.tygabeast.edelraids.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One of the 24 elements, loaded from data/<namespace>/edelraids/element/*.json and synced to clients.
 *
 * @param ring     glittering ring 1-4
 * @param position position 0-5 inside the ring; each element beats the next one (position + 1)
 * @param color    gem and glow tint, written as "#RRGGBB" in JSON
 * @param forms    weapon forms this element can roll; empty means "any form"
 * @param onHit    effect applied when her weapon form hits
 * @param rollable whether new Edel Raids (spawn eggs, natural spawns) can roll this element; ring 4 is Edel Garden only
 */
public record Element(int ring, int position, int color, List<ResourceLocation> forms, OnHit onHit, boolean rollable) {

    public static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(s -> {
        try {
            String t = s.startsWith("#") ? s.substring(1) : s;
            return DataResult.success((int) (Long.parseLong(t, 16) & 0xFFFFFF));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color: " + s);
        }
    }, c -> String.format("#%06X", c & 0xFFFFFF));

    public static final Codec<Element> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4).fieldOf("ring").forGetter(Element::ring),
            Codec.intRange(0, 5).fieldOf("position").forGetter(Element::position),
            HEX_COLOR.fieldOf("color").forGetter(Element::color),
            ResourceLocation.CODEC.listOf().optionalFieldOf("forms", List.of()).forGetter(Element::forms),
            OnHit.CODEC.optionalFieldOf("on_hit", OnHit.NONE).forGetter(Element::onHit),
            Codec.BOOL.optionalFieldOf("rollable", true).forGetter(Element::rollable)
    ).apply(i, Element::new));

    /** @param type an on-hit effect id handled by {@link OnHitEffects}; strength meaning depends on the type */
    public record OnHit(ResourceLocation type, float strength) {
        public static final OnHit NONE = new OnHit(ResourceLocation.fromNamespaceAndPath("edelraids", "none"), 0f);
        public static final Codec<OnHit> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(OnHit::type),
                Codec.FLOAT.optionalFieldOf("strength", 1f).forGetter(OnHit::strength)
        ).apply(i, OnHit::new));
    }
}
