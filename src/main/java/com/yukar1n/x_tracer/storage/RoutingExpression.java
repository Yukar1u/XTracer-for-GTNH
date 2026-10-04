package com.yukar1n.x_tracer.storage;

import java.util.Locale;

public final class RoutingExpression {
    public static final int MAX_LENGTH = 64;
    private final String value;

    public RoutingExpression(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > MAX_LENGTH) normalized = normalized.substring(0, MAX_LENGTH);
        this.value = normalized;
    }

    public String value() { return value; }
    public String matchKey() { return value.toLowerCase(Locale.ROOT); }
    public boolean isEmpty() { return value.isEmpty(); }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoutingExpression && matchKey().equals(((RoutingExpression) other).matchKey());
    }

    @Override
    public int hashCode() { return matchKey().hashCode(); }
}
