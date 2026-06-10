import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    application
}

group = "com.ak.keycepass"
version = "1.0.0"

dependencies {
    // Module partagé
    implementation(project(":shared"))

    // Kotlin Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)

    // Ktor Server (CIO engine)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)

    // Exposed ORM + SQLite
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.sqlite.jdbc)

    // Import Excel
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Génération QR Code
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
