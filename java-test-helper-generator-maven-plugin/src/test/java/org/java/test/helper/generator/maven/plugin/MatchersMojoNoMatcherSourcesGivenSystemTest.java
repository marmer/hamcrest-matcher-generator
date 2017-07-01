package org.java.test.helper.generator.maven.plugin;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;


public class MatchersMojoNoMatcherSourcesGivenSystemTest {
	@Rule
	public TestProjectResource testproject = new TestProjectResource(
			"testprojectWithoutMatcherSource");

	@Test
	public void testPluginExecutionShouldWorkWithoutAnyErrors() throws Exception {
		// Preparation

		// Execution
		final int exitStatus = testproject.executeGoals("testHelperGenerator:matchers");

		// Expectation
		assertThat("Execution exit status", exitStatus, is(1));
	}

}
