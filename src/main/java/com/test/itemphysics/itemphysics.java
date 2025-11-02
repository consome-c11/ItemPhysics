package com.test.itemphysics;

import net.minecraftforge.fml.common.Mod;

@Mod("itemphysics")
public class itemphysics {
    public static final String MODID = "itemphysics";

    public itemphysics() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

}