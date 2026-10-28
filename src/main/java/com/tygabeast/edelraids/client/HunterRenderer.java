package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.entity.HunterEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Vanilla humanoid model and item-in-hand layer; one texture per hunter type. No armor layer, to keep them cheap. */
public class HunterRenderer extends HumanoidMobRenderer<HunterEntity, HunterRenderer.HunterModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(EdelRaids.id("hunter"), "main");

    private final ResourceLocation texture;

    public HunterRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx, new HunterModel(ctx.bakeLayer(LAYER)), 0.5f);
        this.texture = EdelRaids.id("textures/entity/" + name + ".png");
    }

    static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 64));
    }

    @Override
    public ResourceLocation getTextureLocation(HunterEntity entity) {
        return texture;
    }

    /** Crossbow poses like a pillager's, and a weapon swing for the melee hunter while attacking. */
    public static class HunterModel extends HumanoidModel<HunterEntity> {
        public HunterModel(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(HunterEntity e, float limbSwing, float limbAmount, float partialTick) {
            rightArmPose = e.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            leftArmPose = ArmPose.EMPTY;
            super.prepareMobModel(e, limbSwing, limbAmount, partialTick);
        }

        @Override
        public void setupAnim(HunterEntity e, float limbSwing, float limbAmount, float ageInTicks, float headYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbAmount, ageInTicks, headYaw, headPitch);
            if (e.isHoldingCrossbow()) {
                if (e.isChargingCrossbow()) AnimationUtils.animateCrossbowCharge(rightArm, leftArm, e, true);
                else if (e.isAggressive()) AnimationUtils.animateCrossbowHold(rightArm, leftArm, head, true);
            } else if (e.isAggressive()) {
                AnimationUtils.swingWeaponDown(rightArm, leftArm, e, attackTime, ageInTicks);
            }
        }
    }
}
