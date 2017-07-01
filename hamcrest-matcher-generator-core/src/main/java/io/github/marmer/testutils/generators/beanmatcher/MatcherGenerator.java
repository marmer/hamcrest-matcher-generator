package io.github.marmer.testutils.generators.beanmatcher;

import java.io.IOException;


/**
 * Generator for Matchers.
 *
 * @author  marmer
 * @date    20.06.2017
 */
public interface MatcherGenerator {

	/**
	 * Generate helper for all classes under the given Package or the qualified class name given.
	 *
	 * @param   packageOrQualifiedClassNames  the package or qualified class names
	 *
	 * @throws  IOException  Signals that an I/O exception has occurred. TODO should have option to
	 *                       be robust on errors.
	 */
	void generateHelperForClassesAllIn(String... packageOrQualifiedClassNames) throws IOException;
}
