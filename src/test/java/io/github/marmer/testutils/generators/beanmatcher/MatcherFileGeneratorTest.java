package io.github.marmer.testutils.generators.beanmatcher;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.nio.file.Path;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class MatcherFileGeneratorTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@InjectMocks
	private MatcherFileGenerator classUnderTest;
	@Mock
	private PotentialPojoClassFinder potentialPojoClassFinder;
	@Mock
	private HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;

	private final String packageName = "packageName";
	@Mock
	private Path outputDir;

	@Mock
	private FactoryMethodFacadeGenerator factoryMethodFacadeGenerator;
	@Mock
	private JavaFileClassLoader javaFileClassLoader;

	@Test
	public void testGenerateHelperForClassesAllIn_PackageGiven_ClassesOfPackageShouldBeUsedToGenerateMatchersPerClassFound()
		throws Exception {

		// Preparation
		doReturn(Arrays.asList(SamplePojo1.class,
				SamplePojo2.class)).when(potentialPojoClassFinder).findClasses(packageName);

		// Execution
		classUnderTest.generateHelperForClassesAllIn(packageName);

		// Assertion
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo1.class, outputDir);
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo2.class, outputDir);
	}

	@Test
	public void testGenerateHelperForClassesAllIn_MatchersHaveBeenGenerated_GeneratedMatcherFilesShouldBeUsedToCreateAFacadeMatcher()
		throws Exception {

		// Preparation
		final Path simplePojo1MatcherPath = mock(Path.class, "simplePojo1MatcherPath");
		final Path simplePojo2MatcherPath = mock(Path.class, "simplePojo2MatcherPath");

		final List<Class<?>> classesToGenerateFacadeFor = Arrays.asList(SamplePojo1Matcher.class,
				SamplePojo2Matcher.class);
		doReturn(Arrays.asList(SamplePojo1.class,
				SamplePojo2.class)).when(potentialPojoClassFinder).findClasses(packageName);
		doReturn(simplePojo1MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
			SamplePojo1.class, outputDir);
		doReturn(simplePojo2MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
			SamplePojo2.class, outputDir);
		doReturn(classesToGenerateFacadeFor).when(javaFileClassLoader).load(
			Arrays.asList(simplePojo1MatcherPath,
				simplePojo2MatcherPath));

		// Execution
		classUnderTest.generateHelperForClassesAllIn(packageName);

		// Assertion
		verify(factoryMethodFacadeGenerator).generateFacadeFor(classesToGenerateFacadeFor);
	}

	private static class SamplePojo1 { }

	private static class SamplePojo2 { }

	private static class SamplePojo1Matcher { }

	private static class SamplePojo2Matcher { }

}
