package com.tygabeast.edelraids.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Stone and Refined Stabilizers. Their use is handled by the Sting Raid herself (StingRaidEntity#mobInteract). */
public class StabilizerItem extends Item {
    private final String tooltipKey;

    public StabilizerItem(Properties props, String tooltipKey) {
        super(props);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
