package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import org.apache.maven.plugin.logging.Log;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MojoLogTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @InjectMocks
    private MojoLog underTest;
    @Mock
    private Log log;


    @Test
    public void testError_MessageGiven_ShouldLogMessage()
            throws Exception {
        // Preparation
        final Exception cause = mock(Exception.class);

        // Execution
        underTest.error("some message", cause);

        // Assertion
        verify(log).error("some message", cause);
    }

}