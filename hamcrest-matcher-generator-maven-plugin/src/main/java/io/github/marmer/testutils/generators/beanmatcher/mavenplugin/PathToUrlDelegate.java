package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import java.net.MalformedURLException;
import java.net.URL;

import java.nio.file.Path;


/**
 * Class is used for delegation to {@link PathToUrlDelegate}'s static methods to be able to test
 * hard to producable errors as well.
 *
 * @author  marmer
 * @since   01.07.2017
 */
public class PathToUrlDelegate {

	/**
	 * To url.
	 *
	 * @param   path  the path
	 *
	 * @return  the url
	 *
	 * @throws  MalformedURLException  the malformed URL exception
	 */
	public URL toUrl(final Path path) throws MalformedURLException {
		return path.toUri().toURL();
	}

}
