package org.java.test.helper.generator.maven.plugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Path;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;


public class NewOperatorMatcherGeneratorFactoryTest {

	private NewOperatorMatcherGeneratorFactory classUnderTest = new NewOperatorMatcherGeneratorFactory();
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void testCreateBy_DependenciesGiven_ShouldReturnAnInstance() throws Exception {

		// Preparation
		final URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		final Path outputPath = temp.newFolder().toPath();

		// Execution
		final MatcherGenerator matcherGenerator = classUnderTest.createBy(classLoader, outputPath);

		// Assertion
		assertThat(matcherGenerator, is(instanceOf(MatcherFileGenerator.class)));
	}
}
