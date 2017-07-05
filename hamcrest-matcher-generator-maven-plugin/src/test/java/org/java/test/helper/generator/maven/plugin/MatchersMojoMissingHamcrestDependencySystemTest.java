package org.java.test.helper.generator.maven.plugin;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.hamcrest.io.FileMatchers;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.hamcrest.io.FileMatchers.anExistingFileOrDirectory;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;


public class MatchersMojoMissingHamcrestDependencySystemTest {
    @Rule
    public TestProjectResource testproject =
        new TestProjectResource("testprojectWithMissingHamcrestDependency");

    @Test
    public void testTestprojectShouldHavePom() throws Exception {
        assertThat(testproject.pomFile(), FileMatchers.aFileNamed(equalTo("pom.xml")));
    }

    @Test
    public void testAllowedMissingHamcrestDependencyShouldFinishTheProcessSuccessfull()
        throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals(
                "hamcrestMatcherGenerator:matchers -DallowMissingHamcrestDependency=true");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(0));
    }

    @Test
    public void testNotAllowedMissingHamcrestDependencyShouldBreakTheBuild() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals(
                "hamcrestMatcherGenerator:matchers -DallowMissingHamcrestDependency=false");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(1));
    }

    @Test
    public void testDefaultSettingOfNotAllowedMissingHamcrestDependencyShouldBreakTheBuild()
        throws Exception {
        // Preparation

        // Execution
        final int exitStatus = testproject.executeGoals("hamcrestMatcherGenerator:matchers");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(1));
    }

    @Test
    public void testAllowedMissingHamcrestDependencyShouldGenerateMatchersProperly()
        throws Exception {
        // Preparation
        assertThat("For this test required file is missing",
            testproject.srcMainJava("some/pck/model/SimpleModel.java"),
            is(anExistingFile()));

        // Execution
        testproject.executeGoals(
            "hamcrestMatcherGenerator:matchers -DallowMissingHamcrestDependency=true");

        // Expectation
        assertThat(
            "Should have been generated: " +
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            is(anExistingFile()));
    }

    @Test
    public void testNotAllowedMissingHamcrestDependencyShouldNotGenerateMatchers()
        throws Exception {
        // Preparation
        assertThat("For this test required file is missing",
            testproject.srcMainJava("some/pck/model/SimpleModel.java"),
            is(anExistingFile()));

        // Execution
        testproject.executeGoals(
            "hamcrestMatcherGenerator:matchers -DallowMissingHamcrestDependency=false");

        // Expectation
        assertThat(
            "Should have been generated: " +
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            testproject.generatedTestSourcesDir("some/pck/model/SimpleModelMatcher.java"),
            is(not(anExistingFileOrDirectory())));
    }
}
