package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;


/**
 * A factory for creating NewOperatorDependencyValidator objects.
 *
 * @author  marmer
 * @since   05.07.2017
 */
public class NewOperatorDependencyValidatorFactory implements DependencyValidatorFactory {

	/* (non-Javadoc)
	 * @see org.java.test.helper.generator.maven.plugin.DependencyValidatorFactory#createBy(org.apache.maven.project.MavenProject, org.apache.maven.project.ProjectDependenciesResolver, org.apache.maven.execution.MavenSession)
	 */
	@Override
	public DependencyValidator createBy(final MavenProject mavenProject,
		final ProjectDependenciesResolver projectDependenciesResolver, final MavenSession mavenSession) {
		return new MavenDependencyValidator(mavenProject, projectDependenciesResolver, mavenSession);
	}

}
