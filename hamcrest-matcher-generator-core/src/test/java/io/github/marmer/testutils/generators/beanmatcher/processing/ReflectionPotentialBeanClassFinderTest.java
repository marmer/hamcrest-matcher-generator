package io.github.marmer.testutils.generators.beanmatcher.processing;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import sample.classes.*;
import sample.classes.subpackage.ClassInSubPackage;
import sample.classes.subpackage.ExceptionWithProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


public class ReflectionPotentialBeanClassFinderTest {
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Mock
	private BeanPropertyExtractor beanPropertyExtractor;

	private ReflectionPotentialBeanClassFinder classUnderTest;

	@Before
	public void setUp() throws Exception {
		this.classUnderTest = new ReflectionPotentialBeanClassFinder(beanPropertyExtractor, false, false);
	}

	@Test
	public void testFindClasses_PackageWithClassesGiven_ShouldFindAllRelevantClassesInThePackageAndItsSubPackage()
			throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("sample.classes");

		// Assertion
		assertThat("Classes found",
				classes,
				containsInAnyOrder(SimpleSampleClass.class,
						ClassInSubPackage.class,
						sample.classes.subpackage2.ClassInSubPackage.class,
						ComplexSample.class,
						ComplexSample.InnerClass.class,
						ComplexSample.InnerStaticClass.class,
						ExceptionWithProperties.class,
						SampleWithoutAnyProperty.class));
	}

	@Test
	public void testFindClasses_PackageWithClassesGivenAndInterfacesAreAllowed_ShouldFindAllRelevantClassesAndInterfacesInThePackageAndItsSubPackage()
			throws Exception {
		// Preparation
		this.classUnderTest = new ReflectionPotentialBeanClassFinder(beanPropertyExtractor, false, true);

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("sample.classes");

		// Assertion
		assertThat("Classes found",
				classes,
				containsInAnyOrder(SimpleSampleClass.class,
						ClassInSubPackage.class,
						sample.classes.subpackage2.ClassInSubPackage.class,
						ComplexSample.class,
						ComplexSample.InnerClass.class,
						ComplexSample.InnerStaticClass.class,
						ExceptionWithProperties.class,
						SampleWithoutAnyProperty.class,
						SomeInterface.class,
						SomeAnnotation.class));
	}

	@Test
	public void testFindClasses_FullQualifiedClassNameGiven_ShouldReturnOnlyTheRelatedClass() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"sample.classes.SimpleSampleClass");

		// Assertion
		assertThat("Classes found", classes, containsInAnyOrder(SimpleSampleClass.class));
	}

	@Test
	public void testFindClasses_NotExistingPackageGiven_ShouldNotReturnNoClasses() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"some.not.existing.package.given");

		// Assertion
		assertThat("Classes found", classes, is(empty()));
	}

	@Test
	public void testFindClasses_NoPackageGiven_ShouldNotReturnNoClasses() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("");

		// Assertion
		assertThat("Classes found", classes, is(empty()));
	}

	@Test
	public void testFindClasses_NullGiven_ShouldNotReturnNoClasses() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(null);

		// Assertion
		assertThat("Classes found", classes, is(empty()));
	}

	@Test
	public void testFindClasses_MultipleClassesGiven_ShouldReturnAllRelatedClasses() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"sample.classes.subpackage.ClassInSubPackage",
				"sample.classes.subpackage2.ClassInSubPackage");

		// Assertion
		assertThat("Classes found",
			classes,
			containsInAnyOrder(ClassInSubPackage.class,
				sample.classes.subpackage2.ClassInSubPackage.class));
	}

	@Test
	public void testFindClasses_ClassWithoutPropertyGivenOnIgnoringConfiguration_NotReturnTheRelatedClass()
		throws Exception {

		// Preparation
		when(beanPropertyExtractor.getPropertiesOf(SampleWithoutAnyProperty.class)).thenReturn(Collections.emptyList());

		final boolean ignoreClassesWithoutProperties = true;
		classUnderTest = new ReflectionPotentialBeanClassFinder(beanPropertyExtractor, ignoreClassesWithoutProperties, false);

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"sample.classes.SampleWithoutAnyProperty");

		// Assertion
		assertThat("Classes found",
			classes,
			is(empty()));
	}

	@Test
	public void testFindClasses_ClassWithoutPropertyGivenOnNotIgnoringConfiguration_ShouldReturnClass()
		throws Exception {

		// Preparation
		final boolean ignoreClassesWithoutProperties = false;
		classUnderTest = new ReflectionPotentialBeanClassFinder(beanPropertyExtractor, ignoreClassesWithoutProperties, false);

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"sample.classes.SampleWithoutAnyProperty");

		// Assertion
		assertThat("Classes found",
			classes,
			contains(SampleWithoutAnyProperty.class));
	}

	@Test
	public void testFindClasses_ClassWithPropertiesGivenOnNotIgnoringConfiguration_ShouldReturnClass()
		throws Exception {

		// Preparation
		final boolean ignoreClassesWithoutProperties = true;
		when(beanPropertyExtractor.getPropertiesOf(ClassInSubPackage.class)).thenReturn(Arrays.asList(
				new BeanProperty("someProperty", String.class)));
		classUnderTest = new ReflectionPotentialBeanClassFinder(beanPropertyExtractor, ignoreClassesWithoutProperties, false);

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses(
				"sample.classes.subpackage.ClassInSubPackage");

		// Assertion
		assertThat("Classes found",
			classes,
			contains(ClassInSubPackage.class));
	}
}
