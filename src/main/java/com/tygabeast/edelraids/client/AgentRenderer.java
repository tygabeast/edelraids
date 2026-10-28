package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.entity.ArcAileAgentEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Arc Aile agents share the hunters' humanoid model layer. His partner renders in his hand through the normal item
 * layer, so she uses the same weapon renderer (and gem tint) as in a Pledger's hand.
 */
public class AgentRenderer extends HumanoidMobRenderer<ArcAileAgentEntity, AgentRenderer.AgentModel> {
    private static final ResourceLocation TEXTURE = EdelRaids.id("textures/entity/arc_aile_agent.png");

    public AgentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AgentModel(ctx.bakeLayer(HunterRenderer.LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcAileAgentEntity entity) {
        return TEXTURE;
    }

    /** Fighting: a weapon swing. Yielded: down on one knee, head bowed, arms hanging. */
    public static class AgentModel extends HumanoidModel<ArcAileAgentEntity> {
        public AgentModel(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(ArcAileAgentEntity e, float limbSwing, float limbAmount, float partialTick) {
            rightArmPose = e.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            leftArmPose = ArmPose.EMPTY;
            crouching = e.isYielded();
            super.prepareMobModel(e, limbSwing, limbAmount, partialTick);
        }

        @Override
        public void setupAnim(ArcAileAgentEntity e, float limbSwing, float limbAmount, float ageInTicks, float headYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbAmount, ageInTicks, headYaw, headPitch);
            if (e.isYielded()) {
                head.xRot = 0.7f;
                hat.copyFrom(head);
                rightLeg.xRot = -1.2f;
                leftLeg.xRot = 0.3f;
                rightArm.xRot = 0.35f;
                leftArm.xRot = 0.35f;
            } else if (e.isAggressive()) {
                AnimationUtils.swingWeaponDown(rightArm, leftArm, e, attackTime, ageInTicks);
            }
        }
    }
}
