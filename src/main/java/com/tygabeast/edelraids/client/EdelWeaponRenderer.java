package com.tygabeast.edelraids.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renders every Edel Raid weapon. Per frame it reads the stack's data once, toggles the awakened bones and tints the gem.
 * The element color lookup is cached per element id so most frames do no registry lookup at all.
 */
public class EdelWeaponRenderer extends GeoItemRenderer<EdelWeaponItem> {
    private static final int DEFAULT_COLOR = 0x4FC9A0;

    private ResourceLocation lastElement;
    private Object lastLevel;
    private int lastColor = DEFAULT_COLOR;
    private int frameColor = DEFAULT_COLOR;

    public EdelWeaponRenderer() {
        super(new EdelWeaponModel());
    }

    private int colorFor(ResourceLocation element) {
        var level = Minecraft.getInstance().level;
        if (!element.equals(lastElement) || level != lastLevel) {
            lastElement = element;
            lastLevel = level;
            Element e = level == null ? null : Elements.element(level.registryAccess(), element);
            lastColor = e == null ? DEFAULT_COLOR : e.color();
        }
        return lastColor;
    }

    @Override
    public void preRender(PoseStack poseStack, EdelWeaponItem animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        EdelRaidData d = EdelWeaponItem.data(getCurrentItemStack());
        frameColor = d == null ? DEFAULT_COLOR : colorFor(d.element());
        boolean awakened = d != null && d.awakened();
        model.getBone(GemTint.AWAKENED_BONE).ifPresent(b -> {
            b.setHidden(!awakened);
            b.setChildrenHidden(!awakened);
        });
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EdelWeaponItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (GemTint.GEM_BONE.equals(bone.getName())) {
            packedLight = GemTint.FULL_BRIGHT;
            colour = GemTint.argb(frameColor);
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
