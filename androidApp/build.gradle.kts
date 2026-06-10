plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ak.keycepass.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ak.keycepass.android"
        minSdk = 24 // Conforme à l'exigence ENF_04 (Android 7.0+)
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
    implementation(project(":shared"))
    // Les autres dépendances (Room, Jetpack Compose, CameraX) seront déclarées ici
}
