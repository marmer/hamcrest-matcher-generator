package io.github.marmer.testutils.generators.beanmatcher;

import org.junit.Test;

import sample.classes.ComplexSample;
import sample.classes.SimpleSampleClass;

import sample.classes.subpackage.ClassInSubPackage;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import static org.junit.Assert.assertThat;


public class ReflectionPotentialBeanClassFinderTest {
	private PotentialPojoClassFinder classUnderTest = new ReflectionPotentialBeanClassFinder();

	@Test
	public void testFindClasses_PackageWithClassesGiven_ShouldFindAllRelevantClassesInThePackageAndItsSubPackage()
		throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("sample.classes");

		// Assertion
		assertThat("Classes found", classes,
			containsInAnyOrder(SimpleSampleClass.class, ClassInSubPackage.class,
				sample.classes.subpackage2.ClassInSubPackage.class, ComplexSample.class, ComplexSample.InnerClass.class,
				ComplexSample.InnerStaticClass.class));
	}

	@Test
	public void testFindClasses_FullQualifiedClassNameGiven_ShouldReturnOnlyTheRelatedClass() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("sample.classes.SimpleSampleClass");

		// Assertion
		assertThat("Classes found", classes, containsInAnyOrder(SimpleSampleClass.class));
	}

	@Test
	public void testFindClasses_NotExistingPackageGiven_ShouldNotReturnNoClasses() throws Exception {
		// Preparation

		// Execution
		final List<Class<?>> classes = classUnderTest.findClasses("some.not.existing.package.given");

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
		final List<Class<?>> classes = classUnderTest.findClasses("sample.classes.subpackage.ClassInSubPackage",
				"sample.classes.subpackage2.ClassInSubPackage");

		// Assertion
		assertThat("Classes found", classes,
			containsInAnyOrder(ClassInSubPackage.class,
				sample.classes.subpackage2.ClassInSubPackage.class));
	}

}
