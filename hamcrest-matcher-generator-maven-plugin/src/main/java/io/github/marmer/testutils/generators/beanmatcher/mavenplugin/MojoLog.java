package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import lombok.EqualsAndHashCode;
import org.apache.maven.plugin.logging.Log;

@EqualsAndHashCode
public class MojoLog implements io.github.marmer.testutils.generators.beanmatcher.Log {
    private final Log log;

    public MojoLog(final Log log) {
        this.log = log;
    }

    @Override
    public void error(final String message, final Exception cause) {
        log.error(message, cause);
    }
}
