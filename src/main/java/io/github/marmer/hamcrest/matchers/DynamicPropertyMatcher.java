package io.github.marmer.hamcrest.matchers;

import static org.hamcrest.Matchers.hasProperty;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

public final class DynamicPropertyMatcher<T> extends TypeSafeMatcher<T> {

	private Matcher<T> instanceOfMatcher;

	DynamicPropertyMatcher(final Class<T> expectedClass) {
		this.instanceOfMatcher = Matchers.instanceOf(expectedClass);
	}

	public Matcher<T> withMyFancyProperty() {
		// FIXME static implementation is only a helper to prepare a test.
		// FIXME Make me dynamic
		return hasProperty("myFancyProperty");
	}

	public void describeTo(final Description description) {
		instanceOfMatcher.describeTo(description);
	}

	@Override
	protected boolean matchesSafely(final T item) {
		return instanceOfMatcher.matches(item);
	}

	public Matcher<T> withMyFancyProperty(final Matcher<?> matching) {
		// FIXME static implementation is only a helper to prepare a test.
		// FIXME Make me dynamic
		return hasProperty("myFancyProperty", matching);
	}

	public Matcher<T> with(final String propertyName, final Matcher<String> matcher) {
		return hasProperty(propertyName, matcher);
	}
}