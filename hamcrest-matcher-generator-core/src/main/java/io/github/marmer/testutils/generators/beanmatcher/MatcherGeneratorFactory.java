package io.github.marmer.testutils.generators.beanmatcher;

import io.github.marmer.testutils.generators.beanmatcher.generation.MatcherNamingStrategy;
import lombok.Builder;
import lombok.Generated;
import lombok.Value;

import java.nio.file.Path;


/**
 * A factory for creating MatcherGenerator objects.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public interface MatcherGeneratorFactory {

	/**
	 * Creates a new MatcherGenerator object.
	 *
	 * @param   matcherGeneratorConfiguration  The configuration to use for generation.
	 *
	 * @return  the matcher generator
	 */
	MatcherGenerator createBy(final MatcherGeneratorConfiguration matcherGeneratorConfiguration);

	@Value
	@Builder
	@Generated
	class MatcherGeneratorConfiguration {
		private final ClassLoader classLoader;
		private final Path outputPath;
		private final boolean ignoreClassesWithoutProperties;
		private final boolean allowInterfaces;
		private final MatcherNamingStrategy.Name namingStrategy;
	}
}
