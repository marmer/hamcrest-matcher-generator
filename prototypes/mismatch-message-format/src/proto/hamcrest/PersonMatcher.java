package proto.hamcrest;

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Matcher;
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

    public PersonMatcher withName(String v) { return withName(equalTo(v)); }
    public PersonMatcher withName(Matcher<? super String> m) { return check("name", m); }
    public PersonMatcher ignoringName() { return ignore("name"); }

    public PersonMatcher withAge(int v) { return withAge(equalTo(v)); }
    public PersonMatcher withAge(Matcher<? super Integer> m) { return check("age", m); }
    public PersonMatcher ignoringAge() { return ignore("age"); }

    public PersonMatcher withEmail(String v) { return withEmail(equalTo(v)); }
    public PersonMatcher withEmail(Matcher<? super String> m) { return check("email", m); }
    public PersonMatcher ignoringEmail() { return ignore("email"); }

    public PersonMatcher withPhone(String v) { return withPhone(equalTo(v)); }
    public PersonMatcher withPhone(Matcher<? super String> m) { return check("phone", m); }
    public PersonMatcher ignoringPhone() { return ignore("phone"); }
}
