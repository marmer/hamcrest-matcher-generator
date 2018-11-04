package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory.MatcherGeneratorConfiguration;
import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy.Name;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import java.io.File;
import java.io.IOException;

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

	private ClassLoaderFactory classloaderFactory = new ByClasspathStringPathElementURLClassLoaderFactory(
			getClass().getClassLoader());
	private MatcherGeneratorFactory matcherGeneratorFactory = new NewOperatorMatcherGeneratorFactory();
	private DependencyValidatorFactory dependencyValidatorFactory = new NewOperatorDependencyValidatorFactory();
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
	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;
	/**
	 * Determines whether to generate matchers for all classes configured with {@link
	 * #matcherSources} or only the ones with properties (which have getters). The matchers
	 * generated for classes without properties can only be used to match the type.
	 */
	@Parameter(
		required = false,
		defaultValue = "false"
	)
	private boolean ignoreClassesWithoutProperties;
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
	/**
	 * Possibility to generate matchers for Interfaces as well.
	 */
	@Parameter(
			required = true,
			defaultValue = "false",
			property = "allowInterfaces"
	)
	private boolean allowInterfaces;
	/**
	 * Strategy of how to name generated classes.
	 * <p>
	 * Avoid PLAIN, if you generate matchers for inner classes.
	 *
	 * <ul>
	 *     <li>
	 *         <em>PACKAGE</em>
	 *         The matcher is named exactly as the class it is generated for but added to a package which looks pretty much
	 *         like the full qualified classname it was generated for.
	 *         <p>
	 *         E.g. This constellation
	 *         some.package.OuterClass1.InnerClass
	 *         some.package.OuterClass2.InnerClass
	 *         </p>
	 *         <p>
	 *         ...would produce the following machers
	 *         </p>
	 *         <p>
	 *         some.package.OuterClass1Matcher
	 *         some.package.outerclass1.InnerClassMatcher
	 *         some.package.OuterClass2Matcher
	 *         some.package.outerclass2.InnerClassMatcher
	 *         </p>
	 *     </li>
	 *     <li>
	 *         <em>PARENT</em>
	 *         The generated matcher contains the typename of the enclosing type(s) and lies in their package.
	 *         <p>
	 *         E.g. This constellation
	 *         some.package.OuterClass1.InnerClass
	 *         some.package.OuterClass2.InnerClass.InnerInnerClass
	 *         </p>
	 *         <p>
	 *         ...would produce the following machers
	 *         </p>
	 *         <p>
	 *         some.package.OuterClass1Matcher
	 *         some.package.OuterClass1$InnerClassMatcher
	 *         some.package.OuterClass2Matcher
	 *         some.package.OuterClass2$InnerClassMatcher
	 *         some.package.OuterClass2$InnerClass$InnerInnerClassMatcher
	 *
	 *         </p>
	 *     </li>
	 *     <li>
	 *         <em>PLAIN</em>
	 *         The matcher is named exactly as the class it is generate for with exactly the package it lies in.
	 *         <p>
	 *         This strategy can be used if no matchers are generated for inner classes or if there is always only one
	 *         inner class in a package with a name.
	 *         If you have two classes with each an inner class an both inner classes are named the same, only one wins.
	 *         </p>
	 *         <p>
	 *         E.g. This constellation
	 *         some.package.OuterClass1.InnerClass
	 *         some.package.OuterClass2.InnerClass
	 *         </p>
	 *         <p>
	 *         ...would produce the following Matchers:
	 *         some.package.OuterClass1Matcher
	 *         some.package.OuterClass2Matcher
	 *         some.package.InnerClassMatcher
	 *     </li>
	 *
	 * </ul>
	 */
	@Parameter(
			required = true,
			defaultValue = "PACKAGE",
			property = "namingStrategy"
	)
	private Name namingStrategy;

	@Override
	public void execute() throws MojoFailureException {
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
			matcherFileGenerator.generateHelperForClassesAllIn(matcherSources)
					.forEach(generatedType -> getLog().info("Generated: " + generatedType));
		} catch (final IOException e) {
			throw new MojoFailureException("Error on matcher generation", e);
		}
	}

	private MatcherGenerator prepareMatcherGenerator() throws MojoFailureException {
		final ClassLoader classLoader;

		try {
			classLoader = classloaderFactory.creatClassloader(project.getTestClasspathElements());
		} catch (final DependencyResolutionRequiredException e) {
			throw new MojoFailureException("Cannot access Dependencies", e);
		}

		return matcherGeneratorFactory.createBy(
				MatcherGeneratorConfiguration.builder()
						.classLoader(classLoader)
						.outputPath(outputDir.toPath())
						.ignoreClassesWithoutProperties(ignoreClassesWithoutProperties)
						.allowInterfaces(allowInterfaces)
						.namingStrategy(namingStrategy).build());
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
