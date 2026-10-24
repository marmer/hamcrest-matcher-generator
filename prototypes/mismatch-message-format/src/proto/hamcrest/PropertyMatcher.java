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

    protected void property(String name, Function<T, ?> getter) {
        properties.put(name, getter);
        VALUE_RENDERERS.computeIfAbsent(type, k -> new LinkedHashMap<>()).put(name, getter);
    }

    // ---- rendering actual values of generated types (they often have no toString) -------

    private static final Map<Class<?>, Map<String, Function<?, ?>>> VALUE_RENDERERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** {@code OrderLine {sku "B-2", quantity <2>}} for generated types, Hamcrest's value text otherwise. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static String describeValue(Object value) {
        Map<String, Function<?, ?>> getters = value == null ? null : VALUE_RENDERERS.get(value.getClass());
        if (getters == null) return StringDescription.toString(new ValueOnly(value));
        List<String> parts = new ArrayList<>();
        getters.forEach((name, g) -> parts.add(name + " " + describeValue(((Function) g).apply(value))));
        return value.getClass().getSimpleName() + " {" + String.join(", ", parts) + "}";
    }
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
            else if (m instanceof ItemsInOrder<?> items) items.collectChecks(prefix + name, out);
            else if (m instanceof ItemsInAnyOrder<?> items) items.collectChecks(prefix + name, out);
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
            } else if (m instanceof ItemsInOrder<?> items && value instanceof List<?> list) {
                items.collectFailures(list, prefix + name, failures, unaddressed);
            } else if (m instanceof ItemsInAnyOrder<?> items && value instanceof java.util.Collection<?> c) {
                items.collectFailures(c, prefix + name, failures);
            } else {
                String mismatch = foreignMismatch(m, value);
                if (mismatch != null) failures.add(new Failure(prefix + name, m, value, mismatch));
            }
        });
        if (checkingAllProperties) {
            properties.keySet().stream().filter(p -> !checks.containsKey(p)).forEach(p -> unaddressed.add(prefix + p));
        }
    }

    // ---- generated matchers inside foreign matchers (contains, hasItem, ...) -------------

    /** Depth of foreign (non-generated) matchers on the stack: > 0 means "render on one line". */
    private static final ThreadLocal<int[]> FOREIGN = ThreadLocal.withInitial(() -> new int[1]);

    public static boolean oneLineInsideForeignMatchers = true; // switch for the before/after comparison

    private static boolean insideForeign() { return oneLineInsideForeignMatchers && FOREIGN.get()[0] > 0; }

    /** Mismatch text of a foreign matcher, or null if it matches. */
    private static String foreignMismatch(Matcher<?> m, Object value) {
        FOREIGN.get()[0]++;
        try {
            if (m.matches(value)) return null;
            StringDescription mismatch = new StringDescription();
            m.describeMismatch(value, mismatch);
            return mismatch.toString();
        } finally {
            FOREIGN.get()[0]--;
        }
    }

    private static String foreignDescription(org.hamcrest.SelfDescribing m) {
        FOREIGN.get()[0]++;
        try {
            return StringDescription.toString(m);
        } finally {
            FOREIGN.get()[0]--;
        }
    }

    // ---- lists: item-by-item, indexed paths (candidate L3) --------------------------------

    /** Runtime-provided list matcher: one matcher per item, in order; failures get paths like {@code lines[1].quantity}. */
    public static <E> ItemsInOrder<E> itemsInOrder(List<? extends Matcher<? super E>> itemMatchers) {
        return new ItemsInOrder<>(itemMatchers);
    }

    @SafeVarargs
    public static <E> ItemsInOrder<E> itemsInOrder(Matcher<? super E>... itemMatchers) {
        return new ItemsInOrder<E>(java.util.Arrays.asList(itemMatchers));
    }

    public static final class ItemsInOrder<E> extends org.hamcrest.TypeSafeMatcher<List<?>> {
        private final List<? extends Matcher<? super E>> items;

        ItemsInOrder(List<? extends Matcher<? super E>> items) { this.items = items; }

        void collectChecks(String path, List<Check> out) {
            for (int i = 0; i < items.size(); i++) {
                Matcher<?> m = items.get(i);
                if (m instanceof PropertyMatcher<?, ?> nested) nested.collectChecks(path + "[" + i + "].", out);
                else out.add(new Check(path + "[" + i + "]", m));
            }
        }

        void collectFailures(List<?> list, String path, List<Failure> failures, List<String> unaddressed) {
            if (list.size() != items.size()) {
                failures.add(new Failure(path, new SizeIs(items.size()), list,
                        "was " + list.size() + " items"));
            }
            for (int i = 0; i < Math.min(list.size(), items.size()); i++) {
                Object value = list.get(i);
                Matcher<?> m = items.get(i);
                if (m instanceof PropertyMatcher<?, ?> nested && nested.type.isInstance(value)) {
                    nested.collectFailures(value, path + "[" + i + "].", failures, unaddressed);
                } else {
                    String mismatch = foreignMismatch(m, value);
                    if (mismatch != null) failures.add(new Failure(path + "[" + i + "]", m, value, mismatch));
                }
            }
        }

        @Override protected boolean matchesSafely(List<?> list) {
            List<Failure> f = new ArrayList<>();
            collectFailures(list, "", f, new ArrayList<>());
            return f.isEmpty();
        }

        @Override public void describeTo(Description d) { d.appendText(items.size() + " items in order"); }
    }

    // ---- unordered collections: match without index (candidate U2) --------------------------

    /** Runtime-provided matcher for unordered collections: every item matcher needs its own item, no extra items. */
    @SafeVarargs
    public static <E> ItemsInAnyOrder<E> itemsInAnyOrder(Matcher<? super E>... itemMatchers) {
        return new ItemsInAnyOrder<E>(java.util.Arrays.asList(itemMatchers));
    }

    public static <E> ItemsInAnyOrder<E> itemsInAnyOrder(List<? extends Matcher<? super E>> itemMatchers) {
        return new ItemsInAnyOrder<>(itemMatchers);
    }

    public static final class ItemsInAnyOrder<E> extends org.hamcrest.TypeSafeMatcher<java.util.Collection<?>> {
        private final List<? extends Matcher<? super E>> items;

        ItemsInAnyOrder(List<? extends Matcher<? super E>> items) { this.items = items; }

        void collectChecks(String path, List<Check> out) {
            for (Matcher<?> m : items) {
                if (m instanceof PropertyMatcher<?, ?> nested) nested.collectChecks(path + "[any].", out);
                else out.add(new Check(path + "[any]", m));
            }
        }

        void collectFailures(java.util.Collection<?> actual, String path, List<Failure> failures) {
            List<Object> unused = new ArrayList<>(actual);
            List<Matcher<?>> missing = new ArrayList<>();
            for (Matcher<?> m : items) {
                Object hit = null;
                for (Object a : unused) if (foreignMismatch(m, a) == null) { hit = a; break; }
                if (hit != null) unused.remove(hit); else missing.add(m);
            }
            for (Matcher<?> m : missing) {
                String text = "missing " + foreignDescription(m);
                Object closest = null;
                List<Failure> closestDiff = null;
                if (m instanceof PropertyMatcher<?, ?> pm) {
                    for (Object a : unused) {
                        if (!pm.type.isInstance(a)) continue;
                        List<Failure> diff = new ArrayList<>();
                        pm.collectFailures(a, "", diff, new ArrayList<>());
                        if (closestDiff == null || diff.size() < closestDiff.size()) { closest = a; closestDiff = diff; }
                    }
                }
                if (closest != null) {
                    unused.remove(closest);
                    List<String> parts = new ArrayList<>();
                    for (Failure f : closestDiff) parts.add(oneLine(f));
                    text += "; closest " + describeValue(closest) + " differs in " + String.join(", ", parts);
                }
                failures.add(new Failure(path, null, actual, text));
            }
            for (Object a : unused) failures.add(new Failure(path, null, actual, "unexpected " + describeValue(a)));
        }

        @Override protected boolean matchesSafely(java.util.Collection<?> c) {
            List<Failure> f = new ArrayList<>();
            collectFailures(c, "", f);
            return f.isEmpty();
        }

        @Override public void describeTo(Description d) { d.appendText(items.size() + " items in any order"); }
    }

    private static String oneLine(Failure f) {
        if (f.expected == null) return f.path + ": " + f.mismatch;
        return f.path + ": expected " + foreignDescription(compact(f.expected)) + " but " + f.mismatch;
    }

    private record SizeIs(int size) implements Matcher<Object> {
        @Override public boolean matches(Object o) { return false; }
        @Override public void describeMismatch(Object o, Description d) { }
        @Override public void describeTo(Description d) { d.appendText(size + " items"); }
        @SuppressWarnings("deprecation") @Override public void _dont_implement_Matcher___instead_extend_BaseMatcher_() { }
    }

    // ---- Hamcrest hooks ------------------------------------------------------------------

    @Override
    protected boolean matchesSafely(T item, Description mismatch) {
        List<Failure> failures = new ArrayList<>();
        List<String> unaddressed = new ArrayList<>();
        collectFailures(item, "", failures, unaddressed);
        if (failures.isEmpty() && unaddressed.isEmpty()) return true;

        if (insideForeign()) {
            List<String> parts = new ArrayList<>();
            for (Failure f : failures) {
                parts.add(format == Format.A_PATH_LIST ? f.path + ": " + f.mismatch : oneLine(f));
            }
            if (!unaddressed.isEmpty()) parts.add(unaddressedLine(unaddressed));
            mismatch.appendText(type.getSimpleName() + " {" + String.join(", ", parts) + "}");
            return false;
        }

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
                    mismatch.appendText(sep).appendText(oneLine(f));
                    sep = INDENT;
                }
                if (!unaddressed.isEmpty()) mismatch.appendText(sep).appendText(unaddressedLine(unaddressed));
            }
            case C_BLOCKS -> {
                List<Check> all = new ArrayList<>();
                collectChecks("", all);
                mismatch.appendText(failures.size() + " of " + all.size() + " property checks failed");
                for (Failure f : failures) {
                    if (f.expected == null) {
                        mismatch.appendText(INDENT + "  " + f.path).appendText(INDENT + "    " + f.mismatch);
                        continue;
                    }
                    mismatch.appendText(INDENT + "  " + f.path)
                            .appendText(INDENT + "    expected: ").appendText(foreignDescription(compact(f.expected)))
                            .appendText(INDENT + "    actual:   " + describeValue(f.actual));
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
        if (insideForeign()) {
            List<String> parts = new ArrayList<>();
            for (Check c : all) parts.add(c.path + " " + foreignDescription(c.matcher));
            d.appendText(type.getSimpleName() + " {" + String.join(", ", parts) + "}");
            return;
        }
        if (format == Format.A_PATH_LIST) {
            d.appendText(type.getSimpleName());
            for (Check c : all) d.appendText(INDENT + "  " + c.path + ": ").appendText(foreignDescription(c.matcher));
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
                    d.appendText(sep + c.path + " ").appendText(foreignDescription(c.matcher));
                    sep = ", ";
                }
            }
        };
    }

    private record ValueOnly(Object value) implements org.hamcrest.SelfDescribing {
        @Override public void describeTo(Description d) { d.appendValue(value); }
    }
}
