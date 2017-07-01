package io.github.marmer.testutils.generators.beanmatcher;

import java.nio.file.Path;


/**
 * A factory for creating MatcherGenerator objects.
 *
 * @author  marmer
 * @date    01.07.2017
 */
public interface MatcherGeneratorFactory {

	/**
	 * Creates a new MatcherGenerator object.
	 *
	 * @param   classLoaderToUse  the class loader to use
	 * @param   outputPath        the output path
	 *
	 * @return  the matcher generator
	 */
	MatcherGenerator createBy(ClassLoader classLoaderToUse, Path outputPath);

}
