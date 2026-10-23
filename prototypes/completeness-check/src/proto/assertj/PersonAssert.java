package proto.assertj;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import proto.Person;

/**
 * PROTOTYPE. Candidate A1: runtime completeness check (AssertJ backend).
 * AssertJ assertions run eagerly, one call at a time, so the check needs an
 * explicit end: either a terminal call (A1a) or a scope (A1b).
 * Simplified: the real backend generates AbstractPersonAssert + PersonAssert.
 */
public class PersonAssert extends AbstractObjectAssert<PersonAssert, Person> {

    private static final List<String> PROPERTIES = List.of("name", "age", "email", "phone");
    private final Set<String> addressed = new LinkedHashSet<>();

    public PersonAssert(Person actual) {
        super(actual, PersonAssert.class);
    }

    public static PersonAssert assertThat(Person actual) {
        return new PersonAssert(actual);
    }

    public PersonAssert hasName(String expected) { return has("name", actual.getName(), expected); }
    public PersonAssert hasAge(int expected) { return has("age", actual.getAge(), expected); }
    public PersonAssert hasEmail(String expected) { return has("email", actual.getEmail(), expected); }
    public PersonAssert hasPhone(String expected) { return has("phone", actual.getPhone(), expected); }

    public PersonAssert hasNameSatisfying(Consumer<String> r) { addressed.add("name"); Assertions.assertThat(actual.getName()).satisfies(r); return myself; }

    public PersonAssert ignoringName() { addressed.add("name"); return myself; }
    public PersonAssert ignoringAge() { addressed.add("age"); return myself; }
    public PersonAssert ignoringEmail() { addressed.add("email"); return myself; }
    public PersonAssert ignoringPhone() { addressed.add("phone"); return myself; }

    /** A1a: terminal call at the end of the chain. Forgetting it silently checks nothing extra. */
    public PersonAssert hasAllPropertiesChecked() {
        isNotNull();
        List<String> missing = new ArrayList<>();
        for (String p : PROPERTIES) if (!addressed.contains(p)) missing.add(p);
        if (!missing.isEmpty()) {
            failWithMessage("%nExpecting every property of Person to be checked or ignored, but these were neither:%n  %s",
                    String.join(", ", missing));
        }
        return myself;
    }

    /** A1b: scope. The check runs after the block, so it cannot be forgotten. */
    public PersonAssert satisfiesCheckingAllProperties(Consumer<PersonAssert> checks) {
        checks.accept(myself);
        return hasAllPropertiesChecked();
    }

    private PersonAssert has(String property, Object actualValue, Object expected) {
        isNotNull();
        addressed.add(property);
        if (!Objects.equals(actualValue, expected)) {
            failWithMessage("%nExpecting %s of:%n  %s%nto be:%n  %s%nbut was:%n  %s",
                    property, actual, expected, actualValue);
        }
        return myself;
    }
}
