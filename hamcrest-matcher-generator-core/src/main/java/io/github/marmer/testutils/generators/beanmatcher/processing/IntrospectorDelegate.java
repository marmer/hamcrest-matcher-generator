package io.github.marmer.testutils.generators.beanmatcher.processing;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;


/**
 * Class is used for delegation to {@link Introspector}'s static methods to be able to test hard to
 * producable errors as well.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public class IntrospectorDelegate {

	/**
	 * Gets the bean info.
	 *
	 * @param   any  the any
	 *
	 * @return  the bean info
	 *
	 * @throws  IntrospectionException  the introspection exception
	 */
	public BeanInfo getBeanInfo(final Class<?> any) throws IntrospectionException {
		return Introspector.getBeanInfo(any, Object.class);
	}

}
