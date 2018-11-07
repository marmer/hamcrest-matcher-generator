package io.github.marmer.testutils.generators.beanmatcher;

/**
 * Used for logging concerns.
 */
public interface Log {
    /**
     * Loggs on error level.
     *
     * @param message Logmessage
     * @param cause   Cause
     */
    void error(String message, Exception cause);
}
