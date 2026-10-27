package io.github.marmer.matchergenerator.hamcrest;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.hamcrest.StringDescription;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link StringDescription} printing values of generated types from their properties, never via
 * {@code toString()}: {@code OrderLine {sku "B-2", quantity <2>}}.
 */
@NullMarked
final class PropertyDescription extends StringDescription {
    private static final int MAX_DEPTH = 5;

    private int depth;

    static String describe(final SelfDescribing selfDescribing) {
        final PropertyDescription description = new PropertyDescription();
        selfDescribing.describeTo(description);
        return description.toString();
    }

    static String describeValue(final @Nullable Object value) {
        return new PropertyDescription().appendValue(value).toString();
    }

    private static boolean isGeneratedType(final @Nullable Object value) {
        return value != null && PropertyAccess.propertiesOfGeneratedType(value.getClass()) != null;
    }

    private static boolean containsGeneratedType(final @Nullable Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream().anyMatch(PropertyDescription::containsGeneratedType);
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).entrySet().stream().anyMatch(PropertyDescription::containsGeneratedType);
        }
        if (value instanceof Map.Entry<?, ?>) {
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
            return containsGeneratedType(entry.getKey()) || containsGeneratedType(entry.getValue());
        }
        return isGeneratedType(value);
    }

    @Override
    public Description appendValue(final @Nullable Object value) {
        if (value == null || depth >= MAX_DEPTH) {
            return super.appendValue(value);
        }
        depth++;
        try {
            final List<String> properties = PropertyAccess.propertiesOfGeneratedType(value.getClass());
            if (properties != null) {
                appendProperties(value, properties);
            } else if (value instanceof Collection<?> && containsGeneratedType(value)) {
                appendAll("[", ((Collection<?>) value).iterator(), "]");
            } else if (value instanceof Map<?, ?> && containsGeneratedType(value)) {
                appendAll("{", ((Map<?, ?>) value).entrySet().iterator(), "}");
            } else if (value instanceof Map.Entry<?, ?> && containsGeneratedType(value)) {
                appendEntry((Map.Entry<?, ?>) value);
            } else {
                super.appendValue(value);
            }
            return this;
        } finally {
            depth--;
        }
    }

    private void appendAll(final String start, final Iterator<?> values, final String end) {
        append(start);
        while (values.hasNext()) {
            final @Nullable Object value = values.next();
            if (value instanceof Map.Entry<?, ?>) {
                appendEntryParts((Map.Entry<?, ?>) value);
            } else {
                appendValue(value);
            }
            if (values.hasNext()) {
                append(", ");
            }
        }
        append(end);
    }

    private void appendEntry(final Map.Entry<?, ?> entry) {
        append("<");
        appendEntryParts(entry);
        append(">");
    }

    private void appendEntryParts(final Map.Entry<?, ?> entry) {
        appendPlainOrGenerated(entry.getKey());
        append("=");
        appendPlainOrGenerated(entry.getValue());
    }

    private void appendPlainOrGenerated(final @Nullable Object value) {
        if (containsGeneratedType(value)) {
            appendValue(value);
        } else {
            append(String.valueOf(value));
        }
    }

    private void appendProperties(final Object value, final List<String> properties) {
        append(value.getClass().getSimpleName());
        append(" {");
        var first = true;
        for (final String property : properties) {
            final @Nullable Object propertyValue = PropertyAccess.read(value, property);
            if (propertyValue == PropertyAccess.NO_SUCH_PROPERTY) {
                continue;
            }
            if (!first) {
                append(", ");
            }
            first = false;
            append(property);
            append(" ");
            appendValue(propertyValue);
        }
        append("}");
    }
}
