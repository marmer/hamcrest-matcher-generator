package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import static org.hamcrest.Matchers.is;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;


public class MatchersMojoNoMatcherSourcesGivenSystemTest {
    @Rule
    public TestProjectResource testproject =
        new TestProjectResource("testprojectWithoutMatcherSource");

    @Test
    public void testPluginExecutionShouldWorkWithoutAnyErrors() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals("hamcrestMatcherGenerator:matchers");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(1));
    }
}
