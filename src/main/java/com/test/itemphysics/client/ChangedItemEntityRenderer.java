package com.test.itemphysics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public class ChangedItemEntityRenderer extends net.minecraft.client.renderer.entity.ItemEntityRenderer {
    private static final Map<ItemEntity, SpinData> SPIN_MAP = new WeakHashMap<>();
    private final ItemRenderer itemRenderer;
    RandomSource random;

    public ChangedItemEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
        this.random = RandomSource.create();
    }

    @Override
    public void render(ItemEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        SpinData spin = SPIN_MAP.get(entity);
        if (spin == null) {
            spin = new SpinData();
            ItemStack s0 = entity.getItem();
            int seed0 = s0.isEmpty() ? 187 : net.minecraft.world.item.Item.getId(s0.getItem()) + s0.getDamageValue();
            spin.initSeed(seed0, entity.getId());
            SPIN_MAP.put(entity, spin);
        }

        boolean onGround = entity.onGround();
        spin.update(onGround);

        poseStack.pushPose();
        try {
            ItemStack stack = entity.getItem();
            int seed = stack.isEmpty() ? 187 : net.minecraft.world.item.Item.getId(stack.getItem()) + stack.getDamageValue();
            this.random.setSeed(seed);

            BakedModel model = this.itemRenderer.getModel(stack, entity.level(), null, entity.getId());
            boolean is3d = model.isGui3d();
            int amount = this.getRenderAmount(stack);

            for (int k = 0; k < amount; ++k) {
                poseStack.pushPose();
                try {
                    long instSeed = (long)seed * 31L + k * 0x9E3779B97F4A7C15L;
                    RandomSource instRand = RandomSource.create(instSeed);

                    float spread = Math.max(0.32f, 0.32f + (amount - 1) * 0.02f);

                    float angle = (float)(instRand.nextDouble() * Math.PI * 2.0);
                    float radius = (float)(instRand.nextDouble() * spread);
                    float dx = (float) (Math.cos(angle) * radius);
                    float dz = (float) (Math.sin(angle) * radius);

                    float extraSpin = (float)(instRand.nextDouble() * 12.0 - 6.0);
                    float extraPitch = (float)(instRand.nextDouble() * 6.0 - 3.0);
                    float instanceScale = 1.0f - (float)(instRand.nextDouble() * 0.06);

                    poseStack.translate(dx, 0, dz);

                    float spinDeg = spin.getInterpolated(partialTicks) + extraSpin;
                    float pitchDeg = spin.getPitchInterpolated(partialTicks) + extraPitch;

                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spinDeg));
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitchDeg));
                    poseStack.translate(0.0, -0.15, is3d ? -0.15 : 0.0);

                    if (Math.abs(instanceScale - 1.0f) > 1e-6) {
                        poseStack.scale(instanceScale, instanceScale, instanceScale);
                    }

                    try {
                        if (!entity.isInWater()) {
                            //TODO: add inWater rotate?
                        }
                    } catch (Throwable ignored) {}

                    this.itemRenderer.render(
                            stack,
                            ItemDisplayContext.GROUND,
                            false,
                            poseStack,
                            buffer,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            model
                    );
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            poseStack.popPose();
        }

    }

    @Override
    public ResourceLocation getTextureLocation(ItemEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static class SpinData {
        private static final float AIR_PITCH_TARGET = 70.0f;
        private static final float GROUND_PITCH_TARGET = 90.0f;
        private static final float AIR_DRAG = 0.995f;
        private static final float GROUND_DAMP = 0.6f;
        private static final float GROUND_STIFFNESS = 0.03f;
        private static final float GROUND_DAMPING = 0.65f;
        private static final float AIR_STIFFNESS = 0.01f;
        private static final float AIR_DAMPING = 0.25f;
        float spin = 0f;
        float spinVel = 0f;
        float pitch = 0f;
        float pitchVel = 0f;
        boolean prevOnGround = false;
        private int seedRand = 0;
        private int age = 0;
        void initSeed(int seed, int id) {
            this.seedRand = seed ^ id * 31;
            long v = (seedRand * 6364136223846793005L) >>> 48;

            this.spinVel = (float) ((v % 11) - 5) * 0.6f;

            RandomSource rand = RandomSource.create(seedRand);
            this.spin = rand.nextFloat() * 360f;

            this.pitchVel = 0f;
            this.prevOnGround = false;
        }
        void update(boolean onGround) {
            if (age == 0) {
                this.spinVel = (float) ((this.seedRand % 11) - 5) * 0.35f;
            }

            if (onGround) {
                this.spinVel *= GROUND_DAMP;
            } else {
                this.spinVel *= AIR_DRAG;
            }

            this.spin += this.spinVel;
            age++;

            float target = onGround ? GROUND_PITCH_TARGET : AIR_PITCH_TARGET;
            float stiffness = onGround ? GROUND_STIFFNESS : AIR_STIFFNESS;
            float damping = onGround ? GROUND_DAMPING : AIR_DAMPING;

            float force = (target - this.pitch) * stiffness;
            this.pitchVel = this.pitchVel * (1.0f - damping) + force;
            this.pitch += this.pitchVel;

            if (this.spin > 36000f || this.spin < -36000f) this.spin %= 360f;
            if (this.pitch > 36000f || this.pitch < -36000f) this.pitch %= 360f;

            this.prevOnGround = onGround;
        }

        float getInterpolated(float partialTicks) {
            return this.spin + this.spinVel * partialTicks;
        }

        float getPitchInterpolated(float partialTicks) {
            return this.pitch + this.pitchVel * partialTicks;
        }
    }
}

