package foo.bar.sample.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static foo.bar.sample.model.SomeLombokPojoMatcher.isSomeLombokPojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class SomeLombokPojoTest {
    @Test
    @DisplayName("Generated matchers match properties as expected")
    void testMatchers_GeneratedMatchersMatchPropertiesAsExpected()
            throws Exception {
        // Preparation
        final SomeLombokPojo somePojo = new SomeLombokPojo();
        somePojo.setSomeProp("somePropVal");


        // Assertion
        assertThat(somePojo, isSomeLombokPojo()
                .withClass(SomeLombokPojo.class)
                .withSomeProp(is(equalTo("somePropVal")))
        );
    }

}