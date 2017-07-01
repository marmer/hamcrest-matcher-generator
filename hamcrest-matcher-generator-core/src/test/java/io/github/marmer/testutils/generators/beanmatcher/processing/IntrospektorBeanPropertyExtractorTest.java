package io.github.marmer.testutils.generators.beanmatcher.processing;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.InjectMocks;
import org.mockito.Spy;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.beans.IntrospectionException;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.when;


public class IntrospektorBeanPropertyExtractorTest {
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@InjectMocks
	private BeanPropertyExtractor classUnderTest = new IntrospektorBeanPropertyExtractor();

	@Spy
	private IntrospectorDelegate introspectorDelegate = new IntrospectorDelegate();

	@Test
	public void testGetPropertiesOf_TypeWithSomeProperties_ShouldReturnThePropertiesWithItsNames() throws Exception {
		// Preparation

		// Execution
		final List<BeanProperty> properties = classUnderTest.getPropertiesOf(ClassWithSingleProperty.class);

		// Assertion
		assertThat("Properties", properties,
			containsInAnyOrder(hasProperty("name", equalTo("firstProperty")),
				hasProperty("name", equalTo("secondProperty"))));
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
		when(introspectorDelegate.getBeanInfo(ClassWithSingleProperty.class)).thenThrow(IntrospectionException.class);

		// Execution
		final List<BeanProperty> properties = classUnderTest.getPropertiesOf(ClassWithSingleProperty.class);

		// Assertion
		assertThat("Properties", properties, is(empty()));
	}

	public static class ClassWithSingleProperty {
		private String firstProperty;

		public String getFirstProperty() {
			return firstProperty;
		}

		public Integer getSecondProperty() {
			return 42;
		}

		public String nonPropertyAccessorMethod() {
			return "iAmNotAProperty";
		}
	}
}
