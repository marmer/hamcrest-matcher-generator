package org.java.test.helper.generator.maven.plugin;

import org.apache.commons.io.FileUtils;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.codehaus.plexus.util.cli.CommandLineException;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import org.hamcrest.io.FileMatchers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import java.util.Arrays;


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
        final MatchersMojo myMojo = (MatchersMojo) rule.lookupMojo("matchers", pom());
        assertNotNull(myMojo);
        myMojo.execute();
    }

    @Test
    public void testTestprojectShouldHavePom() throws Exception {
        assertThat(pom(), FileMatchers.aFileNamed(equalTo("pom.xml")));
    }

    @Test
    public void testTestprojectShouldBeBuildWithoutMojoExecution() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = executeGoals("clean", "compile");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(0));
    }

    @Test
    public void testPluginExecutionShouldWorkWithoutAnyErrors() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = executeGoals("testHelperGenerator:matchers");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(0));
    }

    @Test
    public void testPhaseTestShouldStillWorkAfterPluginExecutionWithoutAnyErrors()
        throws Exception {
        // Preparation

        // Execution
        final int exitStatus = executeGoals("testHelperGenerator:matchers test");

        // Expectation
        assertThat("Execution exit status", exitStatus, is(0));
    }

    private File pom() {
        return new File(testProject, "pom.xml");
    }

    private int executeGoals(final String... goals) throws MavenInvocationException,
        CommandLineException {
        final InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom());
        request.setGoals(Arrays.asList(goals));

        final Invoker invoker = new DefaultInvoker();
        prepareExecutableFor(invoker);

        final InvocationResult result = invoker.execute(request);

        if (result.getExecutionException() != null) {
            throw result.getExecutionException();
        }

        return result.getExitCode();
    }

    private void prepareExecutableFor(final Invoker invoker) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            final String getenv = System.getenv("M2_HOME");
            final File mavenExecutable = new File(getenv, "bin/mvn.cmd");
            invoker.setMavenExecutable(mavenExecutable);
        }
    }
}
