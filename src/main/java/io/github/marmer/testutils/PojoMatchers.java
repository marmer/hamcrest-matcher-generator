package io.github.marmer.testutils;

import lombok.experimental.UtilityClass;

/**
 * Collection of dynamic Matchers.
 *
 * @author marmer
 * @date 12.06.2017
 *
 */
@UtilityClass
public class PojoMatchers {

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
	public static <T> BeanPropertyMatcher<T> beanOf(final Class<T> type) {
		return new BeanPropertyMatcher<T>(type);
	}

}
