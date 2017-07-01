package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;


/**
 * The Class MatcherFileGenerator.
 *
 * @author  marmer
 * @date    20.06.2017
 */
@CommonsLog
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
		if (log.isDebugEnabled()) {
			log.debug("Classes found:");
			potentialPojoClasses.stream().forEach(log::debug);
		}

		final List<Path> generatedMatcherPaths = generateMatchersFor(potentialPojoClasses,
				packageOrQualifiedClassNames);
		javaFileClassLoader.load(generatedMatcherPaths);
	}

	private List<Path> generateMatchersFor(final List<Class<?>> potentialPojoClasses,
		final String... packageOrQualifiedClassNames) throws IOException {
		final List<Path> generatedMatcherPaths = new ArrayList<>(ArrayUtils.getLength(packageOrQualifiedClassNames));
		for (final Class<?> potentialPojoClass : potentialPojoClasses) {
			generatedMatcherPaths.add(hasPropertyMatcherClassGenerator.generateMatcherFor(potentialPojoClass));
		}
		return generatedMatcherPaths;
	}
}
