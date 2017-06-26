package org.java.test.helper.generator.maven.plugin;

import org.apache.commons.io.FileUtils;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;

import org.hamcrest.io.FileMatchers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import static org.hamcrest.io.FileMatchers.anExistingFile;

import static org.junit.Assert.assertThat;


public class MatchersMojoITest {
	@Rule
	public TestResources testResources = new TestResources();
	@Rule
	public MojoRule rule = new MojoRule();

	private File testProject;

	@Before
	public void setUp() throws Exception {
		FileUtils.deleteQuietly(testProject);
		testProject = testResources.getBasedir("testproject");
	}

	@Test
	public void testMojoCanBeExecutedInIsulation() throws Exception {
		executeMojo("matchers");
	}

	@Test
	@Ignore // TODO think about it
	public void testPluginRunHasCreatedMatcherSource() throws Exception {

		// Preparation
		assertThat("For this test required file is missing",
			inSrcMainJava("some/pck/model/SimpleModel.java"),
			is(anExistingFile()));

		// Execution
		executeMojo("matchers");

		// Expectation
		assertThat("Should have been generated: " + inGeneratedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			inGeneratedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			is(anExistingFile()));
	}

	private void executeMojo(final String goal) throws Exception {
		rule.executeMojo(testProject, goal);
	}

	@Test
	public void testTestprojectShouldHavePom() throws Exception {
		assertThat(pomFile(), FileMatchers.aFileNamed(equalTo("pom.xml")));
	}

	private File inGeneratedTestSourcesDir(final String path) {
		return new File(generatedTestSourcesDir(), path);
	}

	private File inSrcMainJava(final String path) {
		return new File(getSrcMainJavaDir(), path);
	}

	private File getSrcMainJavaDir() {
		return new File(testProject, "src/main/java");
	}

	private File generatedTestSourcesDir() {
		return new File(targetDir(), "generated-test-sources");
	}

	private File targetDir() {
		return new File(testProject, "target");
	}

	private File pomFile() {
		return new File(testProject, "pom.xml");
	}
}
