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
        java.util.Set<String> selected = new java.util.HashSet<>();
        check(SelectorLogic.toggleOreSelection(selected, "oreCopper", 64), "select canonical name");
        check(selected.contains("orecopper"), "selection uses normalized key");
        check(SelectorLogic.toggleOreSelection(selected, "ORECOPPER", 64), "deselect alternate case");
        check(selected.isEmpty(), "alternate case does not create duplicate");
        for (int i = 0; i < 64; i++) check(SelectorLogic.toggleOreSelection(selected, "ore" + i, 64), "select up to limit");
        check(!SelectorLogic.toggleOreSelection(selected, "oreOverflow", 64), "reject 65th target");
        check(selected.size() == 64 && !selected.contains("oreoverflow"), "rejection preserves selection");
        check(SelectorLogic.toggleOreSelection(selected, "ORE0", 64), "deselect at limit");
        check(SelectorLogic.toggleOreSelection(selected, "oreOverflow", 64), "replace deselected target");
        com.yukar1n.x_tracer.storage.ContainerReference legacy = new com.yukar1n.x_tracer.storage.ContainerReference(0, 1, 2, 3, "legacy");
        check(legacy.routeEnabled, "legacy bindings default to route enabled");
        check(!legacy.withRouteEnabled(false).routeEnabled, "binding route state can be disabled");
        java.util.List<com.yukar1n.x_tracer.storage.ContainerReference> refs = new java.util.ArrayList<>();
        refs.add(legacy);
        refs.add(legacy.withRouteEnabled(false));
        check(1 == refs.stream().filter(value -> value.routeEnabled).count(), "disabled bindings are filterable");
        System.out.println("SelectorLogic checks passed: ore case, selection limit, search paging, metadata/drop classification, binding route state");
    }
}
