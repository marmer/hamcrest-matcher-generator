package io.github.marmer.annotationprocessing.hamcrest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HamcrestBackendDependencyRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.github.marmer.annotationprocessing");

    @Test
    @DisplayName("The Hamcrest backend depends on no other backend")
    void theHamcrestBackendDependsOnNoOtherBackend() {
        noClasses()
            .that().resideInAPackage("io.github.marmer.annotationprocessing.hamcrest..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.assertj..",
                "io.github.marmer.annotationprocessing.assertj..",
                "io.github.marmer.annotationprocessing.kotlin.."
            )
            .check(CLASSES);
    }
}
