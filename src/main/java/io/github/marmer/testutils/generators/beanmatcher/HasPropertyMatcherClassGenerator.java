package io.github.marmer.testutils.generators.beanmatcher;

import org.hamcrest.beans.HasProperty;

import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsInstanceOf;

import java.io.IOException;

import java.nio.file.Path;


/**
 * This class can be used to generate a compile safe matcher for a property bean which acts as a
 * combination of {@link AllOf}, {@link IsInstanceOf} and {@link HasProperty} like:
 *
 * <pre>
    is(allOf(
        instanceOf(Foo.class),
        hasProperty("someProperty", equalTo(42)),
        hasProperty("anotherProperty", greaterThan(42))));
 * </pre>
 *
 * <p>The result can be used as follows.</p>
 *
 * <pre>
    Foo.isFoo().withSomeProperts(equalTo(42))
                            .withAnotherProperty(greaterThan(42));
 * </pre>
 *
 * <p>When you automate the generation before the compiler starts, the compiler will show you all
 * the locations you matched the property. So you don't have to wait and whatch your tests fail.</p>
 *
 * @author  marmer
 * @date    12.06.2017
 */
public interface HasPropertyMatcherClassGenerator {

	/**
	 * Generate matcher for the given type in the given class directory (including its package
	 * folders).
	 *
	 * @param   type  the type
	 *
	 * @return  TODO DOCUMENT ME!
	 *
	 * @throws  IOException  Signals that an I/O exception has occurred.
	 */
	Path generateMatcherFor(Class<?> type) throws IOException;

}
