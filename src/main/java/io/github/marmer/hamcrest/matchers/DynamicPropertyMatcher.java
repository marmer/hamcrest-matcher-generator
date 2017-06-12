package io.github.marmer.hamcrest.matchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * TODO Add javadoc!
 * 
 * @author marmer
 * @date 13.06.2017
 *
 * @param <T>
 */
public final class DynamicPropertyMatcher<T> extends TypeSafeMatcher<T> {

	private final List<Matcher<?>> hasPropertyMatcher = new ArrayList<>();
	private Matcher<T> instanceOfMatcher;

	DynamicPropertyMatcher(final Class<T> expectedClass) {
		this.instanceOfMatcher = Matchers.instanceOf(expectedClass);
	}

	public Matcher<T> withMyFancyProperty() {
		// FIXME static implementation is only a helper to prepare a test.
		// FIXME Make me dynamic
		return hasProperty("myFancyProperty");
	}

	@Override
	public void describeTo(final Description description) {
		getFullInnerMatcher().describeTo(description);
	}

	@Override
	protected boolean matchesSafely(final T item) {
		return getFullInnerMatcher().matches(item);
	}

	private Matcher<?> getFullInnerMatcher() {
		if (hasPropertyMatcher.isEmpty()) {
			return instanceOfMatcher;
		}
		@SuppressWarnings("unchecked")
		Matcher<Object> allOf = Matchers.<Object>allOf(hasPropertyMatcher.toArray(new Matcher[0]));
		return is(allOf(instanceOfMatcher, allOf));
	}

	public Matcher<T> withMyFancyProperty(final Matcher<?> matching) {
		// FIXME static implementation is only a helper to prepare a test.
		// FIXME Make me dynamic
		return hasProperty("myFancyProperty", matching);
	}

	public DynamicPropertyMatcher<T> with(final String propertyName, final Matcher<String> matcher) {
		this.hasPropertyMatcher.add(hasProperty(propertyName, matcher));

		return this;
	}

	public DynamicPropertyMatcher<T> with(final String propertyName) {
		this.hasPropertyMatcher.add(hasProperty(propertyName));

		return this;
	}
}