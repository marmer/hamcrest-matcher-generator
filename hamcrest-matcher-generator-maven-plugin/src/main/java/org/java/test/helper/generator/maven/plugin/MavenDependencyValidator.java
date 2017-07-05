package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Validator of Maven dependencies.
 *
 * @author  mertinat
 * @since   05.07.2017
 */
public class MavenDependencyValidator implements DependencyValidator {

	private MavenProject mavenProject;
	private ProjectDependenciesResolver projectDependenciesResolver;
	private MavenSession mavenSession;

	/**
	 * Instantiates a new maven dependency validator.
	 *
	 * @param  mavenProject                 the maven project
	 * @param  projectDependenciesResolver  the project dependencies resolver
	 * @param  mavenSession                 the maven session
	 */
	public MavenDependencyValidator(final MavenProject mavenProject,
		final ProjectDependenciesResolver projectDependenciesResolver, final MavenSession mavenSession) {
		this.mavenProject = mavenProject;
		this.projectDependenciesResolver = projectDependenciesResolver;
		this.mavenSession = mavenSession;
	}

	/* (non-Javadoc)
	 * @see org.java.test.helper.generator.maven.plugin.DependencyValidator#validateProjectHasNeededDependencies()
	 */
	@Override
	public void validateProjectHasNeededDependencies() throws MojoFailureException {
		boolean foundHamcrestDependency = false;
		boolean foundOwnDependency = false;
		for (final Artifact artifact : getDependencyArtifacts()) {
			foundHamcrestDependency |= isHamcrestDependency(artifact);
			foundOwnDependency |= isOwnDependency(artifact);
		}
		if (!foundHamcrestDependency) {
			throw new MojoFailureException(
				"Cannot find any hamcrest dependency. Make sure you have added directly or indiredtly org.hamcrest:java-hamcrest in any version or org.hamcrest:hamcrest-all at version 1.2 accessible from test scope. Otherwise the generated classes may not compile work.");
		}
		if (!foundOwnDependency) {
			throw new MojoFailureException(
				"Cannot find any hamcrest-matcher-generator-dependencies. Make sure you have added it directly or indiredtly io.github.marmer.testutils.hamcrest-matcher-generator-dependencies in the same version as the related plugin. Otherwise the generated classes may not compile work");
		}
	}

	private boolean isOwnDependency(final Artifact artifact) {
		return "io.github.marmer.testutils".equals(artifact.getGroupId()) &&
			"hamcrest-matcher-generator-dependencies".equals(artifact.getArtifactId());
	}

	private boolean isHamcrestDependency(final Artifact artifact) {
		return "org.hamcrest".equals(artifact.getGroupId()) &&
			("java-hamcrest".equals(artifact.getArtifactId()) || "hamcrest-all".equals(artifact.getArtifactId()));
	}

	private List<Artifact> getDependencyArtifacts() throws MojoFailureException {
		final DependencyResolutionResult resolutionResult = resolveDependencies();
		final List<Dependency> dependencies = resolutionResult.getDependencies();
		return dependencies.stream().map(Dependency::getArtifact).collect(Collectors
				.toList());
	}

	private DependencyResolutionResult resolveDependencies() throws MojoFailureException {
		try {
			return projectDependenciesResolver.resolve(
					new DefaultDependencyResolutionRequest(mavenProject,
						mavenSession.getRepositorySession()));
		} catch (DependencyResolutionException e) {
			throw new MojoFailureException("Cannot resolve dependencies", e);
		}
	}

}
