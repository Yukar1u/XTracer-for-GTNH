package com.yukar1n.x_tracer;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ClientConfig {
    public int maxRadius = 30;
    public int radius = 10;

    // block/ore/gtName
    public String curMatchMode = "block";

    public String targetBlockID = "minecraft:stone@0";
    public List<String> targetList = new ArrayList<>();
    public List<String> targetOreNameList = new ArrayList<>();
    public String targetGtName = "";

    public ClientConfig() {
        targetList.add(targetBlockID);
    }

    // 设置目标方块 ID
    public void setTargetBlockID(List<String> targetLst) {
        targetList = new ArrayList<>(targetLst);
    }

    // 设置目标矿辞
    public void setTargetOreName(List<String> OreNameList) {
        targetOreNameList = OreNameList;
    }

    // 获取目标方块 ID
    public List<String> getTargetBlockIDs() {
        return Collections.unmodifiableList(targetList);
    }

    // 获取目标矿词名
    public List<String> getTargetOreNameList() {
        return Collections.unmodifiableList(targetOreNameList);
    }

    public String getTargetGtName() {
        return targetGtName;
    }
}
