package com.yukar1n.x_tracer;

import java.util.Locale;

/** Pure selector rules that can be checked without starting Minecraft. */
public final class SelectorLogic {
    private SelectorLogic() {}

    public static String matchKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static String canonical(String value, Iterable<String> registered) {
        if (value == null) return null;
        String trimmed = value.trim();
        String key = matchKey(trimmed);
        String fallback = null;
        for (String name : registered) {
            if (name.equals(trimmed)) return name;
            if (fallback == null && matchKey(name).equals(key)) fallback = name;
        }
        return fallback;
    }

    public static int pageAfterSearch(int page, String before, String after) {
        return before.equals(after) ? page : 0;
    }

    public static boolean toggleOreSelection(java.util.Set<String> selected, String name, int limit) {
        String key = matchKey(name);
        if (selected.remove(key)) return true;
        if (selected.size() >= limit) return false;
        selected.add(key);
        return true;
    }

    public static boolean needsDropTarget(boolean special, int itemDamage, int worldMetadata) {
        return special || itemDamage > 15 || worldMetadata < 0 || worldMetadata > 15;
    }
}
