package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;


/**
 * The Class MatcherFileGenerator.
 *
 * @author  marmer
 * @since   20.06.2017
 */
public class MatcherFileGenerator implements MatcherGenerator {
	private final PotentialPojoClassFinder potentialPojoClassFinder;
	private final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;

	private JavaFileClassLoader javaFileClassLoader;

	public MatcherFileGenerator(final PotentialPojoClassFinder potentialPojoClassFinder,
		final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator,
		final JavaFileClassLoader javaFileClassLoader) {
		this.potentialPojoClassFinder = potentialPojoClassFinder;
		this.hasPropertyMatcherClassGenerator = hasPropertyMatcherClassGenerator;
		this.javaFileClassLoader = javaFileClassLoader;
	}

	@Override
	public void generateHelperForClassesAllIn(final String... packageOrQualifiedClassNames) throws IOException {
		final List<Class<?>> potentialPojoClasses = potentialPojoClassFinder.findClasses(packageOrQualifiedClassNames);
		final List<Path> generatedMatcherPaths = generateMatchersFor(potentialPojoClasses,
				packageOrQualifiedClassNames);
		javaFileClassLoader.load(generatedMatcherPaths);
	}

	private List<Path> generateMatchersFor(final List<Class<?>> potentialPojoClasses,
		final String... packageOrQualifiedClassNames) throws IOException {
		final List<Path> generatedMatcherPaths = new ArrayList<>(ArrayUtils.getLength(packageOrQualifiedClassNames));
		for (final Class<?> potentialPojoClass : potentialPojoClasses) {
			final Path generateMatcher = hasPropertyMatcherClassGenerator.generateMatcherFor(potentialPojoClass);
			if (generateMatcher != null) {
				generatedMatcherPaths.add(generateMatcher);
			}
		}
		return generatedMatcherPaths;
	}
}
