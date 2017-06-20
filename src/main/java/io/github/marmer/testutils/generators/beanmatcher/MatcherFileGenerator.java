package io.github.marmer.testutils.generators.beanmatcher;

import java.io.IOException;

import java.nio.file.Path;

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

	private final Path outputDir;

	public MatcherFileGenerator(final PotentialPojoClassFinder potentialPojoClassFinder, final Path outputDir,
		final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator) {
		this.potentialPojoClassFinder = potentialPojoClassFinder;
		this.hasPropertyMatcherClassGenerator = hasPropertyMatcherClassGenerator;
		this.outputDir = outputDir;
	}

	@Override
	public void generateHelperForClassesAllIn(final String... packageOrQualifiedClassNames) throws IOException {
		final List<Class<?>> potentialPojoClasses = potentialPojoClassFinder.findClasses(packageOrQualifiedClassNames);
		for (final Class<?> potentialPojoClass : potentialPojoClasses) {
			hasPropertyMatcherClassGenerator.generateMatcherFor(potentialPojoClass, outputDir);
		}
	}
}
