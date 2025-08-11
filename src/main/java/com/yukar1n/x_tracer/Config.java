package com.yukar1n.x_tracer;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
    // Custom
    public static String targetBlockID = "minecraft:stone";

    // 设置目标方块 ID
    public static void setTargetBlockID(String blockID) {
        targetBlockID = blockID;
    }

    // 获取目标方块 ID
    public static String getTargetBlockID() {
        return targetBlockID;
    }
}
