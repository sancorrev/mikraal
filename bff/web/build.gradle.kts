import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.27.0"
}

group = "app.mikraal"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("com.nimbusds:nimbus-jose-jwt:10.10")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val contracts = layout.projectDirectory.dir("../../contracts/openapi")
val generatedDir = layout.buildDirectory.dir("generated")

// A JSON merge patch must keep the difference between an absent field and an explicit null,
// which generated models without JsonNullable cannot express. It travels as a raw JSON tree.
val rawMergePatch = mapOf("UpdateTask" to "tools.jackson.databind.JsonNode")

val generateBffApi by tasks.registering(GenerateTask::class) {
    generatorName = "spring"
    inputSpec = contracts.file("bff-web.yaml")
    outputDir = generatedDir.map { it.dir("bff-api") }
    apiPackage = "app.mikraal.bff.web.api"
    modelPackage = "app.mikraal.bff.web.api.model"
    schemaMappings = rawMergePatch
    configOptions = mapOf(
        "interfaceOnly" to "true",
        "useTags" to "true",
        "useSpringBoot3" to "true",
        "openApiNullable" to "false",
        "useBeanValidation" to "false",
        "generateJsonIncludeAnnotations" to "true",
        "generateJsonSetterNullsAnnotations" to "false",
        "skipDefaultInterface" to "true",
        "configPackage" to "app.mikraal.bff.web.api.configuration",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "sourceFolder" to "src/main/java",
    )
}

val generateActivitiesClient by tasks.registering(GenerateTask::class) {
    generatorName = "spring"
    library = "spring-http-interface"
    inputSpec = contracts.file("tasks.yaml")
    outputDir = generatedDir.map { it.dir("activities-client") }
    apiPackage = "app.mikraal.bff.web.activities.api"
    modelPackage = "app.mikraal.bff.web.activities.model"
    // Prefix and suffix keep the client apart from the server types of the same name (Task, TasksApi...).
    modelNamePrefix = "Activities"
    apiNameSuffix = "Client"
    schemaMappings = rawMergePatch
    // APIs and models only: the BFF builds the clients with Spring Boot's HTTP service groups.
    globalProperties = mapOf("apis" to "", "models" to "")
    configOptions = mapOf(
        "useTags" to "true",
        "useSpringBoot3" to "true",
        "openApiNullable" to "false",
        "useBeanValidation" to "false",
        "generateJsonIncludeAnnotations" to "true",
        "generateJsonSetterNullsAnnotations" to "false",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "sourceFolder" to "src/main/java",
    )
}

sourceSets {
    main {
        java {
            srcDir(generateBffApi.map { it.outputDir.dir("src/main/java") })
            srcDir(generateActivitiesClient.map { it.outputDir.dir("src/main/java") })
        }
    }
}

tasks.compileJava {
    dependsOn(generateBffApi, generateActivitiesClient)
}

tasks.test {
    useJUnitPlatform()
}
