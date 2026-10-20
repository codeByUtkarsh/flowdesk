package com.flowdesk.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
                    .importPackages("com.flowdesk.auth");

    @Test
    void controllersMayOnlyCallServices() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controllers")
                .definedBy("com.flowdesk.auth.controller..")
                .layer("Services")
                .definedBy("com.flowdesk.auth.service..")
                .layer("Repositories")
                .definedBy("com.flowdesk.auth.repository..")
                .whereLayer("Controllers")
                .mayOnlyAccessLayers("Services")
                .whereLayer("Services")
                .mayOnlyAccessLayers("Repositories")
                .whereLayer("Repositories")
                .mayNotAccessAnyLayer()
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void controllersOnlyDependOnServices() {
        classes()
                .that()
                .resideInAPackage("com.flowdesk.auth.controller..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "com.flowdesk.auth.controller..",
                        "com.flowdesk.auth.service..",
                        "com.flowdesk.common..",
                        "java..",
                        "javax..",
                        "jakarta..",
                        "org.springframework..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void servicesOnlyDependOnRepositories() {
        classes()
                .that()
                .resideInAPackage("com.flowdesk.auth.service..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "com.flowdesk.auth.service..",
                        "com.flowdesk.auth.repository..",
                        "com.flowdesk.auth.entity..",
                        "com.flowdesk.common..",
                        "java..",
                        "javax..",
                        "jakarta..",
                        "org.springframework..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void noDirectAccessToOtherServicePersistence() {
        noClasses()
                .that()
                .resideInAPackage("com.flowdesk.auth..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
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
