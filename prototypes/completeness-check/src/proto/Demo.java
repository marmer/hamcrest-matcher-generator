package proto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static proto.hamcrest.PersonMatcher.isPerson;
import static proto.hamcrest.PersonMatcher.isPersonLike;
import static proto.staged.PersonMatchers.isPersonWithAll;

import proto.assertj.PersonAssert;

/** PROTOTYPE. Runs every scenario and prints what the test author would see. */
public class Demo {

    static final Person ADA = new Person("Ada", 36, "ada@example.org", "+44 20 0000");

    public static void main(String[] args) {
        System.out.println("Person has: name, age, email, phone (phone = the property added later)\n");

        scenario("H1 Hamcrest, runtime opt-in: all addressed -> passes", () ->
                assertThat(ADA, isPerson().checkingAllProperties()
                        .withName("Ada").withAge(36).withEmail(startsWith("ada@")).ignoringPhone()));

        scenario("H1 Hamcrest, runtime opt-in: test written before phone existed -> fails", () ->
                assertThat(ADA, isPerson().checkingAllProperties()
                        .withName("Ada").withAge(36).ignoringEmail()));

        scenario("H1 Hamcrest, runtime opt-in: value mismatch AND unaddressed property", () ->
                assertThat(ADA, isPerson().checkingAllProperties()
                        .withName("Bob").withAge(36).ignoringEmail()));

        scenario("H1 Hamcrest, without opt-in (5.x behaviour): phone not checked -> passes", () ->
                assertThat(ADA, isPerson().withName("Ada")));

        scenario("H1 Hamcrest, reference object: complete by construction; resetPhone() re-opens it -> fails", () ->
                assertThat(ADA, isPersonLike(ADA).resetPhone()));

        scenario("H1 Hamcrest, reference object with one override -> passes", () ->
                assertThat(ADA, isPersonLike(ADA).withEmail(startsWith("ada"))));

        scenario("H2 Hamcrest, compile-time staged builder: all addressed -> passes", () ->
                assertThat(ADA, isPersonWithAll()
                        .name("Ada").age(36).email(startsWith("ada@")).ignoringPhone()));

        scenario("A1a AssertJ, terminal call: test written before phone existed -> fails", () ->
                PersonAssert.assertThat(ADA)
                        .hasName("Ada").hasAge(36).ignoringEmail()
                        .hasAllPropertiesChecked());

        scenario("A1a AssertJ, terminal call forgotten -> passes silently (the pitfall)", () ->
                PersonAssert.assertThat(ADA)
                        .hasName("Ada").hasAge(36).ignoringEmail());

        scenario("A1b AssertJ, scope: test written before phone existed -> fails", () ->
                PersonAssert.assertThat(ADA).satisfiesCheckingAllProperties(p -> p
                        .hasName("Ada").hasAge(36).ignoringEmail()));

        scenario("A1b AssertJ, scope: value mismatch fails first (AssertJ is eager)", () ->
                PersonAssert.assertThat(ADA).satisfiesCheckingAllProperties(p -> p
                        .hasName("Bob").hasAge(36).ignoringEmail()));
    }

    static void scenario(String title, Runnable test) {
        System.out.println("=== " + title);
        try {
            test.run();
            System.out.println("PASSED\n");
        } catch (AssertionError e) {
            System.out.println("FAILED: " + e.getMessage().strip() + "\n");
        }
    }
}
