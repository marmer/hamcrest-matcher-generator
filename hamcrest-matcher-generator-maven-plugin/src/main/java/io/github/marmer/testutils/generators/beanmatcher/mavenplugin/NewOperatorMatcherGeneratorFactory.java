package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.BeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.CommonsJciJavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.IntrospektorBeanPropertyExtractor;
import io.github.marmer.testutils.generators.beanmatcher.processing.JavaFileClassLoader;
import io.github.marmer.testutils.generators.beanmatcher.processing.PotentialPojoClassFinder;
import io.github.marmer.testutils.generators.beanmatcher.processing.ReflectionPotentialBeanClassFinder;


/**
 * A factory for creating {@link MatcherGenerator} objects by connecting them manually.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public class NewOperatorMatcherGeneratorFactory implements MatcherGeneratorFactory {

	@Override
	public MatcherGenerator createBy(final MatcherGeneratorConfiguration matcherGeneratorConfiguration) {
		final PotentialPojoClassFinder potentialPojoClassFinder = new ReflectionPotentialBeanClassFinder(
				matcherGeneratorConfiguration.getClassLoader());
		final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor();
		final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator =
			new JavaPoetHasPropertyMatcherClassGenerator(
				propertyExtractor, matcherGeneratorConfiguration.getOutputPath());
		final JavaFileClassLoader javaFileClassLoader = new CommonsJciJavaFileClassLoader(
				matcherGeneratorConfiguration.getOutputPath(),
				matcherGeneratorConfiguration.getClassLoader());
		return new MatcherFileGenerator(potentialPojoClassFinder,
				hasPropertyMatcherClassGenerator,
				javaFileClassLoader);
	}
}
