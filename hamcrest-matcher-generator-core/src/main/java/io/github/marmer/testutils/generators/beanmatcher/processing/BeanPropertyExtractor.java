package io.github.marmer.testutils.generators.beanmatcher.processing;

import java.util.List;


/**
 * The Interface Extracts properties of a bean.
 *
 * @author   marmer
 * @since     17.06.2017
 */
public interface BeanPropertyExtractor {

	/**
	 * Gets the bean properties of the given type.
	 *
	 * @param   type  the type
	 *
	 * @return  the properties of
	 */
	List<BeanProperty> getPropertiesOf(Class<?> type);
}
