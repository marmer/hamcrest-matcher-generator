package io.github.marmer.annotationprocessing;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreDependencyRulesTest {

    private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.github.marmer.annotationprocessing");

    @Test
    @DisplayName("The core depends on neither an assertion library nor a backend")
    void theCoreDependsOnNeitherAnAssertionLibraryNorABackend() {
        noClasses()
            .that().resideInAPackage("io.github.marmer.annotationprocessing..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.hamcrest..",
                "org.assertj..",
                "io.github.marmer.annotationprocessing.hamcrest..",
                "io.github.marmer.annotationprocessing.assertj..",
                "io.github.marmer.annotationprocessing.kotlin.."
            )
            .check(CORE_CLASSES);
    }
}
