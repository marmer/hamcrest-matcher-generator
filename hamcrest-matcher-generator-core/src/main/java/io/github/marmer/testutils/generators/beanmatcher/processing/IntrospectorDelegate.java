package io.github.marmer.testutils.generators.beanmatcher.processing;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;


public class IntrospectorDelegate {

	public BeanInfo getBeanInfo(final Class<?> any) throws IntrospectionException {
		return Introspector.getBeanInfo(any, Object.class);
	}

}
