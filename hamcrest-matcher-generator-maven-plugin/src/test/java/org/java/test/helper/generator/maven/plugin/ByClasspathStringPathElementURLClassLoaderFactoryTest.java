package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.mockito.InjectMocks;
import org.mockito.Spy;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.quality.Strictness;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.when;


public class ByClasspathStringPathElementURLClassLoaderFactoryTest {
	private ClassLoader classLoaderParent = new URLClassLoader(new URL[0], getClass().getClassLoader());
	@InjectMocks
	private ByClasspathStringPathElementURLClassLoaderFactory classUnderTest =
		new ByClasspathStringPathElementURLClassLoaderFactory(
			classLoaderParent);
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@Spy
	private PathToUrlDelegate pathToUrlDelegate = new PathToUrlDelegate();
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void testCreateClassLoader_ValidPathUrlsGiven_ResultingClassLoaderShouldHaveGivenParent() throws Exception {

		// Preparation
		final Path somePath = temp.newFolder().toPath();
		final Path anotherPath = temp.newFolder().toPath();

		// Execution
		final URLClassLoader classLoader = (URLClassLoader) classUnderTest
				.creatClassloader(Arrays.asList(somePath.toString(), anotherPath.toString()));

		// Assertion
		assertThat("Classloader Parent", classLoader.getParent(), is(sameInstance(this.classLoaderParent)));
	}

	@Test
	public void testCreateClassLoader_ValidPathUrlsGiven_ShouldHaveUrlsOfGivenPaths() throws Exception {

		// Preparation
		final Path somePath = temp.newFolder().toPath();
		final Path anotherPath = temp.newFolder().toPath();

		// Execution
		final URLClassLoader classLoader = (URLClassLoader) classUnderTest
				.creatClassloader(Arrays.asList(somePath.toString(), anotherPath.toString()));

		// Assertion
		assertThat(classLoader.getURLs(),
			arrayContainingInAnyOrder(somePath.toUri().toURL(), anotherPath.toUri().toURL()));
	}

	@Test
	public void testCreatClassloader_ErrorOnProcessingClasspathElements_ShouldThrowBuildBreakingExceptionWithAppropriateDescription()
		throws Exception {

		// Preparation
		final MalformedURLException malformedURLException = new MalformedURLException();
		final String classpathElementPath = "anyClasspathElement";
		final Path path = Paths.get(classpathElementPath);
		when(pathToUrlDelegate.toUrl(path)).thenThrow(malformedURLException);

		// Assertion
		exception.expect(MojoFailureException.class);
		exception.expectCause(is(sameInstance(malformedURLException)));
		exception.expectMessage(is(equalTo("Error resolving classpath elements")));

		// Execution
		classUnderTest.creatClassloader(Collections.singletonList(classpathElementPath));
	}

}
