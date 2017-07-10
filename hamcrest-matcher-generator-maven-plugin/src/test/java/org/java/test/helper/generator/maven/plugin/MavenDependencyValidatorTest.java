package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import org.powermock.api.mockito.PowerMockito;

import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Dependency.class)
public class MavenDependencyValidatorTest {
	private static final String MISSING_HAMCREST_DEPENDENCY_ERROR_MESSAGE =
		"Cannot find any hamcrest dependency. Make sure you have added directly or indiredtly org.hamcrest:java-hamcrest in any version or org.hamcrest:hamcrest-all at version 1.2 accessible from test scope. Otherwise the generated classes may not compile work.";

	private static final String MISSING_OWN_DEPENDENCY_ERROR_MESSAGE =
		"Cannot find any hamcrest-matcher-generator-dependencies. Make sure you have added it directly or indiredtly io.github.marmer.testutils.hamcrest-matcher-generator-dependencies in the same version as the related plugin. Otherwise the generated classes may not compile work";

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@InjectMocks
	private MavenDependencyValidator classUnderTest;

	@Mock
	private MavenProject mavenProject;
	@Mock
	private ProjectDependenciesResolver projectDependenciesResolver;
	@Mock
	private MavenSession mavenSession;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private Dependency ownDependency = dependency("io.github.marmer.testutils",
			"hamcrest-matcher-generator-dependencies");

	private Dependency oldHamcrestDependency = dependency("org.hamcrest", "hamcrest-all");

	private Dependency newHamcrestDependency = dependency("org.hamcrest", "java-hamcrest");

	@Test
	public void testValidateProjectHasNeededDependencies_CannotFindOwnDependencyButItsGroupId_ExceptionWithAnAppropriateMessageIsThrown()
		throws Exception {

		// Preparation
		final Dependency dependencyWithOwnGroupId = dependency("io.github.marmer.testutils",
				"someUnexpectedArtifactId");
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();
		final List<Dependency> allRelevantDependencies = allRelevantDependenciesExcept(
				ownDependency);
		allRelevantDependencies.add(dependencyWithOwnGroupId);
		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependencies);

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(
				equalTo(
					MISSING_OWN_DEPENDENCY_ERROR_MESSAGE)));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();
	}

	@Test
	public void testValidateProjectHasNeededDependencies_CannotFindAnyHamcrestDependencyButItsGroupId_ExceptionWithAnAppropriateMessageIsThrown()
		throws Exception {

		// Preparation
		final Dependency dependencyWithHamcrestGroupId = dependency("org.hamcrest", "someUnexpectedArtifactId");
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();
		final List<Dependency> allRelevantDependencies = allRelevantDependenciesExcept(
				newHamcrestDependency, oldHamcrestDependency);
		allRelevantDependencies.add(dependencyWithHamcrestGroupId);
		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependencies);

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(
				equalTo(
					MISSING_HAMCREST_DEPENDENCY_ERROR_MESSAGE)));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();
	}

	@Test
	public void testValidateProjectHasNeededDependencies_CannotFindAnyHamcrestDependency_ExceptionWithAnAppropriateMessageIsThrown()
		throws Exception {

		// Preparation
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();
		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependenciesExcept(
				newHamcrestDependency, oldHamcrestDependency));

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(
				equalTo(
					MISSING_HAMCREST_DEPENDENCY_ERROR_MESSAGE)));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();
	}

	@Test
	public void testValidateProjectHasNeededDependencies_CannotFindOwnDependency_ExceptionWithAnAppropriateMessageIsThrown()
		throws Exception {

		// Preparation
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();

		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependenciesExcept(
				ownDependency));

		// Expectation
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(
				equalTo(
					MISSING_OWN_DEPENDENCY_ERROR_MESSAGE)));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();
	}

	@Test
	public void testValidateProjectHasNeededDependencies_AllDependenciesFound_NoExceptionIsThrown() throws Exception {

		// Preparation
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();

		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependenciesExcept());

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();

		// Expectation
		// No Exception thrown
	}

	@Test
	public void testValidateProjectHasNeededDependencies_AllDependenciesAndOnlyOldHamcrestDependencyFound_NoExceptionIsThrown()
		throws Exception {

		// Preparation
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();

		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependenciesExcept(
				newHamcrestDependency));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();

		// Expectation
		// No Exception thrown
	}

	@Test
	public void testValidateProjectHasNeededDependencies_AllDependenciesAndOnlyNewHamcrestDependencyFound_NoExceptionIsThrown()
		throws Exception {

		// Preparation
		final DependencyResolutionResult dependencyResolutionResult = prepareDependencyResolutionResult();

		when(dependencyResolutionResult.getDependencies()).thenReturn(allRelevantDependenciesExcept(
				oldHamcrestDependency));

		// Execution
		classUnderTest.validateProjectHasNeededDependencies();

		// Expectation
		// No Exception thrown
	}

	@Test
	public void testValidateProjectHasNeededDependencies_ErrorsOnResolvingRependencies_ShouldBreakTheBuild()
		throws Exception {

		// Vorbereitung
		final RepositorySystemSession repositorySystemSession = mock(RepositorySystemSession.class);
		when(mavenSession.getRepositorySession()).thenReturn(repositorySystemSession);
		when(projectDependenciesResolver.resolve(
				any(DefaultDependencyResolutionRequest.class))).thenThrow(DependencyResolutionException.class);

		// Prüfung
		exception.expect(MojoFailureException.class);
		exception.expectMessage(is(equalTo("Cannot resolve dependencies")));
		exception.expectCause(is(instanceOf(DependencyResolutionException.class)));

		// Ausführung
		classUnderTest.validateProjectHasNeededDependencies();

	}

	private DependencyResolutionResult prepareDependencyResolutionResult() throws DependencyResolutionException {
		final DependencyResolutionResult dependencyResolutionResult = mock(DependencyResolutionResult.class);
		final RepositorySystemSession repositorySystemSession = mock(RepositorySystemSession.class);
		when(mavenSession.getRepositorySession()).thenReturn(repositorySystemSession);
		when(projectDependenciesResolver.resolve(
				any(DefaultDependencyResolutionRequest.class))).thenReturn(
			dependencyResolutionResult);
		return dependencyResolutionResult;
	}

	private List<Dependency> allRelevantDependenciesExcept(final Dependency... excludes) {
		final ArrayList<Dependency> allRelevantDependencies = new ArrayList<>(Arrays.asList(oldHamcrestDependency,
					newHamcrestDependency, ownDependency));
		for (final Dependency exclude : excludes) {
			allRelevantDependencies.remove(exclude);
		}
		return allRelevantDependencies;
	}

	private Dependency dependency(final String groupId, final String artifactId) {
		final Dependency dependency = PowerMockito.mock(Dependency.class);
		final Artifact artifact = mock(Artifact.class, groupId + ":" + artifactId);
		when(artifact.getGroupId()).thenReturn(groupId);
		when(artifact.getArtifactId()).thenReturn(artifactId);
		when(dependency.getArtifact()).thenReturn(artifact);
		return dependency;
	}
}
