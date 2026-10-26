package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Class;
import java.lang.Override;
import javax.annotation.processing.Generated;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@Generated(value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor", date = "2020-01-01T00:00")
public class SimplePojoMatcher extends TypeSafeMatcher<SimplePojo> {
    private final BeanPropertyMatcher<SimplePojo> beanPropertyMatcher;

    public SimplePojoMatcher() {
        beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo>(SimplePojo.class);
    }

    public SimplePojoMatcher withSomeProperty(final Matcher<? super SimplePojo.InnerType.InnerEnum> matcher) {
        beanPropertyMatcher.with("someProperty", matcher);
        return this;
    }

    public SimplePojoMatcher resetSomeProperty() {
        beanPropertyMatcher.reset("someProperty");
        return this;
    }

    public SimplePojoMatcher withSomeProperty(final SimplePojo.InnerType.InnerEnum value) {
        beanPropertyMatcher.with("someProperty", Matchers.equalTo(value));
        return this;
    }

    @Override
    public void describeTo(final Description description) {
        beanPropertyMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final SimplePojo item) {
        return beanPropertyMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(final SimplePojo item, final Description description) {
        beanPropertyMatcher.describeMismatch(item, description);
    }

    public static SimplePojoMatcher isSimplePojo() {
        return new SimplePojoMatcher();
    }

    @Generated(value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor", date = "2020-01-01T00:00")
    public static class InnerTypeMatcher extends TypeSafeMatcher<SimplePojo.InnerType> {
        private final BeanPropertyMatcher<SimplePojo.InnerType> beanPropertyMatcher;

        public InnerTypeMatcher() {
            beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType>(SimplePojo.InnerType.class);
        }

        @Override
        public void describeTo(final Description description) {
            beanPropertyMatcher.describeTo(description);
        }

        @Override
        protected boolean matchesSafely(final SimplePojo.InnerType item) {
            return beanPropertyMatcher.matches(item);
        }

        @Override
        protected void describeMismatchSafely(final SimplePojo.InnerType item,
                                              final Description description) {
            beanPropertyMatcher.describeMismatch(item, description);
        }

        public static InnerTypeMatcher isInnerType() {
            return new InnerTypeMatcher();
        }

        @Generated(value = "io.github.marmer.annotationprocessing.MatcherGenerationProcessor", date = "2020-01-01T00:00")
        public static class InnerEnumMatcher extends TypeSafeMatcher<SimplePojo.InnerType.InnerEnum> {
            private final BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum> beanPropertyMatcher;

            public InnerEnumMatcher() {
                beanPropertyMatcher = new BeanPropertyMatcher<SimplePojo.InnerType.InnerEnum>(SimplePojo.InnerType.InnerEnum.class);
            }

            public InnerEnumMatcher withDeclaringClass(final Matcher<? super Class<?>> matcher) {
                beanPropertyMatcher.with("declaringClass", matcher);
                return this;
            }

            public InnerEnumMatcher withClass(final Matcher<? super Class<?>> matcher) {
                beanPropertyMatcher.with("class", matcher);
                return this;
            }

            public InnerEnumMatcher resetDeclaringClass() {
                beanPropertyMatcher.reset("declaringClass");
                return this;
            }

            public InnerEnumMatcher resetClass() {
                beanPropertyMatcher.reset("class");
                return this;
            }

            public InnerEnumMatcher withDeclaringClass(final Class<?> value) {
                beanPropertyMatcher.with("declaringClass", Matchers.equalTo(value));
                return this;
            }

            public InnerEnumMatcher withClass(final Class<?> value) {
                beanPropertyMatcher.with("class", Matchers.equalTo(value));
                return this;
            }

            @Override
            public void describeTo(final Description description) {
                beanPropertyMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final SimplePojo.InnerType.InnerEnum item) {
                return beanPropertyMatcher.matches(item);
            }

            @Override
            protected void describeMismatchSafely(final SimplePojo.InnerType.InnerEnum item,
                                                  final Description description) {
                beanPropertyMatcher.describeMismatch(item, description);
            }

            public static InnerEnumMatcher isInnerEnum() {
                return new InnerEnumMatcher();
            }
        }
    }
}
