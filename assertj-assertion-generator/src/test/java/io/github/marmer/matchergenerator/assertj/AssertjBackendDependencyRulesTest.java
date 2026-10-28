/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.marmer.matchergenerator.assertj;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssertjBackendDependencyRulesTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("io.github.marmer.matchergenerator");

    @Test
    @DisplayName("The AssertJ backend depends on no other backend and on no Hamcrest")
    void theAssertjBackendDependsOnNoOtherBackendAndOnNoHamcrest() {
        noClasses()
                .that()
                .resideInAPackage("io.github.marmer.matchergenerator.assertj..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.hamcrest..", "io.github.marmer.matchergenerator.hamcrest..")
                .check(CLASSES);
    }
}
