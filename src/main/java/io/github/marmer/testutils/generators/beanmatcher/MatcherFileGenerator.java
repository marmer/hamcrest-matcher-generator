package io.github.marmer.testutils.generators.beanmatcher;

import org.apache.commons.lang3.ArrayUtils;

import io.github.marmer.testutils.generators.beanmatcher.generation.FactoryMethodFacadeGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;

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
public class MatcherFileGenerator implements TestHelperMatcherGenerator {
	private final PotentialPojoClassFinder potentialPojoClassFinder;
	private final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator;

	private final FactoryMethodFacadeGenerator factoryMethodFacadeGenerator;
	private JavaFileClassLoader javaFileClassLoader;

	public MatcherFileGenerator(final PotentialPojoClassFinder potentialPojoClassFinder,
		final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator,
		final FactoryMethodFacadeGenerator factoryMethodFacadeGenerator,
		final JavaFileClassLoader javaFileClassLoader) {
		this.potentialPojoClassFinder = potentialPojoClassFinder;
		this.hasPropertyMatcherClassGenerator = hasPropertyMatcherClassGenerator;
		this.factoryMethodFacadeGenerator = factoryMethodFacadeGenerator;
		this.javaFileClassLoader = javaFileClassLoader;
	}

	@Override
	public void generateHelperForClassesAllIn(final String... packageOrQualifiedClassNames) throws IOException {
		final List<Class<?>> potentialPojoClasses = potentialPojoClassFinder.findClasses(packageOrQualifiedClassNames);

		final List<Path> generatedMatcherPaths = generateMatchersFor(potentialPojoClasses,
				packageOrQualifiedClassNames);
		final List<Class<?>> generatedMatcherClasses = javaFileClassLoader.load(generatedMatcherPaths);
		factoryMethodFacadeGenerator.generateFacadeFor(generatedMatcherClasses);
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
