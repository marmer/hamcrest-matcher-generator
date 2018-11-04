package io.github.marmer.testutils.generators.beanmatcher;

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
		private final NamingStrategy namingStrategy;

		/**
		 * Strategy of how to name generated matchers.
		 */
		public enum NamingStrategy {
			/**
			 * The matcher is named exactly as the class it is generate for with exactly the package it lies in.
			 * <p>
			 * This strategy can be used if no matchers are generated for inner classes or if there is always only one inner class in a package with a name.
			 * If you have two classes with each an inner class an both inner classes are named the same, only one wins.
			 * <p>
			 * E.g. This constellation
			 * OuterClass1.InnerClass
			 * OuterClass2.InnerClass
			 * <p>
			 * ...would produce OuterClass1Matcher, OuterClass2Matcher and InnerClassMatcher but you never know what the InnerClassMatcher is for.
			 */
			PLAIN
		}
	}
}
