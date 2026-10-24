package proto.hamcrest;

import static org.hamcrest.Matchers.anything;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * PROTOTYPE. Stand-in for the 6.0.0 Hamcrest runtime base class. Nested generated matchers are
 * flattened into property paths ({@code customer.address.city}); the static {@link #format}
 * switches between the three candidate message formats.
 */
public abstract class PropertyMatcher<T, SELF extends PropertyMatcher<T, SELF>>
        extends TypeSafeDiagnosingMatcher<T> {

    public enum Format { A_PATH_LIST, B_INLINE, C_BLOCKS }

    public static Format format = Format.B_INLINE;

    private static final Matcher<Object> IGNORED = anything("ignored");
    private static final String INDENT = "\n          "; // aligns with Hamcrest's "     but: "

    private final Class<T> type;
    private final Map<String, Function<T, ?>> properties = new LinkedHashMap<>();
    private final Map<String, Matcher<?>> checks = new LinkedHashMap<>();
    private boolean checkingAllProperties;

    protected PropertyMatcher(Class<T> type) {
        super(type);
        this.type = type;
    }

    protected void property(String name, Function<T, ?> getter) { properties.put(name, getter); }
    protected SELF check(String name, Matcher<?> m) { checks.put(name, m); return self(); }
    protected SELF ignore(String name) { checks.put(name, IGNORED); return self(); }
    protected SELF reset(String name) { checks.remove(name); return self(); }

    public SELF checkingAllProperties() {
        checkingAllProperties = true;
        return self();
    }

    @SuppressWarnings("unchecked")
    private SELF self() { return (SELF) this; }

    // ---- model: flattened checks and failures -------------------------------------------

    private record Check(String path, Matcher<?> matcher) {}

    private record Failure(String path, Matcher<?> expected, Object actual, String mismatch) {}

    private void collectChecks(String prefix, List<Check> out) {
        checks.forEach((name, m) -> {
            if (m == IGNORED) return;
            if (m instanceof PropertyMatcher<?, ?> nested) nested.collectChecks(prefix + name + ".", out);
            else out.add(new Check(prefix + name, m));
        });
    }

    private void collectFailures(Object item, String prefix, List<Failure> failures, List<String> unaddressed) {
        T t = type.cast(item);
        checks.forEach((name, m) -> {
            if (m == IGNORED) return;
            Object value = properties.get(name).apply(t);
            if (m instanceof PropertyMatcher<?, ?> nested && nested.type.isInstance(value)) {
                nested.collectFailures(value, prefix + name + ".", failures, unaddressed);
            } else if (!m.matches(value)) {
                StringDescription mismatch = new StringDescription();
                m.describeMismatch(value, mismatch);
                failures.add(new Failure(prefix + name, m, value, mismatch.toString()));
            }
        });
        if (checkingAllProperties) {
            properties.keySet().stream().filter(p -> !checks.containsKey(p)).forEach(p -> unaddressed.add(prefix + p));
        }
    }

    // ---- Hamcrest hooks ------------------------------------------------------------------

    @Override
    protected boolean matchesSafely(T item, Description mismatch) {
        List<Failure> failures = new ArrayList<>();
        List<String> unaddressed = new ArrayList<>();
        collectFailures(item, "", failures, unaddressed);
        if (failures.isEmpty() && unaddressed.isEmpty()) return true;

        switch (format) {
            case A_PATH_LIST -> {
                String sep = "";
                for (Failure f : failures) {
                    mismatch.appendText(sep).appendText(f.path + ": " + f.mismatch);
                    sep = INDENT;
                }
                if (!unaddressed.isEmpty()) mismatch.appendText(sep).appendText(unaddressedLine(unaddressed));
            }
            case B_INLINE -> {
                String sep = "";
                for (Failure f : failures) {
                    mismatch.appendText(sep).appendText(f.path + ": expected ")
                            .appendDescriptionOf(compact(f.expected)).appendText(" but " + f.mismatch);
                    sep = INDENT;
                }
                if (!unaddressed.isEmpty()) mismatch.appendText(sep).appendText(unaddressedLine(unaddressed));
            }
            case C_BLOCKS -> {
                List<Check> all = new ArrayList<>();
                collectChecks("", all);
                mismatch.appendText(failures.size() + " of " + all.size() + " property checks failed");
                for (Failure f : failures) {
                    mismatch.appendText(INDENT + "  " + f.path)
                            .appendText(INDENT + "    expected: ").appendDescriptionOf(compact(f.expected))
                            .appendText(INDENT + "    actual:   ").appendValue(f.actual);
                    String plain = "was " + StringDescription.toString(new ValueOnly(f.actual));
                    if (!f.mismatch.equals(plain)) mismatch.appendText(INDENT + "    because:  " + f.mismatch);
                }
                if (!unaddressed.isEmpty()) mismatch.appendText(INDENT + unaddressedLine(unaddressed));
            }
        }
        return false;
    }

    private static String unaddressedLine(List<String> unaddressed) {
        return "neither checked nor ignored: " + String.join(", ", unaddressed);
    }

    @Override
    public void describeTo(Description d) {
        List<Check> all = new ArrayList<>();
        collectChecks("", all);
        if (format == Format.A_PATH_LIST) {
            d.appendText(type.getSimpleName());
            for (Check c : all) d.appendText(INDENT + "  " + c.path + ": ").appendDescriptionOf(c.matcher);
            if (checkingAllProperties) d.appendText(INDENT + "  with every property checked or ignored");
        } else {
            d.appendText(type.getSimpleName() + " with " + all.size() + " property checks");
            if (checkingAllProperties) d.appendText(", every property checked or ignored");
        }
    }

    /** A nested generated matcher used as a leaf (actual value null): one line, not a block. */
    private static Matcher<?> compact(Matcher<?> m) {
        if (!(m instanceof PropertyMatcher<?, ?> pm)) return m;
        List<Check> all = new ArrayList<>();
        pm.collectChecks("", all);
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override public boolean matches(Object o) { return false; }
            @Override public void describeTo(Description d) {
                d.appendText("a " + pm.type.getSimpleName() + " with ");
                String sep = "";
                for (Check c : all) {
                    d.appendText(sep + c.path + " ").appendDescriptionOf(c.matcher);
                    sep = ", ";
                }
            }
        };
    }

    private record ValueOnly(Object value) implements org.hamcrest.SelfDescribing {
        @Override public void describeTo(Description d) { d.appendValue(value); }
    }
}
