package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.testing.MojoRule;

import org.hamcrest.io.FileMatchers;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import static org.hamcrest.io.FileMatchers.anExistingFile;

import static org.junit.Assert.assertThat;


public class MatchersMojoITest {
	@Rule
	public TestProjectResource testProject = new TestProjectResource(
			"testprojectJava6AndOldestPossibleHamcrestVersion");
	@Rule
	public MojoRule rule = new MojoRule();

	@Test
	public void testMojoCanBeExecutedInIsulation() throws Exception {
		executeMojo("matchers");
	}

	@Test
	public void testPluginRunHasCreatedMatcherSource() throws Exception {

		// Preparation
		assertThat("For this test required file is missing",
			testProject.srcMainJava("some/pck/model/SimpleModel.java"),
			is(anExistingFile()));

		// Execution
		executeMojo("matchers");

		// Expectation
		assertThat("Should have been generated: " +
			testProject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			testProject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			is(anExistingFile()));
	}

	private void executeMojo(final String goal) throws Exception {
		rule.executeMojo(testProject.getBaseDir(), goal);
	}

	@Test
	public void testTestprojectShouldHavePom() throws Exception {
		assertThat(testProject.pomFile(), FileMatchers.aFileNamed(equalTo("pom.xml")));
	}

}
