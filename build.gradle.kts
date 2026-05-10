import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
}

group = "com.massari"
version = "0.0.1-SNAPSHOT"
description = "energy-tracker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Web + Actuator + Validation + JPA
    implementation(libs.bundles.spring.app)

    // Persistência (driver e Flyway plugin DB ficam só em runtime)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // Spring Modulith
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)

    // Dev
    developmentOnly(libs.spring.boot.devtools)

    // Testes
    testImplementation(libs.bundles.testing.integration)
    testRuntimeOnly(libs.junit.platform.launcher)

    // API documentation / Swagger UI
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val openApiSpec = layout.projectDirectory.file("api-spec/openapi.yaml")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(openApiSpec.asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.massari.energytracker.api.generated")
    modelPackage.set("com.massari.energytracker.api.generated.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "openApiNullable" to "false",
            "dateLibrary" to "java8",
            "useBeanValidation" to "true",
        )
    )
}

tasks.named<ValidateTask>("openApiValidate") {
    inputSpec.set(openApiSpec.asFile.absolutePath)
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}