package io.github.marmer.matchergenerator.bridge.dependencies;

/**
 * Bridge for the 5.x artifact {@code hamcrest-matcher-generator-dependencies}. It holds no functionality: the artifact exists so that builds which name
 * it, among them annotation processor paths of older {@code maven-compiler-plugin} versions, still find a jar. The
 * Hamcrest backend, the runtime library and the configuration annotations come from
 * {@code hamcrest-matcher-generator}, which this artifact depends on.
 */
public final class Bridge {

    private Bridge() {
    }
}
