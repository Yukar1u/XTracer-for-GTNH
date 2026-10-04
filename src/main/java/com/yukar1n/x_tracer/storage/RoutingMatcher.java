package com.yukar1n.x_tracer.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RoutingMatcher {
    private RoutingMatcher() {}

    public static boolean matches(ItemKey key, List<String> accepts, List<String> excludes) {
        if (key == null) return false;
        String text = key.matchText();
        boolean accepted = accepts == null || accepts.isEmpty();
        if (!accepted) {
            for (String value : accepts) {
                if (value != null && !value.trim().isEmpty() && text.contains(value.trim().toLowerCase())) {
                    accepted = true;
                    break;
                }
            }
        }
        if (!accepted) return false;
        if (excludes != null) {
            for (String value : excludes) {
                if (value != null && !value.trim().isEmpty() && text.contains(value.trim().toLowerCase())) return false;
            }
        }
        return true;
    }

    public static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            RoutingExpression expression = new RoutingExpression(value);
            if (!expression.isEmpty() && !result.contains(expression.value())) result.add(expression.value());
        }
        return Collections.unmodifiableList(result);
    }
}
