package com.tygabeast.edelraids.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Placeholder model; the gem picks up her element color, which arrives once as synced entity data. */
public class EdelRaidRenderer extends GeoEntityRenderer<EdelRaidEntity> {
    public EdelRaidRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(EdelRaids.id("edel_raid"), true));
        this.shadowRadius = 0.4f;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EdelRaidEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (GemTint.GEM_BONE.equals(bone.getName())) {
            packedLight = GemTint.FULL_BRIGHT;
            colour = GemTint.argb(animatable.getElementColor());
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
