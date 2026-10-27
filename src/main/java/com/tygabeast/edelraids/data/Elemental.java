package com.tygabeast.edelraids.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Anything with an element (Edel Raids and Sting Raids) takes part in ring advantage. */
public interface Elemental {
    @Nullable
    ResourceLocation getElementId();
}
