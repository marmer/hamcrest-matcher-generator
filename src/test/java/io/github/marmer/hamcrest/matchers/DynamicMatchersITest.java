package io.github.marmer.hamcrest.matchers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import lombok.Value;

public class DynamicMatchersITest {
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void testInstanceOf_OnlyTypeIsGiven_ShouldMatchIfAnInstanceOfTheGivenTypeIsTriedToMatch() throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class);

		// Assertion
		assertThat("matches", propertyMatcher.matches(new SamplePojo("someValue")), is(true));
	}

	@Test
	public void testInstanceOf_OnlyTypeIsGiven_ShouldNotMatchIfAnInstanceOfTheAnotherTypeIsTriedToMatch()
			throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class);

		// Assertion
		assertThat("matches", propertyMatcher.matches(String.class), is(false));
	}

	@Test
	public void testInstanceOf_OnlyTypeIsGiven_DescriptionShouldContainInstanceOfDescriptionText() throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class);

		// Assertion
		Description description = new StringDescription();
		propertyMatcher.describeTo(description);
		assertThat("Matcher description Text", description.toString(), containsString(instanceOfDescriptionText()));
	}

	@Test
	public void testAndWith_RelatedPropertyExists_ShouldAddHasPropertyDescription() throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class).withMyFancyProperty();

		// Assertion
		Description description = new StringDescription();
		propertyMatcher.describeTo(description);
		assertThat("Matcher description Text", description.toString(),
				containsString(hasPropertyMatcherWithoutParamDescriptionText("myFancyProperty")));
	}

	@Test
	public void testAndWith_RelatedPropertyExistsButAdditionalPropertyMatchFails_ShouldNotMatch() throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class)
				.withMyFancyProperty(equalTo("test"));

		// Assertion
		assertThat("Matcher of property matches", propertyMatcher.matches(new SamplePojo("notTest")), is(false));
	}

	@Test
	public void testAndWith_RelatedPropertyExistsButAdditionalPropertyMatchMatches_ShouldMatch() throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class)
				.withMyFancyProperty(equalTo("test"));

		// Assertion
		assertThat("Matcher of property matches", propertyMatcher.matches(new SamplePojo("test")), is(true));
	}

	@Test
	public void testAndWith_RelatedPropertyExistsButAdditionalPropertyMatchMatches_ShouldAddHasPropertyDescriptionWithValue()
			throws Exception {
		// Preparation

		// Execution
		Matcher<SamplePojo> propertyMatcher = DynamicMatchers.instanceOf(SamplePojo.class)
				.withMyFancyProperty(equalTo("mops"));

		// Assertion
		Description description = new StringDescription();
		propertyMatcher.describeTo(description);
		assertThat("Matcher description Text", description.toString(),
				containsString(hasPropertyMatcherWithoutParamDescriptionText("myFancyProperty", equalTo("mops"))));
	}

	private String hasPropertyMatcherWithoutParamDescriptionText(final String propertyName,
			final Matcher<String> innerMatcher) {
		Description instanceOfDescription = new StringDescription();
		Matchers.hasProperty(propertyName, innerMatcher).describeTo(instanceOfDescription);
		return instanceOfDescription.toString();
	}

	private String hasPropertyMatcherWithoutParamDescriptionText(final String propertyName) {
		Description instanceOfDescription = new StringDescription();
		Matchers.hasProperty(propertyName).describeTo(instanceOfDescription);
		return instanceOfDescription.toString();
	}

	private String instanceOfDescriptionText() {
		Description instanceOfDescription = new StringDescription();
		Matchers.instanceOf(SamplePojo.class).describeTo(instanceOfDescription);
		return instanceOfDescription.toString();
	}

	@Value
	public static class SamplePojo {
		private String myFancyProperty;
	}
}
