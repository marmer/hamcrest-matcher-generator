package io.github.marmer.hamcrest.matchers;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class DynamicMatchers {

	public static <T> Matcher<T> instanceOf(final Class<T> type) {
		return Matchers.instanceOf(type);
	}

}
