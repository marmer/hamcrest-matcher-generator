package foo.bar.sample.model.inheritance;

import static foo.bar.sample.model.inheritance.FirstChildMatcher.isFirstChild;
import static foo.bar.sample.model.inheritance.MainMatcher.isMain;
import static foo.bar.sample.model.inheritance.SecondChildMatcher.isSecondChild;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;

import java.util.UUID;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
            .withParents(contains(
                (Matcher) isFirstChild()
                    .withId(firstId)
                    .withName(firstName),
                isSecondChild()
                    .withId(secondId)
                    .withName(secondName)
            )));
    }
}
