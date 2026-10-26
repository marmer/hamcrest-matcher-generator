package some.other.pck;

import io.github.marmer.testutils.generators.beanmatcher.dependencies.BeanPropertyMatcher;
import java.lang.Override;
import java.lang.String;
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

    public SimplePojoMatcher withProperty(final Matcher<? extends String> value) {
        beanPropertyMatcher.with("property", Matchers.equalTo(value));
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
}
