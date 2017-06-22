package org.java.test.helper.generator.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import static org.hamcrest.io.FileMatchers.anExistingFileOrDirectory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import java.util.Collections;


public class MatchersMojoTest {
    @Rule
    public TestResources testResources = new TestResources();
    @Rule
    public MojoRule rule = new MojoRule();

    @Test
    public void testSomething() throws Exception {
        final File testProject = testResources.getBasedir("testproject");

        assertThat(testProject, anExistingFileOrDirectory());
    }

    private void mojoLoadingExample() throws Exception, MojoExecutionException {
        final MatchersMojo myMojo =
            (MatchersMojo) rule.lookupMojo("matchers",
                "src/test/resources/unit/testproject/pom.xml");
        assertNotNull(myMojo);
        myMojo.execute();
    }

    private void invokerusageExample() throws MavenInvocationException {
        // Not working yet ;)
        final InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("/path/to/pom.xml"));
        request.setGoals(Collections.singletonList("install"));

        final Invoker invoker = new DefaultInvoker();
        invoker.execute(request);
    }
}
