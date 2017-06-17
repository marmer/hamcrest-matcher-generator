package io.github.marmer.testutils.samplepojos;

import java.util.List;


/**
 * The Interface Extracts properties of a bean.
 *
 * @author   $author$
 * @version  $Revision$, $Date$
 * @date     17.06.2017
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
