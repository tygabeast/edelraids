package com.tygabeast.edelraids.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tygabeast.edelraids.registry.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Something a sheltered Edel Raid carried into Edel Garden, given to the one who leads her out. Each tells a little of
 * what the Garden costs: outside, time kept passing for everyone she knew.
 */
public class KeepsakeItem extends Item {
    public static final int STORIES = 4;

    /** @param owner the Edel Raid it belonged to; @param story which keepsake (0 to STORIES - 1) */
    public record Keepsake(String owner, int story) {
        public static final Codec<Keepsake> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("owner").forGetter(Keepsake::owner),
                Codec.intRange(0, STORIES - 1).fieldOf("story").forGetter(Keepsake::story)
        ).apply(i, Keepsake::new));
        public static final StreamCodec<ByteBuf, Keepsake> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Keepsake::owner, ByteBufCodecs.VAR_INT, Keepsake::story, Keepsake::new);
    }

    public KeepsakeItem(Properties props) {
        super(props);
    }

    public static ItemStack create(Item item, String owner, int story) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.KEEPSAKE.get(), new Keepsake(owner, Math.floorMod(story, STORIES)));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Keepsake k = stack.get(ModComponents.KEEPSAKE.get());
        return k == null ? super.getName(stack) : Component.translatable("item.edelraids.keepsake." + k.story());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Keepsake k = stack.get(ModComponents.KEEPSAKE.get());
        if (k == null) return;
        tooltip.add(Component.translatable("tooltip.edelraids.keepsake." + k.story() + ".1", k.owner()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.edelraids.keepsake." + k.story() + ".2", k.owner()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
