package foo.bar.sample.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static foo.bar.sample.model.ParentPojoMatcher.isParentPojo;
import static foo.bar.sample.model.SomePojoMatcher.InnerClassMatcher.InnerInnerPojoMatcher.isInnerInnerPojo;
import static foo.bar.sample.model.SomePojoMatcher.NonStaticInnerClassMatcher.isNonStaticInnerClass;
import static foo.bar.sample.model.SomePojoMatcher.isSomePojo;
import static org.hamcrest.MatcherAssert.assertThat;
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
        somePojo.setIntProperty(42);


        // Assertion
        assertThat(somePojo, isSomePojo()
                .withClass(SomePojo.class)
                .withParentField("someParentFieldValue")
                .withParentField(is(equalTo("someParentFieldValue")))
                .withPojoField("pojoFieldValue")
                .withPojoField(is(equalTo("pojoFieldValue")))
                .withIntProperty(42)
                .withIntProperty(is(42))
        );
    }

    @Test
    @DisplayName("Matchers of parent classes should work fine as well")
    void testMatchers_MatchersOfParentClassesShouldWorkFineAsWell()
            throws Exception {
        // Preparation
        final SomePojo somePojo = new SomePojo();
        somePojo.setPojoField("pojoFieldValue");
        somePojo.setParentField("someParentFieldValue");

        // Assertion
        assertThat(somePojo, isParentPojo()
                .withClass(SomePojo.class)
                .withParentField("someParentFieldValue")
                .withParentField(is(equalTo("someParentFieldValue")))
        );
    }

    @Test
    @DisplayName("Matcher for innerclasses should work")
    void testMatchers_MatcherForInnerclassesShouldWork()
            throws Exception {
        // Preparation
        final SomePojo.InnerClass.InnerInnerPojo innerInnerPojo = new SomePojo.InnerClass.InnerInnerPojo("someValue");

        // Execution

        // Assertion
        assertThat(innerInnerPojo, isInnerInnerPojo()
                .withSomeField("someValue"));
    }

    @Test
    @DisplayName("Matcher for non static innerclasses should work")
    void testMatchers_MatcherForNonStaticInnerclassesShouldWork()
            throws Exception {
        // Preparation
        final SomePojo.NonStaticInnerClass nonStaticInnerClass = new SomePojo.NonStaticInnerClass("42");

        // Execution

        // Assertion
        assertThat(nonStaticInnerClass, isNonStaticInnerClass()
                .withSomeField("42"));
    }

}