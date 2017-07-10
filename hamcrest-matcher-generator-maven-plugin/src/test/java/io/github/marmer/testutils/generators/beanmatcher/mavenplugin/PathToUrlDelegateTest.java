package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

import org.junit.Test;

import io.github.marmer.testutils.generators.beanmatcher.mavenplugin.PathToUrlDelegate;

import java.net.URL;

import java.nio.file.Path;
import java.nio.file.Paths;


public class PathToUrlDelegateTest {
    private PathToUrlDelegate classUnderTest = new PathToUrlDelegate();

    @Test
    public void testtoUrl_PathGiven_UrlOfPathShouldBeReturned() throws Exception {
        // Preparation
        final Path path = Paths.get("somePath");

        // Execution
        final URL url = classUnderTest.toUrl(path);

        // Expectation
        assertThat("converted URL", url, is(equalTo(path.toUri().toURL())));
    }
}
