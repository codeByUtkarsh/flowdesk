package com.flowdesk.gateway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.flowdesk.gateway");

    @Test
    void gatewayLayersAreRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Filters")
                .definedBy("com.flowdesk.gateway.filter..")
                .layer("Config")
                .definedBy("com.flowdesk.gateway.config..")
                .whereLayer("Filters")
                .mayOnlyAccessLayers("Config")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void noDirectAccessToOtherServicePersistence() {
        noClasses()
                .that()
                .resideInAPackage("com.flowdesk.gateway..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.flowdesk.auth.repository..",
                        "com.flowdesk.auth.entity..",
                        "com.flowdesk.project.repository..",
                        "com.flowdesk.project.entity..",
                        "com.flowdesk.task.repository..",
                        "com.flowdesk.task.entity..",
                        "com.flowdesk.notification.repository..",
                        "com.flowdesk.notification.entity..",
                        "com.flowdesk.analytics.repository..",
                        "com.flowdesk.analytics.entity..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
