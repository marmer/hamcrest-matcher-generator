package io.github.marmer.testutils.samplepojos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import lombok.Value;

public class HasPropertyMatcherGeneratorTest {
	private static final String MATCHER_POSTFIX = "Matcher.java";
	@Rule
	public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
	@InjectMocks
	private HasPropertyMatcherGenerator classUnderTest;
	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	private Path outputDir;

	@Before
	public void setUp() throws Exception {
		this.outputDir = temp.newFolder().toPath();
	}

	@Test
	public void testGenerateMatcherFor_SimplePojoClassGiven_ShouldCreateMatcherClass() throws Exception {
		// Preparation
		classUnderTest.generateMatcherFor(SimplePojo.class, outputDir);

		// Assertion
		assertThat(matcherPathFor(SimplePojo.class), is(anExistingFile()));
	}

	private File matcherPathFor(final Class<?> type) {
		return outputDir.resolve(getPackagePath(type)).resolve(type.getSimpleName() + MATCHER_POSTFIX).toFile();
	}

	private Path getPackagePath(final Class<?> type) {
		return Paths.get(getPackageNameOf(type).replaceAll("\\.", "/"));
	}

	private String getPackageNameOf(final Class<?> type) {
		return type.getPackage().getName();
	}

	@Value
	public class SimplePojo {
		private String simpleProp;
	}

}
