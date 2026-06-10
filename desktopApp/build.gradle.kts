plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.ak.keycepass"
version = "1.0.0"

dependencies {
    // Core Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Project shared
    implementation(project(":shared"))

    // Ktor Server (embedded)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.status.pages)

    // Database (H2 + Exposed)
    implementation(libs.h2.database)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)

    // QR Code generation
    implementation(libs.zxing.core)
}

application {
    applicationDefaultJvmArgs = listOf("-Dskiko.renderApi=OPENGL")
    mainClass.set("com.ak.keycepass.desktop.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
