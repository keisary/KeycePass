plugins {
    alias(libs.plugins.android.application)
<<<<<<< HEAD
    alias(libs.plugins.kotlin.serialization)
=======
>>>>>>> origin/main
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ak.keycepass.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ak.keycepass.android"
        minSdk = 24 // Android 7.0+
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Module partagé (modèles + logique métier commune)
    implementation(project(":shared"))

    // Coroutines Android
    implementation(libs.kotlinx.coroutines.android)

    // Room (base de données locale pour les scans en attente)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)

    // EncryptedSharedPreferences
    implementation(libs.security.crypto)

    // Ktor Client (HTTP vers le serveur Ktor du poste Desktop)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)

    // Lifecycle + ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Jetpack Navigation
    implementation(libs.navigation.compose)

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Material Icons Extended
    implementation(libs.material.icons.extended)

    // ZXing QR Code Generation
    implementation(libs.zxing.core)
}
