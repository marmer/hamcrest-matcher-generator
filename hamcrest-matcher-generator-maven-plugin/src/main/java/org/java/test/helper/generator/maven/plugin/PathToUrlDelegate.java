package org.java.test.helper.generator.maven.plugin;

import java.net.MalformedURLException;
import java.net.URL;

import java.nio.file.Path;


public class PathToUrlDelegate {

	public URL toUrl(final Path path) throws MalformedURLException {
		return path.toUri().toURL();
	}

}
