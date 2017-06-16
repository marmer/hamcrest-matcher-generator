package io.github.marmer.testutils.samplepojos;

import io.github.marmer.testutils.BeanPropertyMatcher;
import io.github.marmer.testutils.samplepojos.HasPropertyMatcherGeneratorTest.SimplePojoChild;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Generated;


/**
 * TODO DELETE ME!
 *
 * @author  marmer
 * @since   16.06.2017
 */
@Generated("io.github.marmer.testutils.samplepojos.HasPropertyMatcherGenerator")
public class SimplePojoChildMatcherWay2 extends TypeSafeMatcher<SimplePojoChild> {
	private final BeanPropertyMatcher<SimplePojoChild> beanPropertyMatcher;

	public SimplePojoChildMatcherWay2() {
		beanPropertyMatcher = new BeanPropertyMatcher<SimplePojoChild>(SimplePojoChild.class);
	}

	public SimplePojoChildMatcherWay2 withSimpleProp(final Matcher<?> matcher) {
		beanPropertyMatcher.with("simpleProp", matcher);
		return this;
	}

	@Override
	public void describeTo(final Description description) {
		beanPropertyMatcher.describeTo(description);
	}

	@Override
	protected boolean matchesSafely(final SimplePojoChild item) {
		return beanPropertyMatcher.matches(item);
	}

	@Override
	protected void describeMismatchSafely(final SimplePojoChild item, final Description description) {
		beanPropertyMatcher.describeMismatch(item, description);
	}
}
