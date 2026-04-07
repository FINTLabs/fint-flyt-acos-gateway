plugins {
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
}

group = "no.novari"
version = "0.0.1-SNAPSHOT"
val apiVersion = "3.21.10"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.fintlabs.no/releases")
}

tasks.jar {
    isEnabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    compileOnly("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.security:spring-security-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("no.fintlabs:fint-model-resource:0.5.0")
    implementation("no.fint:fint-arkiv-resource-model-java:$apiVersion")
    implementation("no.fint:fint-administrasjon-resource-model-java:$apiVersion")

    implementation("no.novari:flyt-web-resource-server:2.0.0")
    implementation("no.novari:flyt-web-instance-gateway:1.3.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
