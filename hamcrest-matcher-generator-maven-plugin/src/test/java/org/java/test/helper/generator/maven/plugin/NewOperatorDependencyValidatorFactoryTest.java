package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import org.junit.Test;

import org.mockito.InjectMocks;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;


public class NewOperatorDependencyValidatorFactoryTest {
	@InjectMocks
	private NewOperatorDependencyValidatorFactory classUnderTest = new NewOperatorDependencyValidatorFactory();

	@Test
	public void testCreateBy_AllParametersGiven_ShouldReturnAnInstance() throws Exception {

		// Preparation
		final MavenProject mavenProject = mock(MavenProject.class);
		final ProjectDependenciesResolver projectDependenciesResolver = mock(ProjectDependenciesResolver.class);
		final MavenSession mavenSession = mock(MavenSession.class);

		// Execution
		final DependencyValidator instance = classUnderTest.createBy(mavenProject, projectDependenciesResolver,
				mavenSession);

		// Expectation
		assertThat(instance, is(instanceOf(DependencyValidator.class)));
	}
}
