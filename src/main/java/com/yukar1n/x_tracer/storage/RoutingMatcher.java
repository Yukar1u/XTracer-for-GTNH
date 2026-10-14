package com.yukar1n.x_tracer.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RoutingMatcher {
    private RoutingMatcher() {}

    public enum Reason { ACCEPTED, NO_ACCEPT_MATCH, EXCLUDED }

    public static final class Decision {
        public final boolean accepted;
        public final Reason reason;
        public final String rule;
        public final List<String> accepts;
        public final List<String> excludes;
        public final String text;

        private Decision(Reason reason, String rule, List<String> accepts, List<String> excludes, String text) {
            this.accepted = reason == Reason.ACCEPTED;
            this.reason = reason;
            this.rule = rule;
            this.accepts = accepts;
            this.excludes = excludes;
            this.text = text;
        }
    }

    public static boolean matches(ItemKey key, List<String> accepts, List<String> excludes) {
        return evaluate(key, accepts, excludes).accepted;
    }

    public static Decision evaluate(ItemKey key, List<String> accepts, List<String> excludes) {
        return evaluate(key, accepts, excludes, key == null ? null : key.displayText());
    }

    public static Decision evaluate(ItemKey key, List<String> accepts, List<String> excludes, String localizedDisplayName) {
        List<String> normalizedAccepts = normalize(accepts);
        List<String> normalizedExcludes = normalize(excludes);
        if (key == null) {
            return new Decision(Reason.NO_ACCEPT_MATCH, String.join(", ", normalizedAccepts),
                    normalizedAccepts, normalizedExcludes, "");
        }
        if (normalizedAccepts.isEmpty() && normalizedExcludes.isEmpty()) {
            String text = (key.itemId() + " " + key.itemId() + "@" + key.damage()).toLowerCase(Locale.ROOT);
            return new Decision(Reason.ACCEPTED, "", normalizedAccepts, normalizedExcludes, text);
        }
        String text = key.matchText(localizedDisplayName);
        for (String value : normalizedExcludes) {
            if (text.contains(value.toLowerCase(Locale.ROOT))) {
                return new Decision(Reason.EXCLUDED, value, normalizedAccepts, normalizedExcludes, text);
            }
        }
        for (String value : normalizedAccepts) {
            if (text.contains(value.toLowerCase(Locale.ROOT))) {
                return new Decision(Reason.ACCEPTED, value, normalizedAccepts, normalizedExcludes, text);
            }
        }
        if (normalizedAccepts.isEmpty()) {
            return new Decision(Reason.ACCEPTED, "", normalizedAccepts, normalizedExcludes, text);
        }
        return new Decision(Reason.NO_ACCEPT_MATCH, String.join(", ", normalizedAccepts),
                normalizedAccepts, normalizedExcludes, text);
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
