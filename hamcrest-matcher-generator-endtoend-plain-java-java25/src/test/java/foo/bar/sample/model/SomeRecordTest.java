package foo.bar.sample.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static foo.bar.sample.model.SomeRecordMatcher.isSomeRecord;

class SomeRecordTest {

    @Test
    void generatedMatcherShouldMatchRecordByComponent() {
        assertThat(
            new SomeRecord("hello", 42),
            isSomeRecord().withSomeField("hello").withSomeNumber(42)
        );
    }
}
