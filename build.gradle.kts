import org.springframework.boot.gradle.plugin.SpringBootPlugin

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        constraints {
            classpath("org.apache.commons:commons-lang3:3.22.0") {
                because("Fixes CVE-2025-48924 in spring-boot-buildpack-platform")
            }
        }
    }
}

plugins {
    id("org.springframework.boot") version "4.2.0"
    id("io.github.ben-manes.versions") version "0.67.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
}

group = "no.novari"
version = "0.0.1-SNAPSHOT"

val fintModelResourceVersion = "1.0.1"
val fintResourceModelVersion = "4.1.0"

kotlin {
    jvmToolchain(25)
}

// Dependabot resolves with JDK 21; an explicit release keeps JVM 25 as the resolution target so gradle.lockfile gets updated.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.fintlabs.no/releases")
}

tasks.jar {
    isEnabled = false
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.7")
    implementation("tools.jackson.module:jackson-module-kotlin")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation("no.novari:fint-model-resource:$fintModelResourceVersion")
    implementation("no.novari:fint-arkiv-resource-model-java:$fintResourceModelVersion")
    implementation("no.novari:fint-administrasjon-resource-model-java:$fintResourceModelVersion")

    implementation("no.novari:flyt-gateway-starter:6.0.0-rc-4")
    implementation("no.novari:flyt-cache:4.0.0-rc-4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
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
