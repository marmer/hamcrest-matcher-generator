/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.hamcrest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HamcrestBackendDependencyRulesTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.github.marmer.matchergenerator");

    @Test
    @DisplayName("The Hamcrest backend depends on no other backend")
    void theHamcrestBackendDependsOnNoOtherBackend() {
        noClasses()
                .that()
                .resideInAPackage("io.github.marmer.matchergenerator.hamcrest..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.assertj..",
                        "io.github.marmer.matchergenerator.assertj..",
                        "io.github.marmer.matchergenerator.hamcrest.kotlin..")
                .check(CLASSES);
    }
}
