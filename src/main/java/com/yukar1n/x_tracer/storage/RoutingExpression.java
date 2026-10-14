package com.yukar1n.x_tracer.storage;

import java.util.Locale;

public final class RoutingExpression {
    public static final int MAX_LENGTH = 64;
    private final String value;

    public RoutingExpression(String value) {
        String normalized = trimBoundarySpaces(value == null ? "" : value);
        if (normalized.length() > MAX_LENGTH) normalized = trimBoundarySpaces(normalized.substring(0, MAX_LENGTH));
        this.value = normalized;
    }

    private static String trimBoundarySpaces(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) break;
            start += Character.charCount(codePoint);
        }
        while (end > start) {
            int codePoint = value.codePointBefore(end);
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) break;
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
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
