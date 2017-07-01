package io.github.marmer.testutils.generators.beanmatcher.processing;

import java.nio.file.Path;

import java.util.List;


/**
 * Creates and loads classes from a List of java source files.
 *
 * @author  marmer
 * @date    20.06.2017
 */
public interface JavaFileClassLoader {

	/**
	 * Creates and loads classes from a List of java source files.
	 *
	 * @param   sourceFiles  the source files
	 *
	 * @return  the list of class files read.
	 */
	List<Class<?>> load(List<Path> sourceFiles);

}
