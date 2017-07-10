package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import org.apache.maven.plugin.MojoFailureException;


/**
 * Validates project dependencies.
 *
 * @author  mertinat
 * @since   05.07.2017
 */
public interface DependencyValidator {

	/**
	 * Validate project has needed dependencies.
	 *
	 * @throws  MojoFailureException  the mojo failure exception
	 */
	void validateProjectHasNeededDependencies() throws MojoFailureException;

}
