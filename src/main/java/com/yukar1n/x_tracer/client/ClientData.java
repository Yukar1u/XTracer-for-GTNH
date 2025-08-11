package com.yukar1n.x_tracer.client;

import java.util.ArrayList;
import java.util.List;

public class ClientData {

    private static List<int[]> highlightBlocks = new ArrayList<>();

    public static void setHighlightBlocks(List<int[]> blocks) {
        synchronized (highlightBlocks) {
            highlightBlocks.clear();
            highlightBlocks.addAll(blocks);
        }
    }

    public static List<int[]> getHighlightBlocks() {
        synchronized (highlightBlocks) {
            return new ArrayList<>(highlightBlocks);
        }
    }
}

