package proto;

import static org.hamcrest.MatcherAssert.assertThat;
import static proto.staged.PersonMatchers.isPersonWithAll;

/** PROTOTYPE. Written when Person had only name, age, email. Must stop compiling. */
class OldStagedTest {
    void test(Person p) {
        assertThat(p, isPersonWithAll().name("Ada").age(36).ignoringEmail());
    }
}
