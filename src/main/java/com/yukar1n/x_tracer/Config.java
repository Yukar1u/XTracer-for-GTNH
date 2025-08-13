package com.yukar1n.x_tracer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
    public static int maxRadius = 30;
    @SideOnly(Side.CLIENT)
    public static int radius = 10;
    @SideOnly(Side.CLIENT)
    public static String curMatchMode = "block";
    @SideOnly(Side.CLIENT)
    public static String targetBlockID = "minecraft:stone@0";
    @SideOnly(Side.CLIENT)
    public static List<String> targetList = new ArrayList<>();
    static {
        targetList.add(targetBlockID);
    }
    @SideOnly(Side.CLIENT)
    public static List<String> targetOreNameList = new ArrayList<>();



    // 设置目标方块 ID
    public static void setTargetBlockID(List<String> targetLst) {
        targetList = new ArrayList<>(targetLst);
    }

    //设置目标矿辞
    public static void setTargetOreName(List<String> OreNameList) {
        targetOreNameList = OreNameList;
    }

    // 获取目标方块 ID
    public static List<String> getTargetBlockIDs() {
        return Collections.unmodifiableList(targetList);
    }

    //获取目标矿词名
    public static List<String> getTargetOreNameList(){
        return Collections.unmodifiableList(targetOreNameList);
    }
}
