/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

// Documentation example: the guide and README take their snippets from the tagged regions of this build.
// Never published. The CI job builds it after `mvn install` of the reactor, against mavenLocal().
plugins {
    java
}

// The version of the project's artifacts; release-please bumps it with the release (see release-please-config.json).
// tag::gradle-version[]
val matcherGeneratorVersion = "5.1.0" // x-release-please-version
// end::gradle-version[]

repositories {
    // tag::gradle-repositories[]
    // The project's own artifacts come from mavenLocal() only, where `mvn install` puts the current reactor build.
    // A missing install fails the build; Central's published 5.x jar is never picked up in its place.
    mavenLocal {
        content { includeGroup("io.github.marmer.testutils") }
    }
    mavenCentral {
        content { excludeGroup("io.github.marmer.testutils") }
    }
    // end::gradle-repositories[]
}

dependencies {
    // tag::gradle-dependencies[]
    implementation("io.github.marmer.testutils:hamcrest-matcher-generator:$matcherGeneratorVersion")
    implementation("io.github.marmer.testutils:assertj-assertion-generator:$matcherGeneratorVersion")
    // end::gradle-dependencies[]
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.assertj:assertj-core:3.27.7")

    // tag::gradle-processor[]
    // Both backends run as annotation processors; the Gradle annotationProcessor configuration puts them on the processor path.
    annotationProcessor("io.github.marmer.testutils:hamcrest-matcher-generator:$matcherGeneratorVersion")
    annotationProcessor("io.github.marmer.testutils:assertj-assertion-generator:$matcherGeneratorVersion")
    // end::gradle-processor[]

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    // Same lint as the Maven examples: the processing lint stays off, every other category is fatal.
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
