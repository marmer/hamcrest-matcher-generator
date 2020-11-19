package foo.bar.sample.model.inheritance;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static foo.bar.sample.model.inheritance.MainMatcher.isMain;
import static java.util.Arrays.asList;

class InheritanceTest {
    @Test
    @DisplayName("Matchers.contains should work for mixed inherited types")
    void match_EverythingShouldWorkFineEvenWithInheritance() {

        final UUID firstId = UUID.randomUUID();
        final UUID secondId = UUID.randomUUID();
        final String firstName = "First name";
        final String secondName = "Second name";

        final Main main = new Main(
                asList(
                        new FirstChild(firstId, firstName),
                        new SecondChild(secondId, secondName)
                ));

        MatcherAssert.assertThat(main, isMain()
                .withParents(Matchers.contains(
                        (Matcher) FirstChildMatcher.isFirstChild()
                                .withId(firstId)
                                .withName(firstName),
                        SecondChildMatcher.isSecondChild()
                                .withId(secondId)
                                .withName(secondName)
                )));
    }
}
