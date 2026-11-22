import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Runs pure-JVM unit tests for :core:model alongside Android debug unit tests."
    dependsOn(tasks.named("test"))
}

tasks.register("lintDebug") {
    group = "verification"
    description = "Runs verification checks for :core:model."
    dependsOn(tasks.named("check"))
}
