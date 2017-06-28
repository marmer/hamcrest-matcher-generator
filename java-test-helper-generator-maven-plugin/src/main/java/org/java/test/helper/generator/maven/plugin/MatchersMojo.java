package org.java.test.helper.generator.maven.plugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetFactoryMethodFacadeGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.BeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.CommonsJciJavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.IntrospektorBeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.ReflectionPotentialBeanClassFinder;

import org.apache.maven.artifact.DependencyResolutionRequiredException;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This goal is used to generate matchers based on classes in given packages or qualified names. Not
 * existing packages or nor processable classes are simply ignored.
 *
 * <p>The generated classes will be in the same package as the base of the classes.</p>
 *
 * @author  marmer
 * @date    21.06.2017
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
	private static final String FACADE_PACKAGE = "io.github.marmer.testutils";

	private static final String FACADE_NAME = "BeanPropertyMatchers";

	/** The Project itself. */
	@Parameter(
		defaultValue = "${project}",
		readonly = true
	)
	private MavenProject project;

	/** Packages of classes or qualified class names used to generate matchers for. */
	@Parameter(
		required = true,
		defaultValue = "${project.groupId}"
	)
	private String[] matcherSources;

	/** Location where to put the generated sources to. */
	@Parameter(
		required = true,
		defaultValue = "${project.build.directory}/generated-test-sources"
	)
	private File outputDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		final ClassLoader classLoader;
		try {
			classLoader = new URLClassLoader(toUrls(toPath(project.getTestClasspathElements())),
					getClass().getClassLoader());
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoFailureException("Cannot access Dependencies", e);
		}

		final ReflectionPotentialBeanClassFinder potentialPojoClassFinder = new ReflectionPotentialBeanClassFinder(
				classLoader);
		final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor();
		final JavaPoetHasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator =
			new JavaPoetHasPropertyMatcherClassGenerator(propertyExtractor,
				outputDir.toPath());
		final MatcherFileGenerator matcherFileGenerator = new MatcherFileGenerator(potentialPojoClassFinder,
				hasPropertyMatcherClassGenerator,
				new JavaPoetFactoryMethodFacadeGenerator(outputDir.toPath(), FACADE_PACKAGE,
					FACADE_NAME),
				new CommonsJciJavaFileClassLoader(outputDir.toPath(), classLoader));

		try {
			matcherFileGenerator.generateHelperForClassesAllIn(matcherSources);
		} catch (IOException e) {
			throw new MojoFailureException("Something went wront", e);
		}
	}

	private URL[] toUrls(final List<Path> classpathRootPaths) {
		final List<URL> urls = new LinkedList<>();

		for (final Path path : classpathRootPaths) {
			try {
				urls.add(path.toUri().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException("This shuold not happen", e);
			}
		}

		return urls.toArray(new URL[urls.size()]);
	}

	private List<Path> toPath(final List<String> stringPaths) {
		return stringPaths.stream().map(Paths::get).collect(Collectors.toList());
	}
}
