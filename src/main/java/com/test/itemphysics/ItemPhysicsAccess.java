package com.test.itemphysics;

import net.minecraft.world.phys.Vec3;
public interface ItemPhysicsAccess {

    void enableCustomPhysics();

    boolean isUsingCustomPhysics();
}