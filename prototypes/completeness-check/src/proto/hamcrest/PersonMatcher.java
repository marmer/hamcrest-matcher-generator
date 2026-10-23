package proto.hamcrest;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import proto.Person;

/**
 * PROTOTYPE. Candidate H1: runtime, opt-in completeness check (Hamcrest backend).
 * Hand-written stand-in for what the generator would emit for {@link Person}.
 */
public class PersonMatcher extends TypeSafeDiagnosingMatcher<Person> {

    // Generated: the property list, in declaration order.
    private static final List<String> PROPERTIES = List.of("name", "age", "email", "phone");

    private final Map<String, Matcher<?>> checks = new LinkedHashMap<>();
    private final Map<String, Function<Person, ?>> getters = Map.of(
            "name", Person::getName, "age", Person::getAge,
            "email", Person::getEmail, "phone", Person::getPhone);
    private boolean checkingAllProperties;

    public static PersonMatcher isPerson() {
        return new PersonMatcher();
    }

    /** Reference-object matcher: complete by construction, every property preset. */
    public static PersonMatcher isPersonLike(Person ref) {
        return isPerson().checkingAllProperties()
                .withName(ref.getName()).withAge(ref.getAge())
                .withEmail(ref.getEmail()).withPhone(ref.getPhone());
    }

    /** Opt-in: every property must be checked (withFoo) or ignored (ignoringFoo). */
    public PersonMatcher checkingAllProperties() {
        checkingAllProperties = true;
        return this;
    }

    public PersonMatcher withName(String v) { return withName(equalTo(v)); }
    public PersonMatcher withName(Matcher<? super String> m) { checks.put("name", m); return this; }
    public PersonMatcher ignoringName() { checks.put("name", IGNORED); return this; }
    public PersonMatcher resetName() { checks.remove("name"); return this; }

    public PersonMatcher withAge(int v) { return withAge(equalTo(v)); }
    public PersonMatcher withAge(Matcher<? super Integer> m) { checks.put("age", m); return this; }
    public PersonMatcher ignoringAge() { checks.put("age", IGNORED); return this; }
    public PersonMatcher resetAge() { checks.remove("age"); return this; }

    public PersonMatcher withEmail(String v) { return withEmail(equalTo(v)); }
    public PersonMatcher withEmail(Matcher<? super String> m) { checks.put("email", m); return this; }
    public PersonMatcher ignoringEmail() { checks.put("email", IGNORED); return this; }
    public PersonMatcher resetEmail() { checks.remove("email"); return this; }

    public PersonMatcher withPhone(String v) { return withPhone(equalTo(v)); }
    public PersonMatcher withPhone(Matcher<? super String> m) { checks.put("phone", m); return this; }
    public PersonMatcher ignoringPhone() { checks.put("phone", IGNORED); return this; }
    public PersonMatcher resetPhone() { checks.remove("phone"); return this; }

    private static final Matcher<Object> IGNORED = anything("ignored");

    private List<String> unaddressed() {
        List<String> result = new ArrayList<>();
        for (String p : PROPERTIES) if (!checks.containsKey(p)) result.add(p);
        return result;
    }

    @Override
    protected boolean matchesSafely(Person item, Description mismatch) {
        boolean ok = true;
        for (Map.Entry<String, Matcher<?>> e : checks.entrySet()) {
            Object value = getters.get(e.getKey()).apply(item);
            if (!e.getValue().matches(value)) {
                mismatch.appendText("\n     ").appendText(e.getKey()).appendText(": ");
                e.getValue().describeMismatch(value, mismatch);
                ok = false;
            }
        }
        if (checkingAllProperties && !unaddressed().isEmpty()) {
            mismatch.appendText("\n     neither checked nor ignored: ")
                    .appendValueList("", ", ", "", unaddressed());
            ok = false;
        }
        return ok;
    }

    @Override
    public void describeTo(Description d) {
        d.appendText("Person");
        checks.forEach((p, m) -> {
            d.appendText("\n     ").appendText(p).appendText(": ");
            m.describeTo(d);
        });
        if (checkingAllProperties) d.appendText("\n     with every property checked or ignored");
    }
}
