package proto.assertj;

import org.assertj.core.api.SoftAssertions;
import proto.model.Person;

/** PROTOTYPE. Stand-in for the generated SoftAssertionsProvider mixin. */
public class ProtoSoftAssertions extends SoftAssertions {
    public PersonAssert assertThat(Person actual) { return proxy(PersonAssert.class, Person.class, actual); }
}
