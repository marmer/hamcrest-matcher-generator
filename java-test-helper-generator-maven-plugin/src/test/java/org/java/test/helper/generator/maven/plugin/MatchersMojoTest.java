package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.ReflectionUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MatchersMojoTest {
	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@InjectMocks
	private MatchersMojo classUnderTest;

	@Mock
	private MavenProject mavenProject;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Spy
	private File outputDir = new File("outputDir");

	private String[] matcherSources = new String[] {
		"some.package"
	};

	@Before
	public void setUp() throws Exception {
		ReflectionUtils.setVariableValueInObject(classUnderTest, "matcherSources", matcherSources);
	}

	@Test
	public void testExecute_MavenClassloaderIsNotAbleToResolveRequiredDependencies_ShouldBreakTheBuild()
		throws Exception {

		// Preparation
		final DependencyResolutionRequiredException dependencyResolutionRequiredException = mock(
				DependencyResolutionRequiredException.class);
		when(mavenProject.getTestClasspathElements()).thenThrow(dependencyResolutionRequiredException);

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(equalTo("Cannot access Dependencies")));
		exception.expectCause(is(dependencyResolutionRequiredException));

		// Execution
		classUnderTest.execute();
	}

	@Test
	public void testExecute_OutputDirGiven_OutputDirPathShouldBeUsedAsTestSource() throws Exception {
		// Preparation

		// Execution
		classUnderTest.execute();

		// Assertion
		verify(mavenProject).addTestCompileSourceRoot(outputDir.toString());
	}

	@Test
	public void testExecute_NoPackagesOrQualifiedClassnamesGivenForExecution_ShouldStopTheBuild() throws Exception {

		// Preparation
		ReflectionUtils.setVariableValueInObject(classUnderTest, "matcherSources", new String[0]);

		// Assertion
		exception.expect(MojoFailureException.class);
		exception.expectMessage(
			"Missing MatcherSources. You should at least add one Package or qualified class name in matcherSources");

		// Execution
		classUnderTest.execute();
	}
}
