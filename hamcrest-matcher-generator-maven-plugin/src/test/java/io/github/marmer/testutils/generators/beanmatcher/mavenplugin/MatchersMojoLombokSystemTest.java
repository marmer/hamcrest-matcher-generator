package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import org.hamcrest.io.FileMatchers;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import static org.hamcrest.io.FileMatchers.anExistingFile;

import static org.junit.Assert.assertThat;


public class MatchersMojoLombokSystemTest {
	@Rule
	public TestProjectResource testproject = new TestProjectResource("testprojectLombokModels");

	@Test
	public void testTestprojectShouldHavePom() throws Exception {
		assertThat(testproject.pomFile(), FileMatchers.aFileNamed(equalTo("pom.xml")));
	}

	@Test
	public void testPluginExecutionShouldWorkWithoutAnyErrors() throws Exception {
		// Preparation

		// Execution
		final int exitStatus = testproject.executeGoals("hamcrestMatcherGenerator:matchers");

		// Expectation
		assertThat("Execution exit status", exitStatus, is(0));
	}

	@Test
	public void testPluginRunHasCreatedMatcherSourceOnCallingPluginGoalDirectly() throws Exception {

		// Preparation
		assertThat("For this test required file is missing",
			testproject.srcMainJava("some/pck/model/SimpleModel.java"),
			is(anExistingFile()));

		// Execution
		testproject.executeGoals("hamcrestMatcherGenerator:matchers");

		// Expectation
		assertThat(
			"Should have been generated: " +
			testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
			is(anExistingFile()));
	}

}
