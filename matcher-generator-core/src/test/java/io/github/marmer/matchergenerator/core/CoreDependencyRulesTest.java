/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoreDependencyRulesTest {

    private static final JavaClasses CORE_CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.github.marmer.matchergenerator.core");

    /**
     * A field whose type, or a type argument of it, is an element, a type or a mirror of the
     * language model. Such a field keeps a value of one round alive in the next, which the
     * processor must not do.
     */
    private static final ArchCondition<JavaField> HOLD_NO_LANGUAGE_MODEL_TYPE =
            new ArchCondition<>("hold no element, type or mirror of the language model") {
                @Override
                public void check(final JavaField field, final ConditionEvents events) {
                    field.getAllInvolvedRawTypes().stream()
                            .filter(
                                    type ->
                                            type.getPackageName()
                                                            .startsWith("javax.lang.model.element")
                                                    || type.getPackageName()
                                                            .startsWith("javax.lang.model.type"))
                            .findFirst()
                            .ifPresent(
                                    type ->
                                            events.add(
                                                    SimpleConditionEvent.violated(
                                                            field,
                                                            field.getFullName()
                                                                    + " holds "
                                                                    + type.getName())));
                }
            };

    @Test
    @DisplayName("The core depends on neither an assertion library nor a backend")
    void theCoreDependsOnNeitherAnAssertionLibraryNorABackend() {
        noClasses()
                .that()
                .resideInAPackage("io.github.marmer.matchergenerator.core..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.hamcrest..",
                        "org.assertj..",
                        "io.github.marmer.matchergenerator.hamcrest..",
                        "io.github.marmer.matchergenerator.assertj..",
                        "io.github.marmer.matchergenerator.hamcrest.kotlin..")
                .check(CORE_CLASSES);
    }

    @Test
    @DisplayName(
            "The core does not depend on JavaPoet, so the backends keep their code generation to themselves")
    void theCoreDoesNotDependOnJavaPoet() {
        noClasses()
                .that()
                .resideInAPackage("io.github.marmer.matchergenerator.core..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.palantir.javapoet..")
                .because(
                        "JavaPoet on the core's classpath would spread to every user of the core; the shared"
                                + " type helpers stay in the backends (ADR 0007)")
                .check(CORE_CLASSES);
    }

    @Test
    @DisplayName(
            "The worker keeps no element, type or mirror of a round in a field, not even as a type argument")
    void theWorkerKeepsNoLanguageModelTypeInAField() {
        fields().that()
                .areDeclaredInClassesThat()
                .haveFullyQualifiedName(MatcherGenerationProcessorWorker.class.getName())
                .should(HOLD_NO_LANGUAGE_MODEL_TYPE)
                .because(
                        "a field outlives the round it was set in, so it must keep names and not elements, mirrors"
                                + " or types of that round; the names of a deferred configuration are strings"
                                + " (round rule, arc42 chapter 6)")
                .check(CORE_CLASSES);
    }
}
