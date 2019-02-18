package foo.bar.sample.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static foo.bar.sample.model.SomePojoMatcher.isSomePojo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class SomePojoTest {
    @Test
    @DisplayName("Generated matchers match properties as expected")
    void testMatchers_GeneratedMatchersMatchPropertiesAsExpected()
            throws Exception {
        // Preparation
        final SomePojo somePojo = new SomePojo();
        somePojo.setPojoField("pojoFieldValue");
        somePojo.setParentField("someParentFieldValue");

        // Execution


        // Assertion
        MatcherAssert.assertThat(somePojo, isSomePojo()
                .withClass(SomePojo.class)
                .withParentField("someParentFieldValue")
                .withParentField(is(equalTo("someParentFieldValue")))
                .withPojoField("pojoFieldValue")
                .withPojoField(is(equalTo("pojoFieldValue")))
        );
    }
}