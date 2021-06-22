package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This interface keeps information about the classes generated classes are based of.
 *
 * @author marmer
 * @since 21.06.2017
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BasedOn {
    // TODO: marmer 22.06.2021 Check alternative!

    /**
     * Tha generation base.
     *
     * @return the class
     */
    Class<?> value();
}
