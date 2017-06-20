package io.github.marmer.testutils.generators.beanmatcher;

import org.hamcrest.Matcher;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.nio.file.Path;

import java.util.Arrays;

import static org.junit.Assert.*;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MatcherFileGeneratorTest {
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
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
	private Matcher<SamplePojo1> samplePojo1Matcher;
	@Mock
	private Matcher<SamplePojo2> samplePojo2Matcher;

	@Test
	public void testGenerateHelperForClassesAllIn_PackageGiven_ClassesOfPackageShouldBeUsedToGenerateMatchersPerClassFound()
		throws Exception {

		// Preparation
		when(potentialPojoClassFinder.findClasses(packageName)).thenReturn(Arrays.asList(SamplePojo1.class,
				SamplePojo2.class));

		// Execution
		classUnderTest.generateHelperForClassesAllIn(packageName);

		// Assertion
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo1.class, outputDir);
		verify(hasPropertyMatcherClassGenerator).generateMatcherFor(SamplePojo2.class, outputDir);
	}

	private static class SamplePojo1 { }

	private static class SamplePojo2 { }

}
