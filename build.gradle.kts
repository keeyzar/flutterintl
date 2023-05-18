plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.3"
    kotlin("kapt") version "1.5.10"
}

group = "de.keeyzar.gpt-helper"
version = "1.8"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.4")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "Dart:222.4560",
            "org.jetbrains.plugins.terminal:222.3739.67"
        )
    )
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    runPluginVerifier {
        ideVersions.set(listOf("2022.2.4", "2023.1"))
    }
}

val ktor_version = "2.3.0"
val koin_version = "3.4.0"

dependencies {
    implementation("com.aallam.openai:openai-client:3.2.3")
    implementation("io.ktor:ktor-client-apache5:$ktor_version")
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.+")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.+")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}
