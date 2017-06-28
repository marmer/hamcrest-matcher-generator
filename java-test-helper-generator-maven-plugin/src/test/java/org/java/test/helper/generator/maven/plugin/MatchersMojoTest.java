package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.mock;
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

}
