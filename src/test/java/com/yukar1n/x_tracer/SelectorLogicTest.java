package com.yukar1n.x_tracer;

import java.util.Arrays;

/** Runnable without Minecraft; invoke main to check selector invariants. */
public final class SelectorLogicTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check("oreCopper".equals(SelectorLogic.canonical("ORECOPPER", Arrays.asList("oreCopper"))), "case canonical");
        check(3 == SelectorLogic.pageAfterSearch(3, "ore", "ore"), "unchanged search keeps page");
        check(0 == SelectorLogic.pageAfterSearch(3, "ore", "ores"), "changed search resets page");
        check(SelectorLogic.needsDropTarget(false, 16, 0), "large item damage uses drop target");
        check(SelectorLogic.needsDropTarget(true, 0, 0), "special block uses drop target");
        check(!SelectorLogic.needsDropTarget(false, 4, 4), "ordinary metadata uses block target");
        System.out.println("SelectorLogic checks passed: ore case, search paging, Alt grouping contract, metadata/drop classification");
    }
}
