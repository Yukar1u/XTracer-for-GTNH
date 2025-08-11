package com.yukar1n.x_tracer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientData {
    private static final long RENDER_DURATION = 5000; // 持续渲染5秒
    private static List<int[]> highlightBlocks = new ArrayList<>();
    private static long lastUpdateTime = 0;

    // 接收新坐标时调用
    public static void setHighlightBlocks(List<int[]> blocks) {
        highlightBlocks = new ArrayList<>(blocks); // 复制列表避免引用问题
        lastUpdateTime = System.currentTimeMillis(); // 更新时间戳
        renderTimer();
    }
    public static void renderTimer(){
        long currentTime = System.currentTimeMillis();
        while (currentTime - lastUpdateTime <= RENDER_DURATION){
            currentTime = System.currentTimeMillis();
            ClientRender.render();
        }
        System.out.println("x_trace: Render timer Over");
        //超时清空坐标信息
        highlightBlocks = Collections.emptyList();
    }

    // 获取当前有效的方块坐标
    public static List<int[]> getHighlightBlocks() {
        return new ArrayList<>(highlightBlocks); // 返回副本
    }
}

