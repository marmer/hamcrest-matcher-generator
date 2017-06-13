package io.github.marmer.testutils;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
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
