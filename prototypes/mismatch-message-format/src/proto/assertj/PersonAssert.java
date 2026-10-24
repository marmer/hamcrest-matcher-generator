package proto.assertj;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import proto.model.*;

/** PROTOTYPE. Hand-written stand-in for the generated AssertJ assert of {@link Person}. */
public class PersonAssert extends PropertyAssert<PersonAssert, Person> {
    public PersonAssert(Person actual) { super(actual, PersonAssert.class, "Person"); }
    public static PersonAssert assertThat(Person actual) { return new PersonAssert(actual); }
    public PersonAssert hasName(String v) { return hasProperty("name", actual == null ? null : actual.getName(), v); }
    public PersonAssert hasNameSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("name", actual == null ? null : actual.getName(), r); }
    public PersonAssert hasAge(Integer v) { return hasProperty("age", actual == null ? null : actual.getAge(), v); }
    public PersonAssert hasAgeSatisfying(Consumer<? super Integer> r) { return hasPropertySatisfying("age", actual == null ? null : actual.getAge(), r); }
    public PersonAssert hasEmail(String v) { return hasProperty("email", actual == null ? null : actual.getEmail(), v); }
    public PersonAssert hasEmailSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("email", actual == null ? null : actual.getEmail(), r); }
    public PersonAssert hasPhone(String v) { return hasProperty("phone", actual == null ? null : actual.getPhone(), v); }
    public PersonAssert hasPhoneSatisfying(Consumer<? super String> r) { return hasPropertySatisfying("phone", actual == null ? null : actual.getPhone(), r); }
}
