package org.java.test.helper.generator.maven.plugin;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import org.hamcrest.io.FileMatchers;
import static org.hamcrest.io.FileMatchers.anExistingFile;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;


public class MatchersMojoJDK8AndNewHamcrestSystemTest {
    @Rule
    public TestProjectResource testproject =
        new TestProjectResource("testprojectJava8AndCurrentHamcrestVersion");

    @Test
    public void testTestprojectShouldHavePom() throws Exception {
        assertThat(testproject.pomFile(), FileMatchers.aFileNamed(equalTo("pom.xml")));
    }

    @Test
    public void testTestprojectShouldBeBuildableWithoutMojoExecution() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals("clean", "compile");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(0));
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
    public void testPhaseTestShouldStillWorkAfterPluginExecutionWithoutAnyErrors()
        throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals("hamcrestMatcherGenerator:matchers",
                "test");

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

    @Test
    public void testPluginRunHasCreatedMatcherSourceOnTestGoal() throws Exception {
        // Preparation
        assertThat("For this test required file is missing",
            testproject.srcMainJava("some/pck/model/SimpleModel.java"),
            is(anExistingFile()));

        // Execution
        testproject.executeGoals("test");

        // Expectation
        assertThat(
            "Should have been generated: " +
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            is(anExistingFile()));
    }
}
