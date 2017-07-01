package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A factory for creating {@link ClassLoader} objects by Strings representing paths of classpath
 * elements.
 *
 * @author  marmer
 * @date    01.07.2017
 */
public class ByClasspathStringPathElementURLClassLoaderFactory implements ClassLoaderFactory {
	private PathToUrlDelegate pathToUrlDelegate = new PathToUrlDelegate();
	private ClassLoader classLoader;

	public ByClasspathStringPathElementURLClassLoaderFactory(final ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public ClassLoader creatClassloader(final List<String> classpathElements) throws MojoFailureException {
		return new URLClassLoader(toUrls(toPath(classpathElements)),
				classLoader);
	}

	private URL[] toUrls(final List<Path> classpathRootPaths) throws MojoFailureException {
		final List<URL> urls = new LinkedList<>();

		for (final Path path : classpathRootPaths) {
			try {
				urls.add(pathToUrlDelegate.toUrl(path));
			} catch (MalformedURLException e) {
				throw new MojoFailureException("Error resolving classpath elements", e);
			}
		}

		return urls.toArray(new URL[urls.size()]);
	}

	private List<Path> toPath(final List<String> stringPaths) {
		return stringPaths.stream().map(Paths::get).collect(Collectors.toList());
	}
}
