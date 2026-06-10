plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
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

    // EncryptedSharedPreferences (stockage sécurisé du rôle, matricule, UUID)
    implementation(libs.security.crypto)

    // Ktor Client (HTTP vers le serveur Ktor du poste Desktop)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)

    // Lifecycle + ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
}
