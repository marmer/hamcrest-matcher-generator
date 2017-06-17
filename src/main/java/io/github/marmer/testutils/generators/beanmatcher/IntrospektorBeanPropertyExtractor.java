package io.github.marmer.testutils.generators.beanmatcher;

import lombok.extern.apachecommons.CommonsLog;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@CommonsLog
public class IntrospektorBeanPropertyExtractor implements BeanPropertyExtractor {

	@Override
	public List<BeanProperty> getPropertiesOf(final Class<?> type) {
		if (type != null) {
			try {
				return Arrays.stream(propertyDescriptorsOf(type)).map(descriptor ->
							new BeanProperty(descriptor.getName()))
					.collect(
						Collectors.toList());
			} catch (IntrospectionException e) {
				log.error("Failed to read properties of " + type, e);
			}
		}
		return Collections.emptyList();
	}

	private PropertyDescriptor[] propertyDescriptorsOf(final Class<?> type) throws IntrospectionException {
		final BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
		return beanInfo.getPropertyDescriptors();
	}

}
