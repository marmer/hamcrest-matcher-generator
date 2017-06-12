package io.github.marmer.hamcrest.matchers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import lombok.Value;

public class DynamicPropertyMatcherTest {
	@Test
	public void testMatches_OnlyWithMatchingTypeInitialized_ShouldNotMatchInstanceOfDifferentType() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> dynamicPropertyMatcher = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = dynamicPropertyMatcher.matches(new AnotherClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(false));
	}

	@Test
	public void testMatches_OnlyWithMatchingTypeInitialized_ShouldMatchInstanceOfSameType() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> dynamicPropertyMatcher = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = dynamicPropertyMatcher.matches(new ClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(true));
	}

	@Value
	public static class ClassWithSingleProperty {
		private String someProperty;
	}

	@Value
	public static class AnotherClassWithSingleProperty {
		private String someProperty;
	}

	@Value
	public static class ClassTwoProperties {
		private String firstProperty;
		private String secondProperty;
	}
}
