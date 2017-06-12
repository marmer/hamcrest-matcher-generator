package io.github.marmer.hamcrest.matchers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.Test;

import lombok.Value;

public class DynamicPropertyMatcherTest {
	@Test
	public void testMatches_OnlyWithMatchingTypeInitialized_ShouldNotMatchInstanceOfDifferentType() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.matches(new AnotherClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(false));
	}

	@Test
	public void testMatches_OnlyWithMatchingTypeInitialized_ShouldMatchInstanceOfSameType() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.matches(new ClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(true));
	}

	@Test
	public void testDescribeTo_OnlyWithMatchingTypeInitialized_DescriptionTextShouldContainInstanceOfDescriptionText()
			throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);
		Description description = new StringDescription();

		// Execution
		classUnderTest.describeTo(description);

		// Assertion
		assertThat("Matcher description Text", description.toString(),
				containsString(instanceOfDescriptionText(ClassWithSingleProperty.class)));
	}

	@Test
	public void testMatches_InitializedWithDynamicPropertyAndCalledWithMatchingProperty_ShouldMatch() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.with("someProperty", is(equalTo("someValue")))
				.matches(new ClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(true));
	}

	@Test
	public void testMatches_InitializedWithDynamicPropertyAndCalledWithNotMatchingProperty_ShouldNotMatch()
			throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.with("someProperty", is(equalTo("someValue")))
				.matches(new ClassWithSingleProperty("someDifferentValue"));

		// Assertion
		assertThat("matches", matches, is(false));
	}

	@Test
	public void testMatches_InitializedWithDynamicPropertyAndCallWithNotExistingProperty_ShouldNotMatch()
			throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.with("differentProperty").matches(new ClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(false));
	}

	@Test
	public void testMatches_InitializedWithDynamicPropertyAndCallWithExistingProperty_ShouldMatch() throws Exception {
		// Preparation
		DynamicPropertyMatcher<ClassWithSingleProperty> classUnderTest = new DynamicPropertyMatcher<ClassWithSingleProperty>(
				ClassWithSingleProperty.class);

		// Execution
		boolean matches = classUnderTest.with("someProperty").matches(new ClassWithSingleProperty("someValue"));

		// Assertion
		assertThat("matches", matches, is(true));
	}

	private String instanceOfDescriptionText(final Class<ClassWithSingleProperty> type) {
		Description instanceOfDescription = new StringDescription();
		Matchers.instanceOf(type).describeTo(instanceOfDescription);
		return instanceOfDescription.toString();
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
