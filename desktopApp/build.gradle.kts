plugins {
    kotlin("jvm")
    application
}

group = "com.ak.keycepass"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    // Les autres dépendances (Ktor Server, Database centralisée, Compose pour Desktop) seront déclarées ici
}

application {
    mainClass.set("com.ak.keycepass.desktop.MainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}
