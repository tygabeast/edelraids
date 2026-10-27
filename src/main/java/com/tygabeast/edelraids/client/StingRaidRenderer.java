package com.tygabeast.edelraids.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.StingArm;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Same body as an Edel Raid; the differences are all here and cheap. Her forearm swaps for one of three arm-weapon
 * bone groups while she is Reacted, the arm weapon takes a pale wash of her element, and her artificial gem is dimmer
 * than a real one and flickers, faster as strain builds. Nothing is allocated per frame.
 */
public class StingRaidRenderer extends GeoEntityRenderer<StingRaidEntity> {
    private static final String FOREARM = "right_forearm";
    private static final String[] ARM_BONES = {"sting_blade", "sting_heavy", "sting_launcher"};

    private int gemColour;
    private int armColour;

    public StingRaidRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(EdelRaids.id("sting_raid"), true));
        this.shadowRadius = 0.4f;
    }

    @Override
    public void preRender(PoseStack poseStack, StingRaidEntity e, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        boolean armed = e.isReacted() || e.isReacting();
        StingArm arm = e.getArm();
        model.getBone(FOREARM).ifPresent(b -> b.setHidden(armed));
        for (int i = 0; i < ARM_BONES.length; i++) {
            boolean show = armed && arm.ordinal() == i;
            model.getBone(ARM_BONES[i]).ifPresent(b -> {
                b.setHidden(!show);
                b.setChildrenHidden(!show);
            });
        }

        int rgb = e.getElementColor();
        // Greyed toward stone: an imitation never quite holds the element's true color.
        int r = ((rgb >> 16) & 0xFF) * 7 / 10 + 0x60 * 3 / 10;
        int g = ((rgb >> 8) & 0xFF) * 7 / 10 + 0x60 * 3 / 10;
        int bl = (rgb & 0xFF) * 7 / 10 + 0x60 * 3 / 10;
        // Flicker: two out-of-step waves; strain speeds them up and deepens the dips.
        float strain = e.getStrainDisplay() / 100f;
        float t = (e.tickCount + partialTick + e.getId() * 7) * (0.12f + strain * 0.5f);
        float wave = Mth.sin(t) * Mth.sin(t * 2.3f + 1.7f);
        float bright = Mth.clamp(0.78f + 0.22f * wave - (wave < -0.5f ? strain * 0.35f : 0f), 0.35f, 1f);
        gemColour = 0xFF000000 | ((int) (r * bright) << 16) | ((int) (g * bright) << 8) | (int) (bl * bright);
        // Arm weapons: mostly their own pale stone, a sixth of her element.
        armColour = 0xFF000000 | ((0xFF * 5 + ((rgb >> 16) & 0xFF)) / 6 << 16) | ((0xFF * 5 + ((rgb >> 8) & 0xFF)) / 6 << 8) | (0xFF * 5 + (rgb & 0xFF)) / 6;
        super.preRender(poseStack, e, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, StingRaidEntity e, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        String name = bone.getName();
        if (GemTint.GEM_BONE.equals(name)) {
            packedLight = GemTint.FULL_BRIGHT;
            colour = gemColour;
        } else if (name.startsWith("sting_")) {
            colour = armColour;
        }
        super.renderRecursively(poseStack, e, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
