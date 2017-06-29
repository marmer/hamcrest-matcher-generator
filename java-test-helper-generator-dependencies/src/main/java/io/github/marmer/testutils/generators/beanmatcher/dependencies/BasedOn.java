package io.github.marmer.testutils.generators.beanmatcher.dependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This interface keeps information about the classes generated classes are
 * based of.
 *
 * @author marmer
 * @date   21.06.2017
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BasedOn {
    /**
     * Tha generation base.
     *
     * @return the class
     */
    Class<?> value();
}
