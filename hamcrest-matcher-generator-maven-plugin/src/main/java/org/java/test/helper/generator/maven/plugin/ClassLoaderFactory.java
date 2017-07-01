package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;

import java.util.List;


/**
 * A factory for creating Classloader objects.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public interface ClassLoaderFactory {

	/**
	 * Creat classloader by Classpath Elements.
	 *
	 * @param   classpathElements  the classpath elements
	 *
	 * @return  the class loader
	 *
	 * @throws  MojoFailureException  Thrown when the classloader creation is impossible with the
	 *                                given elements.
	 */
	ClassLoader creatClassloader(List<String> classpathElements) throws MojoFailureException;

}
