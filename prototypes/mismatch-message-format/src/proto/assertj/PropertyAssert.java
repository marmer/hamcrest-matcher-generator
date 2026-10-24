package proto.assertj;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractObjectAssert;

/**
 * PROTOTYPE. Stand-in for the generated AssertJ base. {@link #withPath} switches between
 * J1 (native wording, property name only) and J2 (native wording, full path from the root).
 */
public abstract class PropertyAssert<SELF extends PropertyAssert<SELF, A>, A> extends AbstractObjectAssert<SELF, A> {

    public static boolean withPath = false;

    /** J2: the path of the value currently asserted inside a hasFooSatisfying(..) lambda. */
    private static final ThreadLocal<Deque<String>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    private final String path;

    protected PropertyAssert(A actual, Class<?> selfType, String typeName) {
        super(actual, selfType);
        String outer = CONTEXT.get().peek();
        this.path = outer != null ? outer : typeName;
    }

    private void requireActual() {
        if (actual == null) {
            if (withPath) throw failure("%nExpecting " + path + " not to be null");
            isNotNull();
        }
    }

    protected SELF hasProperty(String name, Object actualValue, Object expected) {
        requireActual();
        if (!Objects.deepEquals(actualValue, expected)) {
            String a = info.representation().toStringOf(actualValue);
            String e = info.representation().toStringOf(expected);
            String msg = withPath
                    ? String.format("%nExpecting %s.%s to be:%n  %s%nbut was:%n  %s", path, name, e, a)
                    : String.format("%nExpecting %s of:%n  %s%nto be:%n  %s%nbut was:%n  %s",
                            name, info.representation().toStringOf(actual), e, a);
            throw failureWithActualExpected(actualValue, expected, "%s", msg);
        }
        return myself;
    }

    protected <V> SELF hasPropertySatisfying(String name, V value, Consumer<? super V> requirements) {
        requireActual();
        CONTEXT.get().push(path + "." + name);
        try {
            requirements.accept(value);
        } finally {
            CONTEXT.get().pop();
        }
        return myself;
    }
}
