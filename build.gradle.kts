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

val fintModelResourceVersion = "1.0.1"
val fintResourceModelVersion = "4.0.10"

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

    implementation("no.novari:fint-model-resource:$fintModelResourceVersion")
    implementation("no.novari:fint-arkiv-resource-model-java:$fintResourceModelVersion")
    implementation("no.novari:fint-administrasjon-resource-model-java:$fintResourceModelVersion")

    implementation("no.novari:flyt-web-instance-gateway:2.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-core")
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
