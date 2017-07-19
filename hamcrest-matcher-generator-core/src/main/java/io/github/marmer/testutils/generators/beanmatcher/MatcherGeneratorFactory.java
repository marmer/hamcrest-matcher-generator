package io.github.marmer.testutils.generators.beanmatcher;

import lombok.Builder;
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
	public static class MatcherGeneratorConfiguration {
		@Builder.Default
		private final ClassLoader classLoader = MatcherGeneratorConfiguration.class.getClassLoader();
		private final Path outputPath;
	}
}
