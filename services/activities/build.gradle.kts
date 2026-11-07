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
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("dev.openfga:openfga-sdk:0.11.0")
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-openfga")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val generatedDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName = "spring"
    inputSpec = layout.projectDirectory.file("../../contracts/openapi/tasks.yaml")
    outputDir = generatedDir
    apiPackage = "app.mikraal.activities.tasks.api.generated"
    modelPackage = "app.mikraal.activities.tasks.api.generated.model"
    globalProperties = mapOf("apis" to "", "models" to "", "supportingFiles" to "")
    configOptions = mapOf(
        "interfaceOnly" to "true",
        "useTags" to "true",
        "useSpringBoot3" to "true",
        "openApiNullable" to "false",
        "skipDefaultInterface" to "true",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "hideGenerationTimestamp" to "true",
        "configPackage" to "app.mikraal.activities.tasks.api.generated.configuration",
        "sourceFolder" to "src/main/java",
    )
    // JSON merge patch must tell an absent field from an explicit null, which a generated POJO cannot.
    schemaMappings = mapOf("UpdateTask" to "tools.jackson.databind.node.ObjectNode")
}

sourceSets.main {
    java.srcDir(generatedDir.map { it.dir("src/main/java") })
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.test {
    useJUnitPlatform()
}
