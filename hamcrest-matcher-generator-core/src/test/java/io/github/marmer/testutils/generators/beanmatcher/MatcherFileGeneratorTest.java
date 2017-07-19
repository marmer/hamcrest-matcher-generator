package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static java.util.Collections.singletonList;


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
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo1.class);
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo2.class);
	}

	@Test
	public void testGenerateHelperForClassesAllIn_MatchersHaveBeenGenerated_GeneratedMatcherFilesShouldBeReturned()
		throws Exception {

		// Preparation
		final Path simplePojo1MatcherPath = mock(Path.class, "simplePojo1MatcherPath");
		final Path simplePojo2MatcherPath = mock(Path.class, "simplePojo2MatcherPath");
		final List<Class<?>> generatedClasses = singletonList(String.class);

		doReturn(Arrays.asList(SamplePojo1.class,
				SamplePojo2.class)).when(potentialPojoClassFinder).findClasses(packageName);
		doReturn(simplePojo1MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
			SamplePojo1.class);
		doReturn(simplePojo2MatcherPath).when(hasPropertyMatcherClassGenerator).generateMatcherFor(
			SamplePojo2.class);
		doReturn(generatedClasses).when(javaFileClassLoader).load(Arrays.asList(simplePojo1MatcherPath,
				simplePojo2MatcherPath));

		// Execution
		final List<Class<?>> retVal = classUnderTest.generateHelperForClassesAllIn(packageName);

		// Assertion
		assertThat(retVal, is(sameInstance(generatedClasses)));
	}

	private static class SamplePojo1 { }

	private static class SamplePojo2 { }
}
