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
    }

    // 判断是否仍在渲染期
    public static boolean shouldRender() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdateTime) <= RENDER_DURATION;
    }

    // 获取当前有效的方块坐标
    public static List<int[]> getHighlightBlocks() {
        if (!shouldRender()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(highlightBlocks); // 返回副本
    }
}
