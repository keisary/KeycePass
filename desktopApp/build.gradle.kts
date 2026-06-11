plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    application
}

group = "com.ak.keycepass"
version = "1.0.0"

application {
    mainClass.set("com.ak.keycepass.desktop.MainKt")
}

kotlin {
    jvmToolchain(21)
}

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

    // KotlinX JSON
    implementation(libs.kotlinx.serialization.json)

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

    // mDNS (découverte réseau — keycepass.local)
    implementation("org.jmdns:jmdns:3.5.9")

    // Logging (supprime les warnings SLF4J/Log4j en console)
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}
