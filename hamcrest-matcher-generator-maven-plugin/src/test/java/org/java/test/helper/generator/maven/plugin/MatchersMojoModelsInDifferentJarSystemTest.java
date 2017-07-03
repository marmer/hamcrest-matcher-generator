package org.java.test.helper.generator.maven.plugin;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import org.hamcrest.io.FileMatchers;
import static org.hamcrest.io.FileMatchers.anExistingFile;

import org.java.test.helper.generator.maven.plugin.testutils.TestProjectResource;

import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;


public class MatchersMojoModelsInDifferentJarSystemTest {
    @Rule
    public TestProjectResource testproject =
        new TestProjectResource("testprojectExternalDependencies");

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

    private File matcherFileLocation() {
        return testproject.getBaseDir().toPath().resolve("testprojectDependencyUser")
                          .resolve("target").resolve(TestProjectResource.GENERATED_TEST_SOURCES)
                          .resolve("some/pck/model/SimpleModelMatcher.java").toFile();
    }

    @Test
    public void testPluginRunHasCreatedMatcherSourceOnTestGoal() throws Exception {
        // Preparation

        // Execution
        testproject.executeGoals("test");

        // Expectation
        assertThat("Should have been generated: " + matcherFileLocation(),
            matcherFileLocation(),
            is(anExistingFile()));
    }
}
