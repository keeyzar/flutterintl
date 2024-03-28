plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.2"
    kotlin("kapt") version "1.9.23"
}

group = "de.keeyzar.gpt-helper"
version = "1.18"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "Dart:232.8660.129",
            "org.jetbrains.plugins.terminal:232.8660.88"
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

    withType<Test> {
        useJUnit() //when we use junit 5, we might use useJUnitPlatform() - but we don't.. I'm not entire
//         avoid JBUIScale "Must be precomputed" error, because IDE is not started (LoadingState.APP_STARTED.isOccurred is false)
        jvmArgs("-Djava.awt.headless=true")
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("233.*")
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
        ideVersions.set(listOf("2023.2"))
    }
}

val ktor_version = "2.3.0"
val koin_version = "3.5.0"

//finally got it working, holy moly shit, such an annoying error...
dependencies {
    implementation("com.aallam.openai:openai-client:3.5.1") {
        if (System.getenv("excludeDeps") == "true") {
            exclude(group = "org.slf4j", module = "slf4j-api")
            // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation:when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        }
    }
    implementation("io.ktor:ktor-client-cio:2.3.0") {
        if (System.getenv("excludeDeps") == "true") {
            exclude(group = "org.slf4j", module = "slf4j-api")
            // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation: when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        }
    }

    implementation("io.insert-koin:koin-core:$koin_version") {
        if (System.getenv("excludeDeps") == "true") {
            // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation: when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        }
    }
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+") {
        println("befooore")
        if (System.getenv("excludeDeps") == "true") {
            println("excluding!!")
            // Prevents java.lang.LinkageError: java.lang.LinkageError: loader constraint violation: when resolving method 'long kotlin.time.Duration.toLong-impl(long, kotlin.time.DurationUnit)'
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        }
    }
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.+")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.+")
    implementation("org.slf4j:slf4j-api:2.0.7")

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}
