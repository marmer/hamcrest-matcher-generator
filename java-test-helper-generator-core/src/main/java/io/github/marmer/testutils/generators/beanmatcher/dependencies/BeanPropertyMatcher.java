package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;


/**
 * Matcher for beans.
 *
 * @param   <T>  Type of the bean.
 *
 * @author  marmer
 * @date    13.06.2017
 */
public class BeanPropertyMatcher<T> extends TypeSafeMatcher<T> {

	private final List<Matcher<?>> hasPropertyMatcher = new ArrayList<>();

	public BeanPropertyMatcher(final Class<? extends T> expectedClass) {
		hasPropertyMatcher.add(Matchers.instanceOf(expectedClass));
	}

	@Override
	public void describeTo(final Description description) {
		getFullInnerMatcher().describeTo(description);
	}

	@Override
	protected boolean matchesSafely(final T item) {
		return getFullInnerMatcher().matches(item);
	}

	@SuppressWarnings("unchecked")
	private Matcher<?> getFullInnerMatcher() {
		return allOf(hasPropertyMatcher.toArray(new Matcher[hasPropertyMatcher.size()]));
	}

	public BeanPropertyMatcher<T> with(final String propertyName, final Matcher<?> matcher) {
		this.hasPropertyMatcher.add(hasProperty(propertyName, matcher));
		return this;
	}

	public BeanPropertyMatcher<T> with(final String propertyName) {
		this.hasPropertyMatcher.add(hasProperty(propertyName));
		return this;
	}
}
