package io.github.marmer.testutils.generators.beanmatcher;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;

import static org.junit.Assert.assertThat;


public class IntrospektorBeanPropertyExtractorTest {
	private BeanPropertyExtractor classUnderTest = new IntrospektorBeanPropertyExtractor();

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
