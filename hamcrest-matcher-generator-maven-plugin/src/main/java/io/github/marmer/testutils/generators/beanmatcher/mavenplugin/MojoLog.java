package io.github.marmer.testutils.generators.beanmatcher.mavenplugin;

import lombok.Value;
import org.apache.maven.plugin.logging.Log;

@Value
public class MojoLog implements io.github.marmer.testutils.generators.beanmatcher.Log {
    private final Log log;

    public MojoLog(final Log log) {
        this.log = log;
    }
}
