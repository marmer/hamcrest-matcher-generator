package io.github.marmer.testutils.generators.beanmatcher;

import java.util.List;


/**
 * Generator for a facade class with factory methods for a collection of types.
 *
 * @author  marmer
 * @date    20.06.2017
 */
public interface FactoryMethodFacadeGenerator {

	/**
	 * Generates a facade.
	 *
	 * @param  classesToGenerateFacadeFor  the classes to generate a facade for
	 */
	void generateFacadeFor(List<Class<?>> classesToGenerateFacadeFor);
}
