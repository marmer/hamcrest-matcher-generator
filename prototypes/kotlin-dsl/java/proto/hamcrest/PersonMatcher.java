package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Consumer;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
import proto.model.Person;

/** PROTOTYPE. Hand-written stand-in for the generated matcher of {@link Person}. */
public final class PersonMatcher extends PropertyMatcher<Person, PersonMatcher> {

    private PersonMatcher() {
        super(Person.class);
        property("name", Person::getName);
        property("age", Person::getAge);
        property("email", Person::getEmail);
        property("phone", Person::getPhone);
    }

    public static PersonMatcher isPerson() { return new PersonMatcher(); }

    /** K2 (Java-emitted lambda overload): {@code isPerson(m -> m.withFoo(..))}. */
    public static PersonMatcher isPerson(Consumer<? super PersonMatcher> spec) { PersonMatcher m = isPerson(); spec.accept(m); return m; }

    public PersonMatcher withName(@Nullable String v) { return withName(equalTo(v)); }
    public PersonMatcher withName(Matcher<? super String> m) { return check("name", m); }
    public PersonMatcher ignoringName() { return ignore("name"); }

    public PersonMatcher withAge(int v) { return withAge(equalTo(v)); }
    public PersonMatcher withAge(Matcher<? super Integer> m) { return check("age", m); }
    public PersonMatcher ignoringAge() { return ignore("age"); }

    public PersonMatcher withEmail(@Nullable String v) { return withEmail(equalTo(v)); }
    public PersonMatcher withEmail(Matcher<? super String> m) { return check("email", m); }
    public PersonMatcher ignoringEmail() { return ignore("email"); }

    public PersonMatcher withPhone(@Nullable String v) { return withPhone(equalTo(v)); }
    public PersonMatcher withPhone(Matcher<? super String> m) { return check("phone", m); }
    public PersonMatcher ignoringPhone() { return ignore("phone"); }
}
