package org.java.test.helper.generator.maven.plugin;

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
        testProject = testResources.getBasedir("testproject");
    }

    @Test
    public void testSomething() throws Exception {
        assertThat(pom(), FileMatchers.aFileNamed(equalTo("pom.xml")));
    }

    @Test
    public void testTestprojectCanBeBuildWithoutMojoExecution() throws Exception {
        // Preparation

        // Execution
        final int exitStatus = executeGoals("clean", "compile");

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
        final InvocationResult result = invoker.execute(request);

        if (result.getExecutionException() != null) {
            throw result.getExecutionException();
        }

        return result.getExitCode();
    }
}
