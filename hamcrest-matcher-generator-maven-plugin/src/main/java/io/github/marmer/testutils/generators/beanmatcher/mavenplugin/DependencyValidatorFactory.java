package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;


/**
 * A factory for creating {@link DependencyValidator} objects.
 *
 * @author  mertinat
 * @since   05.07.2017
 */
public interface DependencyValidatorFactory {

	/**
	 * Creates a new DependencyValidator object.
	 *
	 * @param   mavenProject                 the maven project
	 * @param   projectDependenciesResolver  the project dependencies resolver
	 * @param   mavenSession                 the maven session
	 *
	 * @return  the dependency validator
	 */
	DependencyValidator createBy(MavenProject mavenProject, ProjectDependenciesResolver projectDependenciesResolver,
		MavenSession mavenSession);

}
