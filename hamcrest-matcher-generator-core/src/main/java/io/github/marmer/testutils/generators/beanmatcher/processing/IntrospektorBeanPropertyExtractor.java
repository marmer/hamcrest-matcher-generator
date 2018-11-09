package io.github.marmer.testutils.generators.beanmatcher.processing;

import io.github.marmer.testutils.generators.beanmatcher.Log;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The Class IntrospektorBeanPropertyExtractor.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public class IntrospektorBeanPropertyExtractor implements BeanPropertyExtractor {

	private final Log log;
    private IntrospectorDelegate introspectorDelegate;

	public IntrospektorBeanPropertyExtractor(final IntrospectorDelegate introspectorDelegate, final Log log) {
        this.introspectorDelegate = introspectorDelegate;
		this.log = log;
    }

	@Override
	public List<BeanProperty> getPropertiesOf(final Class<?> type) {
		if (type != null) {
			try {
				return Arrays.stream(propertyDescriptorsOf(type)).map(descriptor ->
							new BeanProperty(descriptor.getName(), descriptor.getPropertyType()))
					.collect(
						Collectors.toList());
            } catch (final IntrospectionException e) {
                // TODO: marmer 08.11.2018 Use new introduced Log
				log.error("Failed to read properties of " + type, e);
			}
		}
		return Collections.emptyList();
	}

	private PropertyDescriptor[] propertyDescriptorsOf(final Class<?> type) throws IntrospectionException {
		final BeanInfo beanInfo = introspectorDelegate.getBeanInfo(type);
		return beanInfo.getPropertyDescriptors();
	}

}
