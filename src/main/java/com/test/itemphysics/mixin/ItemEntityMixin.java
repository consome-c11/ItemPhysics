package com.test.itemphysics.mixin;

/*@Mixin(ItemEntity.class)
public class ItemEntityMixin implements ItemSpinAccess {
    @Unique private float clientSpin = 0f;
    @Unique private float clientSpinVel = 0f;

    @Override
    public float getClientSpin(float partialTicks) {
        return this.clientSpin + this.clientSpinVel * partialTicks;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickClientSpin(CallbackInfo ci) {
        ItemEntity self = (ItemEntity)(Object)this;
        if (!self.level().isClientSide()) return;

        boolean onGround = self.onGround();

        if (!onGround) {
            if (Math.abs(this.clientSpinVel) < 0.01f) {
                this.clientSpinVel = (float)(((self.getId() * 997) % 21) - 10) * 0.5f;
            }
            this.clientSpinVel *= 0.995f;
            this.clientSpin += this.clientSpinVel;
        } else {
            this.clientSpinVel *= 0.6f;
            this.clientSpin += this.clientSpinVel;
            float target = 90.0f;
            float diff = target - this.clientSpin;
            this.clientSpin += diff * 0.2f;
            if (Math.abs(this.clientSpinVel) < 0.01f && Math.abs(diff) < 0.5f) {
                this.clientSpinVel = 0f;
                this.clientSpin = target;
            }
        }

        if (this.clientSpin > 36000f || this.clientSpin < -36000f) this.clientSpin %= 360f;
    }
}*/
