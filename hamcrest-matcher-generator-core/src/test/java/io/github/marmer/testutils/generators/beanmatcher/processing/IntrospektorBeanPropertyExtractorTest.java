package io.github.marmer.testutils.generators.beanmatcher.processing;

import io.github.marmer.testutils.generators.beanmatcher.Log;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.beans.IntrospectionException;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


public class IntrospektorBeanPropertyExtractorTest {
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@InjectMocks
	private IntrospektorBeanPropertyExtractor classUnderTest;

	@Spy
	private IntrospectorDelegate introspectorDelegate = new IntrospectorDelegate();
	@Mock
	private Log log;

	@SuppressWarnings("unchecked")
	@Test
	public void testGetPropertiesOf_TypeWithSomeProperties_ShouldReturnTheCorrectlyInitializedValues()
		throws Exception {
		// Preparation

		// Execution
		final List<BeanProperty> properties = classUnderTest.getPropertiesOf(ClassWithSomeProperties.class);

		// Assertion
		assertThat("Properties", properties,
			containsInAnyOrder(
				is(allOf(hasProperty("name", equalTo("firstProperty")), hasProperty("type", equalTo(String.class)))),
				is(allOf(hasProperty("name", equalTo("secondProperty")),
						hasProperty("type", equalTo(Integer.TYPE))))));
	}

	@Test
	public void testGetPropertiesOf_NoTypeGiven_ShouldReturnAnEmptyList() throws Exception {
		// Preparation

		// Execution
		final List<BeanProperty> properties = classUnderTest.getPropertiesOf(null);

		// Assertion
		assertThat("Properties", properties, is(empty()));
	}

	@Test
	public void testGetPropertiesOf_ErrorOnReadingTypeInformation_ShouldReturnAnEmptyList() throws Exception {

		// Preparation
		when(introspectorDelegate.getBeanInfo(ClassWithSomeProperties.class)).thenThrow(IntrospectionException.class);

		// Execution
		final List<BeanProperty> properties = classUnderTest.getPropertiesOf(ClassWithSomeProperties.class);

		// Assertion
		assertThat("Properties", properties, is(empty()));
	}

	public static class ClassWithSomeProperties {
		private String firstProperty;

		public String getFirstProperty() {
			return firstProperty;
		}

		public int getSecondProperty() {
			return 42;
		}

		public String nonPropertyAccessorMethod() {
			return "iAmNotAProperty";
		}
	}
}
