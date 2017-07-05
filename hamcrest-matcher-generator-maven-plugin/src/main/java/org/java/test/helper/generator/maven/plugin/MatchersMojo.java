package org.java.test.helper.generator.maven.plugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;

import org.apache.commons.lang3.ArrayUtils;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import java.io.File;
import java.io.IOException;


/**
 * This goal is used to generate matchers based on classes in given packages or qualified names. Not
 * existing packages or nor processable classes are simply ignored.
 *
 * <p>The generated classes will be in the same package as the base of the classes.</p>
 *
 * @author  marmer
 * @since   21.06.2017
 */
@Mojo(
	name = "matchers",
	defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
	threadSafe = false,
	requiresDependencyResolution = ResolutionScope.COMPILE,
	requiresProject = true
)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class MatchersMojo extends AbstractMojo {

	/** The Project itself. */
	@Parameter(
		defaultValue = "${project}",
		readonly = true
	)
	private MavenProject project;

	/** Packages of classes or qualified class names used to generate matchers for. */
	@Parameter(required = true)
	private String[] matcherSources;

	/** Location where to put the generated sources to. */
	@Parameter(
		required = true,
		defaultValue = "${project.build.directory}/generated-test-sources"
	)
	private File outputDir;

	private ClassLoaderFactory classloaderFactory = new ByClasspathStringPathElementURLClassLoaderFactory(getClass()
			.getClassLoader());

	private MatcherGeneratorFactory matcherGeneratorFactory = new NewOperatorMatcherGeneratorFactory();

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Parameter(
		defaultValue = "${session}",
		readonly = true
	)
	private MavenSession mavenSession;

	/**
	 * The build will break by default (with an appropriate error message), if this plugin is not
	 * able to find all the needed dependencies. With this flag set to true, you can enforce the
	 * generation without the needed dependencies, but you should know what you do.
	 */
	@Parameter(
		required = true,
		defaultValue = "false",
		property = "allowMissingHamcrestDependency"
	)
	private boolean allowMissingHamcrestDependency;

	private DependencyValidatorFactory dependencyValidatorFactory = new NewOperatorDependencyValidatorFactory();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		validateNeededDependencies();
		validateMatcherSourcesSet();
		generateMatchers();
		addGeneratedOutputsPathsToBuildLifecycle();
	}

	private void addGeneratedOutputsPathsToBuildLifecycle() {
		project.addTestCompileSourceRoot(outputDir.toString());
	}

	private void generateMatchers() throws MojoFailureException {
		final MatcherGenerator matcherFileGenerator = prepareMatcherGenerator();

		try {
			matcherFileGenerator.generateHelperForClassesAllIn(matcherSources);
		} catch (IOException e) {
			throw new MojoFailureException("Error on matcher generation", e);
		}
	}

	private MatcherGenerator prepareMatcherGenerator() throws MojoFailureException {
		final ClassLoader classLoader;

		try {
			classLoader = classloaderFactory.creatClassloader(project.getTestClasspathElements());
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoFailureException("Cannot access Dependencies", e);
		}

		return matcherGeneratorFactory.createBy(classLoader,
				outputDir.toPath());
	}

	private void validateNeededDependencies() throws MojoFailureException {
		if (!allowMissingHamcrestDependency) {
			final DependencyValidator dependencyValidator = dependencyValidatorFactory.createBy(project,
					projectDependenciesResolver, mavenSession);
			dependencyValidator.validateProjectHasNeededDependencies();
		}
	}

	private void validateMatcherSourcesSet() throws MojoFailureException {
		if (ArrayUtils.isEmpty(matcherSources)) {
			throw new MojoFailureException(
				"Missing MatcherSources. You should at least add one Package or qualified class name in matcherSources");
		}
	}
}
