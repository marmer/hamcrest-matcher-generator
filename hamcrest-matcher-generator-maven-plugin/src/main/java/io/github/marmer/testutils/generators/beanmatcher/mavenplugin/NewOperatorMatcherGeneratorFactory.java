package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory;
import io.github.marmer.testutils.generators.beanmatcher.generation.HasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.generation.JavaPoetHasPropertyMatcherClassGenerator;
import io.github.marmer.testutils.generators.beanmatcher.processing.*;


/**
 * A factory for creating {@link MatcherGenerator} objects by connecting them manually.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public class NewOperatorMatcherGeneratorFactory implements MatcherGeneratorFactory {
	private MatcherNamingStrategyFactory matcherNamingStrategyFactory = new MatcherNamingStrategyFactory();

	@Override
	public MatcherGenerator createBy(final MatcherGeneratorConfiguration matcherGeneratorConfiguration) {
		final BeanPropertyExtractor propertyExtractor = new IntrospektorBeanPropertyExtractor(new IntrospectorDelegate(), matcherGeneratorConfiguration.getLog());
		final PotentialPojoClassFinder potentialPojoClassFinder = new ReflectionPotentialBeanClassFinder(
				propertyExtractor,
				matcherGeneratorConfiguration.isIgnoreClassesWithoutProperties(),
				matcherGeneratorConfiguration.isAllowInterfaces(),
				matcherGeneratorConfiguration.getClassLoader());
		final HasPropertyMatcherClassGenerator hasPropertyMatcherClassGenerator =
				new JavaPoetHasPropertyMatcherClassGenerator(
						propertyExtractor,
						matcherGeneratorConfiguration.getOutputPath(),
						matcherNamingStrategyFactory.strategyFor(matcherGeneratorConfiguration));
		return new MatcherFileGenerator(potentialPojoClassFinder,
				hasPropertyMatcherClassGenerator,
				new JavaInternalIllegalClassFilter(),
				matcherGeneratorConfiguration.getLog());
	}
}
