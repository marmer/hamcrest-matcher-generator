package io.github.marmer.hamcrest.matchers;

/**
 * Collection of dynamic Matchers.
 *
 * @author marmer
 * @date 12.06.2017
 *
 */
public class DynamicMatchers {

	/**
	 * Prepares a DynamicPropertyMatcher for the given Instance.
	 *
	 * TODO find a better name.
	 *
	 * @param <T>
	 *            the generic type
	 * @param type
	 *            the type
	 * @return the dynamic property matcher
	 */
	public static <T> DynamicPropertyMatcher<T> instanceOf(final Class<T> type) {
		return new DynamicPropertyMatcher<T>(type);
	}

}
