package com.test.itemphysics;

import com.test.itemphysics.ItemPhysicsAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("itemphysics")
public class itemphysics {
    public static final String MODID = "itemphysics";

    public itemphysics() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item) {
            //ItemPhysicsAccess access = (ItemPhysicsAccess) item;
            //if (!access.isUsingCustomPhysics()) {
                //access.enableCustomPhysics();
                //System.out.println("X: " + event.getEntity().getDeltaMovement().x + "Y: " + event.getEntity().getDeltaMovement().y + "Z: " +event.getEntity().getDeltaMovement().z);
            //}
        }
    }
}