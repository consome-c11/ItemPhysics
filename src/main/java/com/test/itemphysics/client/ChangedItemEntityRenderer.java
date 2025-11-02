package com.test.itemphysics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.WeakHashMap;

public class ChangedItemEntityRenderer extends net.minecraft.client.renderer.entity.ItemEntityRenderer {
    private final ItemRenderer itemRenderer;
    private final RandomSource random;

    private static final Map<ItemEntity, SpinData> SPIN_MAP = new WeakHashMap<>();
    private static final float AIR_DRAG = 0.995f;
    private static final float GROUND_DAMP = 0.6f;
    private static final float GROUND_TARGET = 90.0f;
    private static final float SNAP_RATE = 0.2f;
    private static final int CLEAN_THRESHOLD = 2000;

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
                    if (is3d) {
                        poseStack.translate(0.0, -0.05, 0.0);
                    } else {
                        float spinDeg = spin.getInterpolated(partialTicks);
                        float pitchDeg = spin.getPitchInterpolated(partialTicks);

                        if (onGround) {

                            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitchDeg));
                            poseStack.translate(0.0, -0.15, 0.0);
                        } else {
                            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spinDeg));
                            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitchDeg));
                        }

                        try {
                            if (entity.isInWater()) {

                            } else {
                                double interpY = net.minecraft.util.Mth.lerp(partialTicks, entity.yOld, entity.getY());
                                Level lvl = entity.level();
                                if (lvl != null) {
                                    BlockPos posBelow = BlockPos.containing(entity.getX(), interpY - 0.5, entity.getZ());
                                    var state = lvl.getBlockState(posBelow);
                                    if (!state.isAir()) {
                                        VoxelShape shape = state.getShape(lvl, posBelow);
                                        double topLocal = shape.max(net.minecraft.core.Direction.Axis.Y);
                                        if (topLocal > 1e-3) {
                                            double blockTopY = posBelow.getY() + topLocal;
                                            double desiredY = blockTopY + 0.001;
                                            double dy = desiredY - interpY;
                                            if (Math.abs(dy) > 1e-6) {
                                                poseStack.translate(0.0, dy, 0.0);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }

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
        float spin = 0f;
        float spinVel = 0f;

        float pitch = 0f;
        float pitchVel = 0f;

        boolean prevOnGround = false;
        private int seedRand = 0;

        private static final float AIR_PITCH_TARGET = 15.0f;
        private static final float GROUND_PITCH_TARGET = 90.0f;

        private static final float AIR_DRAG = 0.995f;
        private static final float GROUND_DAMP = 0.6f;

        private static final float LEAP_BASE = 3.0f;
        private static final float LEAP_SCALE = 0.35f;
        private static final float LEAP_DECAY = 0.9f;

        private static final float GROUND_STIFFNESS = 0.05f;
        private static final float GROUND_DAMPING = 0.65f;
        private static final float AIR_STIFFNESS = 0.02f;
        private static final float AIR_DAMPING = 0.25f;

        void initSeed(int seed, int id) {
            this.seedRand = seed ^ id * 31;
            long v = (seedRand * 6364136223846793005L) >>> 48;
            this.spinVel = (float)((v % 11) - 5) * 0.6f;
            this.pitch = 0f;
            this.pitchVel = 0f;
            this.prevOnGround = false;
        }

        void update(boolean onGround) {
            if (!onGround) {
                if (Math.abs(this.spinVel) < 0.01f) {
                    this.spinVel = (float)((this.seedRand % 11) - 5) * 0.6f;
                }
                this.spinVel *= AIR_DRAG;
                this.spin += this.spinVel;
            } else {
                this.spinVel *= GROUND_DAMP;
                this.spin += this.spinVel;
            }

            float target = onGround ? GROUND_PITCH_TARGET : AIR_PITCH_TARGET;

            if (!this.prevOnGround && onGround) {
                float sign = Math.signum(this.spinVel);
                float impulse = LEAP_BASE + Math.abs(this.spinVel) * LEAP_SCALE;
                this.pitchVel += sign * impulse;
                this.pitchVel *= LEAP_DECAY;
            }

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

