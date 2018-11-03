package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import io.github.marmer.testutils.generators.beanmatcher.MatcherFileGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGenerator;
import io.github.marmer.testutils.generators.beanmatcher.MatcherGeneratorFactory.MatcherGeneratorConfiguration;
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

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private final NewOperatorMatcherGeneratorFactory classUnderTest = new NewOperatorMatcherGeneratorFactory();

	@Test
	public void testCreateBy_DependenciesGiven_ShouldReturnAnInstance() throws Exception {

		// Preparation
		final URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		final Path outputPath = temp.newFolder().toPath();

		// Execution
		final MatcherGeneratorConfiguration matcherGeneratorConfiguration = MatcherGeneratorConfiguration.builder()
				.classLoader(classLoader)
				.outputPath(outputPath)
				.build();
		final MatcherGenerator matcherGenerator = classUnderTest.createBy(matcherGeneratorConfiguration);

		// Assertion
		assertThat(matcherGenerator, is(instanceOf(MatcherFileGenerator.class)));
	}
}
